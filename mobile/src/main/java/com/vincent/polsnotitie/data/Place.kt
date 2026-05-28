package com.vincent.polsnotitie.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** De drie vaste plekken waaraan locatie-herinneringen gekoppeld kunnen worden. */
enum class PlaceType(val displayName: String) {
    THUIS("Thuis"),
    WERK("Werk"),
    SUPERMARKT("Supermarkt");

    companion object {
        fun fromName(name: String?): PlaceType? = entries.firstOrNull { it.name == name }
    }
}

/** Een ingestelde plek: een punt op de kaart met een straal (meter). */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey val type: String,
    val lat: Double,
    val lng: Double,
    val radius: Float = 100f,
    val address: String? = null
)
