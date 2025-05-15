package com.example.myapplication.model

import java.text.SimpleDateFormat
import java.util.*

data class Mensaje(
    var mensaje: String = "",
    var emisorId: String = "",
    var timestamp: Long = 0L
) {
    // Función para obtener la fecha en formato legible
    fun obtenerFecha(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
