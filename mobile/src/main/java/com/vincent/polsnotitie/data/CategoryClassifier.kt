package com.vincent.polsnotitie.data

class CategoryClassifier {

    data class Result(val category: Category, val text: String)

    private val THRESHOLD = 0.7

    // Keywords from all supported languages — classification works regardless of app language
    private val keywords: Map<Category, List<String>> = mapOf(
        Category.BOODSCHAPPEN  to listOf(
            "boodschappen", "boodschap", "winkel", "supermarkt",
            "einkaufen", "einkauf", "lebensmittel",
            "groceries", "grocery", "shopping", "supermarket"
        ),
        Category.IDEEEN        to listOf(
            "idee", "ideeen", "ideetje", "ideetjes",
            "ideen",
            "idea", "ideas"
        ),
        Category.TODO          to listOf(
            "todo", "to do", "to-do", "taak", "taken",
            "aufgabe", "aufgaben",
            "task", "tasks"
        ),
        Category.HERINNERINGEN to listOf(
            "herinnering", "herinneringen", "herinner",
            "erinnerung", "erinnerungen", "erinnere",
            "reminder", "reminders", "remind"
        ),
        Category.AGENDA        to listOf(
            "agenda", "afspraak", "kalender",
            "termin",
            "appointment", "calendar"
        ),
        Category.OVERIG        to emptyList()
    )

    fun classify(raw: String): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result(Category.OVERIG, trimmed)

        val words = trimmed.split(Regex("\\s+"))
        val first = words[0]
        val firstTwo = if (words.size >= 2) "${words[0]} ${words[1]}" else null

        var bestCategory: Category? = null
        var bestConsumed = 0
        var bestScore = 0.0

        for (category in Category.entries) {
            val kws = keywords[category] ?: continue
            if (kws.isEmpty()) continue
            for (keyword in kws) {
                score(first, keyword)?.let { s ->
                    if (s > bestScore) { bestScore = s; bestCategory = category; bestConsumed = 1 }
                }
                if (firstTwo != null) {
                    score(firstTwo, keyword)?.let { s ->
                        if (s > bestScore) { bestScore = s; bestCategory = category; bestConsumed = 2 }
                    }
                }
            }
        }

        val category = bestCategory ?: return Result(Category.OVERIG, trimmed)
        val remaining = words.drop(bestConsumed).joinToString(" ").trim()
        return Result(category, remaining.ifEmpty { trimmed })
    }

    private fun score(candidate: String, keyword: String): Double? {
        val a = normalize(candidate)
        val b = normalize(keyword)
        if (a.isEmpty() || b.isEmpty()) return null
        val maxLen = maxOf(a.length, b.length)
        val similarity = 1.0 - levenshtein(a, b).toDouble() / maxLen
        return if (similarity >= THRESHOLD) similarity else null
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace("ë", "e").replace("é", "e").replace("è", "e")
            .replace("ï", "i").replace("ö", "o").replace("ü", "u")
            .replace(Regex("[^a-z0-9]"), "")

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}
