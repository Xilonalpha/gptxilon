package com.example.aiassistant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileIntelligence {
    private const val MAX_BINARY = 12 * 1024 * 1024
    private const val MAX_TEXT = 180_000

    fun read(context: Context, uri: Uri): Attachment? {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        var name = "attachment"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0) ?: name
        }
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size > MAX_BINARY) return null
            val ext = name.substringAfterLast('.', "").lowercase()
            val textLike = mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") ||
                mime.contains("csv") || mime.contains("javascript") || ext in setOf("kt","java","py","js","ts","tsx","jsx","css","html","md","csv","sql","gradle","kts","txt","xml","json","yaml","yml","sh","swift","dart","c","cpp","h","hpp")
            if (textLike) Attachment(name, mime, null, bytes.toString(Charsets.UTF_8).take(MAX_TEXT))
            else Attachment(name, mime, bytes, null)
        } catch (_: Exception) { null }
    }
}
