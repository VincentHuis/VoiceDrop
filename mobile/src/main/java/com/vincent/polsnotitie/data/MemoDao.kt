package com.vincent.polsnotitie.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(memo: Memo)

    @Query("SELECT * FROM memos WHERE category != :excludeCategory ORDER BY timestamp DESC")
    fun getAll(excludeCategory: String): Flow<List<Memo>>

    @Query(
        "SELECT * FROM memos WHERE text LIKE '%' || :query || '%' " +
            "AND category != :excludeCategory ORDER BY timestamp DESC"
    )
    fun search(query: String, excludeCategory: String): Flow<List<Memo>>

    @Query("SELECT * FROM memos WHERE category = :category ORDER BY checkedAt IS NOT NULL, timestamp DESC")
    fun byCategory(category: String): Flow<List<Memo>>

    /** Blokkerende query voor de widget (draait op een achtergrondthread). */
    @Query("SELECT * FROM memos WHERE category = :category AND checkedAt IS NULL ORDER BY timestamp DESC")
    fun byCategoryUncheckedNow(category: String): List<Memo>

    @Query("UPDATE memos SET checkedAt = :checkedAt WHERE id = :id")
    suspend fun setChecked(id: String, checkedAt: Long?)

    @Query("UPDATE memos SET remindAt = :remindAt WHERE id = :id")
    suspend fun setRemindAt(id: String, remindAt: Long?)

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: String): Memo?

    /** Blokkerende query voor de boot-receiver: toekomstige herinneringen herplannen. */
    @Query("SELECT * FROM memos WHERE remindAt IS NOT NULL AND remindAt > :after")
    fun upcomingRemindersNow(after: Long): List<Memo>

    /** Blokkerende query voor de geofence-receiver: locatie-herinneringen voor een plek. */
    @Query("SELECT * FROM memos WHERE placeId = :placeId")
    fun byPlaceNow(placeId: String): List<Memo>

    @Query("DELETE FROM memos WHERE id = :id")
    fun deleteByIdNow(id: String)

    @Query("DELETE FROM memos WHERE checkedAt IS NOT NULL AND checkedAt <= :cutoff")
    suspend fun deleteCheckedBefore(cutoff: Long)

    @Delete
    suspend fun delete(memo: Memo)
}
