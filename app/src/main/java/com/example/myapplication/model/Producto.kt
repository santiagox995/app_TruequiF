package com.example.myapplication.model

import java.io.Serializable

data class Producto(
    var id: String = "",
    var titulo: String = "",
    var descripcion: String = "",
    var estado: String = "",
    var categoria: String = "",
    var marca: String = "",
    var imagenes: List<String> = listOf(),
    var usuario: String = "",
    var creadorUID: String = "",
    var truequiHecho: Boolean = false // Nuevo campo para "truequi"
) : Serializable
