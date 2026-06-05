package com.vincent.voicedrop.data

enum class Category {
    // TAKEN = samengevoegde To-do + Herinneringen. Een tijd of plek is een optioneel
    // kenmerk van een taak (zonder = open taak, met = gepland/herinnering).
    BOODSCHAPPEN, IDEEEN, TAKEN, AGENDA, OVERIG;

    companion object {
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OVERIG
    }
}
