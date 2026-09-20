package com.example.aiassistant

object SelfReflectionEngine {
    data class Result(val answer: String, val score: Int, val changed: Boolean, val notes: List<String>)

    fun reflect(input: String, answer: String, confidence: Int, verification: VerificationEngine.Result): Result {
        if (answer.isBlank()) return Result("Nu am putut produce un răspuns local verificabil.", 10, true, listOf("empty"))
        var out = answer.trim()
        val notes = mutableListOf<String>()
        if (verification.warnings.isNotEmpty()) {
            notes += verification.warnings
            out += "\n\n⚠️ Verificare locală: ${verification.warnings.joinToString("; ")}"
        }
        if (confidence < 50) out += "\n\nNivel de încredere local: scăzut — este necesară verificare suplimentară."
        val changed = out != answer.trim()
        return Result(out, verification.score, changed, notes)
    }
}
