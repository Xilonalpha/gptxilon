package com.example.aiassistant

import java.util.Locale

/** Offline deterministic cognitive orchestrator. No neural model, API or network. */
object OfflineBrain {
    private var lastConfidence = 0
    private const val MAX_CYCLES = 3
    private const val ACCEPT = 55

    enum class EvidenceState { KNOWN, EXPLICIT, DERIVED, ASSUMED, UNKNOWN }
    data class Evidence(val text: String, val state: EvidenceState, val source: String)
    data class ReasoningStep(val cycle:Int,val phase:String,val action:String,val result:String,val confidence:Int,val success:Boolean)
    data class CognitiveProblem(
        val originalInput:String,
        val normalizedInput:String,
        val intent:IntentEngine.Result,
        val entities:EntityEngine.Result,
        val context:ContextEngine.Snapshot,
        val evidence:MutableList<Evidence> = mutableListOf(),
        val constraints:MutableList<String> = mutableListOf(),
        val hypotheses:MutableList<String> = mutableListOf(),
        val selectedTools:MutableList<String> = mutableListOf(),
        val intermediateResults:MutableList<String> = mutableListOf(),
        val verificationResults:MutableList<String> = mutableListOf(),
        val reasoningTrace:MutableList<ReasoningStep> = mutableListOf(),
        var cycleCount:Int = 0,
        var finalAnswer:String = "",
        var confidence:Int = 0
    )
    private data class Plan(val action:String,val tools:List<String>,val direct:Boolean,val confidence:Int)
    private data class Execution(val answer:String,val tools:List<String>,val confidence:Int)

    fun reply(input:String):String {
        val msg=input.trim()
        if(msg.isBlank()) return "Nu am primit nicio întrebare."
        val result=cycle(msg)
        lastConfidence=result.confidence
        val finalAnswer=buildString {
            append(result.answer.trim())
            append("\n\n🧠 Local confidence: ").append(result.confidence).append("%")
            if(result.problem.context.topic.isNotBlank()) append(" · topic: ").append(result.problem.context.topic)
        }
        ContextEngine.remember(msg,finalAnswer)
        SemanticMemoryEngine.remember(msg,finalAnswer)
        Learning.record(result.problem.intent.type,result.problem.selectedTools,result.confidence>=ACCEPT)
        return finalAnswer
    }

    fun canHandle(input:String):Boolean=input.isNotBlank()
    fun confidence():Int=lastConfidence

    private fun cycle(input:String):CycleResult {
        val problem=CognitiveProblem(input,normalize(input),IntentEngine.detect(input),EntityEngine.extract(input),ContextEngine.snapshot(input))
        seedEvidence(problem)
        KnowledgeGraphEngine.observe(input)
        ConceptGraphEngine.observe(input)
        var answer=""
        var reflection=SelfReflectionEngine.Result("",0,false,emptyList())

        for(c in 1..MAX_CYCLES){
            problem.cycleCount=c
            trace(problem,c,"UNDERSTAND","Analizează intenția, entitățile și contextul","${problem.intent.type}; entities=${problem.entities.entities.size}",problem.intent.confidence,true)
            val plan=plan(problem,c)
            trace(problem,c,"PLAN",plan.action,plan.tools.joinToString(", ").ifBlank{"direct reasoning"},plan.confidence,plan.tools.isNotEmpty()||plan.direct)
            val execution=execute(problem,plan,c)
            if(execution.answer.isBlank()){
                trace(problem,c,"EXECUTE","Încearcă strategia","nicio strategie locală nu a produs rezultat",0,false)
            }else{
                answer=execution.answer
                problem.selectedTools.clear();problem.selectedTools.addAll(execution.tools.distinct())
                problem.intermediateResults.add(execution.answer.take(2200))
                addDerived(problem,execution.answer,execution.confidence)
                trace(problem,c,"EXECUTE","Execută strategia cognitivă",execution.answer.take(600),execution.confidence,true)
                val verification=VerificationEngine.check(input,execution.answer,execution.confidence)
                problem.verificationResults.add("cycle=$c score=${verification.score} verified=${verification.verified}"+(if(verification.warnings.isEmpty())"" else " warnings=${verification.warnings.joinToString("; ")}"))
                trace(problem,c,"VERIFY","Verifică structural rezultatul",if(verification.warnings.isEmpty())"fără avertismente" else verification.warnings.joinToString("; "),verification.score,verification.verified)
                val confidence=ConfidenceEngine.score(problem.intent,execution.confidence,verification,SemanticMemoryEngine.recall(input,1).isNotEmpty())
                reflection=SelfReflectionEngine.reflect(input,execution.answer,confidence,verification)
                answer=reflection.answer;problem.confidence=reflection.score
                trace(problem,c,"REFLECT","Reevaluează rezultatul",reflection.notes.joinToString("; ").ifBlank{"nu sunt corecții necesare"},reflection.score,reflection.score>=ACCEPT)
                if(verification.verified&&reflection.score>=ACCEPT&&!unknown(execution.answer)){problem.finalAnswer=answer;break}
                if(c<MAX_CYCLES) trace(problem,c,"REPLAN","Schimbă strategia după verificare insuficientă","pregătește pass-ul ${c+1}",reflection.score,false)
            }
            if(c<MAX_CYCLES) enrich(problem)
        }

        if(answer.isBlank()){
            answer=ResponseEngine.unknown(input)
            val v=VerificationEngine.check(input,answer,5)
            reflection=SelfReflectionEngine.reflect(input,answer,5,v)
            answer=reflection.answer
        }
        val confidence=reflection.score.coerceIn(5,98).let{if(unknown(answer))minOf(it,35) else it}
        problem.finalAnswer=answer;problem.confidence=confidence
        return CycleResult(answer,confidence,problem)
    }

    private data class CycleResult(val answer:String,val confidence:Int,val problem:CognitiveProblem)

    private fun plan(p:CognitiveProblem,cycle:Int):Plan{
        if(cycle>1)return when{
            SemanticMemoryEngine.recall(p.originalInput).isNotEmpty()->Plan("Reevaluează memoria semantică și apoi fallback determinist",listOf("semantic-memory","fallback"),true,62)
            p.intent.type==IntentEngine.Type.REASONING||p.intent.type==IntentEngine.Type.LOGIC->Plan("Reevaluează premisele în ordine alternativă",listOf("logic","symbolic-math"),true,70)
            else->Plan("Reevaluează prin motoare deterministe de siguranță",fallbackTools(p),true,60)
        }
        return when(p.intent.type){
            IntentEngine.Type.CAUSAL->Plan("Modelează cauza și efectul explicit",emptyList(),true,72)
            IntentEngine.Type.HYPOTHESIS->Plan("Generează și separă ipotezele de fapte",emptyList(),true,68)
            IntentEngine.Type.PLANNING->Plan("Construiește pași și dependențe",emptyList(),true,86)
            IntentEngine.Type.VERIFICATION->Plan("Execută verificarea structurală locală",emptyList(),true,82)
            IntentEngine.Type.TIME->Plan("Aplică timp și interpretare temporală",listOf("time"),false,78)
            IntentEngine.Type.STATISTICS->Plan("Aplică analiza statistică",listOf("statistics"),false,80)
            IntentEngine.Type.CONVERSION->Plan("Aplică conversia de unități",listOf("conversion"),false,78)
            IntentEngine.Type.CALCULATION->Plan("Încearcă simbolic apoi numeric",listOf("symbolic-math","math"),false,80)
            IntentEngine.Type.KNOWLEDGE,IntentEngine.Type.EXPLANATION->Plan("Caută cunoștințe locale apoi analiză text",listOf("knowledge","text"),false,75)
            IntentEngine.Type.LOGIC,IntentEngine.Type.REASONING->Plan("Combină logica și raționamentul simbolic",listOf("logic","symbolic-math"),false,75)
            else->Plan("Caută strategie deterministă",fallbackTools(p),true,50)
        }
    }

    private fun execute(p:CognitiveProblem,plan:Plan,cycle:Int):Execution{
        val input=p.originalInput
        /*
         * Autonomous Agent Swarm is the central execution layer.
         *
         * Specialized engines such as causal reasoning, hypothesis,
         * planning and verification are now invoked by the swarm itself.
         * This prevents OfflineBrain from bypassing the swarm and keeps
         * Planning -> ToolChain -> ToolRegistry -> Agents ->
         * Verification -> Confidence -> SelfReflection in one pipeline.
         */
        val swarm = AutonomousAgentSwarmEngine.run(input,p.intent.type)
        if (swarm.handled) {
            return Execution(
                swarm.answer,
                swarm.agents.map { "agent:${it.capability}" }.distinct(),
                swarm.confidence
            )
        }

        if (plan.tools.contains("semantic-memory")) {
            val memory=SemanticMemoryEngine.recall(input)
            if(memory.isNotEmpty()) {
                val recalled=buildString {
                    append("Am găsit potriviri în memoria semantică locală. Nu le tratez ca fapte noi.\n\n")
                    memory.forEachIndexed { i,item ->
                        append("${i+1}. Întrebare similară: ${item.text}\n")
                        append("Răspuns anterior: ${item.response}\n")
                    }
                }
                return Execution(recalled,listOf("semantic-memory"),62)
            }
        }
        val chain=toolChain(input,plan.tools);if(chain.answer.isNotBlank())return chain
        val direct=directFallback(input,cycle);if(direct.answer.isNotBlank())return direct
        TemporalReasoningEngine.infer(input)?.let{return Execution("Interpretare temporală locală: $it.",listOf("temporal"),84)}
        val memory=SemanticMemoryEngine.recall(input)
        if(memory.isNotEmpty())return Execution(buildString{append("Am găsit potriviri în memoria semantică locală. Nu le tratez ca fapte noi.\n\n");memory.forEachIndexed{i,item->append("${i+1}. Întrebare similară: ${item.text}\n");append("Răspuns anterior: ${item.response}\n")}},listOf("semantic-memory"),62)
        return Execution("",emptyList(),0)
    }

    private fun toolChain(input:String,requested:List<String>):Execution{
        if(requested.isEmpty())return Execution("",emptyList(),0)
        val outputs=mutableListOf<String>();val used=mutableListOf<String>()
        for(name in requested.distinct()){val r=ToolRegistry.execute(name,input);if(!r.isNullOrBlank()){outputs+=r.trim();used+=name}}
        if(outputs.isEmpty())return Execution("",emptyList(),0)
        return Execution(outputs.distinct().joinToString("\n\n"),used,if(used.size>=2)90 else 88)
    }

    private fun directFallback(input:String,cycle:Int):Execution{
        val checks=if(cycle==1)listOf("symbolic","reasoning","contradiction","math","conversion","time","logic","text","knowledge","statistics") else listOf("contradiction","reasoning","symbolic","logic","statistics","math","conversion","time","knowledge","text")
        for(name in checks){
            val a=when(name){
                "symbolic"->if(SymbolicMathEngine.canHandle(input))SymbolicMathEngine.answer(input)else null
                "reasoning"->if(ReasoningEngine.canHandle(input))ReasoningEngine.answer(input)else null
                "contradiction"->if(ContradictionEngine.canHandle(input))ContradictionEngine.answer(input)else null
                "math"->if(MathEngine.canHandle(input))MathEngine.calculate(input)else null
                "conversion"->if(UnitConversionEngine.canHandle(input))UnitConversionEngine.convert(input)else null
                "time"->if(TimeEngine.canHandle(input))TimeEngine.answer(input)else null
                "logic"->if(LogicEngine.canHandle(input))LogicEngine.answer(input)else null
                "text"->if(TextAnalysisEngine.canHandle(input))TextAnalysisEngine.analyze(input)else null
                "knowledge"->LocalKnowledgeBase.answer(input)
                "statistics"->if(StatisticsEngine.canHandle(input))StatisticsEngine.answer(input)else null
                else->null
            }
            if(!a.isNullOrBlank())return Execution(a,listOf(name),when(name){"contradiction","symbolic","math","statistics"->90;"knowledge"->82;"logic","reasoning"->80;"conversion","time"->82;else->75})
        }
        return Execution("",emptyList(),0)
    }

    private fun fallbackTools(p:CognitiveProblem)=when(p.intent.type){
        IntentEngine.Type.STATISTICS->listOf("statistics")
        IntentEngine.Type.CONVERSION->listOf("conversion")
        IntentEngine.Type.TIME->listOf("time")
        IntentEngine.Type.CALCULATION->listOf("symbolic-math","math")
        IntentEngine.Type.KNOWLEDGE,IntentEngine.Type.EXPLANATION->listOf("knowledge","text")
        IntentEngine.Type.LOGIC,IntentEngine.Type.REASONING->listOf("logic","symbolic-math")
        else->emptyList()
    }

    private fun seedEvidence(p:CognitiveProblem){
        if(p.originalInput.isNotBlank())p.evidence+=Evidence(p.originalInput.take(1200),EvidenceState.EXPLICIT,"user-input")
        val localEntry = LocalKnowledgeBase.lookup(p.originalInput)

        if (localEntry != null) {
            p.evidence += Evidence(
                localEntry.answer.take(1200),
                EvidenceState.KNOWN,
                "local-knowledge:${localEntry.domain}"
            )
        }

        val memory=SemanticMemoryEngine.recall(p.originalInput,3)
        memory.forEach{p.evidence+=Evidence("Potrivire semantică: ${it.text.take(300)}",EvidenceState.ASSUMED,"semantic-memory")}

        if(memory.isEmpty() && localEntry == null) {
            p.evidence += Evidence(
                "Nu există încă dovadă locală suficientă pentru afirmațiile externe.",
                EvidenceState.UNKNOWN,
                "offline-boundary"
            )
        }
        listOf("trebuie" to "obiectiv obligatoriu","fără" to "restricție explicită","fara" to "restricție explicită","doar" to "restricție de exclusivitate","înainte" to "ordine temporală","dupa" to "ordine temporală","după" to "ordine temporală").forEach{if(p.normalizedInput.contains(it.first))p.constraints+=it.second}
    }

    private fun addDerived(p:CognitiveProblem,answer:String,confidence:Int){
        p.evidence+=Evidence(answer.take(1400),EvidenceState.DERIVED,"local-engine")
        if(confidence<ACCEPT)p.evidence+=Evidence("Rezultatul necesită verificare suplimentară.",EvidenceState.UNKNOWN,"confidence")
        while(p.evidence.size>24)p.evidence.removeAt(0)
    }

    private fun enrich(p:CognitiveProblem){
        SemanticMemoryEngine.recall(p.originalInput,3).forEach{item->{val marker="memorie:${item.text.take(180)}";if(p.intermediateResults.none{it.contains(marker)})p.intermediateResults+=marker}}
        KnowledgeGraphEngine.query(p.context.topic).take(5).forEach{p.evidence+=Evidence("${it.from} —${it.relation}→ ${it.to}",EvidenceState.KNOWN,"knowledge-graph")}
        ConceptGraphEngine.related(p.context.topic,5).forEach{p.evidence+=Evidence("Concept relation: ${it.a} ↔ ${it.b} [${it.weight}]",EvidenceState.ASSUMED,"concept-graph")}
    }

    private fun trace(p:CognitiveProblem,c:Int,phase:String,action:String,result:String,confidence:Int,success:Boolean){p.reasoningTrace+=ReasoningStep(c,phase,action,result.take(800),confidence,success)}
    private fun normalize(input:String)=input.trim().lowercase(Locale.getDefault()).replace(Regex("\\s+")," ")
    private fun unknown(answer:String):Boolean{val s=answer.lowercase(Locale.getDefault());return s.contains("nu am suficiente cunoștințe locale")||s.contains("nu am această informație în baza mea locală")||s.contains("nu am putut produce un răspuns local verificabil")||s.startsWith("nu am această informație")}

    private object Learning{
        private data class Stat(var attempts:Int,var successes:Int)
        private val stats=linkedMapOf<String,Stat>()
        @Synchronized fun record(intent:IntentEngine.Type,tools:List<String>,success:Boolean){
            val key="${intent.name}:${tools.joinToString("+").ifBlank{"direct"}}";val s=stats.getOrPut(key){Stat(0,0)};s.attempts++;if(success)s.successes++
            while(stats.size>64){val k=stats.keys.firstOrNull()?:break;stats.remove(k)}
        }
    }
}
