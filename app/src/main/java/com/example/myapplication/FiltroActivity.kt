package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class
FiltroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filtro)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Menú inferior
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

        // Función para abrir Home con categoría seleccionada
        fun irAHomeConCategoria(categoria: String) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("categoria_filtrada", categoria)
            startActivity(intent)
        }

        // Asignación de listeners a los botones de categorías
        findViewById<Button>(R.id.button7).setOnClickListener {
            irAHomeConCategoria("Ropa")
        }
        findViewById<Button>(R.id.button8).setOnClickListener {
            irAHomeConCategoria("Accesorios")
        }
        findViewById<Button>(R.id.button9).setOnClickListener {
            irAHomeConCategoria("Muebles")
        }
        findViewById<Button>(R.id.button10).setOnClickListener {
            irAHomeConCategoria("Calzado")
        }
        findViewById<Button>(R.id.button11).setOnClickListener {
            irAHomeConCategoria("Entretenimiento")
        }
        findViewById<Button>(R.id.button12).setOnClickListener {
            irAHomeConCategoria("Servicios")
        }
    }
}
