package com.example.aiassistant

import java.util.Locale

object FactCheckEngine {
    data class Check(
        val supported: Int,
        val weak: Int,
        val conflict: Int,
        val unknown: Int,
        val report: String
    )

    fun check(answer: String, evidence: List<WebEvidence>): Check {
        val claims = splitClaims(answer)
        if (claims.isEmpty()) {
            return Check(0, 0, 0, 0, "Nu au fost detectate afirmații suficient de lungi pentru verificare.")
        }

        var supported = 0
        var weak = 0
        var conflict = 0
        var unknown = 0
        val details = StringBuilder()

        claims.take(20).forEachIndexed { index, claim ->
            val result = EvidenceReasoningEngine.checkClaim(claim, evidence)
            when (result.label) {
                "SUPPORTED" -> supported++
                "WEAK" -> weak++
                "CONFLICT" -> conflict++
                else -> unknown++
            }

            details.append(index + 1)
                .append(". ")
                .append(result.icon)
                .append(' ')
                .append(result.labelText)
                .append(" — scor ")
                .append(result.score)
                .append("%")
                .append('\n')
                .append("   ")
                .append(claim.take(320))
                .append('\n')
                .append("   Surse relevante: ")
                .append(result.sourceCount)
                .append(" | domenii: ")
                .append(result.domainCount)
                .append(" | ")
                .append(result.explanation)
                .append('\n')
        }

        val report = buildString {
            append("🔎 ADVANCED FACT CHECK\n\n")
            append("Afirmații analizate: ").append(claims.size).append('\n')
            append("✅ Susținute: ").append(supported).append('\n')
            append("🟡 Susținere limitată: ").append(weak).append('\n')
            append("⚠️ Posibil conflict: ").append(conflict).append('\n')
            append("❓ Necunoscut: ").append(unknown).append("\n\n")
            append(details)
            append("\nMetodă: potrivire normalizată de concepte + scor de sursă + diversitate de domenii "
                + "+ semnale de contradicție. Rezultatul este un filtru automat de evidență, nu o certificare umană.")
        }

        return Check(supported, weak, conflict, unknown, report)
    }

    private fun splitClaims(text: String): List<String> =
        text.replace(Regex("(?m)^\\s*[-•]\\s*"), "")
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 45 }
            .filterNot { it.startsWith("http://") || it.startsWith("https://") }
            .filterNot { it.startsWith("🔎") || it.startsWith("📌") }
            .distinct()
            .take(20)

    private val STOP = setOf(
        "care","este","sunt","pentru","despre","acest","această","aceste","acestea",
        "unei","unui","mai","foarte","poate","fost","din","în","și","sau","with","from",
        "that","this","there","their","have","will","about","into","were","been","than",
        "the","and","for","are","was","has","had"
    )
}
