package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.BuzonItem

class BuzonAdapter(
    private val items: List<BuzonItem>,
    private val onClick: (BuzonItem) -> Unit,
    private val onEliminarClick: (BuzonItem) -> Unit // NUEVO parámetro agregado
) : RecyclerView.Adapter<BuzonAdapter.BuzonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuzonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_buzon, parent, false)
        return BuzonViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuzonViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class BuzonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.tvTitulo)
        private val mensajeTextView: TextView = view.findViewById(R.id.tvMensaje)
        private val userNameTextView: TextView = view.findViewById(R.id.tvNombreUsuario)
        private val eliminarButton: Button = view.findViewById(R.id.btnEliminar) // NUEVO botón

        init {
            view.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = items[adapterPosition]
                    onClick(item)
                }
            }


            eliminarButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = items[adapterPosition]
                    onEliminarClick(item) // NUEVA acción de eliminar
                }
            }
        }

        fun bind(item: BuzonItem) {
            titleTextView.text = item.titulo.ifEmpty { "Sin título" }
            userNameTextView.text = item.nombre.ifEmpty { "Anónimo" }
        }
    }
}
