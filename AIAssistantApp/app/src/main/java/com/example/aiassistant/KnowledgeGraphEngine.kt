package com.example.aiassistant

import java.util.Locale

object KnowledgeGraphEngine {
    data class Edge(val from: String, val relation: String, val to: String, val confidence: Int, val time: Long = System.currentTimeMillis())
    private val edges = mutableListOf<Edge>()

    @Synchronized fun observe(text: String) {
        val s = text.trim().replace(Regex("\\s+"), " ")
        val patterns = listOf(
            Regex("(?i)^(.{2,60})\\s+(?:este|este un|este o)\\s+(.{2,80})$"),
            Regex("(?i)^(.{2,60})\\s+(?:cauzează|cauzeaza|duce la|produce)\\s+(.{2,80})$"),
            Regex("(?i)^(.{2,60})\\s+->\\s+(.{2,80})$")
        )
        patterns.firstNotNullOfOrNull { it.find(s) }?.let {
            val relation = when {
                s.contains("cauzează", true) || s.contains("cauzeaza", true) -> "CAUZEAZĂ"
                s.contains("duce la", true) -> "DUCE_LA"
                s.contains("produce", true) -> "PRODUCE"
                s.contains("->") -> "RELATED_TO"
                else -> "ESTE"
            }
            add(it.groupValues[1], relation, it.groupValues[2])
        }
    }

    @Synchronized fun add(from: String, relation: String, to: String, confidence: Int = 65) {
        val f = clean(from); val t = clean(to); if (f.isBlank() || t.isBlank()) return
        if (edges.none { it.from == f && it.relation == relation && it.to == t }) edges += Edge(f, relation, t, confidence.coerceIn(0,100))
        if (edges.size > 250) edges.removeAt(0)
    }

    @Synchronized fun query(term: String): List<Edge> {
        val q = term.lowercase(Locale.ROOT)
        return edges.filter { it.from.contains(q) || it.to.contains(q) || it.relation.contains(q) }.takeLast(30)
    }

    @Synchronized fun text(term: String = ""): String {
        val selected = if (term.isBlank()) edges.takeLast(20) else query(term)
        return if (selected.isEmpty()) "Graful local nu are încă relații relevante." else selected.joinToString("\n") { "• ${it.from} —${it.relation}→ ${it.to} [${it.confidence}%]" }
    }

    private fun clean(s: String) = s.trim().trim('.', ',', ';', ':').lowercase(Locale.ROOT).take(100)
}
