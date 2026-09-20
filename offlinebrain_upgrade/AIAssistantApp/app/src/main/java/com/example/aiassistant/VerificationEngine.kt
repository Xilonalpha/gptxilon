package com.example.aiassistant

import java.util.Locale

object VerificationEngine {
    data class Result(val score:Int,val warnings:List<String>,val verified:Boolean)

    fun canHandle(input:String):Boolean {
        val s=input.lowercase(Locale.ROOT)
        return listOf("verifică","verifica","contradicție","contradictie","este adevărat","este adevarat",
            "este fals","este falsă","este falsa","dovadă","dovada","check","verify","true or false")
            .any { s.contains(it) } ||
            Regex("""\b[a-zăâîșț][a-zăâîșț0-9_]*\s*=\s*-?\d""").containsMatchIn(s)
    }

    fun check(input:String,answer:String,confidence:Int):Result {
        val warnings=mutableListOf<String>()
        if(answer.isBlank()) warnings+="Răspuns gol."
        if(Regex("(?i)\\b(100% sigur|sigur fără dubiu|certitudine absolută)\\b").containsMatchIn(answer))
            warnings+="Limbaj de certitudine excesivă."
        val numbers=Regex("-?\\d+(?:[.,]\\d+)?").findAll(answer)
            .map { it.value.replace(',','.').toDoubleOrNull() }.filterNotNull().toList()
        if(numbers.any { it.isNaN() || it.isInfinite() }) warnings+="Rezultat numeric invalid."
        val assignments=Regex("""\b([A-Za-zĂÂÎȘȚăâîșț][A-Za-z0-9ĂÂÎȘȚăâîșț_]*)\s*=\s*(-?\d+(?:[.,]\d+)?)""")
            .findAll(input).groupBy { it.groupValues[1].lowercase(Locale.ROOT) }
        if(assignments.values.any { it.map { m -> m.groupValues[2].replace(',','.').toDoubleOrNull() }.distinct().size>1 })
            warnings+="Aceeași variabilă are valori incompatibile în enunț."
        if(input.contains("=") && answer.contains("contradic",true)) warnings+="Enunțul conține o posibilă contradicție."
        val score=(confidence-warnings.size*15).coerceIn(0,100)
        return Result(score,warnings,warnings.isEmpty() && score>=55)
    }

    fun answer(input:String):String? {
        if(!canHandle(input)) return null
        val structural=mutableListOf<String>()
        val assignments=Regex("""\b([A-Za-zĂÂÎȘȚăâîșț][A-Za-z0-9ĂÂÎȘȚăâîșț_]*)\s*=\s*(-?\d+(?:[.,]\d+)?)""")
            .findAll(input).groupBy { it.groupValues[1].lowercase(Locale.ROOT) }
        assignments.filter { it.value.map { m -> m.groupValues[2].replace(',','.') }.distinct().size>1 }
            .keys.forEach { structural+="contradicție pentru $it" }
        if(Regex("(?i)\\b(nu|nu este|nu sunt|fals|falsă|falsa)\\b").containsMatchIn(input))
            structural+="enunțul conține negație"
        return if(structural.isEmpty())
            "Verificare locală: nu am detectat o contradicție structurală evidentă. Acest rezultat nu demonstrează singur adevărul în lumea reală."
        else
            "Verificare locală: ${structural.joinToString("; ")}. Acesta este un rezultat structural, nu o verificare externă."
    }
}
