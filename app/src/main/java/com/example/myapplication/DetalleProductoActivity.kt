package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.adapter.FotoAdapter
import com.example.myapplication.model.Producto
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var viewPagerFotos: ViewPager2
    private lateinit var tvUsuario: TextView
    private lateinit var tvTitulo: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnTruequi: Button
    private lateinit var btnOferta: Button
    private lateinit var  btnperfil: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto)

        try {
            viewPagerFotos = findViewById(R.id.viewPagerFotos)
            tvUsuario = findViewById(R.id.tvUsuario)
            tvTitulo = findViewById(R.id.tvTitulo)
            tvDescripcion = findViewById(R.id.tvDescripcion)
            tvEstado = findViewById(R.id.tvEstado)
            tvCategoria = findViewById(R.id.tvCategoria)
            btnEditar = findViewById(R.id.btnEditar)
            btnEliminar = findViewById(R.id.btnEliminar)
            btnTruequi = findViewById(R.id.btnTruequi)
            btnOferta = findViewById(R.id.btnChat)
            btnperfil = findViewById(R.id.btnperfil)// 👈 Importante
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar vistas: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("DetalleProducto", "Error en findViewById", e)
            finish()
            return
        }

        val producto = intent.getSerializableExtra("producto") as? Producto

        if (producto == null) {
            Toast.makeText(this, "No se pudo cargar el producto", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val tvTruequiStatus = findViewById<TextView>(R.id.tvTruequiStatus)

        if (producto.truequiHecho) {
            tvTruequiStatus.text = "✅ Truequi hecho"
            tvTruequiStatus.visibility = TextView.VISIBLE
        } else {
            tvTruequiStatus.visibility = TextView.GONE
        }

        tvUsuario.text = producto.usuario.ifEmpty { "Usuario desconocido" }


        tvTitulo.text = producto.titulo.ifEmpty { "Sin título" }
        tvDescripcion.text = producto.descripcion.ifEmpty { "Sin descripción" }
        tvEstado.text = producto.estado.ifEmpty { "No especificado" }
        tvCategoria.text = producto.categoria.ifEmpty { "No especificada" }

        if (producto.imagenes.isNotEmpty()) {
            viewPagerFotos.adapter = FotoAdapter(producto.imagenes)
        } else {
            Toast.makeText(this, "Este producto no tiene imágenes.", Toast.LENGTH_SHORT).show()
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (producto.creadorUID == userId) {
            // Usuario actual es el dueño del producto
            btnEditar.visibility = Button.VISIBLE
            btnEliminar.visibility = Button.VISIBLE
            btnTruequi.visibility = Button.VISIBLE
            btnOferta.visibility = Button.GONE
        } else {
            // Usuario es un visitante
            btnEditar.visibility = Button.GONE
            btnEliminar.visibility = Button.GONE
            btnTruequi.visibility = Button.GONE
            btnOferta.visibility = Button.VISIBLE
        }
        btnperfil.setOnClickListener{
            val intent = Intent(this, PerfilPublicoActivity::class.java)
            intent.putExtra("usuarioId", producto.creadorUID)
            startActivity(intent)

        }

        btnEliminar.setOnClickListener {
            val ref = FirebaseDatabase.getInstance()
                .getReference("productos")
                .child(producto.creadorUID ?: "")
                .child(producto.id)

            ref.removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("forzar_recarga", true)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
        }



        btnEditar.setOnClickListener {
            val intent = Intent(this, EditarProductoActivity::class.java)
            intent.putExtra("producto", producto)
            startActivity(intent)
        }

        btnTruequi.setOnClickListener {
            val ref = FirebaseDatabase.getInstance()
                .getReference("productos")
                .child(producto.creadorUID)
                .child(producto.id)

            ref.child("truequiHecho").setValue(true).addOnSuccessListener {
                Toast.makeText(this, "Truequi hecho", Toast.LENGTH_SHORT).show()
                tvTruequiStatus.text = "✅ Truequi hecho"
                tvTruequiStatus.visibility = TextView.VISIBLE
                btnTruequi.visibility = Button.GONE
            }.addOnFailureListener {
                Toast.makeText(this, "Error al marcar como truequi", Toast.LENGTH_SHORT).show()
            }
        }
        btnOferta.setOnClickListener {
            val receptorId = producto.creadorUID
            val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid

            if (receptorId != null && currentUserUID != null) {
                val chatID = "${currentUserUID}_to_${receptorId}"

                val chatDetails = mapOf(
                    "chatID" to chatID,
                    "usuario" to producto.usuario,
                    "titulo" to producto.titulo,
                    "productoID" to producto.id,
                    "fecha" to System.currentTimeMillis()
                )

                val refBuzonUsuario = FirebaseDatabase.getInstance()
                    .getReference("buzones")
                    .child(currentUserUID)
                    .child(chatID)

                refBuzonUsuario.setValue(chatDetails).addOnSuccessListener {
                    val refBuzonReceptor = FirebaseDatabase.getInstance()
                        .getReference("buzones")
                        .child(receptorId)
                        .child(chatID)

                    refBuzonReceptor.setValue(chatDetails).addOnSuccessListener {
                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("receptorId", receptorId)
                        intent.putExtra("chatID", chatID)
                        startActivity(intent)
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al agregar el chat al buzón del receptor", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al agregar el chat al buzón del usuario", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se pudo cargar el ID del receptor o el usuario actual", Toast.LENGTH_SHORT).show()
            }
        }





        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, HomeActivity::class.java))
                R.id.like -> startActivity(Intent(this, LikeActivity::class.java))
                R.id.añadir -> startActivity(Intent(this, AgregarActivity::class.java))
                R.id.buzon -> startActivity(Intent(this, BuzonActivity::class.java))
                R.id.nav_perfil -> startActivity(Intent(this, PerfilActivity::class.java))
            }
            true
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

