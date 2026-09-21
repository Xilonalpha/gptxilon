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
     * Loads all offline knowledge datasets listed by knowledge_index.json.
     *
     * Knowledge is kept outside Kotlin so the database can grow
     * independently into thousands or tens of thousands of entries.
     */
    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val assets = context.applicationContext.assets

            val indexText = assets
                .open("knowledge_index.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val index = JSONObject(indexText)
            val datasets = index.optJSONArray("datasets") ?: JSONArray()

            val loaded = ArrayList<Entry>()

            for (i in 0 until datasets.length()) {
                val dataset = datasets.getJSONObject(i)
                val file = dataset.optString("file")

                if (file.isBlank()) continue

                val text = assets
                    .open(file)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val root = JSONObject(text)
                val array = root.optJSONArray("entries") ?: JSONArray()

                for (j in 0 until array.length()) {
                    val item = array.getJSONObject(j)
                    val aliasesJson = item.optJSONArray("aliases") ?: JSONArray()

                    val aliases = ArrayList<String>(aliasesJson.length())

                    for (k in 0 until aliasesJson.length()) {
                        val alias = aliasesJson.optString(k)
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

        // 1. Exact alias match.
        normalized
            .firstOrNull { q == it.first }
            ?.second
            ?.let { return it }

        // 2. Multi-word aliases may match as complete phrases.
        normalized
            .asSequence()
            .filter { it.first.contains(" ") }
            .firstOrNull { q.containsPhrase(it.first) }
            ?.second
            ?.let { return it }

        // 3. Single-word aliases require an actual knowledge-question
        //    structure. A random occurrence of "atom", "gold", etc.
        //    must NOT trigger a factual answer.
        normalized
            .asSequence()
            .filter { !it.first.contains(" ") }
            .firstOrNull { isKnowledgeTargetQuestion(q, it.first) }
            ?.second
            ?.let { return it }

        return null
    }

    private fun isKnowledgeTargetQuestion(
        query: String,
        alias: String
    ): Boolean {
        val q = " ${query.trim()} "
        val a = " ${alias.trim()} "

        if (!q.contains(a)) return false

        val patterns = listOf(
            "what is",
            "what are",
            "what was",
            "who is",
            "who was",
            "where is",
            "where was",
            "when was",
            "define",
            "explain",
            "tell me about",
            "information about",
            "what do you know about",
            "ce este",
            "ce sunt",
            "cine este",
            "cine a fost",
            "unde este",
            "cand a fost",
            "definește",
            "defineste",
            "explica",
            "explică",
            "spune-mi despre",
            "ce știi despre",
            "ce stii despre",
            "capital of",
            "capitala",
            "chemical symbol for",
            "symbol for",
            "simbolul chimic",
            "simbolul pentru"
        )

        return patterns.any { q.contains(" $it ") }
    }

    private fun String.containsPhrase(phrase: String): Boolean {
        val paddedQuery = " ${trim()} "
        val paddedPhrase = " ${phrase.trim()} "
        return paddedQuery.contains(paddedPhrase)
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
