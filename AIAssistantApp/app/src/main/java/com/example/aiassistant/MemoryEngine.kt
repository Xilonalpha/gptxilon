package com.example.aiassistant

class MemoryEngine(
    private val brain: Brain,
    private val intelligence: IntelligenceEngine,
    private val geminiLearning: GeminiLearningEngine? = null,
    private val webMemory: WebKnowledgeMemory? = null
) {

    fun observe(
        userText: String,
        aiText: String,
        geminiResponse: AiResponse? = null,
        webEvidence: List<WebEvidence> = emptyList()
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

        if (geminiResponse != null) {
            geminiLearning?.learn(
                normalized,
                geminiResponse
            )
        }

        if (webEvidence.isNotEmpty()) {
            webMemory?.learn(
                normalized,
                webEvidence
            )
        }

        brain.observe(
            normalized,
            aiText
        )
    }

    fun context(
        query: String
    ): String {

        val graph =
            intelligence
                .graphText(query)
                .take(3500)

        val learned =
            geminiLearning
                ?.context(query)
                .orEmpty()
                .take(1800)

        val web =
            webMemory
                ?.context(query)
                .orEmpty()
                .take(2500)

        return buildString {

            if (graph.isNotBlank()) {
                append(graph)
            }

            if (learned.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(learned)
            }

            if (web.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(web)
            }

        }.take(7000)
    }

    fun dashboard(): String =
        "🧠 STRUCTURED MEMORY\n" +
            "• Brain facts + corrections\n" +
            "• Project/preferences memory\n" +
            "• Temporal updates\n" +
            "• Confidence + source metadata\n\n" +
            geminiLearning?.dashboard().orEmpty() +
            "\n\n" +
            webMemory?.dashboard().orEmpty()
}
