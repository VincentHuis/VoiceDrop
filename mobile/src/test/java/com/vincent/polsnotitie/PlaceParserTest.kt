package com.vincent.polsnotitie

import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.reminder.PlaceParser
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceParserTest {

    private fun assertPlace(input: String, place: PlaceType?, text: String) {
        val r = PlaceParser.parse(input)
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
}
