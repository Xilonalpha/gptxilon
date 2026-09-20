package com.example.aiassistant

import java.util.Locale

object IntentEngine {
    enum class Type { CONVERSATION, CALCULATION, STATISTICS, CONVERSION, TIME, LOGIC, REASONING, KNOWLEDGE, PLANNING, CAUSAL, HYPOTHESIS, VERIFICATION, EXPLANATION, UNKNOWN }
    data class Result(val type:Type,val confidence:Int,val signals:List<String>)

    fun detect(input:String):Result {
        val s=input.lowercase(Locale.getDefault())
        val scores=linkedMapOf<Type,Int>().withDefault{0}
        fun add(t:Type,n:Int){scores[t]=scores.getValue(t)+n}
        if(DialogueEngine.canHandle(input))add(Type.CONVERSATION,100)
        if(Regex("""[-+*/^]\s*\d|\d+(?:[.,]\d+)?\s*%\s*din""").containsMatchIn(s))add(Type.CALCULATION,80)
        if(listOf("medie","mediană","mediana","varianță","varianta","deviație standard","outlier","statistic").any{s.contains(it)})add(Type.STATISTICS,80)
        if(listOf("convert","câți km","cati km","câte kg","cate kg","în metri","in metri","celsius","fahrenheit").any{s.contains(it)})add(Type.CONVERSION,70)
        if(listOf("ora","astăzi","azi","mâine","maine","ieri","data de","ce zi").any{s.contains(it)})add(Type.TIME,60)
        if(listOf("mai mare","mai mic","egal","dacă","daca","atunci","logic","deduce").any{s.contains(it)})add(Type.REASONING,65)
        if(listOf("cauzează","cauzeaza","de ce apare","duce la","efectul","cauza").any{s.contains(it)})add(Type.CAUSAL,90)
        if(listOf("ipoteză","ipoteza","presupune","ce s-ar întâmpla","ce s-ar intampla","posibilă explicație","posibila explicatie").any{s.contains(it)})add(Type.HYPOTHESIS,90)
        if(VerificationEngine.canHandle(input))add(Type.VERIFICATION,85)
        if(listOf("planifică","planifica","pașii","pasii","cum pot să","cum pot sa","strategie").any{s.contains(it)})add(Type.PLANNING,80)
        if(listOf("ce este","ce înseamnă","ce inseamna","explică","explica","cum funcționează","cum functioneaza").any{s.contains(it)})add(Type.EXPLANATION,65)
        if(LocalKnowledgeBase.canHandle(input))add(Type.KNOWLEDGE,90)
        if(scores.values.maxOrNull()==null||scores.values.maxOrNull()==0)return Result(Type.UNKNOWN,0,emptyList())
        val best=scores.maxByOrNull{it.value}!!
        return Result(best.key,best.value.coerceAtMost(99),scores.filterValues{it>0}.keys.map{it.name.lowercase()})
    }
}
