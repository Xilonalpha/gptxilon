package com.example.aiassistant

import java.util.ArrayDeque
import java.util.Locale

object ContextEngine {
    data class Turn(val user: String, val assistant: String, val time: Long = System.currentTimeMillis())
    data class Snapshot(val input: String, val recent: List<Turn>, val topic: String, val referencedPrevious: Boolean)

    private val turns = ArrayDeque<Turn>()

    @Synchronized fun snapshot(input: String): Snapshot {
        val recent = turns.toList().takeLast(8)
        val s = input.lowercase(Locale.getDefault())
        val reference = listOf("asta", "aceasta", "acest lucru", "anterior", "mai sus", "ce ai spus", "ce am spus", "acolo").any { s.contains(it) }
        val topic = extractTopic(input, recent)
        return Snapshot(input, recent, topic, reference)
    }

    @Synchronized fun remember(user: String, assistant: String) {
        turns.addLast(Turn(user.take(1200), assistant.take(1800)))
        while (turns.size > 12) turns.removeFirst()
    }

    private fun extractTopic(input: String, recent: List<Turn>): String {
        val tokens = EntityEngine.extract(input).entities.filter { it.type == "TOKEN" }.map { it.text.lowercase() }.filter { it.length >= 4 }
        if (tokens.isNotEmpty()) return tokens.take(5).distinct().joinToString(" ")
        return recent.lastOrNull()?.user?.take(120)?.lowercase().orEmpty()
    }
}
