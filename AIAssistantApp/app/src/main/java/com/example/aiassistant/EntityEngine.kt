package com.example.aiassistant

import java.util.Locale

object EntityEngine {
    data class Entity(val text: String, val type: String, val start: Int, val end: Int)
    data class Result(val entities: List<Entity>)

    private val number = Regex("-?\\d+(?:[.,]\\d+)?")
    private val variable = Regex("\\b[A-Za-zĂÂÎȘȚăâîșț][A-Za-z0-9ĂÂÎȘȚăâîșț_]{0,20}\\b")
    private val date = Regex("\\b(?:[0-3]?\\d[./-][01]?\\d[./-](?:20)?\\d{2}|20\\d{2}[./-][01]?\\d[./-][0-3]?\\d)\\b")

    fun extract(input: String): Result {
        val out = mutableListOf<Entity>()
        number.findAll(input).forEach { out += Entity(it.value, "NUMBER", it.range.first, it.range.last + 1) }
        date.findAll(input).forEach { out += Entity(it.value, "DATE", it.range.first, it.range.last + 1) }
        variable.findAll(input).forEach {
            val w = it.value.lowercase(Locale.getDefault())
            if (w.length >= 3 && w !in STOP && !w.all { c -> c.isDigit() }) out += Entity(it.value, "TOKEN", it.range.first, it.range.last + 1)
        }
        return Result(out.sortedBy { it.start }.distinctBy { "${it.start}:${it.end}:$it" })
    }

    private val STOP = setOf("care", "este", "sunt", "pentru", "despre", "acest", "aceasta", "aceste", "acestea", "unei", "unui", "mai", "foarte", "poate", "fost", "din", "în", "si", "și", "sau", "the", "and", "for", "with", "from", "this", "that")
}
