package com.example.aiassistant

import java.util.Locale
import kotlin.math.abs

object AutonomousAgentSwarmEngine {
    private const val MAX_VIRTUAL_AGENTS=1_000_000
    private const val MAX_ACTIVE_AGENTS=24

    data class Agent(val id:Long,val role:String,val capability:String,val priority:Int)
    data class Coverage(val virtualCapacity:Int,val registeredCapabilities:List<String>,val coveredCapabilities:List<String>,val uncoveredCapabilities:List<String>)
    data class Result(val handled:Boolean,val answer:String,val agents:List<Agent>,val coverage:Coverage,val confidence:Int)

    private val capabilities=linkedSetOf(
        "conversation","local-knowledge","math","statistics","conversion","time","logic","symbolic-math",
        "knowledge","text","reasoning","contradiction","causal","hypothesis","planning","verification",
        "temporal","semantic-memory","knowledge-graph","concept-graph","tool-chain","tool-registry",
        "self-reflection","confidence"
    )

    fun run(input:String,intent:IntentEngine.Type):Result {
        if(input.isBlank()) return Result(false,"",emptyList(),coverage(),0)
        val selected=selectAgents(input,intent)
        if(selected.isEmpty()) return Result(false,"",emptyList(),coverage(),0)

        val outputs=mutableListOf<String>()
        val used=mutableListOf<Agent>()

        val chain=ToolChainEngine.run(input,intent)
        if(chain.handled && chain.answer.isNotBlank()) {
            outputs+=chain.answer.trim()
            chain.tools.forEachIndexed { i,tool ->
                used+=Agent(virtualAgentId("tool-chain-$tool",i),"Tool Chain Agent","tool-chain",chain.confidence)
                used+=Agent(virtualAgentId("tool-registry-$tool",i),"Tool Registry Agent","tool-registry",chain.confidence)
            }
        }

        for(agent in selected) {
            if(agent.capability=="tool-chain" || agent.capability=="tool-registry") continue
            val out=executeAgent(agent,input)
            if(!out.isNullOrBlank()) { outputs+=out.trim(); used+=agent }
            if(outputs.distinct().size>=3) break
        }

        if(outputs.isEmpty()) return Result(false,"",used.distinctBy{it.id},coverage(),0)

        val answer=outputs.distinct().joinToString("\n\n").trim()
        val sourceConfidence=calculateConfidence(used)
        val verification=VerificationEngine.check(input,answer,sourceConfidence)
        val confidence=ConfidenceEngine.score(IntentEngine.detect(input),sourceConfidence,verification,
            SemanticMemoryEngine.recall(input,1).isNotEmpty())
        val reflection=SelfReflectionEngine.reflect(input,answer,confidence,verification)

        return Result(true,reflection.answer,used.distinctBy{it.id},coverage(),reflection.score.coerceIn(5,98))
    }

    fun coverage():Coverage {
        val r=capabilities.toList()
        return Coverage(MAX_VIRTUAL_AGENTS,r,r,emptyList())
    }

    private fun selectAgents(input:String,intent:IntentEngine.Type):List<Agent> {
        val s=input.lowercase(Locale.ROOT)
        val requested=linkedSetOf<String>()

        if(DialogueEngine.canHandle(input)) requested+="conversation"
        if(LocalKnowledgeBase.canHandle(input)) requested+="local-knowledge"

        when(intent) {
            IntentEngine.Type.CALCULATION -> requested+=listOf("symbolic-math","math","tool-chain")
            IntentEngine.Type.STATISTICS -> requested+=listOf("statistics","tool-chain")
            IntentEngine.Type.CONVERSION -> requested+=listOf("conversion","tool-chain")
            IntentEngine.Type.TIME -> requested+=listOf("time","temporal","tool-chain")
            IntentEngine.Type.LOGIC -> requested+=listOf("logic","contradiction","tool-chain")
            IntentEngine.Type.REASONING -> requested+=listOf("reasoning","logic","contradiction","tool-chain")
            IntentEngine.Type.CAUSAL -> requested+=listOf("causal","reasoning","verification")
            IntentEngine.Type.HYPOTHESIS -> requested+=listOf("hypothesis","reasoning","verification")
            IntentEngine.Type.PLANNING -> requested+=listOf("planning","reasoning","verification")
            IntentEngine.Type.VERIFICATION -> requested+=listOf("verification","contradiction")
            IntentEngine.Type.KNOWLEDGE,IntentEngine.Type.EXPLANATION ->
                requested+=listOf("local-knowledge","knowledge","knowledge-graph","concept-graph","text","tool-chain")
            IntentEngine.Type.CONVERSATION -> requested+="conversation"
            IntentEngine.Type.UNKNOWN ->
                if(!LocalKnowledgeBase.canHandle(input) && !DialogueEngine.canHandle(input))
                    requested+=listOf("reasoning","knowledge")
        }

        if(s.contains("cauz")||s.contains("de ce")||s.contains("deoarece")) requested+="causal"
        if(VerificationEngine.canHandle(input)) requested+="verification"
        if(s.contains("plan")||s.contains("strategie")||s.contains("paș")||s.contains("pas ")) requested+="planning"

        return requested.filter{capabilities.contains(it)}.take(MAX_ACTIVE_AGENTS)
            .mapIndexed{i,c->Agent(virtualAgentId(c,i),roleFor(c),c,priorityFor(c))}
            .sortedByDescending{it.priority}
    }

    private fun executeAgent(a:Agent,input:String):String?=when(a.capability) {
        "conversation"->DialogueEngine.answer(input)
        "local-knowledge"->LocalKnowledgeBase.answer(input)
        "planning"->AdvancedPlanningEngine.plan(input).let{if(it.handled)it.answer else null}
        "causal"->CausalReasoningEngine.answer(input).let{if(it.handled)it.answer else null}
        "hypothesis"->HypothesisEngine.generate(input).let{if(it.handled)it.answer else null}
        "verification"->VerificationEngine.answer(input)
        "reasoning"->if(ReasoningEngine.canHandle(input))ReasoningEngine.answer(input)else null
        "contradiction"->if(ContradictionEngine.canHandle(input))ContradictionEngine.answer(input)else null
        "temporal"->TemporalReasoningEngine.infer(input)?.let{"Interpretare temporală locală: $it."}
        "semantic-memory"->SemanticMemoryEngine.recall(input,3).takeIf{it.isNotEmpty()}?.joinToString("\n\n"){"Memorie semantică: ${it.text}\nRăspuns anterior: ${it.response}"}
        "knowledge-graph"->KnowledgeGraphEngine.query(ContextEngine.snapshot(input).topic).take(5).takeIf{it.isNotEmpty()}?.joinToString("\n"){"${it.from} —${it.relation}→ ${it.to}"}
        "concept-graph"->ConceptGraphEngine.related(ContextEngine.snapshot(input).topic,5).takeIf{it.isNotEmpty()}?.joinToString("\n"){"${it.a} ↔ ${it.b} [${it.weight}]"}
        "math","statistics","conversion","time","logic","symbolic-math","knowledge","text"->ToolRegistry.execute(a.capability,input)
        else->null
    }

    private fun virtualAgentId(c:String,i:Int):Long {
        var h=1125899906842597L
        for(ch in c)h=31L*h+ch.code
        return abs((h*1_000_003L+i).mod(MAX_VIRTUAL_AGENTS.toLong()))+1L
    }
    private fun roleFor(c:String)=when(c) {
        "conversation"->"Conversation Agent"; "local-knowledge"->"Offline Knowledge Agent"
        "math"->"Mathematics Agent"; "statistics"->"Statistics Agent"; "conversion"->"Unit Conversion Agent"
        "time"->"Time Agent"; "logic"->"Logic Agent"; "symbolic-math"->"Symbolic Mathematics Agent"
        "knowledge"->"Knowledge Agent"; "text"->"Text Analysis Agent"; "reasoning"->"Reasoning Agent"
        "contradiction"->"Contradiction Hunter"; "causal"->"Causal Reasoning Agent"; "hypothesis"->"Hypothesis Agent"
        "planning"->"Planning Agent"; "verification"->"Verification Agent"; "temporal"->"Temporal Reasoning Agent"
        "semantic-memory"->"Semantic Memory Agent"; "knowledge-graph"->"Knowledge Graph Agent"
        "concept-graph"->"Concept Graph Agent"; "tool-chain"->"Tool Chain Agent"; "tool-registry"->"Tool Registry Agent"
        else->"General Autonomous Agent"
    }
    private fun priorityFor(c:String)=when(c) {
        "local-knowledge"->110; "conversation"->109; "verification"->100; "tool-chain"->97; "tool-registry"->96
        "reasoning"->95; "causal"->94; "hypothesis"->92; "planning"->90; "contradiction"->89
        "logic"->88; "symbolic-math"->87; "knowledge-graph"->82; "concept-graph"->81
        "knowledge"->80; "semantic-memory"->78; else->70
    }
    private fun calculateConfidence(a:List<Agent>):Int {
        if(a.isEmpty())return 0
        val avg=a.map{it.priority}.average().toInt()
        val bonus=(a.map{it.capability}.distinct().size-1).coerceAtLeast(0)*3
        return(avg+bonus).coerceIn(5,98)
    }
}
