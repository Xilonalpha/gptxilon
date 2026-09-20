package com.example.aiassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

object LocalKnowledgeBase {

    data class Entry(
        val topic: String,
        val aliases: List<String>,
        val answer: String,
        val domain: String
    )

    @Volatile
    private var initialized = false

    private var entries: List<Entry> = emptyList()

    private var normalized: List<Pair<String, Entry>> = emptyList()

    /**
     * Loads the deterministic offline knowledge dataset from assets.
     *
     * The knowledge itself is deliberately kept outside this Kotlin file
     * so the dataset can grow independently to thousands or tens of
     * thousands of entries.
     */
    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val text = context.applicationContext.assets
                .open("local_knowledge.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val root = JSONObject(text)
            val array = root.optJSONArray("entries") ?: JSONArray()

            val loaded = ArrayList<Entry>(array.length())

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val aliasesJson = item.optJSONArray("aliases") ?: JSONArray()

                val aliases = ArrayList<String>(aliasesJson.length())

                for (j in 0 until aliasesJson.length()) {
                    val alias = aliasesJson.optString(j)
                    if (alias.isNotBlank()) {
                        aliases += alias
                    }
                }

                loaded += Entry(
                    topic = item.optString("topic"),
                    aliases = aliases,
                    answer = item.optString("answer"),
                    domain = item.optString("domain")
                )
            }

            entries = loaded

            normalized = entries.flatMap { entry ->
                entry.aliases.map { alias ->
                    normalize(alias) to entry
                }
            }

            initialized = true
        }
    }

    fun lookup(input: String): Entry? {
        if (!initialized) return null

        val q = normalize(input)

        normalized
            .firstOrNull { q.contains(it.first) }
            ?.second
            ?.let { return it }

        val tokens = q
            .split(' ')
            .filter { it.length >= 3 }
            .toSet()

        return normalized
            .map { it.second to score(tokens, normalize(it.first)) }
            .filter { it.second >= 0.72 }
            .maxByOrNull { it.second }
            ?.first
    }

    fun canHandle(input: String): Boolean =
        lookup(input) != null

    fun answer(input: String): String? =
        lookup(input)?.answer

    fun domain(input: String): String? =
        lookup(input)?.domain

    fun size(): Int =
        entries.size

    private fun score(q: Set<String>, a: String): Double {
        val tokens = a
            .split(' ')
            .filter { it.length >= 3 }
            .toSet()

        if (tokens.isEmpty()) return 0.0

        return q.intersect(tokens).size.toDouble() / tokens.size
    }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
