package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.adapter.ProductoAdapter
import com.example.myapplication.model.Producto
import com.google.firebase.database.*
import android.widget.ImageView
import android.widget.Button
import android.content.Intent
import android.util.Log
import android.widget.RatingBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class PerfilPublicoActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var textViewNombre: TextView
    private lateinit var textViewPromedio: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var imageViewFotoPerfil: ImageView
    private lateinit var btnCalificar: Button

    private val productoList = mutableListOf<Producto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_publico)

        textViewNombre = findViewById(R.id.textViewNombrePublico)
        textViewPromedio = findViewById(R.id.textViewPromedioPublico)
        recyclerView = findViewById(R.id.recyclerViewPublico)
        imageViewFotoPerfil = findViewById(R.id.imageViewFotoPerfilPublico)
        btnCalificar = findViewById(R.id.btnCalificar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val uidPublico = intent.getStringExtra("usuarioId")
        Log.d("PerfilPublico", "UID recibido: $uidPublico")

        if (uidPublico.isNullOrEmpty()) {
            Toast.makeText(this, "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        mostrarPerfilPublico(uidPublico)
        setupBottomNavigation(bottomNav)

        btnCalificar.setOnClickListener {
            mostrarDialogoCalificacion(uidPublico)
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

    private fun mostrarPerfilPublico(uid: String) {
        val userRef = database.child("Usuarios").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java)
                val fotoUrl = snapshot.child("fotoPerfil").getValue(String::class.java)

                textViewNombre.text = nombre ?: "Nombre no disponible"

                if (!fotoUrl.isNullOrEmpty()) {
                    Glide.with(this@PerfilPublicoActivity)
                        .load(fotoUrl)
                        .circleCrop()
                        .into(imageViewFotoPerfil)
                }

                cargarProductosDelUsuario(uid, nombre ?: "")
                mostrarPromedioCalificaciones(uid)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PerfilPublicoActivity, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@PerfilPublicoActivity, "Este usuario aún no ha publicado productos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PerfilPublicoActivity, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoCalificacion(uid: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_user, null)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val btnEnviarCalificacion = dialogView.findViewById<Button>(R.id.btnEnviarCalificacion)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnEnviarCalificacion.setOnClickListener {
            val calificacion = ratingBar.rating
            guardarCalificacion(uid, calificacion)
            Toast.makeText(this, "Calificación enviada: $calificacion estrellas", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun guardarCalificacion(uid: String, calificacion: Float) {
        val calificacionRef = database.child("calificaciones").child(uid).push()

        val calificacionMap = mapOf(
            "calificacion" to calificacion,
            "calificadoPor" to obtenerUidActual()
        )

        calificacionRef.setValue(calificacionMap)
    }

    private fun obtenerUidActual(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "desconocido"
    }

    private fun mostrarPromedioCalificaciones(uid: String) {
        val calificacionesRef = database.child("calificaciones").child(uid)

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
            }

            override fun onCancelled(error: DatabaseError) {
                textViewPromedio.text = "Promedio: Error"
            }
        })
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
}
