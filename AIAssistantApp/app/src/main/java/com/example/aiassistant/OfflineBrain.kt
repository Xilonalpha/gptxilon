package com.example.aiassistant

object OfflineBrain {
    fun reply(input: String): String {
        val msg = input.trim()
        if (msg.isBlank()) return "Nu am primit nicio întrebare."

        if (ConversationEngine.canHandle(msg)) return ConversationEngine.answer(msg)
        if (PlanningEngine.canHandle(msg)) return PlanningEngine.answer(msg)
        if (StatisticsEngine.canHandle(msg)) return StatisticsEngine.answer(msg)
        if (SymbolicMathEngine.canHandle(msg)) return SymbolicMathEngine.answer(msg)
        if (ReasoningEngine.canHandle(msg)) return ReasoningEngine.answer(msg)
        if (ContradictionEngine.canHandle(msg)) return ContradictionEngine.answer(msg)
        if (MathEngine.canHandle(msg)) return MathEngine.calculate(msg)
        if (UnitConversionEngine.canHandle(msg)) return UnitConversionEngine.convert(msg)
        if (TimeEngine.canHandle(msg)) return TimeEngine.answer(msg)
        if (LogicEngine.canHandle(msg)) return LogicEngine.answer(msg)
        if (TextAnalysisEngine.canHandle(msg)) return TextAnalysisEngine.analyze(msg)
        if (KnowledgeEngine.canHandle(msg)) return KnowledgeEngine.answer(msg)

        return ResponseEngine.unknown(msg)
    }

    fun canHandle(input: String): Boolean = input.isNotBlank()
}
