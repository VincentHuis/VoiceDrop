package com.vincent.polsnotitie

import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig
import com.vincent.polsnotitie.reminder.PlaceParser
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceParserTest {

    private fun assertPlace(input: String, place: PlaceType?, text: String) {
        val r = PlaceParser(NlLanguageConfig).parse(input)
        assertEquals("place for '$input'", place, r.place)
        assertEquals("text for '$input'", text, r.text)
    }

    @Test
    fun thuis() {
        assertPlace("als ik thuis ben brood uit de vriezer", PlaceType.THUIS, "brood uit de vriezer")
        assertPlace("bij thuiskomst de planten water geven", PlaceType.THUIS, "de planten water geven")
    }

    @Test
    fun werk() {
        assertPlace("op werk de mail beantwoorden", PlaceType.WERK, "de mail beantwoorden")
        assertPlace("als ik op kantoor ben Jan spreken", PlaceType.WERK, "Jan spreken")
    }

    @Test
    fun supermarkt() {
        assertPlace("bij de supermarkt tandpasta", PlaceType.SUPERMARKT, "tandpasta")
    }

    @Test
    fun geenPlek() {
        assertPlace("tandarts bellen", null, "tandarts bellen")
    }

    @Test
    fun deThuis() {
        val r = PlaceParser(DeLanguageConfig).parse("zuhause Milch kaufen")
        assertEquals(PlaceType.THUIS, r.place)
        assertEquals("Milch kaufen", r.text)
    }

    @Test
    fun deWerk() {
        val r = PlaceParser(DeLanguageConfig).parse("auf der arbeit dokument drucken")
        assertEquals(PlaceType.WERK, r.place)
    }

    @Test
    fun deSupermarkt() {
        val r = PlaceParser(DeLanguageConfig).parse("im supermarkt Äpfel")
        assertEquals(PlaceType.SUPERMARKT, r.place)
        assertEquals("Äpfel", r.text)
    }

    @Test
    fun enHome() {
        val r = PlaceParser(EnLanguageConfig).parse("when i get home buy milk")
        assertEquals(PlaceType.THUIS, r.place)
        assertEquals("buy milk", r.text)
    }

    @Test
    fun enWork() {
        val r = PlaceParser(EnLanguageConfig).parse("at work print document")
        assertEquals(PlaceType.WERK, r.place)
    }

    @Test
    fun enSupermarket() {
        val r = PlaceParser(EnLanguageConfig).parse("at the supermarket get apples")
        assertEquals(PlaceType.SUPERMARKT, r.place)
        assertEquals("get apples", r.text)
    }
}
