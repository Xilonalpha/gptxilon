package com.example.aiassistant

import java.net.URI
import java.util.Locale
import kotlin.math.roundToInt

object EvidenceReasoningEngine {

    data class Analysis(
        val coverage: Int,
        val independence: Int,
        val distinctDomains: Int,
        val summary: String,
        val conflicts: List<String>
    )

    data class ClaimResult(
        val label: String,
        val labelText: String,
        val icon: String,
        val score: Int,
        val sourceCount: Int,
        val domainCount: Int,
        val explanation: String
    )

    fun analyze(
        query: String,
        evidence: List<WebEvidence>
    ): Analysis {

        if (evidence.isEmpty()) {
            return Analysis(
                0,
                0,
                0,
                "Nu există evidență analizabilă.",
                emptyList()
            )
        }

        val validEvidence = evidence.filter {
            isRealEvidence(it)
        }

        if (validEvidence.isEmpty()) {
            return Analysis(
                0,
                0,
                0,
                "Nu există articole sau documente publice verificabile.",
                emptyList()
            )
        }

        val domains = validEvidence
            .mapNotNull { domain(it.url) }
            .distinct()

        val queryTerms = terms(query)

        val coverageValues = validEvidence.map {
            overlap(
                queryTerms,
                terms(
                    it.content + " " +
                        it.snippet + " " +
                        it.title
                )
            )
        }

        val coverage =
            (
                coverageValues
                    .average()
                    .coerceIn(0.0, 1.0) * 100.0
                ).roundToInt()

        /*
         * Acesta este un indicator de diversitate a domeniilor,
         * nu o demonstrație de independență editorială.
         *
         * Un articol de la Reuters și o copie sindicalizată
         * pe alt domeniu pot avea domenii diferite, dar nu sunt
         * automat două dovezi independente.
         */
        val domainDiversity =
            domains.size.toDouble() /
                validEvidence.size.coerceAtLeast(1).toDouble()

        val independence =
            (
                domainDiversity
                    .coerceIn(0.0, 1.0) * 100.0
                ).roundToInt()

        val conflicts = findConflicts(validEvidence)

        val summary = buildString {
            append("Am analizat ")
            append(validEvidence.size)
            append(" articole/documente din ")
            append(domains.size)
            append(" domenii. ")

            when {
                domains.size >= 3 -> {
                    append(
                        "Există diversitate de domenii. " +
                            "Acest indicator măsoară diversitatea " +
                            "domeniilor, nu dovedește singur " +
                            "independența editorială."
                    )
                }

                domains.size == 2 -> {
                    append(
                        "Există două domenii distincte, dar " +
                            "independența informațiilor trebuie " +
                            "verificată și prin conținut."
                    )
                }

                else -> {
                    append(
                        "Există un singur domeniu distinct; " +
                            "nu există diversitate suficientă " +
                            "pentru o estimare solidă a independenței."
                    )
                }
            }

            if (conflicts.isNotEmpty()) {
                append(
                    " Au fost detectate semnale textuale " +
                        "care merită verificate manual."
                )
            }
        }

        return Analysis(
            coverage = coverage,
            independence = independence,
            distinctDomains = domains.size,
            summary = summary,
            conflicts = conflicts
        )
    }

    fun checkClaim(
        claim: String,
        evidence: List<WebEvidence>
    ): ClaimResult {

        val claimTerms = terms(claim)

        if (claimTerms.isEmpty()) {
            return ClaimResult(
                "UNKNOWN",
                "NEVERIFICABIL",
                "❓",
                0,
                0,
                0,
                "Nu au fost extrase concepte utile."
            )
        }

        val validEvidence = evidence.filter {
            isRealEvidence(it)
        }

        data class Match(
            val source: WebEvidence,
            val overlap: Double
        )

        val matches = validEvidence
            .mapNotNull { source ->
                val sourceTerms = terms(
                    source.title + " " +
                        source.snippet + " " +
                        source.content
                )

                val value = overlap(
                    claimTerms,
                    sourceTerms
                )

                if (value >= 0.22) {
                    Match(source, value)
                } else {
                    null
                }
            }
            .sortedByDescending {
                it.overlap
            }

        val domains = matches
            .mapNotNull {
                domain(it.source.url)
            }
            .distinct()

        val strong = matches.count {
            it.overlap >= 0.45
        }

        val medium = matches.count {
            it.overlap >= 0.30
        }

        val contradiction =
            hasContradictionSignal(claim) ||
                matches.any {
                    hasContradictionSignal(
                        it.source.content + " " +
                            it.source.snippet
                    )
                }

        val score = (
            strong * 24 +
                medium * 10 +
                domains.size * 10 +
                if (
                    matches.any {
                        it.source.content.length > 1000
                    }
                ) {
                    8
                } else {
                    0
                }
            ).coerceIn(0, 100)

        return when {
            contradiction && matches.isNotEmpty() -> {
                ClaimResult(
                    "CONFLICT",
                    "POSIBIL CONFLICT / CONTEXT",
                    "⚠️",
                    score,
                    matches.size,
                    domains.size,
                    "Există potrivire cu surse, dar și un semnal de negație/contrast."
                )
            }

            strong >= 2 && domains.size >= 2 -> {
                ClaimResult(
                    "SUPPORTED",
                    "SUSȚINUT MULTI-SURSĂ",
                    "✅",
                    score,
                    matches.size,
                    domains.size,
                    "Potrivire puternică în mai multe surse și domenii."
                )
            }

            matches.isNotEmpty() -> {
                ClaimResult(
                    "WEAK",
                    "SUSȚINERE LIMITATĂ",
                    "🟡",
                    score,
                    matches.size,
                    domains.size,
                    "Există evidență relevantă, dar independența sau potrivirea nu este suficientă."
                )
            }

            else -> {
                ClaimResult(
                    "UNKNOWN",
                    "NEVERIFICAT",
                    "❓",
                    0,
                    0,
                    0,
                    "Nu a fost găsită suficientă evidență relevantă."
                )
            }
        }
    }

    private fun isRealEvidence(
        item: WebEvidence
    ): Boolean {
        val uri = try {
            URI(item.url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        if (host.isBlank()) return false

        val searchHosts = setOf(
            "google.com",
            "www.google.com",
            "bing.com",
            "www.bing.com",
            "duckduckgo.com",
            "www.duckduckgo.com",
            "html.duckduckgo.com"
        )

        if (
            host in searchHosts ||
            host.endsWith(".google.com") ||
            host.endsWith(".bing.com")
        ) {
            return false
        }

        val path = uri.path
            ?.lowercase(Locale.ROOT)
            .orEmpty()
            .trimEnd('/')

        if (path.isBlank()) return false

        return item.content.trim().length >= 180
    }

    private fun terms(
        text: String
    ): Set<String> =
        Regex("[\\p{L}\\p{Nd}]{4,}")
            .findAll(normalize(text))
            .map {
                it.value
            }
            .filterNot {
                it in STOP
            }
            .toSet()

    private fun normalize(
        text: String
    ): String =
        text.lowercase(Locale.ROOT)
            .replace('ă', 'a')
            .replace('â', 'a')
            .replace('î', 'i')
            .replace('ș', 's')
            .replace('ş', 's')
            .replace('ț', 't')
            .replace('ţ', 't')

    private fun overlap(
        a: Set<String>,
        b: Set<String>
    ): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0

        return a.intersect(b)
            .size
            .toDouble() /
            a.size.toDouble()
    }

    private fun domain(
        url: String
    ): String? =
        try {
            URI(url)
                .host
                ?.lowercase(Locale.ROOT)
                ?.removePrefix("www.")
        } catch (_: Exception) {
            null
        }

    private fun findConflicts(
        evidence: List<WebEvidence>
    ): List<String> {

        val signals = evidence.filter {
            hasContradictionSignal(
                it.content + " " +
                    it.snippet
            )
        }

        return signals
            .take(6)
            .map {
                "${it.title}: conține un semnal de negație/contrast; verifică pasajul original."
            }
    }

    private fun hasContradictionSignal(
        text: String
    ): Boolean =
        Regex(
            "(?i)\\b(" +
                "contradict|" +
                "contradiction|" +
                "disput|" +
                "deny|" +
                "denied|" +
                "false|" +
                "incorrect|" +
                "debunk|" +
                "however|" +
                "but|" +
                "although|" +
                "nu este|" +
                "nu a fost|" +
                "nu există|" +
                "fals|" +
                "incorect|" +
                "contrazice|" +
                "contestat" +
                ")\\b"
        ).containsMatchIn(text)

    private val STOP = setOf(
        "care",
        "este",
        "sunt",
        "pentru",
        "despre",
        "acest",
        "aceasta",
        "aceste",
        "acestea",
        "unei",
        "unui",
        "mai",
        "foarte",
        "poate",
        "fost",
        "din",
        "intr",
        "între",
        "prin",
        "după",
        "the",
        "and",
        "for",
        "with",
        "from",
        "that",
        "this",
        "there",
        "their",
        "have",
        "will",
        "about",
        "into",
        "were",
        "been",
        "than",
        "also",
        "over",
        "under"
    )
}
