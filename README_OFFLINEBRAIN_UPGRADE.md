# OfflineBrain Cognitive Core v2

Scope: OfflineBrain cognitive architecture only.

Added local deterministic/procedural components:
- IntentEngine
- EntityEngine
- ContextEngine
- KnowledgeGraphEngine
- SemanticMemoryEngine
- CausalReasoningEngine
- HypothesisEngine
- AdvancedPlanningEngine
- ToolRegistry
- ToolChainEngine
- VerificationEngine
- SelfReflectionEngine
- ConfidenceEngine
- TemporalReasoningEngine
- ConceptGraphEngine

Updated:
- OfflineBrain.kt

No neural model, API or network access is used by these components.

Important:
- SemanticMemoryEngine and ContextEngine are bounded in-process memory. They do not replace the existing persistent Brain/MemoryEngine.
- KnowledgeGraphEngine is a lightweight local graph, not a full ontology database.
- Confidence is an internal heuristic, not a probability of truth.
- External factual verification still belongs to the web/Gemini layer.

Validation:
The new cognitive-core sources compile together under Kotlin/JVM when existing engine interfaces are present.
