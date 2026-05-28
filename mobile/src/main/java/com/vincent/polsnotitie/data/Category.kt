package com.vincent.polsnotitie.data

/**
 * Categorie van een memo, afgeleid van het eerste (gesproken) woord. Elke categorie heeft
 * meerdere trefwoorden/spellingen; fuzzy matching in [CategoryClassifier] vangt versprekingen.
 */
enum class Category(val displayName: String, val keywords: List<String>) {
    BOODSCHAPPEN("Boodschappen", listOf("boodschappen", "boodschap", "winkel", "supermarkt")),
    IDEEEN("Ideeën", listOf("idee", "ideeen", "ideetje", "ideetjes")),
    TODO("To-do", listOf("todo", "to do", "to-do", "taak", "taken")),
    HERINNERINGEN("Herinneringen", listOf("herinnering", "herinneringen", "herinner")),
    AGENDA("Agenda", listOf("agenda", "afspraak", "kalender")),
    OVERIG("Overig", emptyList());

    companion object {
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OVERIG
    }
}
