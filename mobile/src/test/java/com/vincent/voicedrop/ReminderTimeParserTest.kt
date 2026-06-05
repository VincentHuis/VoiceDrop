package com.vincent.voicedrop

import com.vincent.voicedrop.language.configs.DeLanguageConfig
import com.vincent.voicedrop.language.configs.EnLanguageConfig
import com.vincent.voicedrop.language.configs.NlLanguageConfig
import com.vincent.voicedrop.reminder.ReminderTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderTimeParserTest {

    // Vast referentiemoment: woensdag 3 juni 2026, 10:00.
    private fun now(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 3, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }

    @Test
    fun morgenOmTijd() {
        // "9 uur" zonder dagdeel -> overdag, dus 21:00 (1..10 schuift naar +12)
        val r = ReminderTimeParser(NlLanguageConfig).parse("morgen om 9 uur tandarts", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(21, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals("tandarts", r.text)
    }

    @Test
    fun ochtendsForceertAm() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("morgen om 9 uur 's ochtends tandarts", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
        assertEquals("tandarts", r.text)
    }

    @Test
    fun overEenUur() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over een uur bellen", n)
        assertEquals(n + 3_600_000L, r.remindAt)
        assertEquals("bellen", r.text)
    }

    @Test
    fun overEenKwartier() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over een kwartier de was ophangen", n)
        assertEquals(n + 15 * 60_000L, r.remindAt)
        assertEquals("de was ophangen", r.text)
    }

    @Test
    fun overDrieKwartier() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over drie kwartier bellen", n)
        assertEquals(n + 45 * 60_000L, r.remindAt)
        assertEquals("bellen", r.text)
    }

    @Test
    fun overHalfUur() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over een half uur eten", n)
        assertEquals(n + 30 * 60_000L, r.remindAt)
        assertEquals("eten", r.text)
    }

    @Test
    fun overTwintigMinuten() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over 20 minuten oven uit", n)
        assertEquals(n + 20 * 60_000L, r.remindAt)
        assertEquals("oven uit", r.text)
    }

    @Test
    fun overTienMinuten() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("over tien minuten oven uitzetten", n)
        assertEquals(n + 10 * 60_000L, r.remindAt)
        assertEquals("oven uitzetten", r.text)
    }

    @Test
    fun weekdagMetTijd() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("vrijdag om 15:30 vergadering", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.FRIDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(15, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        assertTrue(r.remindAt!! > now())
        assertEquals("vergadering", r.text)
    }

    @Test
    fun vanavondDefaultTijd() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("vanavond Jan bellen", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(19, c.get(Calendar.HOUR_OF_DAY))
        assertEquals("Jan bellen", r.text)
    }

    @Test
    fun halfNegenOverdag() {
        // "half negen" -> 8:30, zonder dagdeel overdag -> 20:30 (vandaag, want > 10:00)
        val r = ReminderTimeParser(NlLanguageConfig).parse("half negen opstaan", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        assertEquals(Calendar.WEDNESDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals("opstaan", r.text)
    }

    @Test
    fun losUurStandaardOverdag() {
        // "1 uur" zonder dagdeel -> 13:00 (overdag), morgen
        val r = ReminderTimeParser(NlLanguageConfig).parse("morgen om 1 uur naar de tandarts", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(13, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals("naar de tandarts", r.text)
    }

    @Test
    fun avondsSchuiftNaarPm() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("vanavond om 8 uur eten", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals("eten", r.text)
    }

    @Test
    fun snachtsBlijftNacht() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("om 1 uur 's nachts feest", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(1, c.get(Calendar.HOUR_OF_DAY))
        assertEquals("feest", r.text)
    }

    @Test
    fun expliciteKloktijdNietAangepast() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("om 06:00 joggen", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(6, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals("joggen", r.text)
    }

    @Test
    fun geenTijdGeeftNull() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("tandarts afspraak maken", now())
        assertNull(r.remindAt)
        assertEquals("tandarts afspraak maken", r.text)
    }

    @Test
    fun deInEinerStunde() {
        val n = now()
        val r = ReminderTimeParser(DeLanguageConfig).parse("in einer stunde anrufen", n)
        assertEquals(n + 3_600_000L, r.remindAt)
        assertEquals("anrufen", r.text)
    }

    @Test
    fun deMorgenUmNeun() {
        val r = ReminderTimeParser(DeLanguageConfig).parse("morgen um 9 uhr zahnarzt", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(21, c.get(Calendar.HOUR_OF_DAY)) // 9 ambiguous -> +12
        assertEquals("zahnarzt", r.text)
    }

    @Test
    fun deHalbNeun() {
        // "halb neun" -> 8:30 (halfHourIsBefore=true, same as Dutch)
        val r = ReminderTimeParser(DeLanguageConfig).parse("morgen halb neun frühstück", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(8, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
    }

    @Test
    fun enInOneHour() {
        val n = now()
        val r = ReminderTimeParser(EnLanguageConfig).parse("in one hour call back", n)
        assertEquals(n + 3_600_000L, r.remindAt)
        assertEquals("call back", r.text)
    }

    @Test
    fun enTomorrowAtNine() {
        val r = ReminderTimeParser(EnLanguageConfig).parse("tomorrow at 9 o'clock dentist", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(21, c.get(Calendar.HOUR_OF_DAY)) // 9 ambiguous -> +12
        assertEquals("dentist", r.text)
    }

    @Test
    fun enHalfNineIs930() {
        // English: "half nine" = 9:30, not 8:30
        val r = ReminderTimeParser(EnLanguageConfig).parse("tomorrow half nine breakfast", now())
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
    }

    // --- Herhaling (recurrence) ---

    @Test
    fun elkeWeekdagMetTijd() {
        // "elke maandag om 20:00" -> WEEK:1, eerste moment a.s. maandag 20:00, "elke" gewist.
        val r = ReminderTimeParser(NlLanguageConfig).parse(
            "elke maandag om 20:00 planten water geven",
            now()
        )
        assertEquals("WEEK:1", r.recurrence)
        assertNotNull(r.remindAt)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertTrue(r.remindAt!! > now())
        assertEquals("planten water geven", r.text)
    }

    @Test
    fun elkeDagMetTijd() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("elke dag om 8 uur pillen", now())
        assertEquals("DAY:1", r.recurrence)
        assertNotNull(r.remindAt)
        assertEquals("pillen", r.text)
    }

    @Test
    fun elkeTweeDagen() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("elke 2 dagen om 9 uur sporten", now())
        assertEquals("DAY:2", r.recurrence)
        assertEquals("sporten", r.text)
    }

    @Test
    fun elkeTweeUurZonderAnker() {
        val n = now()
        val r = ReminderTimeParser(NlLanguageConfig).parse("elke 2 uur drinken", n)
        assertEquals("HOUR:2", r.recurrence)
        // Geen tijd/dag: eerste moment = nu + 2 uur.
        assertEquals(n + 2 * 3_600_000L, r.remindAt)
        assertEquals("drinken", r.text)
    }

    @Test
    fun dagelijksAdverb() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("dagelijks om 7 uur opstaan", now())
        assertEquals("DAY:1", r.recurrence)
        assertEquals("opstaan", r.text)
    }

    @Test
    fun enEveryMondayRecurrence() {
        val r =
            ReminderTimeParser(EnLanguageConfig).parse("every monday at 20:00 water plants", now())
        assertEquals("WEEK:1", r.recurrence)
        val c = cal(r.remindAt!!)
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, c.get(Calendar.HOUR_OF_DAY))
        assertEquals("water plants", r.text)
    }

    @Test
    fun geenHerhalingGeeftNull() {
        val r = ReminderTimeParser(NlLanguageConfig).parse("morgen om 9 uur tandarts", now())
        assertNull(r.recurrence)
    }
}
