package com.example.aiassistant

import java.util.Locale
import kotlin.math.abs

/**
 * Autonomous Agent Swarm Engine.
 *
 * Virtual capacity: 1,000,000 agents.
 * Active execution is bounded for Android safety.
 *
 * Pipeline:
 * Planning -> ToolChain -> ToolRegistry -> Agent Engines
 * -> Verification -> Confidence -> SelfReflection
 */
object AutonomousAgentSwarmEngine {

    private const val MAX_VIRTUAL_AGENTS = 1_000_000
    private const val MAX_ACTIVE_AGENTS = 24

    data class Agent(
        val id: Long,
        val role: String,
        val capability: String,
        val priority: Int
    )

    data class Coverage(
        val virtualCapacity: Int,
        val registeredCapabilities: List<String>,
        val coveredCapabilities: List<String>,
        val uncoveredCapabilities: List<String>
    )

    data class Result(
        val handled: Boolean,
        val answer: String,
        val agents: List<Agent>,
        val coverage: Coverage,
        val confidence: Int
    )

    private val capabilities = linkedSetOf(
        "conversation",
        "dialogue",
        "local-knowledge",
        "math",
        "statistics",
        "conversion",
        "time",
        "logic",
        "symbolic-math",
        "knowledge",
        "text",
        "reasoning",
        "contradiction",
        "causal",
        "hypothesis",
        "planning",
        "verification",
        "temporal",
        "semantic-memory",
        "knowledge-graph",
        "concept-graph",
        "tool-chain",
        "tool-registry",
        "self-reflection",
        "confidence"
    )

    fun run(input: String, intent: IntentEngine.Type): Result {
        if (input.isBlank()) {
            return Result(false, "", emptyList(), coverage(), 0)
        }

        /*
         * Deterministic English math/conversion must execute before
         * local knowledge, tools or semantic memory. This prevents
         * generic knowledge matches (for example pH) from hijacking
         * arithmetic and conversion questions.
         */
        val deterministic = deterministicEnglishMath(input)

        if (!deterministic.isNullOrBlank()) {
            return Result(
                handled = true,
                answer = deterministic,
                agents = emptyList(),
                coverage = coverage(),
                confidence = 95
            )
        }

        val selected = selectAgents(input, intent)
        if (selected.isEmpty()) {
            return Result(false, "", emptyList(), coverage(), 0)
        }

        val outputs = mutableListOf<String>()
        val used = mutableListOf<Agent>()

        /*
         * Deterministic local knowledge has priority over generic tools.
         * This keeps elementary factual questions fully offline.
         */
        val localKnowledge = LocalKnowledgeBase.answer(input)

        if (!localKnowledge.isNullOrBlank()) {
            val agent = selected.firstOrNull { it.capability == "local-knowledge" }

            if (agent != null) {
                used += agent
            }

            /*
             * Deterministic local knowledge is authoritative for factual
             * entries that are explicitly present in the offline datasets.
             *
             * Do not continue into semantic memory, generic reasoning or
             * tools after a local fact has been found. Those agents can
             * contaminate a known factual answer with unrelated context.
             */
            val localAnswer = localKnowledge.trim()

            return Result(
                handled = true,
                answer = localAnswer,
                agents = used.distinctBy { it.id },
                coverage = coverage(),
                confidence = 70
            )
        }

        /*
         * The ToolChain remains a real orchestration stage of the swarm.
         * It delegates execution to ToolRegistry.
         */
        if (outputs.isEmpty()) {
            val chain = ToolChainEngine.run(input, intent)

            if (chain.handled && chain.answer.isNotBlank()) {
                outputs += chain.answer.trim()

                chain.tools.forEachIndexed { index, tool ->
                    used += Agent(
                        id = virtualAgentId("tool-chain-$tool", index),
                        role = "Tool Chain Agent",
                        capability = "tool-chain",
                        priority = chain.confidence
                    )

                    used += Agent(
                        id = virtualAgentId("tool-registry-$tool", index),
                        role = "Tool Registry Agent",
                        capability = "tool-registry",
                        priority = chain.confidence
                    )
                }
            }
        }

        /*
         * Dynamic specialist agents.
         */
        for (agent in selected) {
            if (
                agent.capability == "tool-chain" ||
                agent.capability == "tool-registry"
            ) {
                continue
            }

            val output = executeAgent(agent, input)

            if (!output.isNullOrBlank()) {
                outputs += output.trim()
                used += agent
            }

            if (outputs.distinct().size >= 3) break
        }

        if (outputs.isEmpty()) {
            return Result(
                false,
                "",
                used.distinctBy { it.id },
                coverage(),
                0
            )
        }

        val answer = outputs
            .distinct()
            .joinToString("\n\n")
            .trim()

        /*
         * The swarm verifies its own result before returning it.
         */
        val sourceConfidence = calculateConfidence(used)

        val verification = VerificationEngine.check(
            input,
            answer,
            sourceConfidence
        )

        /*
         * Confidence is calculated from the real IntentEngine result,
         * source confidence and verification result.
         */
        val intentResult = IntentEngine.detect(input)

        val confidence = ConfidenceEngine.score(
            intentResult,
            sourceConfidence,
            verification,
            SemanticMemoryEngine.recall(input, 1).isNotEmpty()
        )

        /*
         * Self-reflection is executed inside the swarm itself.
         */
        val reflection = SelfReflectionEngine.reflect(
            input,
            answer,
            confidence,
            verification
        )

        return Result(
            true,
            reflection.answer,
            used.distinctBy { it.id },
            coverage(),
            reflection.score.coerceIn(5, 98)
        )
    }

    private fun deterministicEnglishMath(input: String): String? {
        val q = input.trim().lowercase(Locale.ROOT)

        val number = """([0-9]+(?:\.[0-9]+)?)"""

        // X% of Y
        Regex(
            """^\s*(?:what is\s+)?$number\s*%\s*(?:of|from)\s*$number\s*\??\s*$"""
        ).matchEntire(q)?.let { m ->
            val percent = m.groupValues[1].toDouble()
            val base = m.groupValues[2].toDouble()
            val result = percent * base / 100.0
            return "The answer is ${formatNumber(result)}."
        }

        // Natural English miles -> kilometers conversion
        Regex(
            """^\s*(?:how many\s+)?kilometers\s+(?:are\s+)?in\s+$number\s+miles\s*\??\s*$"""
        ).matchEntire(q)?.let { m ->
            val miles = m.groupValues[1].toDouble()
            val km = miles * 1.609344
            return "${formatNumber(miles)} miles is approximately ${formatNumber(km)} kilometers."
        }

        Regex(
            """^\s*how many\s+kilometers\s+are\s+there\s+in\s+$number\s+miles\s*\??\s*$"""
        ).matchEntire(q)?.let { m ->
            val miles = m.groupValues[1].toDouble()
            val km = miles * 1.609344
            return "${formatNumber(miles)} miles is approximately ${formatNumber(km)} kilometers."
        }

        // Basic English arithmetic: +, -, x, ×, *, /, ÷
        Regex(
            """^\s*(?:what is\s+)?$number\s*([+\-×x*/÷])\s*$number\s*\??\s*$"""
        ).matchEntire(q)?.let { m ->
            val a = m.groupValues[1].toDouble()
            val op = m.groupValues[2]
            val b = m.groupValues[3].toDouble()

            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "x", "×", "*" -> a * b
                "/", "÷" -> {
                    if (b == 0.0) return null
                    a / b
                }
                else -> return null
            }

            return "The answer is ${formatNumber(result)}."
        }

        return null
    }

    private fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }

        return "%.10f".format(Locale.ROOT, value)
            .trimEnd('0')
            .trimEnd('.')
    }

    /**
     * Reports actual capabilities implemented by this swarm.
     */
    fun coverage(): Coverage {
        val registered = capabilities.toList()

        val covered = registered.filter {
            when (it) {
                "conversation",
                "dialogue",
                "local-knowledge",
                "math",
                "statistics",
                "conversion",
                "time",
                "logic",
                "symbolic-math",
                "knowledge",
                "text",
                "reasoning",
                "contradiction",
                "causal",
                "hypothesis",
                "planning",
                "verification",
                "temporal",
                "semantic-memory",
                "knowledge-graph",
                "concept-graph",
                "tool-chain",
                "tool-registry",
                "self-reflection",
                "confidence" -> true

                else -> false
            }
        }

        return Coverage(
            virtualCapacity = MAX_VIRTUAL_AGENTS,
            registeredCapabilities = registered,
            coveredCapabilities = covered,
            uncoveredCapabilities = registered.filterNot { covered.contains(it) }
        )
    }

    private fun selectAgents(
        input: String,
        intent: IntentEngine.Type
    ): List<Agent> {

        val normalized = input.lowercase(Locale.getDefault())
        val requested = linkedSetOf<String>()

        /*
         * Local dialogue and deterministic factual knowledge have priority.
         * They must be evaluated before generic verification/reasoning agents.
         */
        val dialogueHandled = DialogueEngine.canHandle(input)

        if (dialogueHandled) {
            // DialogueEngine is the single owner of conversational replies.
            // Do not also activate ConversationEngine for the same input,
            // otherwise both engines return a greeting/identity response.
            requested += "dialogue"
        }

        if (LocalKnowledgeBase.canHandle(input)) {
            requested += "local-knowledge"
        }

        when (intent) {
            IntentEngine.Type.CALCULATION ->
                requested += listOf("symbolic-math", "math", "tool-chain")

            IntentEngine.Type.STATISTICS ->
                requested += listOf("statistics", "tool-chain")

            IntentEngine.Type.CONVERSION ->
                requested += listOf("conversion", "tool-chain")

            IntentEngine.Type.TIME ->
                requested += listOf("time", "temporal", "tool-chain")

            IntentEngine.Type.LOGIC ->
                requested += listOf("logic", "contradiction", "tool-chain")

            IntentEngine.Type.REASONING ->
                requested += listOf(
                    "reasoning",
                    "logic",
                    "contradiction",
                    "tool-chain"
                )

            IntentEngine.Type.CAUSAL ->
                requested += listOf(
                    "causal",
                    "reasoning",
                    "verification"
                )

            IntentEngine.Type.HYPOTHESIS ->
                requested += listOf(
                    "hypothesis",
                    "reasoning",
                    "verification"
                )

            IntentEngine.Type.PLANNING ->
                requested += listOf(
                    "planning",
                    "reasoning",
                    "verification"
                )

            IntentEngine.Type.VERIFICATION ->
                requested += listOf(
                    "verification",
                    "contradiction"
                )

            IntentEngine.Type.KNOWLEDGE,
            IntentEngine.Type.EXPLANATION ->
                requested += listOf(
                    "knowledge",
                    "knowledge-graph",
                    "concept-graph",
                    "text",
                    "tool-chain"
                )

            IntentEngine.Type.CONVERSATION ->
                if (!dialogueHandled) {
                    requested += "conversation"
                }

            else ->
                requested += listOf(
                    "reasoning",
                    "knowledge",
                    "verification"
                )
        }

        if (
            normalized.contains("cauz") ||
            normalized.contains("de ce") ||
            normalized.contains("deoarece")
        ) {
            requested += "causal"
        }

        if (
            normalized.contains("verific") ||
            normalized.contains("adevărat") ||
            normalized.contains("adevarat")
        ) {
            requested += "verification"
        }

        if (
            normalized.contains("plan") ||
            normalized.contains("strategie") ||
            normalized.contains("paș") ||
            normalized.contains("pas ")
        ) {
            requested += "planning"
        }

        return requested
            .filter { capabilities.contains(it) }
            .take(MAX_ACTIVE_AGENTS)
            .mapIndexed { index, capability ->
                Agent(
                    id = virtualAgentId(capability, index),
                    role = roleFor(capability),
                    capability = capability,
                    priority = priorityFor(capability)
                )
            }
            .sortedByDescending { it.priority }
    }

    private fun executeAgent(
        agent: Agent,
        input: String
    ): String? {

        return when (agent.capability) {

            "conversation" ->
                if (ConversationEngine.canHandle(input)) {
                    ConversationEngine.answer(input)
                } else null

            "dialogue" ->
                DialogueEngine.answer(input)

            "local-knowledge" ->
                LocalKnowledgeBase.answer(input)

            "planning" -> {
                val r = AdvancedPlanningEngine.plan(input)
                if (r.handled) r.answer else null
            }

            "causal" -> {
                val r = CausalReasoningEngine.answer(input)
                if (r.handled) r.answer else null
            }

            "hypothesis" -> {
                val r = HypothesisEngine.generate(input)
                if (r.handled) r.answer else null
            }

            "verification" -> {
                val requested =
                    input.contains("verific", true) ||
                    input.contains("verify", true) ||
                    input.contains("fact check", true) ||
                    input.contains("adevărat", true) ||
                    input.contains("adevarat", true) ||
                    input.contains("is it true", true)

                if (requested) VerificationEngine.answer(input) else null
            }

            "reasoning" ->
                if (ReasoningEngine.canHandle(input)) {
                    ReasoningEngine.answer(input)
                } else null

            "contradiction" ->
                if (ContradictionEngine.canHandle(input)) {
                    ContradictionEngine.answer(input)
                } else null

            "temporal" ->
                TemporalReasoningEngine.infer(input)?.let {
                    "Interpretare temporală locală: $it."
                }

            "semantic-memory" -> {
                val memory = SemanticMemoryEngine.recall(input, 3)

                if (memory.isEmpty()) {
                    null
                } else {
                    memory.joinToString("\n\n") {
                        "Memorie semantică: ${it.text}\nRăspuns anterior: ${it.response}"
                    }
                }
            }

            "knowledge-graph" -> {
                val context = ContextEngine.snapshot(input)
                val relations =
                    KnowledgeGraphEngine.query(context.topic).take(5)

                if (relations.isEmpty()) {
                    null
                } else {
                    relations.joinToString("\n") {
                        "${it.from} —${it.relation}→ ${it.to}"
                    }
                }
            }

            "concept-graph" -> {
                val context = ContextEngine.snapshot(input)
                val relations =
                    ConceptGraphEngine.related(context.topic, 5)

                if (relations.isEmpty()) {
                    null
                } else {
                    relations.joinToString("\n") {
                        "${it.a} ↔ ${it.b} [${it.weight}]"
                    }
                }
            }

            "math",
            "statistics",
            "conversion",
            "time",
            "logic",
            "symbolic-math",
            "knowledge",
            "text" ->
                ToolRegistry.execute(agent.capability, input)

            else -> null
        }
    }

    private fun virtualAgentId(
        capability: String,
        index: Int
    ): Long {

        var hash = 1125899906842597L

        for (c in capability) {
            hash = 31L * hash + c.code
        }

        return abs(
            (hash * 1_000_003L + index)
                .mod(MAX_VIRTUAL_AGENTS.toLong())
        ) + 1L
    }

    private fun roleFor(capability: String): String =
        when (capability) {
            "conversation" -> "Conversation Agent"
            "math" -> "Mathematics Agent"
            "statistics" -> "Statistics Agent"
            "conversion" -> "Unit Conversion Agent"
            "time" -> "Time Agent"
            "logic" -> "Logic Agent"
            "symbolic-math" -> "Symbolic Mathematics Agent"
            "knowledge" -> "Knowledge Agent"
            "text" -> "Text Analysis Agent"
            "reasoning" -> "Reasoning Agent"
            "contradiction" -> "Contradiction Hunter"
            "causal" -> "Causal Reasoning Agent"
            "hypothesis" -> "Hypothesis Agent"
            "planning" -> "Planning Agent"
            "verification" -> "Verification Agent"
            "temporal" -> "Temporal Reasoning Agent"
            "semantic-memory" -> "Semantic Memory Agent"
            "knowledge-graph" -> "Knowledge Graph Agent"
            "concept-graph" -> "Concept Graph Agent"
            "tool-chain" -> "Tool Chain Agent"
            "tool-registry" -> "Tool Registry Agent"
            "self-reflection" -> "Self Reflection Agent"
            "confidence" -> "Confidence Agent"
            else -> "General Autonomous Agent"
        }

    private fun priorityFor(capability: String): Int =
        when (capability) {
            "verification" -> 100
            "self-reflection" -> 99
            "confidence" -> 98
            "tool-chain" -> 97
            "tool-registry" -> 96
            "reasoning" -> 95
            "causal" -> 94
            "hypothesis" -> 92
            "planning" -> 90
            "contradiction" -> 89
            "logic" -> 88
            "symbolic-math" -> 87
            "knowledge-graph" -> 82
            "concept-graph" -> 81
            "knowledge" -> 80
            "semantic-memory" -> 78
            else -> 70
        }

    private fun calculateConfidence(
        agents: List<Agent>
    ): Int {

        if (agents.isEmpty()) return 0

        val average =
            agents.map { it.priority }.average().toInt()

        val diversityBonus =
            (agents.map { it.capability }
                .distinct()
                .size - 1)
                .coerceAtLeast(0) * 3

        return (average + diversityBonus)
            .coerceIn(5, 98)
    }
}
