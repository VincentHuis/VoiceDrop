package com.vincent.polsnotitie.language.configs

import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.language.AppLanguage
import com.vincent.polsnotitie.language.LanguageConfig
import com.vincent.polsnotitie.language.PlacePatternConfig
import com.vincent.polsnotitie.language.TimeParserConfig
import java.util.Calendar

object KoLanguageConfig : LanguageConfig {

    override val language = AppLanguage.KOREAN

    // Native Korean numbers (for hours) + Sino-Korean (for minutes/general)
    private val numberWords = mapOf(
        "한" to 1, "두" to 2, "세" to 3, "네" to 4, "다섯" to 5,
        "여섯" to 6, "일곱" to 7, "여덟" to 8, "아홉" to 9, "열" to 10,
        "열한" to 11, "열두" to 12,
        "일" to 1, "이" to 2, "삼" to 3, "사" to 4, "오" to 5,
        "육" to 6, "칠" to 7, "팔" to 8, "구" to 9, "십" to 10
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("쇼핑", "장보기", "마트", "슈퍼마켓", "슈퍼"),
        Category.IDEEEN        to listOf("아이디어", "생각"),
        Category.TODO          to listOf("할일", "할 일", "과제", "todo"),
        Category.HERINNERINGEN to listOf("알림", "리마인더", "기억"),
        Category.AGENDA        to listOf("일정", "약속", "캘린더"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("집에 도착하면"),
            Regex("집에 왔을 때"),
            Regex("집에서")
        ),
        workPatterns = listOf(
            Regex("회사에 도착하면"),
            Regex("직장에 도착하면"),
            Regex("회사에서"),
            Regex("직장에서")
        ),
        shopPatterns = listOf(
            Regex("마트에 도착하면"),
            Regex("슈퍼마켓에 도착하면"),
            Regex("마트에서"),
            Regex("슈퍼마켓에서"),
            Regex("슈퍼마켓"),
            Regex("마트")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "월요일" to Calendar.MONDAY,   "화요일" to Calendar.TUESDAY,
            "수요일" to Calendar.WEDNESDAY, "목요일" to Calendar.THURSDAY,
            "금요일" to Calendar.FRIDAY,   "토요일" to Calendar.SATURDAY,
            "일요일" to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("내일"),
        dayAfterTomorrowWords = listOf("모레"),
        todayWords            = listOf("오늘"),
        periodPatterns = listOf(
            "morning"   to Regex("오늘\\s*(?:오전|아침)|(?:오전|아침)"),
            "afternoon" to Regex("오늘\\s*오후|오후"),
            "evening"   to Regex("오늘\\s*저녁|저녁"),
            "night"     to Regex("오늘\\s*밤|밤")
        ),
        todayPrefixes         = listOf("오늘"),
        // "반 시간 후" or "30분 후" handled by relativeUnitPattern
        relativeHalfHourPattern = Regex("반\\s*시간\\s*후"),
        relativeQuarterPattern  = Regex("(?!)"),  // not commonly used
        // "30분 후", "2시간 후", "3일 후", "1주 후"
        relativeUnitPattern     = Regex("(\\d+|$numAlt)\\s*(분|시간|일|주)\\s*후"),
        unitMultipliers = mapOf(
            "분"   to 60_000L,
            "시간" to 3_600_000L,
            "일"   to 86_400_000L,
            "주"   to 7 * 86_400_000L
        ),
        // "9시 30분"
        clockPattern       = Regex("(\\d{1,2})시\\s*(\\d{2})분"),
        // "9시 반" = 9:30
        halfHourPattern    = Regex("(\\d{1,2})시\\s*반"),
        halfHourIsBefore   = false,
        // not common in Korean — use dummy patterns
        quarterOverPattern = Regex("(?!)"),
        quarterToPattern   = Regex("(?!)"),
        // "9시" = 9:00
        hourWordPattern    = Regex("(\\d{1,2})시(?!\\s*\\d)"),
        // "오전 9시" / "오후 3시" — the period detection handles AM/PM
        atHourPattern      = Regex("(\\d{1,2})시(?!\\s*\\d)")
    )
}
