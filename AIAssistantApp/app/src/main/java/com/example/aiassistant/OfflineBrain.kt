package com.example.aiassistant

/**
 * Motor local determinist.
 *
 * Nu este un LLM local.
 * Execută numai operații pe care le poate verifica local.
 *
 * Ordinea:
 * 1. conversație simplă
 * 2. matematică
 * 3. conversii
 * 4. timp/data
 * 5. logică
 * 6. analiză text
 * 7. bază locală de cunoștințe
 * 8. necunoscut -> răspuns explicit
 */
object OfflineBrain {

    fun reply(input: String): String {
        val msg = input.trim()

        if (msg.isBlank()) {
            return "Nu am primit nicio întrebare."
        }

        val normalized = msg.lowercase()

        when {
            isGreeting(normalized) ->
                return "Salut! 👋 Sunt în modul local. Pot calcula, face conversii, verifica logică simplă, analiza texte și răspunde din baza mea locală."

            normalized.contains("mulțumesc") ||
                    normalized.contains("multumesc") ||
                    normalized.contains("mersi") ->
                return "Cu plăcere! 😊"

            normalized.contains("cine ești") ||
                    normalized.contains("cine esti") ||
                    normalized.contains("ce ești") ||
                    normalized.contains("ce esti") ->
                return "Sunt motorul local OfflineBrain. Nu sunt un LLM; execut local calcule, conversii, analiză logică, analiză de text, timp/data și răspunsuri din baza locală."
        }

        if (MathEngine.canHandle(msg)) {
            return MathEngine.calculate(msg)
        }

        if (UnitConversionEngine.canHandle(msg)) {
            return UnitConversionEngine.convert(msg)
        }

        if (TimeEngine.canHandle(msg)) {
            return TimeEngine.answer(msg)
        }

        if (LogicEngine.canHandle(msg)) {
            return LogicEngine.answer(msg)
        }

        if (TextAnalysisEngine.canHandle(msg)) {
            return TextAnalysisEngine.analyze(msg)
        }

        if (KnowledgeEngine.canHandle(msg)) {
            return KnowledgeEngine.answer(msg)
        }

        return """
            Nu am un răspuns verificabil pentru această întrebare în motorul local.

            OfflineBrain poate răspunde local la:
            • calcule matematice
            • conversii de unități
            • timp și dată
            • logică numerică simplă
            • analiză elementară de text
            • câteva concepte din baza locală

            Pentru informații pe care nu le cunosc local este necesar motorul web sau Gemini.
        """.trimIndent()
    }

    fun canHandle(input: String): Boolean {
        val msg = input.trim()
        if (msg.isBlank()) return false

        val normalized = msg.lowercase()

        return isGreeting(normalized) ||
                normalized.contains("mulțumesc") ||
                normalized.contains("multumesc") ||
                normalized.contains("mersi") ||
                normalized.contains("cine ești") ||
                normalized.contains("cine esti") ||
                normalized.contains("ce ești") ||
                normalized.contains("ce esti") ||
                MathEngine.canHandle(msg) ||
                UnitConversionEngine.canHandle(msg) ||
                TimeEngine.canHandle(msg) ||
                LogicEngine.canHandle(msg) ||
                TextAnalysisEngine.canHandle(msg) ||
                KnowledgeEngine.canHandle(msg)
    }

    private fun isGreeting(s: String): Boolean {
        return s == "salut" ||
                s == "bună" ||
                s == "buna" ||
                s == "hei" ||
                s == "hello" ||
                s == "hi"
    }
}
