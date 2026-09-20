package com.example.aiassistant

object ToolChainEngine {
    data class Result(val handled: Boolean, val answer: String, val tools: List<String>, val confidence: Int)

    fun run(input: String, intent: IntentEngine.Type): Result {
        val selected = when (intent) {
            IntentEngine.Type.STATISTICS -> listOf("statistics")
            IntentEngine.Type.CONVERSION -> listOf("conversion")
            IntentEngine.Type.TIME -> listOf("time")
            IntentEngine.Type.CALCULATION -> listOf("symbolic-math", "math")
            IntentEngine.Type.KNOWLEDGE, IntentEngine.Type.EXPLANATION -> listOf("knowledge", "text")
            IntentEngine.Type.LOGIC, IntentEngine.Type.REASONING -> listOf("logic", "symbolic-math")
            else -> emptyList()
        }
        for (name in selected) {
            val result = ToolRegistry.execute(name, input)
            if (!result.isNullOrBlank()) return Result(true, result, listOf(name), 88)
        }
        return Result(false, "", emptyList(), 0)
    }
}
