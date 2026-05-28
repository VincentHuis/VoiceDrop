package com.vincent.polsnotitie.reminder

import com.vincent.polsnotitie.data.PlaceType

/**
 * Herkent een plek-trigger in een herinnering-tekst ("als ik thuis ben", "op werk",
 * "bij de supermarkt") en haalt die woorden uit de tekst. Geen plek herkend -> null.
 */
object PlaceParser {

    data class Result(val place: PlaceType?, val text: String)

    private val PATTERNS = listOf(
        PlaceType.THUIS to listOf(
            "als ik thuis (?:ben|kom|aankom)",
            "wanneer ik thuis (?:ben|kom)",
            "zodra ik thuis (?:ben|kom)",
            "bij thuiskomst",
            "thuis"
        ),
        PlaceType.WERK to listOf(
            "als ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom|aankom)",
            "wanneer ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom)",
            "zodra ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom)",
            "op (?:het |mijn )?werk",
            "op kantoor"
        ),
        PlaceType.SUPERMARKT to listOf(
            "als ik (?:bij|in) de supermarkt ben",
            "bij de supermarkt",
            "in de supermarkt",
            "bij de winkel",
            "supermarkt"
        )
    )

    fun parse(raw: String): Result {
        val lower = raw.lowercase()
        for ((place, patterns) in PATTERNS) {
            for (pattern in patterns) {
                val regex = Regex("(?<![a-z])(?:$pattern)(?![a-z])")
                val match = regex.find(lower) ?: continue
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
