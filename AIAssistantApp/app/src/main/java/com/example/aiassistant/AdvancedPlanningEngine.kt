package com.example.aiassistant

object AdvancedPlanningEngine {
    data class Step(val id: Int, val action: String, val dependsOn: List<Int>)
    data class Result(val handled: Boolean, val answer: String, val steps: List<Step>)

    fun plan(input: String): Result {
        val s = input.lowercase()
        if (!listOf("planifică", "planifica", "pașii", "pasii", "strategie", "cum pot să", "cum pot sa").any { s.contains(it) }) return Result(false, "", emptyList())
        val goal = input.replace(Regex("(?i).*?(?:planifică|planifica|strategie pentru|cum pot să|cum pot sa)\\s*"), "").trim().ifBlank { input.trim() }
        val steps = listOf(
            Step(1, "Definește obiectivul și criteriul de succes pentru: $goal", emptyList()),
            Step(2, "Extrage datele, resursele și constrângerile", listOf(1)),
            Step(3, "Împarte obiectivul în subprobleme verificabile", listOf(1,2)),
            Step(4, "Execută subproblemele în ordinea dependențelor", listOf(3)),
            Step(5, "Verifică rezultatele și caută contradicții", listOf(4)),
            Step(6, "Dacă verificarea eșuează, revino la pasul relevant și ajustează planul", listOf(5)),
            Step(7, "Livrează rezultatul și notează ce rămâne necunoscut", listOf(5,6))
        )
        return Result(true, steps.joinToString("\n") { "${it.id}. ${it.action}${if (it.dependsOn.isEmpty()) "" else " (dep: ${it.dependsOn.joinToString()})"}" }, steps)
    }
}
