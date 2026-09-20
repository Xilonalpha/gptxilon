package com.example.aiassistant

import java.util.Locale

object UnitConversionEngine {

    private data class Unit(val aliases: List<String>, val factor: Double)

    private val length = listOf(
        Unit(listOf("km", "kilometru", "kilometri"), 1000.0),
        Unit(listOf("m", "metru", "metri"), 1.0),
        Unit(listOf("cm", "centimetru", "centimetri"), 0.01),
        Unit(listOf("mm", "milimetru", "milimetri"), 0.001),
        Unit(listOf("mile", "mi"), 1609.344),
        Unit(listOf("ft", "picior", "picioare"), 0.3048)
    )

    private val mass = listOf(
        Unit(listOf("kg", "kilogram", "kilograme"), 1.0),
        Unit(listOf("g", "gram", "grame"), 0.001),
        Unit(listOf("lb", "lbs", "pound", "pounds"), 0.45359237)
    )

    fun canHandle(input: String): Boolean {
        val s = input.lowercase(Locale.getDefault())
        return s.contains("convertește") ||
                s.contains("converteste") ||
                s.contains("transformă") ||
                s.contains("transforma") ||
                Regex("""\d+(?:[.,]\d+)?\s*(km|mile|mi|m|cm|mm|kg|g|lb|lbs|c|f|°c|°f)""").containsMatchIn(s)
    }

    fun convert(input: String): String {
        val s = input.lowercase(Locale.getDefault())
            .replace(",", ".")

        val temp = Regex("""([0-9.]+)\s*(°?c|°?f)\s*(?:în|in|to|->)\s*(°?c|°?f)""")
            .find(s)

        if (temp != null) {
            val value = temp.groupValues[1].toDouble()
            val from = temp.groupValues[2]
            val to = temp.groupValues[3]

            val result = when {
                from.contains("c") && to.contains("f") -> value * 9.0 / 5.0 + 32
                from.contains("f") && to.contains("c") -> (value - 32) * 5.0 / 9.0
                else -> value
            }

            return "Rezultat: ${format(result)} °${to.uppercase().replace("°", "")}"
        }

        val regex = Regex(
            """([0-9.]+)\s*([a-zăâîșț]+)\s*(?:în|in|to|->)\s*([a-zăâîșț]+)"""
        )

        val match = regex.find(s) ?: return "Format: 10 km în mile"

        val value = match.groupValues[1].toDouble()
        val from = match.groupValues[2]
        val to = match.groupValues[3]

        val result = convertLinear(value, from, to)
            ?: return "Nu cunosc această conversie."

        return "Rezultat: ${format(result)} $to"
    }

    private fun convertLinear(value: Double, from: String, to: String): Double? {
        val fromLength = length.find { from in it.aliases }
        val toLength = length.find { to in it.aliases }

        if (fromLength != null && toLength != null) {
            return value * fromLength.factor / toLength.factor
        }

        val fromMass = mass.find { from in it.aliases }
        val toMass = mass.find { to in it.aliases }

        if (fromMass != null && toMass != null) {
            return value * fromMass.factor / toMass.factor
        }

        return null
    }

    private fun format(v: Double): String {
        return if (kotlin.math.abs(v - kotlin.math.round(v)) < 0.0000001)
            kotlin.math.round(v).toLong().toString()
        else
            "%.6f".format(Locale.US, v).trimEnd('0').trimEnd('.')
    }
}
