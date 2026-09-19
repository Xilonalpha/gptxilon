package com.example.aiassistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Local intelligence layer: evidence, temporal knowledge, graph and decision memory. */
class IntelligenceEngine(private val root: File) {
    private val file = File(root, "intelligence.json")
    private val facts = mutableMapOf<String, JSONObject>()
    private val decisions = mutableListOf<JSONObject>()

    init { load() }

    data class Evidence(val confidence: Int, val label: String, val explanation: String)

    fun evidence(sources: List<String>, answer: String): Evidence {
        val clean = sources.filter { it.isNotBlank() }.distinct()
        val domains = clean.mapNotNull { Regex("https?://([^/]+)").find(it)?.groupValues?.get(1)?.lowercase(Locale.ROOT) }.distinct()
        val conflict = Regex("(?i)\\b(contradict|conflict|disputed|unclear|mixed evidence|uncertain|nu există suficiente dovezi)\\b").containsMatchIn(answer)
        val hasDates = Regex("\\b20\\d{2}\\b").containsMatchIn(answer)
        var score = 38 + clean.size.coerceAtMost(5) * 9 + domains.size.coerceAtMost(4) * 5
        if (hasDates) score += 4
        if (conflict) score -= 18
        if (clean.isEmpty()) score -= 8
        score = score.coerceIn(10, 97)
        val label = when { score >= 85 -> "HIGH"; score >= 65 -> "MEDIUM"; else -> "LOW" }
        val explanation = when {
            clean.isEmpty() -> "Răspuns fără surse web atașate; verificarea este limitată."
            conflict -> "${clean.size} surse, ${domains.size} domenii; au fost detectate semnale de conflict/incertitudine."
            else -> "${clean.size} surse distincte din ${domains.size.coerceAtLeast(1)} domenii; scorul măsoară calitatea dovezilor disponibile, nu adevărul absolut."
        }
        return Evidence(score, label, explanation)
    }

    fun remember(topic: String, value: String, source: String = "conversation", confidence: Int = 50) {
        val key = topic.trim().lowercase(Locale.ROOT).take(120)
        if (key.isBlank() || value.isBlank()) return
        facts[key] = JSONObject()
            .put("topic", topic.trim())
            .put("value", value.trim().take(1800))
            .put("source", source)
            .put("confidence", confidence.coerceIn(0,100))
            .put("updated", System.currentTimeMillis())
        save()
    }

    fun rememberDecision(title: String, outcome: String, rationale: String) {
        decisions.add(JSONObject().put("title", title).put("outcome", outcome).put("rationale", rationale).put("time", System.currentTimeMillis()))
        while (decisions.size > 100) decisions.removeAt(0)
        save()
    }

    fun graph(): List<JSONObject> = facts.values.sortedByDescending { it.optLong("updated") }

    fun graphText(query: String = ""): String {
        val q = query.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length >= 3 }
        val selected = facts.values.sortedByDescending { obj ->
            val text = (obj.optString("topic") + " " + obj.optString("value")).lowercase(Locale.ROOT)
            q.count { text.contains(it) } * 1000L + obj.optLong("updated") / 1_000_000L
        }
        return buildString {
            if (selected.isEmpty()) append("Knowledge graph gol.")
            else selected.take(25).forEach {
                append("• ").append(it.optString("topic")).append(" → ").append(it.optString("value"))
                    .append(" [confidence ").append(it.optInt("confidence", 50)).append("%, ")
                    .append(it.optString("source")).append("]\n")
            }
        }
    }

    fun dashboard(brain: Brain): String = buildString {
        append("⚡ INTELLIGENCE CORE 5.0\n\n")
        append("Memory: ${brain.facts.size + brain.corrections.size} elemente\n")
        append("Knowledge Graph: ${facts.size} noduri\n")
        append("Decision Ledger: ${decisions.size} decizii\n")
        append("Evidence Verification: ACTIV\n")
        append("Temporal Knowledge: ACTIV\n")
        append("Agent Orchestration: ACTIV\n")
        append("Multimodal Files: ACTIV\n")
        append("Live Watchers: ACTIV\n")
        append("Model: Gemini 3.8 Flash\n")
        append("API target: Android 16 / API 36\n")
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val j = JSONObject(file.readText())
            val a = j.optJSONArray("facts") ?: JSONArray()
            for (i in 0 until a.length()) { val o=a.getJSONObject(i); facts[o.optString("topic").lowercase(Locale.ROOT)] = o }
            val d=j.optJSONArray("decisions") ?: JSONArray()
            for(i in 0 until d.length()) decisions += d.getJSONObject(i)
        } catch (_: Exception) { }
    }

    private fun save() {
        try { file.writeText(JSONObject().put("facts", JSONArray(facts.values.toList())).put("decisions", JSONArray(decisions)).toString()) } catch (_: Exception) { }
    }
}
