package com.example.aiassistant

object HypothesisEngine {
    data class Hypothesis(val text: String, val status: String, val confidence: Int)
    data class Result(val handled: Boolean, val answer: String, val hypotheses: List<Hypothesis>)

    fun generate(input: String): Result {
        val q = input.trim()
        if (!Regex("(?i)\\b(ipotez|presupun|posibil|ce s-ar întâmpla|ce s-ar intampla|explicație alternativă|explicatie alternativa)\\b").containsMatchIn(q)) return Result(false, "", emptyList())
        val core = q.replace(Regex("(?i)^(ce|care)\\s+(ar putea|este posibil să|este posibila)\\s*"), "").trim()
        val list = listOf(
            Hypothesis("H1: explicația directă sugerată de enunț — $core", "NEVERIFICATĂ", 35),
            Hypothesis("H2: există un factor alternativ care produce același efect — $core", "NEVERIFICATĂ", 25),
            Hypothesis("H3: observația poate proveni din zgomot, eroare de măsurare sau context incomplet — $core", "NEVERIFICATĂ", 20)
        )
        return Result(true, buildString { append("Ipoteze generate fără a le prezenta ca fapte:\n"); list.forEach { append("• ").append(it.text).append(" [").append(it.confidence).append("% prior euristic]\n") }; append("Următorul pas corect este testarea fiecărei ipoteze prin dovezi sau predicții verificabile.") }, list)
    }
}
