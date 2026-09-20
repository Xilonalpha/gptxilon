package com.example.aiassistant

object ContradictionEngine {
    private val assignment = Regex("""\b([A-Za-z][A-Za-z0-9_]*)\s*=\s*(-?\d+(?:[.,]\d+)?)\b""")

    fun canHandle(input: String): Boolean {
        val values = assignment.findAll(input).map {
            it.groupValues[1].lowercase() to it.groupValues[2].replace(',', '.')
        }.toList()
        return values.groupBy { it.first }.values.any { list ->
            list.map { it.second }.distinct().size > 1
        }
    }

    fun answer(input: String): String =
        "Am detectat o contradicție între valori atribuite aceleiași variabile. Nu aleg arbitrar una dintre afirmații."
}
