package com.example.aiassistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WorkspaceManager(context: android.content.Context) {
    private val dir = File(context.filesDir, "workspaces").apply { mkdirs() }
    data class Workspace(val id: String, val name: String, val created: Long, val notes: String)

    fun list(): List<Workspace> = dir.listFiles()?.mapNotNull { f -> try {
        val j = JSONObject(f.readText()); Workspace(j.getString("id"), j.getString("name"), j.getLong("created"), j.optString("notes"))
    } catch (_: Exception) { null } }?.sortedByDescending { it.created } ?: emptyList()

    fun create(name: String): Workspace {
        val id = "ws_${System.currentTimeMillis()}"
        val w = Workspace(id, name.trim().ifBlank { "Workspace nou" }, System.currentTimeMillis(), "")
        File(dir, "$id.json").writeText(JSONObject().put("id", w.id).put("name", w.name).put("created", w.created).put("notes", "").put("messages", JSONArray()).toString())
        return w
    }

    fun appendMessage(workspace: Workspace, message: ChatMessage) {
        val f = File(dir, "${workspace.id}.json")
        if (!f.exists()) return
        val j = JSONObject(f.readText()); val arr = j.optJSONArray("messages") ?: JSONArray()
        arr.put(JSONObject().put("t", message.text).put("u", message.isUser).put("s", JSONArray(message.sources)))
        j.put("messages", arr); f.writeText(j.toString())
    }
}
