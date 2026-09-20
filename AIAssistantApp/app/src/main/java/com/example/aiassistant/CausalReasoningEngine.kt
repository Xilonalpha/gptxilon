package com.example.aiassistant

object CausalReasoningEngine {
    data class Result(val handled: Boolean, val answer: String, val confidence: Int)

    fun answer(input: String): Result {
        val m = Regex("(?i)(.{2,70})\\s+(?:cauzează|cauzeaza|duce la|produce)\\s+(.{2,90})").find(input)
        if (m != null) return Result(true, "Relație cauzală explicită: ${m.groupValues[1].trim()} → ${m.groupValues[2].trim()}. Aceasta este o relație furnizată în enunț, nu o cauzalitate verificată extern.", 72)
        val cond = Regex("(?i)dacă\\s+(.{2,100})\\s+atunci\\s+(.{2,100})").find(input)
        if (cond != null) return Result(true, "Model condițional: dacă ${cond.groupValues[1].trim()}, atunci ${cond.groupValues[2].trim()}. Este o regulă ipotetică, nu o dovadă cauzală.", 70)
        return Result(false, "", 0)
    }
}
