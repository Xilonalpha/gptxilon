package com.example.aiassistant

import java.net.URI
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ResearchEngine(
    private val web: WebFallbackEngine,
    private val webMemory: WebKnowledgeMemory? = null
) {

    data class Report(
        val answer: String,
        val evidence: List<WebEvidence>,
        val queries: List<String>,
        val engines: List<String>
    )

    private val executor = Executors.newFixedThreadPool(5)

    fun research(
        query: String,
        deep: Boolean = true
    ): Report {

        val q = query.trim()

        if (q.isBlank()) {
            return Report(
                "Întrebarea este goală.",
                emptyList(),
                emptyList(),
                emptyList()
            )
        }

        val queries = buildQueries(q, deep)

        val tasks = queries.map { candidate ->
            Callable {
                try {
                    web.ask(candidate)
                } catch (_: Exception) {
                    null
                }
            }
        }

        val results = try {
            executor.invokeAll(tasks)
                .mapNotNull {
                    try {
                        it.get()
                    } catch (_: Exception) {
                        null
                    }
                }
        } catch (_: Exception) {
            emptyList()
        }

        val engines = linkedSetOf<String>()
        val all = mutableListOf<WebEvidence>()

        results.forEach { result ->
            engines += result.enginesTried
            all += result.evidence
        }

        val unique = LinkedHashMap<String, WebEvidence>()

        all.forEach { item ->
            if (!isUsableEvidence(item)) return@forEach

            val normalized = normalizeUrl(item.url)

            if (
                normalized.isNotBlank() &&
                !unique.containsKey(normalized)
            ) {
                unique[normalized] = item
            }
        }

        val evidence = unique.values
            .sortedWith(
                compareByDescending<WebEvidence> {
                    sourceQuality(it)
                }
                    .thenByDescending {
                        it.content.length
                    }
                    .thenBy {
                        it.title.lowercase(Locale.ROOT)
                    }
            )
            .take(if (deep) 20 else 12)

        webMemory?.learn(
            q,
            evidence
        )

        val reasoning =
            EvidenceReasoningEngine.analyze(
                q,
                evidence
            )

        return Report(
            buildAnswer(
                q,
                evidence,
                queries,
                engines.toList(),
                reasoning
            ),
            evidence,
            queries,
            engines.toList()
        )
    }

    private fun buildQueries(
        q: String,
        deep: Boolean
    ): List<String> {

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

        append("Întrebare: ")
            .append(query)
            .append('\n')

        append("Treceri de cercetare: ")
            .append(queries.size)
            .append('\n')

        append("Motoare încercate: ")
            .append(
                if (engines.isEmpty()) {
                    "—"
                } else {
                    engines.joinToString(", ")
                }
            )
            .append('\n')

        append("Surse unice analizate: ")
            .append(evidence.size)
            .append('\n')

        append("Surse distincte: ")
            .append(reasoning.distinctDomains)
            .append('\n')

        append("Acoperire de evidență: ")
            .append(reasoning.coverage)
            .append("%\n")

        append("Independență estimată: ")
            .append(reasoning.independence)
            .append("%\n\n")

        if (evidence.isEmpty()) {
            append(
                "Nu am obținut suficiente articole sau documente " +
                    "publice verificabile. Nu voi inventa un răspuns " +
                    "din paginile motoarelor de căutare.\n"
            )

            return@buildString
        }

        append("🧠 ANALIZĂ A EVIDENȚEI\n")
        append(reasoning.summary)
            .append("\n\n")

        if (reasoning.conflicts.isNotEmpty()) {
            append("⚠️ POSIBILE CONTRADICȚII\n")

            reasoning.conflicts
                .take(5)
                .forEach {
                    append("• ")
                        .append(it)
                        .append('\n')
                }

            append('\n')
        }

        append("📚 SURSE ANALIZATE\n")

        evidence
            .take(if (evidence.size > 8) 12 else evidence.size)
            .forEachIndexed { index, item ->

                append("[")
                    .append(index + 1)
                    .append("] ")

                append(item.title)
                    .append('\n')

                append(item.url)
                    .append('\n')

                append(
                    item.content
                        .ifBlank { item.snippet }
                        .take(1000)
                )
                    .append("\n\n")
            }

        append(
            "📌 Metodă: sursele sunt deduplicate și evaluate " +
                "euristic. Scorurile nu reprezintă o dovadă " +
                "matematică. Informațiile sunt memorate împreună " +
                "cu sursa originală."
        )
    }

    private fun isUsableEvidence(
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

        val text = (
            item.title + " " +
                item.snippet + " " +
                item.content
            ).lowercase(Locale.ROOT)

        val noise = listOf(
            "before you continue to google",
            "prima di continuare su google",
            "about duckduckgo",
            "informazioni su duckduckgo",
            "microsoft e i suoi fornitori",
            "cookie policy",
            "cookie settings",
            "privacy policy",
            "terms of service"
        )

        if (noise.count { text.contains(it) } >= 2) {
            return false
        }

        return item.content.trim().length >= 180
    }

    private fun sourceQuality(
        item: WebEvidence
    ): Int {

        val host = try {
            URI(item.url)
                .host
                .orEmpty()
                .lowercase(Locale.ROOT)
        } catch (_: Exception) {
            ""
        }

        var score = 0

        if (
            host.endsWith(".gov") ||
            host.contains(".gov.") ||
            host.endsWith(".edu") ||
            host.contains(".edu.") ||
            host.endsWith(".int")
        ) {
            score += 40
        }

        if (
            host == "who.int" ||
            host.endsWith(".who.int") ||
            host.endsWith("nasa.gov") ||
            host.endsWith("esa.int")
        ) {
            score += 15
        }

        if (item.content.length > 1000) {
            score += 20
        }

        if (item.snippet.length > 120) {
            score += 5
        }

        return score
    }

    private fun normalizeUrl(
        url: String
    ): String =
        try {
            val u = URI(url)

            (
                u.host.orEmpty()
                    .lowercase(Locale.ROOT) +
                    u.path.orEmpty().trimEnd('/')
            ).trim()
        } catch (_: Exception) {
            url.substringBefore("#")
                .trimEnd('/')
                .lowercase(Locale.ROOT)
        }
}
