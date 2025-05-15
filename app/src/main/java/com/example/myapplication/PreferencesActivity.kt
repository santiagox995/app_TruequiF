package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PreferencesActivity : AppCompatActivity() {

    private lateinit var checkAccesorios: CheckBox
    private lateinit var checkRopa: CheckBox
    private lateinit var checkCalzado: CheckBox
    private lateinit var checkEntretenimiento: CheckBox
    private lateinit var checkServicios: CheckBox
    private lateinit var checkMuebles: CheckBox
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        // Inicializar todos los checkboxes
        checkAccesorios = findViewById(R.id.check_accesorios)
        checkRopa = findViewById(R.id.check_ropa)
        checkCalzado = findViewById(R.id.check_calzado)
        checkEntretenimiento = findViewById(R.id.check_entretenimiento)
        checkServicios = findViewById(R.id.check_servicios)
        checkMuebles = findViewById(R.id.check_muebles)
        btnGuardar = findViewById(R.id.btn_guardar_preferencias)

        btnGuardar.setOnClickListener {
            val preferencias = mutableListOf<String>()

            if (checkAccesorios.isChecked) preferencias.add("Accesorios")
            if (checkRopa.isChecked) preferencias.add("Ropa")
            if (checkCalzado.isChecked) preferencias.add("Calzado")
            if (checkEntretenimiento.isChecked) preferencias.add("Entretenimiento")
            if (checkServicios.isChecked) preferencias.add("Servicios")
            if (checkMuebles.isChecked) preferencias.add("Muebles")

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this, "Usuario no encontrado. Intenta iniciar sesión.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val userId = user.uid
            val dbRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(userId)
            dbRef.child("preferencias").setValue(preferencias)
                .addOnSuccessListener {
                    Toast.makeText(this, "Preferencias guardadas", Toast.LENGTH_SHORT).show()

                    // Ir a LoginActivity después de guardar
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
