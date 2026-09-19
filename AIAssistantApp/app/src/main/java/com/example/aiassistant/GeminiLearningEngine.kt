package com.example.aiassistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

class GeminiLearningEngine(
    private val filesDir: File
) {
    companion object {
        private const val FILE_NAME = "gemini_memory.json"
        private const val MAX_RECORDS = 250
        private const val MAX_QUERY_LENGTH = 1600
        private const val MAX_ANSWER_LENGTH = 12000
        private const val MAX_SOURCES = 20
    }

    private val memoryFile: File
        get() = File(filesDir, FILE_NAME)

    @Synchronized
    fun learn(userQuery: String, response: AiResponse) {
        val query = userQuery.trim()
        val answer = response.text.trim()

        if (query.isBlank() || answer.isBlank()) return

        val records = load()

        val existing = records.firstOrNull {
            it.optString("query") == query &&
                it.optString("answer") == answer
        }

        if (existing != null) {
            existing.put(
                "reuseCount",
                existing.optInt("reuseCount", 1) + 1
            )
            existing.put("lastSeen", System.currentTimeMillis())
            existing.put("tokens", response.tokens)

            val sources = JSONArray()
            response.sources.distinct().take(MAX_SOURCES).forEach {
                sources.put(it)
            }

            if (sources.length() > 0) {
                existing.put("sources", sources)
                existing.put("grounded", true)
            }

            save(records)
            return
        }

        val record = JSONObject()
            .put("id", System.currentTimeMillis().toString())
            .put("createdAt", System.currentTimeMillis())
            .put("lastSeen", System.currentTimeMillis())
            .put("query", query.take(MAX_QUERY_LENGTH))
            .put("answer", answer.take(MAX_ANSWER_LENGTH))
            .put("grounded", response.sources.isNotEmpty())
            .put("tokens", response.tokens)
            .put("reuseCount", 1)
            .put(
                "confidence",
                if (response.sources.isNotEmpty()) 0.90 else 0.65
            )

        val sources = JSONArray()
        response.sources.distinct().take(MAX_SOURCES).forEach {
            sources.put(it)
        }
        record.put("sources", sources)

        records.add(0, record)

        while (records.size > MAX_RECORDS) {
            records.removeAt(records.lastIndex)
        }

        save(records)
    }

    @Synchronized
    fun context(query: String): String {
        val records = load()
        if (records.isEmpty()) return ""

        val normalizedQuery = normalize(query)
        val queryTerms = terms(normalizedQuery)

        val ranked = records
            .map { record ->
                val searchable = normalize(
                    record.optString("query") + " " +
                        record.optString("answer")
                )

                val score = if (queryTerms.isEmpty()) {
                    1.0
                } else {
                    val sourceTerms = terms(searchable)
                    val overlap = queryTerms.count { it in sourceTerms }
                    overlap.toDouble() / queryTerms.size.toDouble()
                }

                record to score
            }
            .sortedByDescending { it.second }
            .take(8)

        val selected = if (queryTerms.isEmpty()) {
            ranked.take(5)
        } else {
            ranked.filter { it.second > 0.0 }.take(5)
        }

        if (selected.isEmpty()) return ""

        return buildString {
            append("🧠 GEMINI LEARNED MEMORY\n")
            append("Acestea sunt răspunsuri/informații învățate anterior de la Gemini.\n")
            append("Nu sunt adevăruri absolute; verifică informațiile când subiectul este actual sau important.\n\n")

            selected.forEachIndexed { index, pair ->
                val record = pair.first
                val grounded = record.optBoolean("grounded", false)
                val score = (pair.second * 100).toInt()

                append("[$index] ")
                append(if (grounded) "GROUNDED" else "NON-GROUNDED")
                append(" • relevanță ")
                append(score)
                append("%\n")

                append("Întrebare anterioară: ")
                append(record.optString("query"))
                append('\n')

                append("Cunoștință/răspuns Gemini: ")
                append(record.optString("answer").take(1800))
                append('\n')

                val sources = record.optJSONArray("sources")
                if (sources != null && sources.length() > 0) {
                    append("Surse Gemini: ")
                    for (i in 0 until minOf(sources.length(), 5)) {
                        if (i > 0) append(" | ")
                        append(sources.optString(i))
                    }
                    append('\n')
                }

                append("Reutilizări: ")
                append(record.optInt("reuseCount", 1))
                append("\n\n")
            }
        }.take(7000)
    }

    @Synchronized
    fun count(): Int = load().size

    @Synchronized
    fun dashboard(): String {
        val records = load()
        val grounded = records.count {
            it.optBoolean("grounded", false)
        }

        return buildString {
            append("🤖 GEMINI LEARNING MEMORY\n\n")
            append("Memorii salvate: ")
            append(records.size)
            append('\n')

            append("Cu grounding/surse: ")
            append(grounded)
            append('\n')

            append("Fără grounding: ")
            append(records.size - grounded)
            append("\n\n")

            append("Memoria este stocată local, în spațiul privat al aplicației.\n")
            append("Motorul învață din răspunsul final Gemini, sursele disponibile și reutilizarea informației.\n")
            append("Nu încearcă să extragă chain-of-thought sau raționamentul privat al lui Gemini.")
        }
    }

    private fun load(): MutableList<JSONObject> {
        if (!memoryFile.exists()) return mutableListOf()

        return try {
            val array = JSONArray(memoryFile.readText())
            MutableList(array.length()) { index ->
                array.getJSONObject(index)
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun save(records: List<JSONObject>) {
        try {
            val array = JSONArray()
            records.forEach { array.put(it) }

            val temp = File(filesDir, "$FILE_NAME.tmp")
            temp.writeText(array.toString())

            if (memoryFile.exists()) {
                memoryFile.delete()
            }

            temp.renameTo(memoryFile)
        } catch (_: Exception) {
            // Memoria nu trebuie să poată opri conversația principală.
        }
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun terms(value: String): Set<String> {
        val stopWords = setOf(
            "și", "sau", "dar", "este", "sunt", "era", "erau",
            "un", "o", "unei", "unui", "de", "din", "în", "la",
            "pe", "cu", "pentru", "care", "ce", "cum", "mai",
            "the", "and", "or", "is", "are", "was", "were",
            "a", "an", "of", "to", "in", "on", "for", "with",
            "what", "how", "this", "that"
        )

        return value
            .split(' ')
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }
}
