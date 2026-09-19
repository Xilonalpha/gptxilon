package com.example.aiassistant

class MemoryEngine(private val brain: Brain, private val intelligence: IntelligenceEngine) {
    fun observe(userText: String, aiText: String) {
        val normalized = userText.trim()
        if (normalized.isBlank()) return
        Regex("(?i)\b(?:proiectul meu este|lucrez la|proiectul)\s+([^.!?]{3,100})").find(normalized)?.let {
            intelligence.remember("proiect", it.groupValues[1].trim(), "conversation", 70)
        }
        Regex("(?i)\b(?:prefer|prefer să|vreau)\s+([^.!?]{3,100})").find(normalized)?.let {
            intelligence.remember("preferință", it.groupValues[1].trim(), "conversation", 65)
        }
        if (normalized.length >= 30) intelligence.remember("ultimul subiect", normalized.take(500), "conversation", 45)
        brain.observe(normalized, aiText)
    }
    fun context(query: String): String = intelligence.graphText(query).take(7000)
    fun dashboard(): String = "🧠 Structured Memory
• Brain facts + corrections
• Project/preferences memory
• Temporal updates
• Confidence + source metadata"
}
