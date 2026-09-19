package com.example.aiassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 📚 Gestionarul de istoric: salvează/încarcă/șterge conversații complete. */
class SessionManager(context: Context) {

    private val dir = File(context.filesDir, "sessions")

    data class Session(val file: File, val title: String, val time: Long)

    init { dir.mkdirs() }

    fun save(messages: List<ChatMessage>): String? {
        if (messages.count { it.isUser } < 1) return null
        val firstUser = messages.firstOrNull { it.isUser }?.text ?: "Conversație"
        val title = firstUser.take(40) + if (firstUser.length > 40) "…" else ""
        val json = JSONObject()
            .put("title", title)
            .put("time", System.currentTimeMillis())
            .put("messages", JSONArray().apply {
                for (m in messages) put(JSONObject()
                    .put("t", m.text)
                    .put("u", m.isUser)
                    .put("s", JSONArray(m.sources)))
            })
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        File(dir, "session_$stamp.json").writeText(json.toString())
        return title
    }

    fun listSessions(): List<Session> {
        return dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { f ->
                try {
                    val j = JSONObject(f.readText())
                    Session(f, j.getString("title"), j.getLong("time"))
                } catch (_: Exception) { null }
            } ?: emptyList()
    }

    fun load(session: Session): List<ChatMessage> {
        val j = JSONObject(session.file.readText())
        val arr = j.getJSONArray("messages")
        val out = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val src = mutableListOf<String>()
            m.optJSONArray("s")?.let { s -> for (k in 0 until s.length()) src.add(s.getString(k)) }
            out.add(ChatMessage(m.getString("t"), m.getBoolean("u"), src, feedbackRated = true))
        }
        return out
    }

    fun delete(session: Session) = session.file.delete()

    fun clear() = dir.listFiles()?.forEach { it.delete() }
}
