package com.example.aiassistant

object AgentEngine {
    fun instruction(userRequest: String): String = """
        AGENT MODE. Rezolvă cererea ca o echipă de roluri într-un singur răspuns:
        1. PLANNER: definește pe scurt obiectivul și pașii necesari.
        2. RESEARCHER: folosește web grounding când informația poate fi actuală și citează sursele disponibile.
        3. ANALYST: compară dovezile și marchează contradicțiile.
        4. SKEPTIC: caută ce ar putea fi greșit sau neverificat.
        5. VERIFIER: separă FAPT CONFIRMAT / INFERENȚĂ / NECUNOSCUT.
        6. EXECUTOR: livrează rezultatul practic cerut.
        Nu afișa raționament intern privat. Afișează doar un rezumat scurt al pașilor, dovezile și rezultatul.
        Cererea utilizatorului: $userRequest
    """.trimIndent()
}
