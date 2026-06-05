package com.vincent.voicedrop.language.configs

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguageConfig
import com.vincent.voicedrop.language.PlacePatternConfig
import com.vincent.voicedrop.language.TimeParserConfig
import java.util.Calendar

object EsLanguageConfig : LanguageConfig {

    override val language = AppLanguage.SPANISH

    private val numberWords = mapOf(
        "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "once" to 11, "doce" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("compras", "compra", "supermercado", "mercado"),
        Category.IDEEEN        to listOf("idea", "ideas"),
        Category.TODO          to listOf("tarea", "tareas", "todo", "to-do", "pendiente"),
        Category.HERINNERINGEN to listOf("recordatorio", "recordar", "recuerda", "recordatorio"),
        Category.AGENDA        to listOf("agenda", "cita", "calendario", "reunion", "reunión"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("(?<![a-z])cuando (?:este|esté) en casa(?![a-z])"),
            Regex("(?<![a-z])al llegar a casa(?![a-z])"),
            Regex("(?<![a-z])en casa(?![a-z])")
        ),
        workPatterns = listOf(
            Regex("(?<![a-z])cuando (?:este|esté) en el trabajo(?![a-z])"),
            Regex("(?<![a-z])al llegar al trabajo(?![a-z])"),
            Regex("(?<![a-z])en (?:el trabajo|la oficina)(?![a-z])")
        ),
        shopPatterns = listOf(
            Regex("(?<![a-z])cuando (?:este|esté) en el supermercado(?![a-z])"),
            Regex("(?<![a-z])al llegar al supermercado(?![a-z])"),
            Regex("(?<![a-z])en el supermercado(?![a-z])"),
            Regex("(?<![a-z])supermercado(?![a-z])")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "lunes"     to Calendar.MONDAY,
            "martes"    to Calendar.TUESDAY,
            "miercoles" to Calendar.WEDNESDAY,  "miércoles" to Calendar.WEDNESDAY,
            "jueves"    to Calendar.THURSDAY,
            "viernes"   to Calendar.FRIDAY,
            "sabado"    to Calendar.SATURDAY,   "sábado"    to Calendar.SATURDAY,
            "domingo"   to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("mañana", "manana"),
        dayAfterTomorrowWords = listOf("pasado mañana", "pasado manana"),
        todayWords            = listOf("hoy"),
        periodPatterns = listOf(
            "morning"   to Regex("(?<![a-z])(?:esta mañana|esta manana|por la mañana|por la manana)(?![a-z])"),
            "afternoon" to Regex("(?<![a-z])(?:esta tarde|por la tarde)(?![a-z])"),
            "evening"   to Regex("(?<![a-z])(?:esta tarde|por la tarde)(?![a-z])"),
            "night"     to Regex("(?<![a-z])(?:esta noche|por la noche)(?![a-z])")
        ),
        todayPrefixes         = listOf("esta ", "este "),
        relativeHalfHourPattern = Regex("\\ben\\s+media\\s+hora\\b"),
        relativeQuarterPattern  = Regex("\\ben\\s+(\\d+|$numAlt)\\s+cuartos?\\s+de\\s+hora\\b"),
        relativeUnitPattern     = Regex("\\ben\\s+(\\d+|$numAlt)\\s+(minutos?|horas?|d[ií]as?|semanas?)\\b"),
        unitMultipliers = mapOf(
            "minuto"  to 60_000L,            "minutos"  to 60_000L,
            "hora"    to 3_600_000L,          "horas"    to 3_600_000L,
            "dia"     to 86_400_000L,         "día"      to 86_400_000L,
            "dias"    to 86_400_000L,         "días"     to 86_400_000L,
            "semana"  to 7 * 86_400_000L,    "semanas"  to 7 * 86_400_000L
        ),
        // "9:30" / "14:30"
        clockPattern       = Regex("\\b(?:a las?\\s+)?(\\d{1,2})[:.](\\d{2})\\b"),
        // "nueve y media" → 9:30, halfHourIsBefore=false (captured number IS current hour)
        halfHourPattern    = Regex("\\b(?:a las?\\s+)?($numAlt|\\d{1,2})\\s+y\\s+media\\b"),
        halfHourIsBefore   = false,
        // "nueve y cuarto" → 9:15
        quarterOverPattern = Regex("\\b(?:a las?\\s+)?($numAlt|\\d{1,2})\\s+y\\s+cuarto\\b"),
        // "diez menos cuarto" → 9:45 (captures next hour: 10)
        quarterToPattern   = Regex("\\b(?:a las?\\s+)?($numAlt|\\d{1,2})\\s+menos\\s+(?:un\\s+)?cuarto\\b"),
        // "a las nueve" → 9:00
        hourWordPattern    = Regex("\\b(?:a )?las?\\s+($numAlt|\\d{1,2})\\b"),
        // "a las 9"
        atHourPattern      = Regex("\\ba las?\\s+(\\d{1,2}|$numAlt)\\b")
    )
}
