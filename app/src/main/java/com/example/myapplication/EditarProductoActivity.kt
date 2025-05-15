package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.model.Producto
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditarProductoActivity<StorageReference> : AppCompatActivity() {

    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etEstado: EditText
    private lateinit var etCategoria: EditText
    private lateinit var btnGuardarCambios: Button

    private lateinit var img1: ImageView
    private lateinit var img2: ImageView
    private lateinit var img3: ImageView
    private lateinit var img4: ImageView

    private lateinit var frameProgress: android.widget.RelativeLayout

    private var imagenes: MutableList<String> = mutableListOf()
    private var urisSeleccionadas: MutableMap<Int, Uri> = mutableMapOf()

    private var productoId: String = ""
    private var creadorUID: String = ""

    private var imagenSeleccionada = -1


    private val galeriaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && imagenSeleccionada != -1) {
                when (imagenSeleccionada) {
                    0 -> img1.setImageURI(uri)
                    1 -> img2.setImageURI(uri)
                    2 -> img3.setImageURI(uri)
                    3 -> img4.setImageURI(uri)
                }
                urisSeleccionadas[imagenSeleccionada] = uri
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_producto)

        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etEstado = findViewById(R.id.etEstado)
        etCategoria = findViewById(R.id.etCategoria)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)

        img1 = findViewById(R.id.imgEditar1)
        img2 = findViewById(R.id.imgEditar2)
        img3 = findViewById(R.id.imgEditar3)
        img4 = findViewById(R.id.imgEditar4)

        frameProgress = findViewById(R.id.frameProgress)

        val producto = intent.getSerializableExtra("producto") as? Producto

        if (producto != null) {
            etTitulo.setText(producto.titulo)
            etDescripcion.setText(producto.descripcion)
            etEstado.setText(producto.estado)
            etCategoria.setText(producto.categoria)

            imagenes = producto.imagenes.toMutableList()
            productoId = producto.id
            creadorUID = producto.creadorUID

            val imageViews = listOf(img1, img2, img3, img4)

            // Mostrar imágenes
            for (i in imagenes.indices) {
                if (i < imageViews.size) {
                    Glide.with(this).load(imagenes[i]).into(imageViews[i])
                }
            }

            // Escoger imagen desde galería
            imageViews.forEachIndexed { index, imageView ->
                imageView.setOnClickListener {
                    imagenSeleccionada = index
                    galeriaLauncher.launch("image/*")
                }
            }

            btnGuardarCambios.setOnClickListener {
                val nuevoTitulo = etTitulo.text.toString().trim()
                val nuevaDescripcion = etDescripcion.text.toString().trim()
                val nuevoEstado = etEstado.text.toString().trim()
                val nuevaCategoria = etCategoria.text.toString().trim()

                if (nuevoTitulo.isEmpty() || nuevaDescripcion.isEmpty() || nuevoEstado.isEmpty() || nuevaCategoria.isEmpty()) {
                    Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                frameProgress.visibility = android.view.View.VISIBLE

                if (urisSeleccionadas.isEmpty()) {
                    // Si no se cambió ninguna imagen, solo actualizar texto
                    actualizarDatosFirebase(nuevoTitulo, nuevaDescripcion, nuevoEstado, nuevaCategoria, imagenes)
                } else {
                    // Subir imágenes cambiadas
                    val storage = FirebaseStorage.getInstance()
                    val nuevasImagenes = imagenes.toMutableList()
                    var contadorSubidas = 0

                    for ((index, uri) in urisSeleccionadas) {
                        val nombreImagen = "productos/$creadorUID/$productoId/img$index.jpg"
                        val ref: com.google.firebase.storage.StorageReference = storage.reference.child(nombreImagen)

                        ref.putFile(uri)
                            .continueWithTask { task ->
                                if (!task.isSuccessful) {
                                    throw task.exception ?: Exception("Error al subir imagen")
                                }
                                ref.downloadUrl
                            }
                            .addOnSuccessListener { downloadUri ->
                                if (index < nuevasImagenes.size) {
                                    nuevasImagenes[index] = downloadUri.toString()
                                } else {
                                    nuevasImagenes.add(downloadUri.toString())
                                }

                                contadorSubidas++
                                if (contadorSubidas == urisSeleccionadas.size) {
                                    // Una vez subidas todas las imágenes, actualizamos el producto
                                    actualizarDatosFirebase(nuevoTitulo, nuevaDescripcion, nuevoEstado, nuevaCategoria, nuevasImagenes)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                                frameProgress.visibility = android.view.View.GONE
                            }
                    }
                }
            }

        } else {
            Toast.makeText(this, "No se pudo cargar el producto", Toast.LENGTH_LONG).show()
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    private fun actualizarDatosFirebase(
        titulo: String,
        descripcion: String,
        estado: String,
        categoria: String,
        nuevasImagenes: List<String>
    ) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("productos")
            .child(creadorUID)
            .child(productoId)

        val cambios = mapOf(
            "titulo" to titulo,
            "descripcion" to descripcion,
            "estado" to estado,
            "categoria" to categoria,
            "imagenes" to nuevasImagenes
        )

        ref.updateChildren(cambios)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("forzar_recarga", true)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar producto", Toast.LENGTH_SHORT).show()
                frameProgress.visibility = android.view.View.GONE
            }
    }

}
