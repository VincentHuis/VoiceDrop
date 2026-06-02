package com.vincent.polsnotitie

import com.vincent.polsnotitie.data.Place
import com.vincent.polsnotitie.reminder.PlaceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceParserTest {

    private val places = listOf(
        Place(id = 1, name = "Home", lat = 0.0, lng = 0.0),
        Place(id = 2, name = "Work", lat = 0.0, lng = 0.0),
        Place(id = 3, name = "Mom's house", lat = 0.0, lng = 0.0),
        Place(id = 4, name = "Supermarkt", lat = 0.0, lng = 0.0)
    )

    private val parser = PlaceParser(places)

    @Test
    fun matchesSimpleName() {
        val r = parser.parse("remember to water plants at home")
        assertNotNull(r.place)
        assertEquals(1L, r.place!!.id)
        assertEquals("remember to water plants at", r.text)
    }

    @Test
    fun matchesCaseInsensitive() {
        val r = parser.parse("Print document at WORK")
        assertNotNull(r.place)
        assertEquals("Work", r.place!!.name)
    }

    @Test
    fun preferLongerNameOverSubstring() {
        // "Mom's house" should beat "Home" even if both appear-ish.
        val r = parser.parse("remind me to call when I get to mom's house")
        assertNotNull(r.place)
        assertEquals("Mom's house", r.place!!.name)
    }

    @Test
    fun supermarktDutch() {
        val r = parser.parse("bij de supermarkt tandpasta")
        assertNotNull(r.place)
        assertEquals("Supermarkt", r.place!!.name)
        assertEquals("bij de tandpasta", r.text)
    }

    @Test
    fun noMatch() {
        val r = parser.parse("tandarts bellen")
        assertNull(r.place)
        assertEquals("tandarts bellen", r.text)
    }

    @Test
    fun emptyPlacesList() {
        val r = PlaceParser(emptyList()).parse("anything at home")
        assertNull(r.place)
        assertEquals("anything at home", r.text)
    }
}
