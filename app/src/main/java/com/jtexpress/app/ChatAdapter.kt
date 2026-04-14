package com.jtexpress.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutUser: LinearLayout = view.findViewById(R.id.layout_user)
        val layoutBot: LinearLayout  = view.findViewById(R.id.layout_bot)
        val tvUser: TextView         = view.findViewById(R.id.tv_user_message)
        val tvBot: TextView          = view.findViewById(R.id.tv_bot_message)
        val tvTimestamp: TextView    = view.findViewById(R.id.tv_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutBot.visibility  = View.GONE
            holder.tvUser.text           = message.text
            holder.tvTimestamp.gravity   = android.view.Gravity.END
        } else {
            holder.layoutBot.visibility  = View.VISIBLE
            holder.layoutUser.visibility = View.GONE
            holder.tvBot.text            = message.text
            holder.tvTimestamp.gravity   = android.view.Gravity.START
        }

        if (message.timestamp.isNotEmpty()) {
            holder.tvTimestamp.visibility = View.VISIBLE
            holder.tvTimestamp.text       = message.timestamp
        } else {
            holder.tvTimestamp.visibility = View.GONE
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }
}