package com.example.aiassistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeEngine {

    fun canHandle(input: String): Boolean {
        val s = input.lowercase(Locale.getDefault())

        return s.contains("ce oră") ||
                s.contains("ce ora") ||
                s.contains("ora acum") ||
                s.contains("cât este ora") ||
                s.contains("cat este ora") ||
                s.contains("ce dată") ||
                s.contains("ce data") ||
                s.contains("data de azi") ||
                s.contains("astăzi") ||
                s.contains("astazi")
    }

    fun answer(input: String): String {
        val s = input.lowercase(Locale.getDefault())

        if (s.contains("oră") || s.contains("ora")) {
            val time = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            return "Ora locală este $time."
        }

        val date = SimpleDateFormat(
            "EEEE, d MMMM yyyy",
            Locale("ro")
        ).format(Date())

        return "Astăzi este $date."
    }
}
