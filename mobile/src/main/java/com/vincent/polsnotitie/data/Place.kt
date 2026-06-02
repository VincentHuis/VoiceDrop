package com.vincent.polsnotitie.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Een door de gebruiker ingestelde plek met vrije naam. */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radius: Float = 100f,
    val address: String? = null
)
