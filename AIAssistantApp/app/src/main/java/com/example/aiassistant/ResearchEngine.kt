package com.example.aiassistant

import java.net.URI
import java.util.LinkedHashMap
import java.util.Locale

class ResearchEngine(private val web: WebFallbackEngine) {
    data class Report(val answer: String, val evidence: List<WebEvidence>, val queries: List<String>, val engines: List<String>)

    fun research(query: String, deep: Boolean = true): Report {
        val q = query.trim()
        if (q.isBlank()) return Report("Întrebarea este goală.", emptyList(), emptyList(), emptyList())
        val queries = buildList {
            add(q)
            if (deep) {
                add(q + " surse oficiale")
                add(q + " analiză independentă")
            }
        }.distinct()
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
            if (normalized.isNotBlank() && !unique.containsKey(normalized)) unique[normalized] = item
        }
        val evidence = unique.values.sortedWith(
            compareByDescending<WebEvidence> { it.content.length }.thenBy { it.title.lowercase(Locale.ROOT) }
        ).take(12)
        return Report(buildAnswer(q, evidence, queries, engines.toList()), evidence, queries, engines.toList())
    }

    private fun buildAnswer(query: String, evidence: List<WebEvidence>, queries: List<String>, engines: List<String>): String = buildString {
        append("🔬 RESEARCH MODE

")
        append("Întrebare: ").append(query).append('
')
        append("Treceri de cercetare: ").append(queries.size).append('
')
        append("Motoare încercate: ").append(if (engines.isEmpty()) "—" else engines.joinToString(", ")).append('
')
        append("Surse unice analizate: ").append(evidence.size).append("

")
        if (evidence.isEmpty()) {
            append("Nu am obținut surse publice utilizabile. Nu voi inventa un răspuns.
")
            return@buildString
        }
        evidence.take(8).forEachIndexed { index, item ->
            append("[").append(index + 1).append("] ").append(item.title).append('
')
            append(item.url).append('
')
            append(item.content.ifBlank { item.snippet }.take(1200)).append("

")
        }
        append("📌 Metodă: sursele au fost deduplicate după URL și păstrate separat. ")
        append("Conținutul poate fi incomplet dacă site-ul blochează WebView, cere autentificare sau livrează o pagină dinamică.")
    }

    private fun normalizeUrl(url: String): String = try {
        val u = URI(url)
        (u.host.orEmpty().lowercase(Locale.ROOT) + u.path.orEmpty().trimEnd('/')).trim()
    } catch (_: Exception) {
        url.substringBefore("#").trimEnd('/').lowercase(Locale.ROOT)
    }
}
