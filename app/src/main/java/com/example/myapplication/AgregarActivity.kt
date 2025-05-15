package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AgregarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar)

        val btncontinuar = findViewById<Button>(R.id.button)

        btncontinuar.setOnClickListener {
            startActivity(Intent(this, Agregar2Activity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.añadir

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.añadir -> {
                    // Ya estás en AgregarActivity, no hacemos nada
                    true
                }
                R.id.like -> {
                    val intent = Intent(this, LikeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.buzon -> {
                    val intent = Intent(this, BuzonActivity::class.java)
                    startActivity(intent)
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
}
