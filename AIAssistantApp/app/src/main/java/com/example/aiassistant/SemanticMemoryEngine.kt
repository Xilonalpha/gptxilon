package com.example.aiassistant

import java.util.Locale
import kotlin.math.max

object SemanticMemoryEngine {
    data class Item(val text: String, val response: String, val time: Long = System.currentTimeMillis())
    private val items = mutableListOf<Item>()

    @Synchronized fun remember(text: String, response: String) {
        if (text.isBlank() || response.isBlank()) return
        items += Item(text.take(900), response.take(1600))
        if (items.size > 80) items.removeAt(0)
    }

    @Synchronized fun recall(query: String, limit: Int = 3): List<Item> {
        val q = tokens(query)
        if (q.isEmpty()) return emptyList()
        return items.map { it to similarity(q, tokens(it.text)) }
            .filter { it.second >= 0.18 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.intersect(b).size.toDouble()
        return (2.0 * inter / max(1, a.size + b.size)).coerceIn(0.0, 1.0)
    }

    private fun tokens(s: String): Set<String> = s.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
        .split(" ")
        .filter { it.length >= 3 && it !in STOP }
        .toSet()

    private val STOP = setOf("care", "este", "sunt", "pentru", "despre", "asta", "acesta", "aceasta", "mai", "foarte", "poate", "the", "and", "for", "with", "from", "this", "that")
}
