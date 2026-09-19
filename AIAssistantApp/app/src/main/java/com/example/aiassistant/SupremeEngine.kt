package com.example.aiassistant

/**
 * High-level orchestration layer. It does not expose private chain-of-thought.
 * It combines routing, memory, evidence, agent roles and optional multi-pass critique.
 */
object SupremeEngine {
    data class Plan(val mode: String, val useWeb: Boolean, val deep: Boolean, val reason: String)

    fun route(query: String, attachment: Attachment?, agent: Boolean, power: Boolean): Plan {
        val q=query.lowercase()
        val web=RouteEngine.needsWeb(q) || q.contains("caută") || q.contains("verific") || q.contains("compară") || agent || power
        val deep=power || agent || q.contains("analizează") || q.contains("cercetează") || q.contains("dovad") || q.contains("proiect")
        val mode=when {
            attachment != null -> "MULTIMODAL"
            deep -> "DEEP_AGENT"
            web -> "LIVE_RESEARCH"
            else -> "DIRECT"
        }
        return Plan(mode,web,deep,"Router bazat pe complexitate, actualitate și tipul intrării")
    }

    fun systemDirective(plan: Plan): String = """
        Ești nucleul unui asistent AI avansat.
        MOD: ${plan.mode}. Motiv: ${plan.reason}.
        Reguli obligatorii:
        - Nu inventa informații.
        - Pentru informații actuale folosește web grounding.
        - Separă FAPT CONFIRMAT, INFERENȚĂ și NECUNOSCUT când există incertitudine.
        - Pentru sarcini complexe verifică afirmațiile și caută contradicții.
        - Dacă utilizatorul cere cod/proiect, livrează structură reală de fișiere folosind [file:path].
        - Nu dezvălui chain-of-thought privat; oferă doar un rezumat al abordării și dovezile relevante.
        - Nu afirma că ai acces la dispozitiv, fișiere sau servicii externe decât dacă sunt efectiv furnizate prin aplicație.
    """.trimIndent()
}
