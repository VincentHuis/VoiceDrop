package com.vincent.voicedrop

import com.vincent.voicedrop.reminder.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurrenceTest {

    // Maandag 1 juni 2026, 20:00.
    private fun base(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 1, 20, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }

    @Test
    fun nextMinutes() {
        val c = cal(Recurrence.next(base(), "MIN:15"))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, c.get(Calendar.MINUTE))
    }

    @Test
    fun nextHours() {
        val c = cal(Recurrence.next(base(), "HOUR:3"))
        assertEquals(23, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun nextDayKeepsTime() {
        val c = cal(Recurrence.next(base(), "DAY:1"))
        assertEquals(Calendar.TUESDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun nextWeekKeepsWeekdayAndTime() {
        val c = cal(Recurrence.next(base(), "WEEK:1"))
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(8, c.get(Calendar.DAY_OF_MONTH)) // 1 juni + 7
    }

    @Test
    fun nextEveryTwoWeeks() {
        val c = cal(Recurrence.next(base(), "WEEK:2"))
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun nextMonth() {
        val c = cal(Recurrence.next(base(), "MONTH:1"))
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun nextInvalidRuleReturnsInput() {
        val b = base()
        assertEquals(b, Recurrence.next(b, "BOGUS"))
        assertEquals(b, Recurrence.next(b, "YEAR:1"))
    }

    @Test
    fun nextWeekWithoutIntervalDefaultsToOne() {
        // "WEEK" zonder interval -> n=1
        val c = cal(Recurrence.next(base(), "WEEK"))
        assertEquals(8, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun nextAfterRollsForwardPastNow() {
        val b = base()                          // ma 1 juni 20:00
        val now = b + 3L * 7 * 86_400_000L + 5_000L  // ~3 weken + 5s later
        val result = Recurrence.nextAfter(b, "WEEK:1", now)
        assertTrue("moet strikt na now liggen", result > now)
        val c = cal(result)
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun nextAfterWhenAlreadyFutureReturnsAfterOneStep() {
        val b = base()
        val now = b - 1000L // base ligt al in de "toekomst" t.o.v. now
        // from <= now is false -> loop draait niet -> retourneert from
        assertEquals(b, Recurrence.nextAfter(b, "WEEK:1", now))
    }

    @Test
    fun nextAfterInvalidRuleReturnsInput() {
        val b = base()
        assertEquals(b, Recurrence.nextAfter(b, "NOPE", b + 100))
    }

    @Test
    fun isValidAcceptsKnownUnits() {
        assertTrue(Recurrence.isValid("DAY:1"))
        assertTrue(Recurrence.isValid("WEEK:2"))
        assertTrue(Recurrence.isValid("MONTH:1"))
        assertTrue(Recurrence.isValid("HOUR:6"))
        assertTrue(Recurrence.isValid("MIN:30"))
        assertTrue(Recurrence.isValid("day:1")) // case-insensitief
    }

    @Test
    fun isValidRejectsBadRules() {
        assertFalse(Recurrence.isValid(null))
        assertFalse(Recurrence.isValid("YEAR:1"))
        assertFalse(Recurrence.isValid("DAY:0"))
        assertFalse(Recurrence.isValid("DAY:-2"))
        assertFalse(Recurrence.isValid(""))
    }
}
