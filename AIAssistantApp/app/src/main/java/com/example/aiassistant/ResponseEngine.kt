package com.example.aiassistant

object ResponseEngine {
    fun unknown(input: String): String =
        "Nu am suficiente cunoștințe locale pentru un răspuns verificabil. " +
        "Pentru informații externe sunt necesare motorul web sau Gemini."
}
