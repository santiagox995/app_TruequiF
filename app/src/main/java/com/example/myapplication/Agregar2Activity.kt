package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class Agregar2Activity : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etMarca: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var btnPublicar: Button
    private lateinit var imageViews: List<ImageView>
    private val imageUris = mutableListOf<Uri?>()
    private lateinit var progreso_publicacion: View


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar2)
        progreso_publicacion = findViewById(R.id.frameProgress)
        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etMarca = findViewById(R.id.etmarca)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerEstado = findViewById(R.id.spinnerEstado)
        btnPublicar = findViewById(R.id.btnPublicar)


        imageViews = listOf(
            findViewById(R.id.img1),
            findViewById(R.id.img2),
            findViewById(R.id.img3),
            findViewById(R.id.img4)
        )

        // Configurar spinners
        spinnerCategoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Ropa", "Accesorios", "Muebles", "Calzado", "Entretenimiento", "Otros")
        )

        spinnerEstado.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Nuevo", "Como nuevo", "Buen estado", "Desgastado")
        )

        // Listener para seleccionar imágenes
        imageViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), index)
            }
        }

        btnPublicar.setOnClickListener {

            if (validarCampos()) {
                progreso_publicacion.visibility = View.VISIBLE
                subirProducto()
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUris.add(requestCode, data.data!!)
            imageViews[requestCode].setImageURI(data.data)
        }
    }

    private fun validarCampos(): Boolean {
        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val marca = etMarca.text.toString().trim()

        if (titulo.isEmpty() || descripcion.isEmpty() || marca.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        if (imageUris.size < 4 || imageUris.any { it == null }) {
            Toast.makeText(this, "Debes seleccionar 4 imágenes", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun subirProducto() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el nombre del usuario desde la base de datos
        val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(userId)
        usuariosRef.get().addOnSuccessListener { snapshot ->
            val nombreUsuario = snapshot.child("nombre").value.toString()

            val databaseRef = FirebaseDatabase.getInstance().getReference("productos").child(userId)
            val productoId = databaseRef.push().key!!

            val storageRef = FirebaseStorage.getInstance().getReference("productos/$userId/$productoId")

            val urls = mutableListOf<String>()
            var subidas = 0

            imageUris.forEachIndexed { index, uri ->
                val ref = storageRef.child("imagen$index.jpg")
                ref.putFile(uri!!).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        urls.add(downloadUrl.toString())
                        subidas++

                        if (subidas == 4) {
                            val datos = mapOf(
                                "titulo" to etTitulo.text.toString().trim(),
                                "descripcion" to etDescripcion.text.toString().trim(),
                                "marca" to etMarca.text.toString().trim(),
                                "categoria" to spinnerCategoria.selectedItem.toString(),
                                "estado" to spinnerEstado.selectedItem.toString(),
                                "imagenes" to urls,
                                "usuario" to nombreUsuario,
                                "creadorUID" to userId
                            )


                            databaseRef.child(productoId).setValue(datos).addOnSuccessListener {
                                Toast.makeText(this, "Producto publicado", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, HomeActivity::class.java)
                                intent.putExtra("forzar_recarga", true)
                                startActivity(intent)
                                finish()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Error al guardar producto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "No se pudo obtener el nombre del usuario", Toast.LENGTH_SHORT).show()
        }
    }
}




