package com.example.aiassistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()
    var onFeedback: ((Int, Boolean) -> Unit)? = null
    var onSpeak: ((String) -> Unit)? = null

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(list: List<ChatMessage>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    fun updateLastMessage(text: String, sources: List<String> = emptyList(), tokens: Int = 0) {
        if (messages.isNotEmpty()) {
            val idx = messages.lastIndex
            val extra = if (tokens > 0) "\n\n_${tokens} tokeni_" else ""
            messages[idx] = messages[idx].copy(text = text + extra, sources = sources)
            notifyItemChanged(idx)
        }
    }

    fun markRated(position: Int) {
        if (position in messages.indices) {
            messages[position] = messages[position].copy(feedbackRated = true)
            notifyItemChanged(position)
        }
    }

    fun getMessages(): List<ChatMessage> = messages

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvSources: TextView = view.findViewById(R.id.tvSources)
        val actionRow: LinearLayout = view.findViewById(R.id.actionRow)
        val btnSpeak: ImageButton = view.findViewById(R.id.btnSpeak)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnDislike: ImageButton = view.findViewById(R.id.btnDislike)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessage.text = msg.text

        val params = holder.tvMessage.layoutParams as LinearLayout.LayoutParams
        if (msg.isUser) {
            holder.tvMessage.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.bubble_user))
            params.gravity = android.view.Gravity.END
            holder.tvSources.visibility = View.GONE
            holder.actionRow.visibility = View.GONE
        } else {
            holder.tvMessage.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.bubble_ai))
            params.gravity = android.view.Gravity.START

            holder.tvSources.visibility = if (msg.sources.isNotEmpty()) View.VISIBLE else View.GONE
            holder.tvSources.text = if (msg.sources.isNotEmpty())
                "🔗 Sursă: " + msg.sources.take(3).joinToString(" | ") else ""

            holder.actionRow.visibility = if (msg.feedbackRated) View.GONE else View.VISIBLE
            holder.btnSpeak.setOnClickListener { onSpeak?.invoke(msg.text) }
            holder.btnLike.setOnClickListener { onFeedback?.invoke(position, true) }
            holder.btnDislike.setOnClickListener { onFeedback?.invoke(position, false) }
        }
        holder.tvMessage.layoutParams = params
        holder.tvMessage.setPadding(32, 28, 32, 28)

        // Long-press = copiază mesajul
        holder.itemView.setOnLongClickListener {
            val cm = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("mesaj", msg.text))
            Toast.makeText(holder.itemView.context, "Copiat 📋", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun getItemCount(): Int = messages.size
}
