package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.ProductoAdapter
import com.example.myapplication.model.Producto
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LikeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private val listaLikes = mutableListOf<Producto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        // Referencia al RecyclerView
        recyclerView = findViewById(R.id.recyclerLikes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Adaptador con acción al hacer clic
        productoAdapter = ProductoAdapter(listaLikes) { producto ->
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra("producto", producto)
            startActivity(intent)
        }
        recyclerView.adapter = productoAdapter

        // Cargar productos con like desde Firebase
        cargarLikes()

        // Navegación inferior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.like

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.like -> true
                R.id.añadir -> {
                    startActivity(Intent(this, AgregarActivity::class.java))
                    true
                }
                R.id.buzon -> {
                    startActivity(Intent(this, BuzonActivity::class.java))
                    true
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun cargarLikes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val likesRef = FirebaseDatabase.getInstance().getReference("likes").child(uid)

        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaLikes.clear()
                for (likeSnap in snapshot.children) {
                    val producto = likeSnap.getValue(Producto::class.java)
                    producto?.let { listaLikes.add(it) }
                }
                productoAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

