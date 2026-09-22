package com.example.aiassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

class WebKnowledgeMemory(
    context: Context
) {

    private val file = File(
        context.filesDir,
        "web_knowledge_memory.json"
    )

    private val lock = Any()

    data class Record(
        val query: String,
        val title: String,
        val url: String,
        val source: String,
        val content: String,
        val firstSeen: Long,
        val lastSeen: Long,
        val reuseCount: Int
    )

    fun learn(
        query: String,
        evidence: List<WebEvidence>
    ) {
        if (evidence.isEmpty()) return

        synchronized(lock) {
            val records = loadInternal().toMutableList()
            val now = System.currentTimeMillis()

            for (item in evidence) {
                val url = normalizeUrl(item.url)

                if (url.isBlank()) continue
                if (item.content.trim().length < 180) continue

                val existingIndex = records.indexOfFirst {
                    normalizeUrl(it.url) == url
                }

                if (existingIndex >= 0) {
                    val old = records[existingIndex]

                    records[existingIndex] = old.copy(
                        lastSeen = now,
                        reuseCount = old.reuseCount + 1
                    )
                } else {
                    records += Record(
                        query = query.take(500),
                        title = item.title.take(500),
                        url = item.url.take(2000),
                        source = item.source.take(100),
                        content = item.content.take(12000),
                        firstSeen = now,
                        lastSeen = now,
                        reuseCount = 1
                    )
                }
            }

            saveInternal(records)
        }
    }

    fun context(
        query: String,
        maxRecords: Int = 8
    ): String {
        val q = query
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
            .distinct()

        if (q.isEmpty()) return ""

        val records = synchronized(lock) {
            loadInternal()
        }

        val ranked = records
            .map { record ->
                val text = (
                    record.title + " " +
                        record.content + " " +
                        record.query
                    ).lowercase(Locale.ROOT)

                val matches = q.count { token ->
                    text.contains(token)
                }

                record to matches
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Record, Int>> {
                    it.second
                }.thenByDescending {
                    it.first.lastSeen
                }.thenByDescending {
                    it.first.reuseCount
                }
            )
            .take(maxRecords)

        if (ranked.isEmpty()) return ""

        return buildString {
            append("🌐 WEB KNOWLEDGE MEMORY\n\n")

            ranked.forEachIndexed { index, pair ->
                val record = pair.first

                append("[")
                    .append(index + 1)
                    .append("] ")
                    .append(record.title)
                    .append('\n')

                append("URL: ")
                    .append(record.url)
                    .append('\n')

                append("Sursă: ")
                    .append(record.source)
                    .append('\n')

                append(
                    record.content
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(1800)
                )

                append("\n\n")
            }
        }.take(7000)
    }

    fun dashboard(): String {
        val count = synchronized(lock) {
            loadInternal().size
        }

        return "🌐 WEB KNOWLEDGE MEMORY\n" +
            "• Surse memorate: $count\n" +
            "• Persistență locală: activă\n" +
            "• Reutilizare informații: activă\n" +
            "• Sursele rămân asociate cu URL-ul original"
    }

    private fun loadInternal(): List<Record> {
        if (!file.exists()) return emptyList()

        return try {
            val text = file.readText()

            if (text.isBlank()) {
                emptyList()
            } else {
                val array = JSONArray(text)
                val result = mutableListOf<Record>()

                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue

                    result += Record(
                        query = o.optString("query"),
                        title = o.optString("title"),
                        url = o.optString("url"),
                        source = o.optString("source"),
                        content = o.optString("content"),
                        firstSeen = o.optLong("firstSeen"),
                        lastSeen = o.optLong("lastSeen"),
                        reuseCount = o.optInt("reuseCount", 1)
                    )
                }

                result
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveInternal(
        records: List<Record>
    ) {
        try {
            val array = JSONArray()

            records
                .takeLast(2000)
                .forEach { record ->
                    array.put(
                        JSONObject().apply {
                            put("query", record.query)
                            put("title", record.title)
                            put("url", record.url)
                            put("source", record.source)
                            put("content", record.content)
                            put("firstSeen", record.firstSeen)
                            put("lastSeen", record.lastSeen)
                            put("reuseCount", record.reuseCount)
                        }
                    )
                }

            val temp = File(
                file.parentFile,
                "${file.name}.tmp"
            )

            temp.writeText(
                array.toString()
            )

            if (!temp.renameTo(file)) {
                file.writeText(array.toString())
                temp.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun normalizeUrl(
        url: String
    ): String {
        return url
            .substringBefore("#")
            .trim()
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }
}
