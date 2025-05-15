package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.model.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*



class ProductoAdapter(
    private val listaProductos: List<Producto>,
    private val onItemClick: (Producto) -> Unit // <- nuevo parámetro
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val likedMap = mutableMapOf<String, Boolean>()

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProducto: ImageView = itemView.findViewById(R.id.imgProducto)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val txtUsuario: TextView = itemView.findViewById(R.id.txtUsuario)
        val imgCorazon: ImageView = itemView.findViewById(R.id.imgCorazon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        holder.txtTitulo.text = producto.titulo
        holder.txtUsuario.text = producto.usuario

        if (producto.imagenes.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(producto.imagenes[0])
                .into(holder.imgProducto)
        } else {
            holder.imgProducto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val uid = auth.currentUser?.uid ?: return
        val likeRef = database.child("likes").child(uid).child(producto.id)

        if (likedMap.containsKey(producto.id)) {
            updateHeartIcon(holder.imgCorazon, likedMap[producto.id] == true)
        } else {
            likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLiked = snapshot.exists()
                    likedMap[producto.id] = isLiked
                    updateHeartIcon(holder.imgCorazon, isLiked)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        holder.imgCorazon.setOnClickListener {
            val isLiked = likedMap[producto.id] == true
            if (isLiked) {
                likeRef.removeValue()
                likedMap[producto.id] = false
                updateHeartIcon(holder.imgCorazon, false)
            } else {
                likeRef.setValue(producto)
                likedMap[producto.id] = true
                updateHeartIcon(holder.imgCorazon, true)
            }
        }


        holder.itemView.setOnClickListener {
            onItemClick(producto)
        }

    }

    private fun updateHeartIcon(heartView: ImageView, isLiked: Boolean) {
        heartView.setImageResource(if (isLiked) R.drawable.corazon_rojo else R.mipmap.like)
    }

    override fun getItemCount(): Int = listaProductos.size
}





