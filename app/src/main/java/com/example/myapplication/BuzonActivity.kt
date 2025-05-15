package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.BuzonAdapter
import com.example.myapplication.model.BuzonItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BuzonActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BuzonAdapter
    private val buzones = mutableListOf<BuzonItem>()
    private lateinit var miId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buzon)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        miId = currentUser.uid

        setupBottomNavigation()
        setupRecyclerView()
        cargarBuzonesDesdeFirebase()

        // Botón TruequiBot (Chatbot)
        val truequibotButton: ImageView = findViewById(R.id.truequibot_button)
        truequibotButton.setOnClickListener {
            val intent = Intent(this, TruequiBotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.buzon

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.like -> {
                    startActivity(Intent(this, LikeActivity::class.java))
                    true
                }
                R.id.añadir -> {
                    startActivity(Intent(this, AgregarActivity::class.java))
                    true
                }
                R.id.buzon -> true
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_buzon)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BuzonAdapter(
            items = buzones,
            onClick = { item ->
                val receptorId = obtenerReceptorIdDesdeChatId(item.chatId)
                val nombreUsuario = item.nombre ?: "Usuario"

                if (receptorId.isNotEmpty()) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("receptorId", receptorId)
                    intent.putExtra("nombreUsuario", nombreUsuario)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No se puede abrir el chat: receptorId vacío", Toast.LENGTH_SHORT).show()
                }
            },
            onEliminarClick = { item ->
                AlertDialog.Builder(this, R.style.CustomAlertDialog)
                    .setTitle("¿Eliminar buzón?")
                    .setMessage("¿Estás seguro de que deseas eliminar este buzón? Esta acción no se puede deshacer.")
                    .setPositiveButton("Sí") { _, _ ->
                        val dbRef = FirebaseDatabase.getInstance().getReference("buzones").child(miId).child(item.chatId)
                        dbRef.removeValue().addOnSuccessListener {
                            buzones.remove(item)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, "Buzón eliminado", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }


        )

        recyclerView.adapter = adapter
    }

    private fun cargarBuzonesDesdeFirebase() {
        val buzonesRef = FirebaseDatabase.getInstance().getReference("buzones").child(miId)

        buzonesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                buzones.clear()
                for (chatSnap in snapshot.children) {
                    val chatId = chatSnap.key ?: continue
                    val categoria = chatSnap.child("categoria").getValue(String::class.java) ?: "Sin categoría"
                    val tituloProducto = chatSnap.child("titulo").getValue(String::class.java) ?: "Producto"
                    val nombreUsuario = chatSnap.child("usuario").getValue(String::class.java) ?: "Usuario"

                    val item = BuzonItem(
                        titulo = tituloProducto,
                        categoria = categoria,
                        nombre = nombreUsuario,
                        chatId = chatId,
                    )

                    if (!buzones.contains(item)) {
                        buzones.add(item)
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BuzonActivity, "Error al cargar buzones", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun obtenerReceptorIdDesdeChatId(chatId: String): String {
        return when {
            chatId.contains("_to_") -> {
                val partes = chatId.split("_to_")
                if (partes.size == 2) {
                    if (partes[0] == miId) partes[1] else partes[0]
                } else ""
            }
            chatId.contains("_") -> {
                val partes = chatId.split("_")
                if (partes.size == 2) {
                    if (partes[0] == miId) partes[1] else partes[0]
                } else ""
            }
            else -> ""
        }
    }
}