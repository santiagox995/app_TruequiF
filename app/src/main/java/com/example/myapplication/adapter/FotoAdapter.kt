package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

class FotoAdapter(private val fotos: List<String>) :
    RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFoto: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val urlImagen = fotos[position]
        Glide.with(holder.itemView.context)
            .load(urlImagen)
            .into(holder.imgFoto)
    }

    override fun getItemCount(): Int = fotos.size
}
