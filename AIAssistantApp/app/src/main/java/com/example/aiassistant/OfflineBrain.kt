package com.example.aiassistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creier offline minimal — folosit doar fără cheie API sau internet. */
object OfflineBrain {

    fun reply(input: String): String {
        val msg = input.lowercase().trim()
        return when {
            msg.contains("salut") || msg.contains("bună") || msg.contains("hei") ->
                "Salut! 👋 Mod offline. Adaugă o cheie Gemini API pentru: căutare live, cod complet, voce, istoric."
            msg.contains("ora") || msg.contains("ceas") ->
                "Este ora ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}."
            msg.contains("dat") || msg.contains("ziua") ->
                "Astăzi este ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ro")).format(Date())}."
            msg.contains("cine ești") || msg.contains("ce ești") ->
                "Sunt AI Assistant Pro cu creier propriu. Offline gândesc local; cu API devin complet."
            msg.startsWith("calculează") || msg.startsWith("calc") -> calculate(msg)
            msg.contains("mulțumesc") || msg.contains("mersi") -> "Cu plăcere! 😊"
            else -> "Mod offline: pot calcula (calculează 5*5+2), ora, data. Cu cheie API: căutare live, cod complet, voce, istoric, export ZIP."
        }
    }

    private fun calculate(msg: String): String {
        val expr = msg.replace("calculează", "").replace("calc", "")
            .replace("x", "*").replace("×", "*").replace("÷", "/").trim()
        return try {
            if (!expr.matches(Regex("[0-9+\-*/(). ]+"))) return "Doar expresii matematice."
            "Rezultat: $expr = ${eval(expr)}"
        } catch (e: Exception) { "Încearcă: calculează 12*(3+4)" }
    }

    private fun eval(expr: String): Double {
        var s = expr.replace(" ", "")
        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var i = 0
        while (i < s.length) {
            when {
                s[i].isDigit() || s[i] == '.' -> {
                    var num = ""
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) { num += s[i]; i++ }
                    numbers.add(num.toDouble()); continue
                }
                s[i] == '(' -> {
                    var depth = 1; var j = i + 1
                    while (j < s.length && depth > 0) { if (s[j] == '(') depth++; if (s[j] == ')') depth--; j++ }
                    numbers.add(eval(s.substring(i + 1, j - 1))); i = j; continue
                }
                s[i] in "+-*/" -> ops.add(s[i])
            }
            i++
        }
        var k = 0
        while (k < ops.size) {
            if (ops[k] == '*' || ops[k] == '/') {
                val res = if (ops[k] == '*') numbers[k] * numbers[k + 1] else numbers[k] / numbers[k + 1]
                numbers[k] = res; numbers.removeAt(k + 1); ops.removeAt(k)
            } else k++
        }
        var result = numbers[0]
        for (idx in ops.indices) {
            result = if (ops[idx] == '+') result + numbers[idx + 1] else result - numbers[idx + 1]
        }
        return result
    }
}
