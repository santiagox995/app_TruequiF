package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Mensaje

class MensajeAdapter(
    private val mensajes: MutableList<Mensaje>, // Cambiado a MutableList para poder actualizarla
    private val miId: String
) : RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder>() {

    // ViewHolder para contener la vista de cada mensaje
    inner class MensajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMensaje: TextView = itemView.findViewById(R.id.tv_mensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        // Determina si usar el layout alineado a la derecha o a la izquierda
        val layoutId = if (viewType == TIPO_MENSAJE_DERECHA) {
            R.layout.item_mensaje_derecha // Layout para el emisor (derecha)
        } else {
            R.layout.item_mensaje_izquierda // Layout para el receptor (izquierda)
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MensajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.tvMensaje.text = mensaje.mensaje
    }

    override fun getItemCount(): Int = mensajes.size

    override fun getItemViewType(position: Int): Int {
        val mensaje = mensajes[position]
        // Si el emisor es el usuario actual, muestra el mensaje a la derecha, de lo contrario a la izquierda
        return if (mensaje.emisorId == miId) TIPO_MENSAJE_DERECHA else TIPO_MENSAJE_IZQUIERDA
    }

    // Función para agregar un nuevo mensaje
    fun agregarMensaje(mensaje: Mensaje) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }

    companion object {
        private const val TIPO_MENSAJE_DERECHA = 1
        private const val TIPO_MENSAJE_IZQUIERDA = 0
    }
}
