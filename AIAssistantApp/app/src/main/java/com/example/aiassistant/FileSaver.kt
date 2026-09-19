package com.example.aiassistant

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 💾 Utilitare de scriere fișiere: blocuri de cod, fișiere individuale, export proiect ZIP. */
object FileSaver {

    fun extractCodeBlocks(text: String): List<Pair<String, String>> {
        val blocks = mutableListOf<Pair<String, String>>()
        val regex = Regex("```([a-zA-Z0-9_+#.-]*)(?:\\s+)(?:\\[file[:=]([^\\]]+)\\]\\s*)?([\\s\\S]*?)```")
        for (m in regex.findAll(text)) {
            val lang = m.groupValues[1].ifEmpty { "txt" }
            val path = m.groupValues[2].trim()
            val code = m.groupValues[3].trim()
            blocks.add((if (path.isNotBlank()) "$lang|$path" else lang) to code)
        }
        return blocks
    }

    fun extensionFor(lang: String): String = when (lang.lowercase()) {
        "kotlin" -> "kt"; "javascript" -> "js"; "typescript" -> "ts"
        "python" -> "py"; "html" -> "html"; "css" -> "css"
        "json" -> "json"; "java" -> "java"; "c++", "cpp" -> "cpp"
        "c#" -> "cs"; "xml" -> "xml"; "sql" -> "sql"
        "bash", "shell", "sh" -> "sh"; "swift" -> "swift"; "php" -> "php"
        else -> "txt"
    }

    /** Scrie toate blocurile de cod într-un folder (SAF) ales de utilizator + arhivă ZIP. */
    fun exportProjectZip(context: Context, treeUri: Uri, blocks: List<Pair<String, String>>): Boolean {
        if (blocks.isEmpty()) return false
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        val zipFile = File(context.cacheDir, "proiect_ai.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            blocks.forEachIndexed { i, (rawLang, code) ->
                val pieces = rawLang.split("|", limit = 2)
                val lang = pieces[0]
                val suggested = pieces.getOrNull(1)?.trim().orEmpty()
                val filename = safePath(suggested.ifBlank { "file_%02d.${extensionFor(lang)}".format(i + 1) })
                zos.putNextEntry(ZipEntry("proiect/$filename")); zos.write(code.toByteArray()); zos.closeEntry()
            }
        }
        val folder = root.createDirectory("AI_Project_${System.currentTimeMillis() % 100000}") ?: return false
        blocks.forEachIndexed { i, (rawLang, code) ->
            val pieces=rawLang.split("|",limit=2); val lang=pieces[0]; val suggested=pieces.getOrNull(1)?.trim().orEmpty()
            val filename=safePath(suggested.ifBlank { "file_%02d.${extensionFor(lang)}".format(i+1) }).substringAfterLast('/')
            val f=folder.createFile("text/plain",filename)
            f?.let { context.contentResolver.openOutputStream(it.uri)?.use { out->out.write(code.toByteArray()) } }
        }
        folder.createFile("application/zip","proiect_ai.zip")?.let { context.contentResolver.openOutputStream(it.uri)?.use { out->out.write(zipFile.readBytes()) } }
        return true
    }

    private fun safePath(path: String): String = path.replace('\\','/').split('/').filter { it.isNotBlank() && it != "." && it != ".." }.joinToString("/").ifBlank { "file.txt" }

}
