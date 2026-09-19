package com.example.aiassistant

import java.util.Locale

object FactCheckEngine {
    data class Check(val supported: Int, val weak: Int, val conflict: Int, val unknown: Int, val report: String)

    fun check(answer: String, evidence: List<WebEvidence>): Check {
        val claims = splitClaims(answer)
        if (claims.isEmpty()) return Check(0, 0, 0, 0, "Nu au fost detectate afirmații verificabile.")
        var supported = 0
        var weak = 0
        var conflict = 0
        var unknown = 0
        val details = StringBuilder()
        claims.take(12).forEachIndexed { index, claim ->
            val terms = keywords(claim)
            val matches = evidence.count { source ->
                val text = (source.title + " " + source.snippet + " " + source.content).lowercase(Locale.ROOT)
                terms.count { text.contains(it) } >= terms.size.coerceAtLeast(2) / 2
            }
            val conflictSignal = Regex(
                "(?i)\b(contradict|disput|deny|denied|false|incorrect|nu este|nu a fost|nu există|however|but)\b"
            ).containsMatchIn(claim)
            val label = when {
                conflictSignal && matches > 0 -> { conflict++; "⚠️ CONFLICT/CONTEXT" }
                matches >= 2 -> { supported++; "✅ SUSȚINUT DE MAI MULTE SURSE" }
                matches == 1 -> { weak++; "🟡 SUSȚINERE LIMITATĂ" }
                else -> { unknown++; "❓ NEVERIFICAT" }
            }
            details.append(index + 1).append(". ").append(label).append(": ").append(claim.take(260)).append('
')
        }
        val report = buildString {
            append("🔎 FACT CHECK

")
            append("Afirmații analizate: ").append(claims.size).append('
')
            append("✅ Susținute de mai multe surse: ").append(supported).append('
')
            append("🟡 Susținere limitată: ").append(weak).append('
')
            append("⚠️ Conflict/context: ").append(conflict).append('
')
            append("❓ Neverificate: ").append(unknown).append("

")
            append(details)
            append("
Acesta este un filtru de evidență local; nu transformă automat potrivirea de cuvinte în dovadă definitivă.")
        }
        return Check(supported, weak, conflict, unknown, report)
    }

    private fun splitClaims(text: String): List<String> =
        text.replace(Regex("(?m)^\s*[-•]\s*"), "")
            .split(Regex("(?<=[.!?])\s+|\n+"))
            .map { it.trim() }
            .filter { it.length >= 45 }
            .filterNot { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .take(20)

    private fun keywords(text: String): List<String> =
        Regex("[\p{L}\p{Nd}]{4,}")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filterNot { it in STOP }
            .distinct()
            .take(10)
            .toList()

    private val STOP = setOf("care","este","sunt","pentru","despre","acest","această","with","from","that","this","there","their","have","will","unei","unui","mai","foarte","poate","fost")
}
