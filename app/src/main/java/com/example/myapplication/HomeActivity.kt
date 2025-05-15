package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.adapter.ProductoAdapter
import com.example.myapplication.model.Producto
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private val productoList = mutableListOf<Producto>()
    private val productoListFiltered = mutableListOf<Producto>()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onResume() {
        super.onResume()

        val forzarRecarga = intent.getBooleanExtra("forzar_recarga", false)
        if (forzarRecarga) {
            intent.removeExtra("forzar_recarga") // para que no se repita
            swipeRefreshLayout.isRefreshing = true
            lifecycleScope.launch {
                delay(1000)
                recargarPagina()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.isRefreshing = true

        swipeRefreshLayout.setOnRefreshListener {
            recargarPagina()
        }

        // Recibir el filtro de categoría (por si vienen desde otro Activity)
        val categoriaFiltrada = intent.getStringExtra("categoria_filtrada")

        // Botón de filtro (opcional)
        val btnFiltro = findViewById<ImageButton>(R.id.btnOpcional)
        btnFiltro.setOnClickListener {
            startActivity(Intent(this, FiltroActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.like -> {
                    startActivity(Intent(this, LikeActivity::class.java))
                    true
                }
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

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        productoAdapter = ProductoAdapter(productoListFiltered) { producto ->
            val intent = Intent(this, DetalleProductoActivity::class.java)
            intent.putExtra("producto", producto)
            startActivity(intent)
        }

        recyclerView.adapter = productoAdapter

        cargarProductosDesdeFirebase(categoriaFiltrada)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                filtrarProductos(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 🔥 NUEVO: configurar botones de categoría
        setupBotonesCategoria()
    }

    private fun cargarProductosDesdeFirebase(categoriaFiltrada: String?) {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios")
        val productosRef = FirebaseDatabase.getInstance().getReference("productos")

        productoList.clear()

        usuariosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usuariosSnapshot: DataSnapshot) {
                val mapaUsuarios = mutableMapOf<String, String>()

                for (usuarioSnap in usuariosSnapshot.children) {
                    val uid = usuarioSnap.key ?: continue
                    val nombre = usuarioSnap.child("nombre").getValue(String::class.java) ?: ""
                    mapaUsuarios[uid] = nombre
                }

                productosRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(productosSnapshot: DataSnapshot) {
                        for (usuarioSnap in productosSnapshot.children) {
                            val uidUsuario = usuarioSnap.key ?: continue
                            val nombreUsuario = mapaUsuarios[uidUsuario] ?: "Desconocido"

                            for (productoSnap in usuarioSnap.children) {
                                try {
                                    val producto = productoSnap.getValue(Producto::class.java)
                                    producto?.let {
                                        it.id = productoSnap.key ?: ""
                                        it.usuario = nombreUsuario

                                        if (categoriaFiltrada == null || it.categoria == categoriaFiltrada) {
                                            productoList.add(it)
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Firebase", "Error al convertir producto: ${e.message}")
                                }
                            }
                        }
                        productoListFiltered.clear()
                        productoListFiltered.addAll(productoList)
                        productoAdapter.notifyDataSetChanged()
                        swipeRefreshLayout.isRefreshing = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e("Firebase", "Error al leer productos: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("Firebase", "Error al leer usuarios: ${error.message}")
            }
        })
    }

    private fun filtrarProductos(query: String) {
        if (query.isEmpty()) {
            productoListFiltered.clear()
            productoListFiltered.addAll(productoList)
        } else {
            productoListFiltered.clear()
            productoList.filterTo(productoListFiltered) { producto ->
                producto.titulo?.lowercase()?.contains(query) == true || producto.categoria?.lowercase()?.contains(query) == true
            }
        }
        productoAdapter.notifyDataSetChanged()
    }

    private fun recargarPagina() {
        recreate()
    }

    // 🔥 NUEVO: para manejar los botones de categorías
    private fun setupBotonesCategoria() {
        val btnMuebles = findViewById<Button>(R.id.btnMuebles)
        val btnAccesorios = findViewById<Button>(R.id.btnAccesorios)
        val btnRopa = findViewById<Button>(R.id.btnRopa)

        btnMuebles.setOnClickListener {
            filtrarPorCategoria("Muebles")
        }

        btnAccesorios.setOnClickListener {
            filtrarPorCategoria("Accesorios")
        }

        btnRopa.setOnClickListener {
            filtrarPorCategoria("Ropa")
        }
    }

    // 🔥 NUEVO: filtra directamente la lista por categoría
    private fun filtrarPorCategoria(categoria: String) {
        productoListFiltered.clear()
        productoList.filterTo(productoListFiltered) { producto ->
            producto.categoria?.equals(categoria, ignoreCase = true) == true
        }
        productoAdapter.notifyDataSetChanged()
    }
}



