package com.vincent.voicedrop.reminder

import java.util.Calendar

/**
 * Herhalingsregel "UNIT:interval" (bv. "WEEK:1", "DAY:2", "HOUR:3"). Het volgende moment
 * wordt via [Calendar] berekend, zodat weekdag/uur correct blijven (ook met zomertijd).
 */
object Recurrence {

    /** Geldig formaat? */
    fun isValid(rule: String?): Boolean = parse(rule) != null

    /** Volgend moment ná [from] (één stap). */
    fun next(from: Long, rule: String): Long {
        val (unit, n) = parse(rule) ?: return from
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (unit) {
            "MIN" -> cal.add(Calendar.MINUTE, n)
            "HOUR" -> cal.add(Calendar.HOUR_OF_DAY, n)
            "DAY" -> cal.add(Calendar.DAY_OF_YEAR, n)
            "WEEK" -> cal.add(Calendar.DAY_OF_YEAR, 7 * n)
            "MONTH" -> cal.add(Calendar.MONTH, n)
            else -> return from
        }
        return cal.timeInMillis
    }

    /** Rol vooruit tot strikt ná [now] (voor gemiste herhalingen nadat het toestel uit stond). */
    fun nextAfter(from: Long, rule: String, now: Long): Long {
        if (parse(rule) == null) return from
        var t = from
        var guard = 0
        while (t <= now && guard < 100_000) {
            val step = next(t, rule)
            if (step <= t) return t // veiligheidsstop tegen vastlopen
            t = step
            guard++
        }
        return t
    }

    private fun parse(rule: String?): Pair<String, Int>? {
        if (rule == null) return null
        val parts = rule.split(":")
        val unit = parts.getOrNull(0)?.uppercase() ?: return null
        if (unit !in setOf("MIN", "HOUR", "DAY", "WEEK", "MONTH")) return null
        val n = parts.getOrNull(1)?.toIntOrNull() ?: 1
        if (n < 1) return null
        return unit to n
    }
}
