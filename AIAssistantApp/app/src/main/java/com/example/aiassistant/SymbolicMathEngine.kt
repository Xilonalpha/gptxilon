package com.example.aiassistant

object SymbolicMathEngine {
    private val equation = Regex("""(-?\d+(?:[.,]\d+)?)\s*x\s*([+-])\s*(-?\d+(?:[.,]\d+)?)\s*=\s*(-?\d+(?:[.,]\d+)?)""")

    fun canHandle(input: String): Boolean =
        input.contains("=") && Regex("""\d\s*x""", RegexOption.IGNORE_CASE).containsMatchIn(input)

    fun answer(input: String): String {
        val m = equation.find(input.replace(',', '.')) ?: return "Pot rezolva ecuații liniare simple de forma ax+b=c."
        val a = m.groupValues[1].toDouble()
        val b0 = m.groupValues[3].toDouble()
        val b = if (m.groupValues[2] == "-") -b0 else b0
        val c = m.groupValues[4].toDouble()
        if (a == 0.0) return if (b == c) "Ecuația are infinit de multe soluții." else "Ecuația nu are soluție."
        return "Rezolvare: x = ${fmt((c - b) / a)}."
    }

    private fun fmt(v: Double) = "%.6f".format(java.util.Locale.US, v).trimEnd('0').trimEnd('.')
}
