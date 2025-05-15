package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.adapter.MensajeAdapter
import com.example.myapplication.model.Mensaje
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MensajeAdapter
    private val mensajes = mutableListOf<Mensaje>()

    private lateinit var receptorId: String
    private lateinit var chatId: String
    private lateinit var chatRef: DatabaseReference
    private lateinit var buzonesRef: DatabaseReference
    private lateinit var miId: String
    private var productoId: String = ""

    private lateinit var fotoPerfil: ImageView
    private lateinit var nombreUsuario: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        miId = currentUser.uid

        // Obtener datos del intent
        receptorId = intent.getStringExtra("receptorId") ?: ""
        productoId = intent.getStringExtra("productoId") ?: ""
        val nombreReceptor = intent.getStringExtra("nombreUsuario") ?: "Usuario"

        if (receptorId.isEmpty()) {
            Toast.makeText(this, "Receptor inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar las referencias a la base de datos
        chatId = generarChatId(miId, receptorId)
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("mensajes")
        buzonesRef = FirebaseDatabase.getInstance().getReference("buzones")

        // Referencias de los elementos de la Toolbar
        fotoPerfil = findViewById(R.id.iv_perfil_receptor)
        nombreUsuario = findViewById(R.id.tv_nombre_receptor)

        // Cargar los datos del receptor (nombre e imagen de perfil)
        cargarDatosReceptor()

        // Configuración del RecyclerView para los mensajes
        recyclerView = findViewById(R.id.rv_mensajes)
        adapter = MensajeAdapter(mensajes, miId)
        recyclerView.adapter = adapter

        // Recibir mensajes en tiempo real
        chatRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val nuevoMensaje = snapshot.getValue(Mensaje::class.java)
                if (nuevoMensaje != null) {
                    mensajes.add(nuevoMensaje)
                    adapter.notifyItemInserted(mensajes.size - 1)
                    recyclerView.smoothScrollToPosition(mensajes.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        })

        // Botón para enviar mensajes
        val btnEnviar = findViewById<ImageButton>(R.id.btn_enviar)
        val etMensaje = findViewById<EditText>(R.id.et_mensaje)

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                val mensaje = Mensaje(texto, miId, System.currentTimeMillis())
                chatRef.push().setValue(mensaje).addOnSuccessListener {
                    actualizarBuzon(mensaje)
                    etMensaje.text.clear()
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            }
        }

        // Menú inferior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.buzon // Para reflejar que estás en la sección de mensajes

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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = nombreReceptor
    }

    private fun cargarDatosReceptor() {
        val usuarioRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(receptorId)

        // Cargar el nombre y la foto de perfil
        usuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                val fotoUrl = snapshot.child("fotoPerfil").getValue(String::class.java)

                nombreUsuario.text = nombre

                // Cargar la imagen del perfil usando Glide
                if (!fotoUrl.isNullOrEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(fotoUrl)
                        .into(fotoPerfil)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Error al cargar los datos del receptor: ${error.message}")
            }
        })
    }

    private fun generarChatId(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }

    private fun actualizarBuzon(mensaje: Mensaje) {
        if (productoId.isEmpty()) {
            Log.e("ChatDebug", "No se proporcionó el ID del producto")
            return
        }

        val productoRef = FirebaseDatabase.getInstance().getReference("productos")
            .child(receptorId).child(productoId)

        val usuarioRef = FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(miId) // 🟢 Obtener tu propio nombre de usuario, no el del receptor

        productoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(productoSnapshot: DataSnapshot) {
                val titulo = productoSnapshot.child("titulo").getValue(String::class.java) ?: "Producto"
                usuarioRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usuarioSnapshot: DataSnapshot) {
                        val nombreUsuario = usuarioSnapshot.child("nombre").getValue(String::class.java) ?: "Usuario"

                        val buzonData = mapOf(
                            "chatID" to chatId,
                            "titulo" to titulo,
                            "usuario" to nombreUsuario,
                            "productoID" to productoId,
                            "fecha" to mensaje.timestamp
                        )

                        buzonesRef.child(miId).child(chatId).setValue(buzonData)
                        buzonesRef.child(receptorId).child(chatId).setValue(buzonData)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ChatDebug", "Error al obtener nombre de usuario: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatDebug", "Error al obtener producto: ${error.message}")
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}