package com.example.aiassistant

object ConceptGraphEngine {
    data class Link(val a: String, val b: String, val weight: Int)
    private val links = mutableListOf<Link>()

    @Synchronized fun observe(input: String) {
        val concepts = EntityEngine.extract(input).entities.filter { it.type == "TOKEN" }.map { it.text.lowercase() }.distinct().filter { it.length >= 4 }.take(10)
        for (i in 0 until concepts.size) for (j in i + 1 until concepts.size) {
            val a = concepts[i]; val b = concepts[j]
            val idx = links.indexOfFirst { (it.a == a && it.b == b) || (it.a == b && it.b == a) }
            if (idx >= 0) links[idx] = links[idx].copy(weight = (links[idx].weight + 1).coerceAtMost(100)) else links += Link(a,b,1)
        }
        if (links.size > 300) links.removeAt(0)
    }

    @Synchronized fun related(term: String, limit: Int = 8): List<Link> {
        val q = term.lowercase()
        return links.filter { it.a.contains(q) || it.b.contains(q) }.sortedByDescending { it.weight }.take(limit)
    }
}
