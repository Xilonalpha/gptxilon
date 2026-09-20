package com.example.aiassistant

import java.util.Locale

object LogicEngine {

    fun canHandle(input: String): Boolean {
        val s = input.lowercase(Locale.getDefault())

        return s.contains("mai mare decât") ||
                s.contains("mai mic decât") ||
                s.contains("egal cu") ||
                s.contains("compară") ||
                s.contains("adevărat sau fals")
    }

    fun answer(input: String): String {
        val s = input.lowercase(Locale.getDefault())
            .replace(",", ".")

        val comparison = Regex(
            """([0-9.]+)\s*(mai mare decât|mai mic decât|egal cu)\s*([0-9.]+)"""
        ).find(s)

        if (comparison != null) {
            val a = comparison.groupValues[1].toDouble()
            val op = comparison.groupValues[2]
            val b = comparison.groupValues[3].toDouble()

            val result = when (op) {
                "mai mare decât" -> a > b
                "mai mic decât" -> a < b
                else -> a == b
            }

            return if (result) "Adevărat." else "Fals."
        }

        return "Pot verifica comparații numerice simple."
    }
}
