package com.vincent.polsnotitie.reminder

import java.util.Calendar

/**
 * Parst een Nederlands tijdstip uit een herinnering-tekst ("morgen om 9 uur", "over een uur",
 * "vrijdag om 15:30"). Conservatief: geeft `remindAt = null` als er geen duidelijke tijd in
 * staat, zodat de app dan om een tijd kan vragen. De herkende tijd-woorden worden uit de tekst
 * gehaald.
 *
 * Losse uren ("1 uur", "om 1", "half twee") zijn standaard **overdag**: 1–10 schuift naar de
 * middag/avond (+12), 11 en 12 blijven ochtend/middag. Met een dagdeel ("'s ochtends/middags/
 * avonds/nachts", "vanavond", …) volgt de tijd dat dagdeel. Een expliciete `HH:MM` blijft ongemoeid.
 */
object ReminderTimeParser {

    data class Result(val remindAt: Long?, val text: String)

    private enum class Period { MORNING, AFTERNOON, EVENING, NIGHT }

    private data class TimeMatch(val hour: Int, val minute: Int, val ambiguous: Boolean)

    private val NUM = mapOf(
        "een" to 1, "één" to 1, "twee" to 2, "drie" to 3, "vier" to 4, "vijf" to 5,
        "zes" to 6, "zeven" to 7, "acht" to 8, "negen" to 9, "tien" to 10,
        "elf" to 11, "twaalf" to 12
    )

    private val WEEKDAYS = mapOf(
        "maandag" to Calendar.MONDAY, "dinsdag" to Calendar.TUESDAY,
        "woensdag" to Calendar.WEDNESDAY, "donderdag" to Calendar.THURSDAY,
        "vrijdag" to Calendar.FRIDAY, "zaterdag" to Calendar.SATURDAY,
        "zondag" to Calendar.SUNDAY
    )

    private const val NUM_ALT = "een|één|twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf"

    fun parse(raw: String, now: Long = System.currentTimeMillis()): Result {
        val lower = raw.lowercase()
        val ranges = mutableListOf<IntRange>()

        // 1) Relatief: "over een half uur", "over 2 uur", "over tien minuten"
        relative(lower, now)?.let { (millis, range) ->
            ranges += range
            return Result(millis, clean(raw, ranges))
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 2) Dagdeel ('s ochtends/middags/avonds/nachts, vanavond, …)
        val (period, vanToday) = detectPeriod(lower, ranges)

        var dayFound = false
        var pushIfPast = false
        var weekdayMatched = false

        // 3) Dag
        if (vanToday) {
            dayFound = true
            pushIfPast = true
        } else {
            when {
                contains(lower, "overmorgen", ranges) -> { cal.add(Calendar.DAY_OF_YEAR, 2); dayFound = true }
                contains(lower, "morgen", ranges) -> { cal.add(Calendar.DAY_OF_YEAR, 1); dayFound = true }
                contains(lower, "vandaag", ranges) -> { dayFound = true; pushIfPast = true }
                else -> {
                    for ((name, dow) in WEEKDAYS) {
                        if (contains(lower, name, ranges)) {
                            val diff = ((dow - cal.get(Calendar.DAY_OF_WEEK)) + 7) % 7
                            cal.add(Calendar.DAY_OF_YEAR, diff)
                            dayFound = true
                            pushIfPast = true
                            weekdayMatched = true
                            break
                        }
                    }
                }
            }
        }

        // 4) Tijdstip
        val time = time(lower, ranges)
        var timeFound = false
        if (time != null) {
            cal.set(Calendar.HOUR_OF_DAY, applyPeriod(time.hour, time.ambiguous, period))
            cal.set(Calendar.MINUTE, time.minute)
            timeFound = true
            if (!dayFound) pushIfPast = true
        } else if (dayFound || period != null) {
            cal.set(Calendar.HOUR_OF_DAY, defaultHourFor(period))
            cal.set(Calendar.MINUTE, 0)
            if (!dayFound) {
                dayFound = true
                pushIfPast = true
            }
        }

        if (!dayFound && !timeFound) return Result(null, raw)

        if (cal.timeInMillis <= now && pushIfPast) {
            val step = if (timeFound && weekdayMatched) 7 else 1
            cal.add(Calendar.DAY_OF_YEAR, step)
        }

        return Result(cal.timeInMillis, clean(raw, ranges))
    }

    /** Past 12/24-uur disambiguatie toe op een los uur; HH:MM (ambiguous=false) blijft ongemoeid. */
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
        Period.MORNING -> 9
        Period.AFTERNOON -> 14
        Period.EVENING -> 19
        Period.NIGHT -> 23
        null -> 9
    }

    private fun detectPeriod(lower: String, ranges: MutableList<IntRange>): Pair<Period?, Boolean> {
        val patterns = listOf(
            Period.EVENING to "'?s\\s+avonds|savonds|vanavond|in\\s+de\\s+avond|avonds|avond",
            Period.AFTERNOON to "'?s\\s+middags|smiddags|vanmiddag|in\\s+de\\s+middag|middags|middag",
            Period.MORNING to "'?s\\s+ochtends|sochtends|'?s\\s+morgens|smorgens|vanochtend|vanmorgen|in\\s+de\\s+ochtend|ochtends|ochtend|morgens",
            Period.NIGHT to "'?s\\s+nachts|snachts|vannacht|in\\s+de\\s+nacht|nachts|nacht"
        )
        for ((period, pattern) in patterns) {
            val m = Regex("(?<![a-z])(?:$pattern)(?![a-z])").find(lower) ?: continue
            ranges += m.range
            return period to m.value.startsWith("van")
        }
        return null to false
    }

    private fun relative(lower: String, now: Long): Pair<Long, IntRange>? {
        Regex("\\bover\\s+een\\s+half\\s*uur\\b").find(lower)?.let {
            return (now + 30 * 60_000L) to it.range
        }
        // "over een kwartier", "over drie kwartier", "over een kwartiertje"
        Regex("\\bover\\s+(\\d+|$NUM_ALT)\\s+kwartier(?:tje)?s?\\b").find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            return (now + n * 15 * 60_000L) to m.range
        }
        val m = Regex("\\bover\\s+(\\d+|$NUM_ALT)\\s+(minuten|minuut|min|uur|uren|dagen|dag|weken|week)\\b")
            .find(lower) ?: return null
        val n = toInt(m.groupValues[1]) ?: return null
        val unitMs = when (m.groupValues[2]) {
            "minuut", "minuten", "min" -> 60_000L
            "uur", "uren" -> 3_600_000L
            "dag", "dagen" -> 86_400_000L
            "week", "weken" -> 7 * 86_400_000L
            else -> return null
        }
        return (now + n * unitMs) to m.range
    }

    private fun time(lower: String, ranges: MutableList<IntRange>): TimeMatch? {
        // om 15:30 / om 15.30 -> expliciet, niet aanpassen
        Regex("\\b(?:om\\s+)?(\\d{1,2})[:.](\\d{2})\\b").find(lower)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) { ranges += m.range; return TimeMatch(h, min, ambiguous = false) }
        }
        // half negen -> 8:30
        Regex("\\b(?:om\\s+)?half\\s+($NUM_ALT)\\b").find(lower)?.let { m ->
            val n = NUM[m.groupValues[1]] ?: return@let
            ranges += m.range; return TimeMatch(hourBefore(n), 30, ambiguous = true)
        }
        // kwart over negen -> 9:15
        Regex("\\bkwart\\s+over\\s+($NUM_ALT|\\d{1,2})\\b").find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(n, 15, ambiguous = true)
        }
        // kwart voor negen -> 8:45
        Regex("\\bkwart\\s+voor\\s+($NUM_ALT|\\d{1,2})\\b").find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(hourBefore(n), 45, ambiguous = true)
        }
        // om 9 uur / 9 uur / om negen uur
        Regex("\\b(?:om\\s+)?(\\d{1,2}|$NUM_ALT)\\s*uur\\b").find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, ambiguous = true) }
        }
        // alleen "om 9"
        Regex("\\bom\\s+(\\d{1,2}|$NUM_ALT)\\b").find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, ambiguous = true) }
        }
        return null
    }

    /** Uur vóór n op de 12-uurs klok: half/kwart voor negen -> 8. half een -> 12. */
    private fun hourBefore(n: Int): Int = ((n - 2 + 12) % 12) + 1

    private fun toInt(token: String): Int? = token.toIntOrNull() ?: NUM[token]

    private fun contains(lower: String, word: String, ranges: MutableList<IntRange>): Boolean {
        val m = Regex("\\b${Regex.escape(word)}\\b").find(lower) ?: return false
        ranges += m.range
        return true
    }

    private fun clean(raw: String, ranges: List<IntRange>): String {
        val sb = StringBuilder(raw)
        for (r in ranges.sortedByDescending { it.first }) {
            if (r.first in sb.indices && r.last in sb.indices) {
                sb.delete(r.first, r.last + 1)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
