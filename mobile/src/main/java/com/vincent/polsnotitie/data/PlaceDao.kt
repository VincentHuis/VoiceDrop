package com.vincent.polsnotitie.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: Place)

    @Query("SELECT * FROM places")
    fun getAll(): Flow<List<Place>>

    /** Blokkerende variant voor het (her)registreren van geofences buiten een coroutine. */
    @Query("SELECT * FROM places")
    fun getAllNow(): List<Place>
}
