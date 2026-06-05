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
    fun todoOneOrTwoWords() {
        assertCategory("to do de auto wassen", Category.TODO, "de auto wassen")
        assertCategory("todo bellen", Category.TODO, "bellen")
        assertCategory("taak factuur sturen", Category.TODO, "factuur sturen")
        assertCategory("taken opruimen", Category.TODO, "opruimen")
    }

    @Test
    fun ideeen() {
        assertCategory("idee nieuwe app", Category.IDEEEN, "nieuwe app")
        assertCategory("ideeën vakantie", Category.IDEEEN, "vakantie")
    }

    @Test
    fun herinneringenFuzzy() {
        assertCategory("herinnering tandarts", Category.HERINNERINGEN, "tandarts")
        assertCategory("herinneringen verjaardag", Category.HERINNERINGEN, "verjaardag")
        assertCategory("herinner mij aan de melk", Category.HERINNERINGEN, "mij aan de melk")
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
        assertEquals(Category.TODO,          clf.classify("Aufgabe Rechnung schicken").category)
        assertEquals(Category.HERINNERINGEN, clf.classify("Erinnerung Zahnarzt").category)
    }

    @Test
    fun enKeywords() {
        val clf = CategoryClassifier()
        assertEquals(Category.BOODSCHAPPEN, clf.classify("Groceries milk and bread").category)
        assertEquals(Category.IDEEEN,        clf.classify("Idea new feature").category)
        assertEquals(Category.TODO,          clf.classify("Task send invoice").category)
        assertEquals(Category.HERINNERINGEN, clf.classify("Reminder dentist").category)
    }
}
