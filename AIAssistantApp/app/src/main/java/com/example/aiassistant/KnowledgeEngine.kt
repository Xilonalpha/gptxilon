package com.example.aiassistant

import java.util.Locale

object KnowledgeEngine {

    private val knowledge = mapOf(
        "relativitate" to
            "Teoria relativității descrie relația dintre spațiu, timp, materie și mișcare. Relativitatea specială tratează mișcarea uniformă și viteza luminii, iar relativitatea generală descrie gravitația ca geometrie a spațiu-timpului.",

        "gravitație" to
            "Gravitația este interacțiunea prin care corpurile cu masă sau energie influențează mișcarea altor corpuri.",

        "gravitatia" to
            "Gravitația este interacțiunea prin care corpurile cu masă sau energie influențează mișcarea altor corpuri.",

        "fotosinteză" to
            "Fotosinteza este procesul prin care plantele și alte organisme fotosintetice transformă energia luminii în energie chimică, folosind în principal dioxid de carbon și apă.",

        "fotosinteza" to
            "Fotosinteza este procesul prin care plantele și alte organisme fotosintetice transformă energia luminii în energie chimică.",

        "black hole" to
            "O gaură neagră este o regiune a spațiu-timpului în care gravitația este atât de puternică încât, dincolo de orizontul evenimentelor, nici lumina nu poate scăpa.",

        "gaură neagră" to
            "O gaură neagră este o regiune a spațiu-timpului în care gravitația este suficient de puternică încât lumina nu poate scăpa dincolo de orizontul evenimentelor.",

        "gaura neagra" to
            "O gaură neagră este o regiune a spațiu-timpului în care gravitația este suficient de puternică încât lumina nu poate scăpa dincolo de orizontul evenimentelor.",

        "atom" to
            "Atomul este unitatea fundamentală a materiei obișnuite, alcătuită dintr-un nucleu cu protoni și neutroni și electroni legați de acesta.",

        "dna" to
            "ADN-ul este molecula care stochează informația genetică a organismelor și a unor virusuri.",

        "inteligență artificială" to
            "Inteligența artificială este domeniul informaticii care dezvoltă sisteme capabile să execute sarcini asociate în mod obișnuit inteligenței umane.",

        "inteligenta artificiala" to
            "Inteligența artificială este domeniul informaticii care dezvoltă sisteme capabile să execute sarcini asociate în mod obișnuit inteligenței umane."
    )

    fun canHandle(input: String): Boolean {
        val s = input.lowercase(Locale.getDefault())
        return knowledge.keys.any { s.contains(it) }
    }

    fun answer(input: String): String {
        val s = input.lowercase(Locale.getDefault())
        val match = knowledge.entries.firstOrNull { s.contains(it.key) }
        return match?.value ?: "Nu am această informație în baza mea locală."
    }
}
