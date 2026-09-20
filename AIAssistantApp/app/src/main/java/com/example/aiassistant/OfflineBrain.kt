package com.example.aiassistant

/**
 * Offline cognitive core: deterministic, symbolic and procedural.
 * No neural model, no API and no network access.
 */
object OfflineBrain {
    private var lastConfidence = 0

    fun reply(input: String): String {
        val msg = input.trim()
        if (msg.isBlank()) return "Nu am primit nicio întrebare."

        val context = ContextEngine.snapshot(msg)
        KnowledgeGraphEngine.observe(msg)
        ConceptGraphEngine.observe(msg)

        val intent = IntentEngine.detect(msg)
        var answer: String? = null
        var sourceConfidence = intent.confidence

        if (ConversationEngine.canHandle(msg)) {
            answer = ConversationEngine.answer(msg)
            sourceConfidence = 92
        }

        if (answer == null) {
            when (intent.type) {
                IntentEngine.Type.CAUSAL -> {
                    val r = CausalReasoningEngine.answer(msg)
                    if (r.handled) { answer = r.answer; sourceConfidence = r.confidence }
                }
                IntentEngine.Type.HYPOTHESIS -> {
                    val r = HypothesisEngine.generate(msg)
                    if (r.handled) { answer = r.answer; sourceConfidence = 68 }
                }
                IntentEngine.Type.PLANNING -> {
                    val r = AdvancedPlanningEngine.plan(msg)
                    if (r.handled) { answer = r.answer; sourceConfidence = 86 }
                }
                IntentEngine.Type.VERIFICATION -> {
                    answer = VerificationEngine.answer(msg)
                    sourceConfidence = 82
                }
                else -> Unit
            }
        }

        if (answer == null) {
            val chain = ToolChainEngine.run(msg, intent.type)
            if (chain.handled) { answer = chain.answer; sourceConfidence = chain.confidence }
        }

        if (answer == null) {
            answer = when {
                SymbolicMathEngine.canHandle(msg) -> SymbolicMathEngine.answer(msg)
                ReasoningEngine.canHandle(msg) -> ReasoningEngine.answer(msg)
                ContradictionEngine.canHandle(msg) -> ContradictionEngine.answer(msg)
                MathEngine.canHandle(msg) -> MathEngine.calculate(msg)
                UnitConversionEngine.canHandle(msg) -> UnitConversionEngine.convert(msg)
                TimeEngine.canHandle(msg) -> TimeEngine.answer(msg)
                LogicEngine.canHandle(msg) -> LogicEngine.answer(msg)
                TextAnalysisEngine.canHandle(msg) -> TextAnalysisEngine.analyze(msg)
                KnowledgeEngine.canHandle(msg) -> KnowledgeEngine.answer(msg)
                else -> null
            }
            if (answer != null) sourceConfidence = maxOf(sourceConfidence, 78)
        }

        if (answer == null) {
            TemporalReasoningEngine.infer(msg)?.let {
                answer = "Interpretare temporală locală: $it."
                sourceConfidence = 84
            }
        }

        if (answer == null) {
            val recalled = SemanticMemoryEngine.recall(msg)
            if (recalled.isNotEmpty()) {
                answer = buildString {
                    append("Am găsit o potrivire în memoria semantică locală. Nu o tratez ca pe un fapt nou.\n\n")
                    recalled.forEachIndexed { i, item ->
                        append("${i + 1}. Întrebare similară: ${item.text}\n")
                        append("Răspuns anterior: ${item.response}\n")
                    }
                }
                sourceConfidence = 62
            }
        }

        if (answer == null) answer = ResponseEngine.unknown(msg)

        val verification = VerificationEngine.check(msg, answer!!, sourceConfidence)
        val confidence = ConfidenceEngine.score(intent, sourceConfidence, verification, SemanticMemoryEngine.recall(msg, 1).isNotEmpty())
        val reflection = SelfReflectionEngine.reflect(msg, answer!!, confidence, verification)
        lastConfidence = reflection.score

        val finalAnswer = buildString {
            append(reflection.answer)
            append("\n\n🧠 Local confidence: ").append(reflection.score).append("%")
            if (context.topic.isNotBlank()) append(" · topic: ").append(context.topic)
        }

        ContextEngine.remember(msg, finalAnswer)
        SemanticMemoryEngine.remember(msg, finalAnswer)
        return finalAnswer
    }

    fun canHandle(input: String): Boolean = input.isNotBlank()

    fun confidence(): Int = lastConfidence
}
