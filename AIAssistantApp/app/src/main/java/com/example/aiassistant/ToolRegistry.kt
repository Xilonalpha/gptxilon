package com.example.aiassistant

object ToolRegistry {
    data class Tool(val name: String, val description: String, val execute: (String) -> String?)

    private val tools = listOf(
        Tool("math", "calcule", { if (MathEngine.canHandle(it)) MathEngine.calculate(it) else null }),
        Tool("statistics", "statistică", { if (StatisticsEngine.canHandle(it)) StatisticsEngine.answer(it) else null }),
        Tool("conversion", "conversii", { if (UnitConversionEngine.canHandle(it)) UnitConversionEngine.convert(it) else null }),
        Tool("time", "timp și date", { if (TimeEngine.canHandle(it)) TimeEngine.answer(it) else null }),
        Tool("logic", "logică numerică", { if (LogicEngine.canHandle(it)) LogicEngine.answer(it) else null }),
        Tool("symbolic-math", "ecuații liniare", { if (SymbolicMathEngine.canHandle(it)) SymbolicMathEngine.answer(it) else null }),
        Tool("knowledge", "cunoștințe locale", { if (KnowledgeEngine.canHandle(it)) KnowledgeEngine.answer(it) else null }),
        Tool("text", "analiză text", { if (TextAnalysisEngine.canHandle(it)) TextAnalysisEngine.analyze(it) else null })
    )

    fun execute(name: String, input: String): String? = tools.firstOrNull { it.name == name }?.execute?.invoke(input)
    fun names(): List<String> = tools.map { it.name }
}
