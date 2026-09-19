package com.example.aiassistant

object CodeIntelligenceEngine {
    fun audit(text: String): String {
        if (text.isBlank()) {
            return """
                ⌨️ CODE AUDIT

                Nu există cod/text pentru audit.
            """.trimIndent()
        }

        val lines = text.lines()
        val longLines = lines.count { it.length > 140 }

        val todos = Regex("""(?i)\b(TODO|FIXME|XXX)\b""")
            .findAll(text)
            .count()

        val emptyCatches = Regex(
            """catch\s*\([^)]*\)\s*\{\s*\}""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(text).count()

        val hardcodedSecrets = Regex(
            """(?i)(api[_-]?key|secret|password|token)\s*[=:]\s*["'][^"']{8,}["']"""
        ).findAll(text).count()

        val dangerousExec = Regex(
            """(?i)Runtime\.getRuntime\(\)\.exec|ProcessBuilder\("""
        ).findAll(text).count()

        val networkCalls = Regex(
            """(?i)https?://|OkHttpClient|HttpURLConnection|WebView"""
        ).findAll(text).count()

        val functions = Regex(
            """(?m)^\s*(?:public\s+|private\s+|internal\s+|protected\s+)?(?:suspend\s+)?fun\s+"""
        ).findAll(text).count()

        val issues = mutableListOf<String>()

        if (hardcodedSecrets > 0) {
            issues += "🔴 Posibile secrete hardcodate: $hardcodedSecrets"
        }

        if (emptyCatches > 0) {
            issues += "🟠 catch-uri goale: $emptyCatches"
        }

        if (dangerousExec > 0) {
            issues += "🟠 execuție de proces detectată: $dangerousExec — verifică inputul."
        }

        if (todos > 0) {
            issues += "🟡 TODO/FIXME/XXX: $todos"
        }

        if (longLines > 0) {
            issues += "🟡 linii >140 caractere: $longLines"
        }

        return buildString {
            append("⌨️ CODE INTELLIGENCE AUDIT\n\n")
            append("Linii: ").append(lines.size).append('\n')
            append("Funcții detectate: ").append(functions).append('\n')
            append("Operații de rețea: ").append(networkCalls).append('\n')
            append("Probleme semnalate: ").append(issues.size).append("\n\n")

            if (issues.isEmpty()) {
                append("✅ Nu au fost găsite semnale locale evidente.\n")
            } else {
                issues.forEach {
                    append(it).append('\n')
                }
            }

            append(
                "\n⚠️ Audit static: rezultatul indică semnale de verificat, " +
                    "nu dovedește existența sau absența unui bug."
            )
        }
    }
}
