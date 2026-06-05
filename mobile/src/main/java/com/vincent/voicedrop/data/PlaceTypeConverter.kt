package com.vincent.voicedrop.data

import androidx.room.TypeConverter

class PlaceTypeConverter {
    @TypeConverter
    fun fromPlaceType(type: PlaceType): String = type.name

    @TypeConverter
    fun toPlaceType(value: String): PlaceType =
        PlaceType.entries.firstOrNull { it.name == value } ?: PlaceType.OVERIGE
}
