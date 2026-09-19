package com.example.aiassistant

class MemoryEngine(
    private val brain: Brain,
    private val intelligence: IntelligenceEngine,
    private val geminiLearning: GeminiLearningEngine? = null
) {
    fun observe(
        userText: String,
        aiText: String,
        geminiResponse: AiResponse? = null
    ) {
        val normalized = userText.trim()
        if (normalized.isBlank()) return

        Regex(
            """(?i)\b(?:proiectul meu este|lucrez la|proiectul)\s+([^.!?]{3,100})"""
        ).find(normalized)?.let {
            intelligence.remember(
                "proiect",
                it.groupValues[1].trim(),
                "conversation",
                70
            )
        }

        Regex(
            """(?i)\b(?:prefer|prefer să|vreau)\s+([^.!?]{3,100})"""
        ).find(normalized)?.let {
            intelligence.remember(
                "preferință",
                it.groupValues[1].trim(),
                "conversation",
                65
            )
        }

        if (normalized.length >= 30) {
            intelligence.remember(
                "ultimul subiect",
                normalized.take(500),
                "conversation",
                45
            )
        }

        /*
         * Învățarea specială Gemini se activează numai când există
         * un AiResponse real venit de la Gemini.
         *
         * Răspunsurile offline/web fallback nu intră în această memorie.
         */
        if (geminiResponse != null) {
            geminiLearning?.learn(normalized, geminiResponse)
        }

        brain.observe(normalized, aiText)
    }

    fun context(query: String): String {
        val graph = intelligence.graphText(query).take(4500)
        val learned = geminiLearning?.context(query).orEmpty().take(2500)

        return buildString {
            if (graph.isNotBlank()) {
                append(graph)
            }

            if (learned.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(learned)
            }
        }.take(7000)
    }

    fun dashboard(): String =
        "🧠 STRUCTURED MEMORY\n" +
            "• Brain facts + corrections\n" +
            "• Project/preferences memory\n" +
            "• Temporal updates\n" +
            "• Confidence + source metadata\n\n" +
            geminiLearning?.dashboard().orEmpty()
}
