package com.vincent.voicedrop.data

enum class Category {
    BOODSCHAPPEN, IDEEEN, TODO, HERINNERINGEN, AGENDA, OVERIG;

    companion object {
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OVERIG
    }
}
