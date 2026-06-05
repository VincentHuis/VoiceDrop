package com.vincent.voicedrop.reminder

import com.vincent.voicedrop.data.Place

/**
 * Matcht ingesproken memo-tekst tegen de namen van opgeslagen plekken.
 * Vergelijking is case-insensitief; eerste woord-match wint.
 */
class PlaceParser(private val places: List<Place>) {

    data class Result(val place: Place?, val text: String)

    fun parse(raw: String): Result {
        if (places.isEmpty()) return Result(null, raw)
        val lower = raw.lowercase()
        // Probeer langere namen eerst zodat "Mom's house" wint van "house".
        val sorted = places.sortedByDescending { it.name.length }
        for (place in sorted) {
            val name = place.name.trim()
            if (name.isEmpty()) continue
            val pattern = Regex("\\b" + Regex.escape(name.lowercase()) + "\\b")
            val match = pattern.find(lower) ?: continue
            val cleaned = StringBuilder(raw)
                .delete(match.range.first, match.range.last + 1)
                .toString()
                .replace(Regex("\\s+"), " ")
                .trim()
            return Result(place, cleaned.ifEmpty { raw })
        }
        return Result(null, raw)
    }
}
