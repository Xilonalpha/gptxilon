package com.example.aiassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 🧠 Creierul local care învață și evoluează.
 * Persistă: fapte, corecții, XP, feedback, tokeni consumați, jurnal de evoluție.
 * Tot ce învață se injectează în system prompt la fiecare conversație.
 */
class Brain(context: Context) {

    private val file = File(context.filesDir, "brain.json")

    var facts = mutableListOf<String>()
    var corrections = mutableListOf<String>()
    var experience = 0
    var feedbackPositive = 0
    var feedbackNegative = 0
    var tokensTotal = 0L
    var evolutionLog = mutableListOf<String>()

    init { load() }

    val level: Int get() = 1 + experience / 25

    fun observe(userMsg: String, aiReply: String, tokens: Int = 0) {
        experience += 1
        tokensTotal += tokens

        Regex("(?:mă numesc|numele meu este) ([A-ZĂÂÎȘȚ][a-zăâîșț]+)").find(userMsg)?.let {
            val fact = "Utilizatorul se numește ${it.groupValues[1]}"
            if (fact !in facts) { facts.add(fact); log("Am învățat: $fact") }
        }
        Regex("îmi (?:place|plac) ([a-zăâîșț0-9 ]{2,30})").find(userMsg.lowercase())?.let {
            val fact = "Utilizatorului îi place ${it.groupValues[1].trim()}"
            if (fact !in facts) { facts.add(fact); log("Am învățat: $fact") }
        }
        Regex("sunt din ([a-zăâîșț ]{2,30})").find(userMsg.lowercase())?.let {
            val fact = "Utilizatorul este din ${it.groupValues[1].trim()}"
            if (fact !in facts) { facts.add(fact); log("Am învățat: $fact") }
        }
        save()
    }

    fun learnFromFeedback(userMsg: String, aiReply: String, positive: Boolean) {
        if (positive) {
            feedbackPositive += 1
            experience += 2
            log("Feedback pozitiv — consolidatez tiparul.")
        } else {
            feedbackNegative += 1
            experience += 3
            corrections.add("Utilizatorul a respins răspunsul la «$userMsg». Data viitoare: verifică sursele, fii mai precis și mai direct.")
            log("Am învățat dintr-o greșeală.")
        }
        save()
    }

    fun brainContext(): String {
        val sb = StringBuilder()
        if (facts.isNotEmpty()) sb.append("Fapte: ").append(facts.joinToString("; ")).append(". ")
        if (corrections.isNotEmpty()) sb.append("Lecții: ").append(corrections.takeLast(5).joinToString("; ")).append(". ")
        sb.append("Nivel: $level (${experience} XP).")
        return sb.toString()
    }

    fun stats(): String = buildString {
        append("🧠 CREIERUL MEU\n\n")
        append("Nivel: $level\n")
        append("Experiență: $experience XP\n")
        append("Tokeni consumați: $tokensTotal\n")
        append("Feedback pozitiv: $feedbackPositive\n")
        append("Feedback negativ: $feedbackNegative\n")
        append("Fapte învățate: ${facts.size}\n")
        append("Corecții: ${corrections.size}\n\n")
        append("TOT ce văd mai sus se injectează în fiecare conversație. ")
        append("Continuă să mă folosești și evoluez treptat.")
    }

    private fun log(entry: String) {
        evolutionLog.add("${System.currentTimeMillis()}: $entry")
        if (evolutionLog.size > 100) evolutionLog.removeAt(0)
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val json = JSONObject(file.readText())
            facts = json.optJSONArray("facts")?.toList() ?: mutableListOf()
            corrections = json.optJSONArray("corrections")?.toList() ?: mutableListOf()
            evolutionLog = json.optJSONArray("log")?.toList() ?: mutableListOf()
            experience = json.optInt("xp")
            feedbackPositive = json.optInt("pos")
            feedbackNegative = json.optInt("neg")
            tokensTotal = json.optLong("tokens")
        } catch (_: Exception) { }
    }

    private fun save() {
        file.writeText(JSONObject()
            .put("facts", JSONArray(facts))
            .put("corrections", JSONArray(corrections))
            .put("log", JSONArray(evolutionLog))
            .put("xp", experience)
            .put("pos", feedbackPositive)
            .put("neg", feedbackNegative)
            .put("tokens", tokensTotal)
            .toString())
    }

    private fun JSONArray.toList(): MutableList<String> {
        val out = mutableListOf<String>()
        for (i in 0 until length()) out.add(getString(i))
        return out
    }
}
