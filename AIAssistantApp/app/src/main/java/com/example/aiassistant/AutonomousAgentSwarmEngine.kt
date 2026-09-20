package com.example.aiassistant

import java.util.Locale
import kotlin.math.abs

/**
 * Autonomous virtual-agent swarm.
 *
 * Agents are generated dynamically from capabilities instead of being
 * instantiated permanently. The swarm can represent up to one million
 * virtual agents while activating only the agents required by a task.
 *
 * No network, API or neural model is required.
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
        "self-reflection",
        "confidence"
    )

    fun run(input: String, intent: IntentEngine.Type): Result {
        val selected = selectAgents(input, intent)

        if (selected.isEmpty()) {
            return Result(
                false,
                "",
                emptyList(),
                coverage(),
                0
            )
        }

        val outputs = mutableListOf<String>()
        val used = mutableListOf<Agent>()

        for (agent in selected) {
            val output = executeAgent(agent, input, intent)

            if (!output.isNullOrBlank()) {
                outputs += output.trim()
                used += agent
            }

            if (outputs.size >= 3) break
        }

        if (outputs.isEmpty()) {
            return Result(
                false,
                "",
                used,
                coverage(),
                0
            )
        }

        val answer = outputs.distinct().joinToString("\n\n")

        return Result(
            true,
            answer,
            used.distinctBy { it.id },
            coverage(),
            calculateConfidence(used)
        )
    }

    /**
     * Reports which capabilities are currently represented by the swarm.
     */
    fun coverage(): Coverage {
        val registered = capabilities.toList()

        val covered = registered.filter { capability ->
            when (capability) {
                "conversation",
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
                "self-reflection",
                "confidence" -> true
                else -> false
            }
        }

        return Coverage(
            MAX_VIRTUAL_AGENTS,
            registered,
            covered,
            registered.filterNot { covered.contains(it) }
        )
    }

    /**
     * Generates deterministic virtual agents only when needed.
     */
    private fun selectAgents(
        input: String,
        intent: IntentEngine.Type
    ): List<Agent> {
        val normalized = input.lowercase(Locale.getDefault())

        val requested = linkedSetOf<String>()

        when (intent) {
            IntentEngine.Type.CALCULATION ->
                requested += listOf("symbolic-math", "math")

            IntentEngine.Type.STATISTICS ->
                requested += "statistics"

            IntentEngine.Type.CONVERSION ->
                requested += "conversion"

            IntentEngine.Type.TIME ->
                requested += "time"

            IntentEngine.Type.LOGIC ->
                requested += listOf("logic", "contradiction")

            IntentEngine.Type.REASONING ->
                requested += listOf(
                    "reasoning",
                    "logic",
                    "contradiction"
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
                    "text"
                )

            IntentEngine.Type.CONVERSATION ->
                requested += "conversation"

            else ->
                requested += listOf(
                    "reasoning",
                    "knowledge",
                    "text",
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
            normalized.contains("paș")
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
        input: String,
        intent: IntentEngine.Type
    ): String? {
        return when (agent.capability) {

            "conversation" ->
                if (ConversationEngine.canHandle(input))
                    ConversationEngine.answer(input)
                else null

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

            "verification" ->
                VerificationEngine.answer(input)

            "reasoning" ->
                if (ReasoningEngine.canHandle(input))
                    ReasoningEngine.answer(input)
                else null

            "contradiction" ->
                if (ContradictionEngine.canHandle(input))
                    ContradictionEngine.answer(input)
                else null

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
                val relations = KnowledgeGraphEngine.query(context.topic).take(5)
                if (relations.isEmpty()) null
                else relations.joinToString("\n") {
                    "${it.from} —${it.relation}→ ${it.to}"
                }
            }

            "concept-graph" -> {
                val context = ContextEngine.snapshot(input)
                val relations = ConceptGraphEngine.related(context.topic, 5)
                if (relations.isEmpty()) null
                else relations.joinToString("\n") {
                    "${it.a} ↔ ${it.b} [${it.weight}]"
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
            "time" -> "Temporal Agent"
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
            "self-reflection" -> "Self Reflection Agent"
            "confidence" -> "Confidence Agent"
            else -> "General Autonomous Agent"
        }

    private fun priorityFor(capability: String): Int =
        when (capability) {
            "verification" -> 100
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
            (agents.map { it.capability }.distinct().size - 1)
                .coerceAtLeast(0) * 3

        return (average + diversityBonus).coerceIn(5, 98)
    }
}
