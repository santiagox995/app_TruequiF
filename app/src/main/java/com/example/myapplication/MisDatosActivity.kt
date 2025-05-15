package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class MisDatosActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextNombre: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmarPassword: TextInputEditText
    private lateinit var buttonConfirmar: Button

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageView.setImageURI(uri)
            uploadProfileImageToFirebase(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mis_datos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.imageView3)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextNombre = findViewById(R.id.editTextUsuario)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmarPassword = findViewById(R.id.editTextConfirmarPassword)
        buttonConfirmar = findViewById(R.id.button2)

        imageView.setOnClickListener {
            pickImage.launch("image/*")
        }

        buttonConfirmar.setOnClickListener {
            actualizarDatos()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.buzon

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, HomeActivity::class.java))
                R.id.like -> startActivity(Intent(this, LikeActivity::class.java))
                R.id.añadir -> startActivity(Intent(this, AgregarActivity::class.java))
                R.id.buzon -> startActivity(Intent(this, BuzonActivity::class.java))
                R.id.nav_perfil -> startActivity(Intent(this, PerfilActivity::class.java))
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)

        dbRef.get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.child("nombre").value?.toString() ?: ""
            val fotoUrl = snapshot.child("fotoPerfil").value?.toString() ?: ""

            editTextNombre.setText(nombre)
            editTextEmail.setText(user.email)

            if (fotoUrl.isNotEmpty()) {
                Glide.with(this).load(fotoUrl).into(imageView)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfileImageToFirebase(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
                        .child("fotoPerfil").setValue(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarDatos() {
        val email = editTextEmail.text.toString().trim()
        val nombre = editTextNombre.text.toString().trim()
        val password = editTextPassword.text.toString()
        val confirmarPassword = editTextConfirmarPassword.text.toString()

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return

        if (email.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(this, "Email y nombre son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isNotEmpty() && password != confirmarPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        user.updateEmail(email).addOnCompleteListener { emailTask ->
            if (emailTask.isSuccessful) {
                FirebaseDatabase.getInstance().getReference("Usuarios")
                    .child(uid).child("nombre").setValue(nombre)

                actualizarNombreUsuarioEnProductos(uid, nombre)

                if (password.isNotEmpty()) {
                    user.updatePassword(password).addOnCompleteListener { passTask ->
                        if (passTask.isSuccessful) {
                            Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al actualizar email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarNombreUsuarioEnProductos(uid: String, nombre: String) {
        val productosRef = FirebaseDatabase.getInstance().getReference("productos")

        productosRef.orderByChild("uidUsuario").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                for (productoSnapshot in snapshot.children) {
                    productoSnapshot.ref.child("usuario").setValue(nombre)
                }
            }

    }
}
