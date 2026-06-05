package com.vincent.voicedrop

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.CategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    private fun assertCategory(input: String, expected: Category, expectedText: String) {
        val result = CategoryClassifier().classify(input)
        assertEquals("category for '$input'", expected, result.category)
        assertEquals("text for '$input'", expectedText, result.text)
    }

    @Test
    fun boodschappen() {
        assertCategory("Boodschappen melk en brood", Category.BOODSCHAPPEN, "melk en brood")
        assertCategory("boodschap kaas", Category.BOODSCHAPPEN, "kaas")
    }

    @Test
    fun takenOneOrTwoWords() {
        // To-do en Herinneringen zijn samengevoegd tot TAKEN.
        assertCategory("to do de auto wassen", Category.TAKEN, "de auto wassen")
        assertCategory("todo bellen", Category.TAKEN, "bellen")
        assertCategory("taak factuur sturen", Category.TAKEN, "factuur sturen")
        assertCategory("taken opruimen", Category.TAKEN, "opruimen")
    }

    @Test
    fun ideeen() {
        assertCategory("idee nieuwe app", Category.IDEEEN, "nieuwe app")
        assertCategory("ideeën vakantie", Category.IDEEEN, "vakantie")
    }

    @Test
    fun herinneringenFuzzy() {
        assertCategory("herinnering tandarts", Category.TAKEN, "tandarts")
        assertCategory("herinneringen verjaardag", Category.TAKEN, "verjaardag")
        assertCategory("herinner mij aan de melk", Category.TAKEN, "mij aan de melk")
    }

    @Test
    fun agenda() {
        assertCategory("agenda morgen 12 uur tandarts", Category.AGENDA, "morgen 12 uur tandarts")
        assertCategory("afspraak kapper", Category.AGENDA, "kapper")
    }

    @Test
    fun unknownFallsBackToOverig() {
        assertCategory("vergaderen om drie uur", Category.OVERIG, "vergaderen om drie uur")
    }

    @Test
    fun emptyInput() {
        assertCategory("", Category.OVERIG, "")
    }

    @Test
    fun deKeywords() {
        val clf = CategoryClassifier()
        assertEquals(Category.BOODSCHAPPEN, clf.classify("Einkaufen Milch").category)
        assertEquals(Category.IDEEEN,        clf.classify("Idee neue App").category)
        assertEquals(Category.TAKEN, clf.classify("Aufgabe Rechnung schicken").category)
        assertEquals(Category.TAKEN, clf.classify("Erinnerung Zahnarzt").category)
    }

    @Test
    fun enKeywords() {
        val clf = CategoryClassifier()
        assertEquals(Category.BOODSCHAPPEN, clf.classify("Groceries milk and bread").category)
        assertEquals(Category.IDEEEN,        clf.classify("Idea new feature").category)
        assertEquals(Category.TAKEN, clf.classify("Task send invoice").category)
        assertEquals(Category.TAKEN, clf.classify("Reminder dentist").category)
    }
}
