package com.vincent.voicedrop.data

import androidx.room.ColumnInfo
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
    val address: String? = null,
    @ColumnInfo(defaultValue = "OVERIGE") val type: PlaceType = PlaceType.OVERIGE
)
