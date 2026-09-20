package com.example.aiassistant

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TemporalReasoningEngine {
    fun infer(input: String): String? {
        val s = input.lowercase(Locale.getDefault())
        val cal = Calendar.getInstance()
        val date = when {
            s.contains("mâine") || s.contains("maine") -> { cal.add(Calendar.DAY_OF_YEAR, 1); cal.time }
            s.contains("ieri") -> { cal.add(Calendar.DAY_OF_YEAR, -1); cal.time }
            s.contains("poimâine") || s.contains("poimaine") -> { cal.add(Calendar.DAY_OF_YEAR, 2); cal.time }
            s.contains("răspoimâine") || s.contains("raspoimaine") -> { cal.add(Calendar.DAY_OF_YEAR, 3); cal.time }
            s.contains("astăzi") || s.contains("azi") -> cal.time
            else -> null
        } ?: return null
        return SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(date)
    }
}
