package com.example.aiassistant

import java.net.URI
import java.util.LinkedHashMap
import java.util.Locale

class ResearchEngine(private val web: WebFallbackEngine) {
    data class Report(
        val answer: String,
        val evidence: List<WebEvidence>,
        val queries: List<String>,
        val engines: List<String>
    )

    fun research(query: String, deep: Boolean = true): Report {
        val q = query.trim()
        if (q.isBlank()) return Report("Întrebarea este goală.", emptyList(), emptyList(), emptyList())

        val queries = buildQueries(q, deep)
        val all = mutableListOf<WebEvidence>()
        val engines = linkedSetOf<String>()

        for (candidate in queries) {
            val result = try { web.ask(candidate) } catch (_: Exception) { null } ?: continue
            engines += result.enginesTried
            all += result.evidence
        }

        val unique = LinkedHashMap<String, WebEvidence>()
        all.forEach { item ->
            val normalized = normalizeUrl(item.url)
            if (normalized.isNotBlank() && !unique.containsKey(normalized)) {
                unique[normalized] = item
            }
        }

        val evidence = unique.values
            .sortedWith(
                compareByDescending<WebEvidence> { sourceQuality(it) }
                    .thenByDescending { it.content.length }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            .take(if (deep) 20 else 12)

        val reasoning = EvidenceReasoningEngine.analyze(q, evidence)
        return Report(
            buildAnswer(q, evidence, queries, engines.toList(), reasoning),
            evidence,
            queries,
            engines.toList()
        )
    }

    private fun buildQueries(q: String, deep: Boolean): List<String> {
        val list = mutableListOf(q)
        if (deep) {
            list += "$q surse oficiale primare"
            list += "$q date studii documente originale"
            list += "$q analiză independentă surse multiple"
            list += "$q contradicții dezacord verificare"
        } else {
            list += "$q surse oficiale"
            list += "$q verificare"
        }
        return list.distinct()
    }

    private fun buildAnswer(
        query: String,
        evidence: List<WebEvidence>,
        queries: List<String>,
        engines: List<String>,
        reasoning: EvidenceReasoningEngine.Analysis
    ): String = buildString {
        append("🔬 DEEP RESEARCH MODE\n\n")
        append("Întrebare: ").append(query).append('\n')
        append("Treceri de cercetare: ").append(queries.size).append('\n')
        append("Motoare încercate: ").append(if (engines.isEmpty()) "—" else engines.joinToString(", ")).append('\n')
        append("Surse unice analizate: ").append(evidence.size).append('\n')
        append("Surse distincte: ").append(reasoning.distinctDomains).append('\n')
        append("Acoperire de evidență: ").append(reasoning.coverage).append("%\n")
        append("Independență estimată: ").append(reasoning.independence).append("%\n\n")

        if (evidence.isEmpty()) {
            append("Nu am obținut surse publice utilizabile. Nu voi inventa un răspuns.\n")
            return@buildString
        }

        append("🧠 ANALIZĂ A EVIDENȚEI\n")
        append(reasoning.summary).append("\n\n")

        if (reasoning.conflicts.isNotEmpty()) {
            append("⚠️ POSIBILE CONTRADICȚII\n")
            reasoning.conflicts.take(5).forEach { append("• ").append(it).append('\n') }
            append('\n')
        }

        append("📚 SURSE ANALIZATE\n")
        evidence.take(if (evidence.size > 8) 12 else evidence.size).forEachIndexed { index, item ->
            append("[").append(index + 1).append("] ")
            append(item.title).append('\n')
            append(item.url).append('\n')
            append(item.content.ifBlank { item.snippet }.take(1000)).append("\n\n")
        }

        append("📌 Metodă: sursele sunt deduplicate, grupate după domeniu și evaluate după "
            + "acoperirea termenilor, independența domeniilor, semnale de conflict și disponibilitatea "
            + "conținutului. Scorurile sunt euristice și nu reprezintă o dovadă matematică.")
    }

    private fun sourceQuality(item: WebEvidence): Int {
        val u = item.url.lowercase(Locale.ROOT)
        var score = 0
        if (u.contains(".gov") || u.contains(".edu") || u.contains(".int")) score += 40
        if (u.contains("who.int") || u.contains("nasa.gov") || u.contains("esa.int")) score += 15
        if (item.content.length > 1000) score += 20
        if (item.snippet.length > 120) score += 5
        return score
    }

    private fun normalizeUrl(url: String): String = try {
        val u = URI(url)
        (u.host.orEmpty().lowercase(Locale.ROOT) + u.path.orEmpty().trimEnd('/')).trim()
    } catch (_: Exception) {
        url.substringBefore("#").trimEnd('/').lowercase(Locale.ROOT)
    }
}
