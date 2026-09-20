package com.example.aiassistant

object PlanningEngine {
    fun canHandle(input: String): Boolean {
        val s = input.lowercase()
        return s.contains("planifică") || s.contains("planifica") ||
                s.contains("pașii") || s.contains("pasii") ||
                s.contains("cum pot să") || s.contains("cum pot sa")
    }

    fun answer(input: String): String = """
        Plan local:
        1. Identifică obiectivul.
        2. Extrage datele și constrângerile.
        3. Împarte problema în subprobleme.
        4. Alege instrumentul potrivit.
        5. Execută pașii.
        6. Verifică rezultatele și contradicțiile.
        7. Produce concluzia.
    """.trimIndent()
}
