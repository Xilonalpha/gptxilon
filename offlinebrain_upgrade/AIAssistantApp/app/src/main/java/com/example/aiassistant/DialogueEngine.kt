package com.example.aiassistant

import java.util.Locale

object DialogueEngine {
    private enum class Intent { GREETING, THANKS, IDENTITY, CAPABILITIES, STATUS, GOODBYE, HELP, ACK, NONE }

    fun canHandle(input:String)=detect(input)!=Intent.NONE

    fun answer(input:String):String?=when(detect(input)) {
        Intent.GREETING -> "Salut! 👋 Sunt AI Assistant. Pot conversa, explica, calcula și raționa local."
        Intent.THANKS -> "Cu plăcere! 😊"
        Intent.IDENTITY -> "Sunt AI Assistant. Pentru funcțiile offline folosesc OfflineBrain: un nucleu local determinist de cunoștințe, dialog, raționament și verificare."
        Intent.CAPABILITIES -> "Pot conversa, răspunde la multe întrebări generale din baza locală, calcula, converti, explica, raționa și verifica. Pentru informații care trebuie să fie actuale, ruta web este separată."
        Intent.STATUS -> "Sunt gata. Spune-mi ce vrei să analizăm."
        Intent.GOODBYE -> "Pe curând! 👋"
        Intent.HELP -> "Spune-mi direct obiectivul. Pot încerca să răspund local, să raționez sau să verific o afirmație."
        Intent.ACK -> "Am înțeles. Continuă."
        Intent.NONE -> null
    }

    private fun detect(input:String):Intent {
        val s=input.trim().lowercase(Locale.ROOT)
        if(s.matches(Regex("^(salut|bună|buna|hei|hello|hi|hey)[!.? ]*$"))) return Intent.GREETING
        if(listOf("mulțumesc","multumesc","mersi","thanks").any{s.contains(it)}) return Intent.THANKS
        if(listOf("cine ești","cine esti","ce ești","ce esti","who are you").any{s.contains(it)}) return Intent.IDENTITY
        if(listOf("ce poți","ce poti","ce știi","ce stii","what can you do").any{s.contains(it)}) return Intent.CAPABILITIES
        if(listOf("cum ești","cum esti","how are you").any{s.contains(it)} || s=="status") return Intent.STATUS
        if(s.matches(Regex("^(pa|bye|goodbye|la revedere|pe curând|pe curand)[!.? ]*$"))) return Intent.GOODBYE
        if(s=="help" || s.contains("ajutor") || s.contains("mă poți ajuta") || s.contains("ma poti ajuta")) return Intent.HELP
        if(s in setOf("ok","bine","perfect","am inteles","am înțeles")) return Intent.ACK
        return Intent.NONE
    }
}
