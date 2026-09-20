package com.example.aiassistant

object TextAnalysisEngine {

    fun canHandle(input: String): Boolean {
        val s = input.lowercase()

        return s.contains("numără cuvintele") ||
                s.contains("numara cuvintele") ||
                s.contains("câte cuvinte") ||
                s.contains("cate cuvinte") ||
                s.contains("lungimea textului") ||
                s.contains("numără caracterele") ||
                s.contains("numara caracterele")
    }

    fun analyze(input: String): String {
        val text = input
            .substringAfter(":", "")
            .ifBlank {
                input
                    .replace(Regex("(?i)numără cuvintele|numara cuvintele|câte cuvinte|cate cuvinte|lungimea textului|numără caracterele|numara caracterele"), "")
                    .trim()
            }

        val words = text
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return "Text: ${words.size} cuvinte, ${text.length} caractere."
    }
}
