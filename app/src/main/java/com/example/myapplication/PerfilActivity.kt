package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.adapter.ProductoAdapter
import com.example.myapplication.model.Producto
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var textViewNombre: TextView
    private lateinit var textViewPromedio: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var imageViewFotoPerfil: ImageView

    private val productoList = mutableListOf<Producto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        textViewNombre = findViewById(R.id.textViewNombre)
        textViewPromedio = findViewById(R.id.textViewPromedio)
        recyclerView = findViewById(R.id.recyclerView)
        imageViewFotoPerfil = findViewById(R.id.imageView3)
        val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        setupRecyclerView()
        setupPopupMenu(btnMenu)
        setupBottomNavigation(bottomNav)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            mostrarNombreUsuario(userId)
            mostrarPromedioCalificaciones(userId)
        } else {
            textViewNombre.text = "Usuario no autenticado"
            Toast.makeText(this, "Debe iniciar sesión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        productoAdapter = ProductoAdapter(productoList) { producto ->
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra("producto", producto)
            startActivity(intent)
        }
        recyclerView.adapter = productoAdapter
    }

    private fun setupPopupMenu(btnMenu: ImageButton) {
        btnMenu.setOnClickListener {
            val popup = PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.perfil_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_mis_datos -> {
                        startActivity(Intent(this, MisDatosActivity::class.java))
                        true
                    }
                    R.id.menu_cerrar_sesion -> {
                        cerrarSesion()
                        true
                    }
                    R.id.menu_eliminar_cuenta -> {
                        eliminarCuenta()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupBottomNavigation(bottomNav: BottomNavigationView) {
        bottomNav.selectedItemId = R.id.nav_perfil

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, HomeActivity::class.java))
                R.id.like -> startActivity(Intent(this, LikeActivity::class.java))
                R.id.añadir -> startActivity(Intent(this, AgregarActivity::class.java))
                R.id.buzon -> startActivity(Intent(this, BuzonActivity::class.java))
                R.id.nav_perfil -> return@setOnItemSelectedListener true
            }
            true
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun eliminarCuenta() {
        auth.currentUser?.delete()?.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Error al eliminar cuenta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarNombreUsuario(userId: String) {
        val userRef = database.child("Usuarios").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java)
                val fotoUrl = snapshot.child("fotoPerfil").getValue(String::class.java)

                textViewNombre.text = nombre ?: "Nombre no disponible"

                if (!fotoUrl.isNullOrEmpty()) {
                    Glide.with(this@PerfilActivity)
                        .load(fotoUrl)
                        .into(imageViewFotoPerfil)
                }

                cargarProductosDelUsuario(userId, textViewNombre.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                textViewNombre.text = "Error al cargar nombre"
                Toast.makeText(this@PerfilActivity, "Error: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun cargarProductosDelUsuario(userId: String, nombreUsuario: String) {
        val productosRef = database.child("productos").child(userId)
        productoList.clear()

        productosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (productoSnap in snapshot.children) {
                    val producto = productoSnap.getValue(Producto::class.java)
                    producto?.apply {
                        id = productoSnap.key ?: ""
                        usuario = nombreUsuario
                        productoList.add(this)
                    }
                }

                productoAdapter.notifyDataSetChanged()

                if (productoList.isEmpty()) {
                    Toast.makeText(
                        this@PerfilActivity,
                        "Aún no has publicado productos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PerfilActivity, "Error: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun mostrarPromedioCalificaciones(userId: String) {
        val calificacionesRef = database.child("calificaciones").child(userId)

        calificacionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var suma = 0f
                var contador = 0

                for (calificacionSnap in snapshot.children) {
                    val calificacion = calificacionSnap.child("calificacion").getValue(Float::class.java)
                    if (calificacion != null) {
                        suma += calificacion
                        contador++
                    }
                }

                val promedio = if (contador > 0) suma / contador else 0f
                textViewPromedio.text = "Promedio: %.1f".format(promedio)

                // *** NUEVO: Guardamos el promedio en el perfil público del usuario ***
                val userRef = database.child("Usuarios").child(userId)
                userRef.child("promedioCalificacion").setValue(promedio)
            }

            override fun onCancelled(error: DatabaseError) {
                textViewPromedio.text = "Promedio: Error"
            }
        })
    }
}
