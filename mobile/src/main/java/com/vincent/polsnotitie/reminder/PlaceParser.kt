package com.vincent.polsnotitie.reminder

import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.language.LanguageConfig

class PlaceParser(private val config: LanguageConfig) {

    data class Result(val place: PlaceType?, val text: String)

    fun parse(raw: String): Result {
        val lower = raw.lowercase()
        val pp = config.placePatterns
        val allPatterns = listOf(
            PlaceType.THUIS      to pp.homePatterns,
            PlaceType.WERK       to pp.workPatterns,
            PlaceType.SUPERMARKT to pp.shopPatterns
        )
        for ((place, patterns) in allPatterns) {
            for (pattern in patterns) {
                val match = pattern.find(lower) ?: continue
                val cleaned = StringBuilder(raw)
                    .delete(match.range.first, match.range.last + 1)
                    .toString()
                    .replace(Regex("\\s+"), " ")
                    .trim()
                return Result(place, cleaned.ifEmpty { raw })
            }
        }
        return Result(null, raw)
    }
}
