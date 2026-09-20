package com.example.aiassistant

import kotlin.math.sqrt

object StatisticsEngine {
    private val number = Regex("""-?\d+(?:[.,]\d+)?""")

    fun canHandle(input: String): Boolean {
        val s = input.lowercase()
        return (s.contains("medie") || s.contains("median") || s.contains("varian") ||
                s.contains("devia") || s.contains("outlier") || s.contains("abatere")) &&
                number.findAll(input).count() >= 2
    }

    fun answer(input: String): String {
        val values = number.findAll(input).map { it.value.replace(',', '.').toDouble() }.toList()
        val sorted = values.sorted()
        val mean = values.average()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        val sd = sqrt(variance)
        return "n=${values.size}; medie=${fmt(mean)}; mediană=${fmt(median)}; varianță=${fmt(variance)}; deviație standard=${fmt(sd)}."
    }

    private fun fmt(v: Double) = "%.6f".format(java.util.Locale.US, v).trimEnd('0').trimEnd('.')
}
