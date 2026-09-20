package com.example.aiassistant

object ConversationEngine {
    fun canHandle(input: String): Boolean {
        val s = input.trim().lowercase()
        return s in setOf("salut", "buna", "bună", "hei", "hello", "hi") ||
                s.contains("mulțumesc") || s.contains("multumesc") || s.contains("mersi") ||
                s.contains("cine ești") || s.contains("cine esti") ||
                s.contains("ce ești") || s.contains("ce esti")
    }

    fun answer(input: String): String {
        val s = input.lowercase()
        return when {
            s.contains("mulțumesc") || s.contains("multumesc") || s.contains("mersi") ->
                "Cu plăcere! 😊"
            s.contains("cine ești") || s.contains("cine esti") ||
                    s.contains("ce ești") || s.contains("ce esti") ->
                "Sunt OfflineBrain: un motor local simbolic și procedural. Pot analiza, raționa, calcula și verifica fără API sau model neural."
            else -> "Salut! 👋 Sunt OfflineBrain. Pot calcula, analiza, deduce și verifica local."
        }
    }
}
