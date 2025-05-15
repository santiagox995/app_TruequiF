package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Mensaje

class ChatAdapter(private val messages: List<Mensaje>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == 1) R.layout.item_user_message else R.layout.item_bot_message
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val emoji = if (message.emisorId == "user") "👤" else "🤖"
        holder.messageText.text = "$emoji ${message.mensaje}"
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].emisorId == "user") 1 else 0
    }

    override fun getItemCount(): Int = messages.size
}
