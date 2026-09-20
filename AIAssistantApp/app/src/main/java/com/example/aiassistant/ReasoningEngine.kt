package com.example.aiassistant

import java.util.Locale

object ReasoningEngine {
    private val gt = Regex("""([A-Za-z]+)\s*(?:este\s*)?mai\s+mare\s+decât\s*([A-Za-z]+)""")
    private val lt = Regex("""([A-Za-z]+)\s*(?:este\s*)?mai\s+mic\s+decât\s*([A-Za-z]+)""")

    fun canHandle(input: String): Boolean {
        val s = input.lowercase(Locale.getDefault())
        return s.contains("dacă") || s.contains("daca") || s.contains("atunci") ||
                s.contains("deduce") || s.contains("logic") ||
                gt.containsMatchIn(s) || lt.containsMatchIn(s)
    }

    fun answer(input: String): String {
        val s = input.lowercase(Locale.getDefault())
        gt.find(s)?.let {
            return "Premisă: ${it.groupValues[1]} > ${it.groupValues[2]}. Concluzie: prima entitate este mai mare."
        }
        lt.find(s)?.let {
            return "Premisă: ${it.groupValues[1]} < ${it.groupValues[2]}. Concluzie: prima entitate este mai mică."
        }
        return "Am identificat o cerință de raționament. Pot evalua premisele, concluziile și relațiile explicite."
    }
}
