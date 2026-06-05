package com.vincent.voicedrop.language.configs

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguageConfig
import com.vincent.voicedrop.language.PlacePatternConfig
import com.vincent.voicedrop.language.TimeParserConfig
import java.util.Calendar

object JaLanguageConfig : LanguageConfig {

    override val language = AppLanguage.JAPANESE

    private val numberWords = mapOf(
        "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
        "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10,
        "十一" to 11, "十二" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("買い物", "かいもの", "スーパー", "買物", "食料品"),
        Category.IDEEEN        to listOf("アイデア", "考え"),
        Category.TODO          to listOf("タスク", "やること", "todo"),
        Category.HERINNERINGEN to listOf("リマインダー", "リマインド", "覚え"),
        Category.AGENDA        to listOf("予定", "スケジュール", "アポ", "カレンダー"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("家に着いたら"),
            Regex("帰宅したら"),
            Regex("自宅に着いたら"),
            Regex("家で")
        ),
        workPatterns = listOf(
            Regex("職場に着いたら"),
            Regex("会社に着いたら"),
            Regex("職場で"),
            Regex("会社で")
        ),
        shopPatterns = listOf(
            Regex("スーパーに着いたら"),
            Regex("スーパーマーケットに着いたら"),
            Regex("スーパーで"),
            Regex("スーパーマーケット"),
            Regex("スーパー")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "月曜日" to Calendar.MONDAY,   "月曜" to Calendar.MONDAY,
            "火曜日" to Calendar.TUESDAY,  "火曜" to Calendar.TUESDAY,
            "水曜日" to Calendar.WEDNESDAY,"水曜" to Calendar.WEDNESDAY,
            "木曜日" to Calendar.THURSDAY, "木曜" to Calendar.THURSDAY,
            "金曜日" to Calendar.FRIDAY,   "金曜" to Calendar.FRIDAY,
            "土曜日" to Calendar.SATURDAY, "土曜" to Calendar.SATURDAY,
            "日曜日" to Calendar.SUNDAY,   "日曜" to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("明日", "あした", "あす"),
        dayAfterTomorrowWords = listOf("明後日", "あさって"),
        todayWords            = listOf("今日", "きょう"),
        periodPatterns = listOf(
            "morning"   to Regex("今日?(?:の)?(?:午前|朝)|(?:午前|朝)"),
            "afternoon" to Regex("今日?(?:の)?(?:午後|昼)|(?:午後|昼)"),
            "evening"   to Regex("今日?(?:の)?夕方|夕方"),
            "night"     to Regex("今日?(?:の)?(?:夜|晩)|(?:夜|晩)")
        ),
        todayPrefixes         = listOf("今日", "今"),
        // "半時間後" or "30分後" — handled by relativeUnitPattern
        relativeHalfHourPattern = Regex("半時間後"),
        relativeQuarterPattern  = Regex("(?!)"),  // not commonly used
        // "30分後", "2時間後", "3日後", "1週間後" — Japanese has no spaces
        relativeUnitPattern     = Regex("(\\d+|$numAlt)(分|時間|日|週間)後"),
        unitMultipliers = mapOf(
            "分"   to 60_000L,
            "時間" to 3_600_000L,
            "日"   to 86_400_000L,
            "週間" to 7 * 86_400_000L
        ),
        // "9時30分" or "9:30"
        clockPattern       = Regex("(\\d{1,2})[時:](\\d{2})"),
        // "9時半" = 9:30
        halfHourPattern    = Regex("(\\d{1,2})時半"),
        halfHourIsBefore   = false,
        // not common in Japanese — dummy patterns
        quarterOverPattern = Regex("(?!)"),
        quarterToPattern   = Regex("(?!)"),
        // "9時" = 9:00 (not followed by digit to avoid matching "9時30分" already caught by clock)
        hourWordPattern    = Regex("(\\d{1,2})時(?!\\d)"),
        atHourPattern      = Regex("(\\d{1,2})時(?!\\d)")
    )
}
