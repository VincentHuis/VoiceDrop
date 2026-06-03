package com.vincent.polsnotitie.reminder

import com.vincent.polsnotitie.language.LanguageConfig
import java.util.Calendar

class ReminderTimeParser(private val config: LanguageConfig) {

    data class Result(val remindAt: Long?, val text: String)

    private enum class Period { MORNING, AFTERNOON, EVENING, NIGHT }
    private data class TimeMatch(val hour: Int, val minute: Int, val ambiguous: Boolean)

    private val tc = config.timeParser

    fun parse(raw: String, now: Long = System.currentTimeMillis()): Result {
        val lower = raw.lowercase()
        val ranges = mutableListOf<IntRange>()

        // 1) Relative: "over een half uur", "in einer stunde", "in one hour"
        tc.relativeHalfHourPattern.find(lower)?.let { m ->
            return Result(now + 30 * 60_000L, clean(raw, listOf(m.range)))
        }
        tc.relativeQuarterPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            return Result(now + n * 15 * 60_000L, clean(raw, listOf(m.range)))
        }
        tc.relativeUnitPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            val unitMs = tc.unitMultipliers[m.groupValues[2]] ?: return@let
            return Result(now + n * unitMs, clean(raw, listOf(m.range)))
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // 2) Period (morning/afternoon/evening/night)
        val (period, vanToday) = detectPeriod(lower, ranges)

        var dayFound = false
        var pushIfPast = false
        var weekdayMatched = false

        // 3) Day
        if (vanToday) {
            dayFound = true; pushIfPast = true
        } else {
            when {
                containsAny(lower, tc.dayAfterTomorrowWords, ranges) -> {
                    cal.add(Calendar.DAY_OF_YEAR, 2); dayFound = true
                }
                containsAny(lower, tc.tomorrowWords, ranges) -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1); dayFound = true
                }
                containsAny(lower, tc.todayWords, ranges) -> {
                    dayFound = true; pushIfPast = true
                }
                else -> {
                    for ((name, dow) in tc.weekdays) {
                        if (contains(lower, name, ranges)) {
                            val diff = ((dow - cal.get(Calendar.DAY_OF_WEEK)) + 7) % 7
                            cal.add(Calendar.DAY_OF_YEAR, diff)
                            dayFound = true; pushIfPast = true; weekdayMatched = true
                            break
                        }
                    }
                }
            }
        }

        // 4) Time
        val time = parseTime(lower, ranges, dayFound)
        var timeFound = false
        if (time != null) {
            cal.set(Calendar.HOUR_OF_DAY, applyPeriod(time.hour, time.ambiguous, period))
            cal.set(Calendar.MINUTE, time.minute)
            timeFound = true
            if (!dayFound) pushIfPast = true
        } else if (dayFound || period != null) {
            cal.set(Calendar.HOUR_OF_DAY, defaultHourFor(period))
            cal.set(Calendar.MINUTE, 0)
            if (!dayFound) { dayFound = true; pushIfPast = true }
        }

        if (!dayFound && !timeFound) return Result(null, raw)

        if (cal.timeInMillis <= now && pushIfPast) {
            val step = if (timeFound && weekdayMatched) 7 else 1
            cal.add(Calendar.DAY_OF_YEAR, step)
        }

        return Result(cal.timeInMillis, clean(raw, ranges))
    }

    private fun detectPeriod(lower: String, ranges: MutableList<IntRange>): Pair<Period?, Boolean> {
        for ((key, pattern) in tc.periodPatterns) {
            val m = pattern.find(lower) ?: continue
            ranges += m.range
            val isToday = tc.todayPrefixes.any { m.value.startsWith(it) }
            val period = when (key) {
                "morning"   -> Period.MORNING
                "afternoon" -> Period.AFTERNOON
                "evening"   -> Period.EVENING
                "night"     -> Period.NIGHT
                else        -> null
            }
            return period to isToday
        }
        return null to false
    }

    private fun parseTime(lower: String, ranges: MutableList<IntRange>, dayFound: Boolean = false): TimeMatch? {
        // HH:mm / HH.mm explicit
        tc.clockPattern.find(lower)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) { ranges += m.range; return TimeMatch(h, min, false) }
        }
        // "half nine" / "halb neun" — semantics differ per language (halfHourIsBefore)
        // ambiguous only when there is no explicit day context (no day word); with a day word
        // "halb neun morgen" already anchors the hour and we don't want +12 applied.
        tc.halfHourPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            val h = if (tc.halfHourIsBefore) hourBefore(n) else n
            ranges += m.range; return TimeMatch(h, 30, ambiguous = !dayFound)
        }
        // quarter past / kwart over / viertel nach
        tc.quarterOverPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(n, 15, true)
        }
        // quarter to / kwart voor / viertel vor
        tc.quarterToPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(hourBefore(n), 45, true)
        }
        // "9 uur" / "9 Uhr" / "9 o'clock"
        tc.hourWordPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, true) }
        }
        // "om 9" / "um 9" / "at 9"
        tc.atHourPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, true) }
        }
        return null
    }

    private fun applyPeriod(hour: Int, ambiguous: Boolean, period: Period?): Int {
        if (!ambiguous) return hour
        return when (period) {
            Period.MORNING -> hour
            Period.AFTERNOON, Period.EVENING -> if (hour in 1..11) hour + 12 else hour
            Period.NIGHT -> hour
            null -> if (hour in 1..10) hour + 12 else hour
        }
    }

    private fun defaultHourFor(period: Period?): Int = when (period) {
        Period.MORNING -> 9; Period.AFTERNOON -> 14; Period.EVENING -> 19; Period.NIGHT -> 23; null -> 9
    }

    private fun hourBefore(n: Int): Int = ((n - 2 + 12) % 12) + 1

    private fun toInt(token: String): Int? = token.toIntOrNull() ?: tc.numberWords[token]

    private fun contains(lower: String, word: String, ranges: MutableList<IntRange>): Boolean {
        // \b does not work with CJK characters; use plain match for non-ASCII words
        val isCjk = word.any { it.code > 0x2E7F }
        val pattern = if (isCjk) Regex(Regex.escape(word))
                      else Regex("\\b${Regex.escape(word)}\\b")
        val m = pattern.find(lower) ?: return false
        ranges += m.range; return true
    }

    private fun containsAny(lower: String, words: List<String>, ranges: MutableList<IntRange>): Boolean {
        for (word in words) { if (contains(lower, word, ranges)) return true }
        return false
    }

    private fun clean(raw: String, ranges: List<IntRange>): String {
        val sb = StringBuilder(raw)
        for (r in ranges.sortedByDescending { it.first }) {
            if (r.first in sb.indices && r.last in sb.indices) sb.delete(r.first, r.last + 1)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
