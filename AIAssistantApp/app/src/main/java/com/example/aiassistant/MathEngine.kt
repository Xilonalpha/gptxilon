package com.example.aiassistant

import java.util.Locale
import kotlin.math.*

object MathEngine {

    fun canHandle(input: String): Boolean {
        val s = normalize(input)
        if (s.isBlank()) return false

        val expression = extractExpression(s)
        if (expression.isBlank()) return false

        return expression.matches(
            Regex("""[0-9+\-*/().%^,\s]+""")
        ) || s.contains("sqrt") || s.contains("rădăcina") ||
                s.contains("radacina") || s.contains("procent")
    }

    fun calculate(input: String): String {
        return try {
            val normalized = normalize(input)

            if (Regex("""[0-9]+(?:[.,][0-9]+)?\s*%\s*din\s*[0-9]+(?:[.,][0-9]+)?""").containsMatchIn(normalized) ||
                normalized.contains("procent din")
            ) {
                return percentage(normalized)
            }

            var expression = extractExpression(normalized)

            expression = expression
                .replace("sqrt", "sqrt")
                .replace("√", "sqrt")
                .replace("^", "^")

            val result = Parser(expression).parse()

            "Rezultat: ${format(result)}"
        } catch (_: Exception) {
            "Nu am putut calcula expresia. Exemplu: calculează 12*(3+4)-5."
        }
    }

    private fun percentage(s: String): String {
        val regex = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*%\s*din\s*([0-9]+(?:[.,][0-9]+)?)""")
        val m = regex.find(s) ?: throw IllegalArgumentException()
        val p = m.groupValues[1].replace(",", ".").toDouble()
        val n = m.groupValues[2].replace(",", ".").toDouble()
        return "Rezultat: ${format(n * p / 100.0)}"
    }

    private fun normalize(input: String): String {
        return input.lowercase(Locale.getDefault())
            .replace(",", ".")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("plus", "+")
            .replace("minus", "-")
    }

    private fun extractExpression(s: String): String {
        return s
            .replace("calculează", "")
            .replace("calculeaza", "")
            .replace("calculeaza-mi", "")
            .replace("calc", "")
            .replace("cât este", "")
            .replace("cat este", "")
            .replace("cât face", "")
            .replace("cat face", "")
            .replace("rezolvă", "")
            .replace("rezolva", "")
            .replace("rădăcina", "")
            .replace("radacina", "")
            .replace("radicalul", "")
            .replace("radical", "")
            .replace("=", "")
            .trim()
    }

    private fun format(value: Double): String {
        if (!value.isFinite()) return "nedefinit"
        if (abs(value - round(value)) < 0.000000001) {
            return round(value).toLong().toString()
        }
        return "%.8f".format(Locale.US, value).trimEnd('0').trimEnd('.')
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun parse(): Double {
            val result = expression()
            skipSpaces()
            if (pos != text.length) error("Invalid expression")
            return result
        }

        private fun expression(): Double {
            var value = term()

            while (true) {
                skipSpaces()
                value = when {
                    match('+') -> value + term()
                    match('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()

            while (true) {
                skipSpaces()
                value = when {
                    match('*') -> value * power()
                    match('/') -> value / power()
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            var value = unary()
            skipSpaces()

            if (match('^')) {
                value = value.pow(power())
            }

            return value
        }

        private fun unary(): Double {
            skipSpaces()

            return when {
                match('+') -> unary()
                match('-') -> -unary()
                matchWord("sqrt") -> sqrt(primary())
                else -> primary()
            }
        }

        private fun primary(): Double {
            skipSpaces()

            if (match('(')) {
                val value = expression()
                if (!match(')')) error("Missing )")
                return value
            }

            val start = pos

            while (pos < text.length &&
                (text[pos].isDigit() || text[pos] == '.')
            ) {
                pos++
            }

            if (start == pos) error("Number expected")

            return text.substring(start, pos).toDouble()
        }

        private fun match(c: Char): Boolean {
            if (pos < text.length && text[pos] == c) {
                pos++
                return true
            }
            return false
        }

        private fun matchWord(word: String): Boolean {
            skipSpaces()
            if (text.regionMatches(pos, word, 0, word.length)) {
                pos += word.length
                return true
            }
            return false
        }

        private fun skipSpaces() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }
    }
}
