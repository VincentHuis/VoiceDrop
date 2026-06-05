package com.vincent.voicedrop.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vincent.voicedrop.R

@Database(
    entities = [Memo::class, Place::class],
    version = 9,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 8, to = 9)]
)
@TypeConverters(PlaceTypeConverter::class)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun placeDao(): PlaceDao

    companion object {
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE places ADD COLUMN address TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN pinnedAt INTEGER")
            }
        }

        private fun migration7to8(context: Context) = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nieuwe places-tabel met auto-id en vrije naam.
                db.execSQL(
                    "CREATE TABLE places_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "lat REAL NOT NULL, " +
                        "lng REAL NOT NULL, " +
                        "radius REAL NOT NULL DEFAULT 100, " +
                        "address TEXT)"
                )

                val homeName = context.getString(R.string.place_home)
                val workName = context.getString(R.string.place_work)
                val shopName = context.getString(R.string.place_supermarket)
                val nameByType = mapOf(
                    "THUIS" to homeName,
                    "WERK" to workName,
                    "SUPERMARKT" to shopName
                )

                // Map oud type -> nieuw long id, om memos.placeId te updaten.
                val typeToNewId = mutableMapOf<String, Long>()
                val cursor = db.query("SELECT type, lat, lng, radius, address FROM places")
                cursor.use {
                    while (it.moveToNext()) {
                        val type = it.getString(0)
                        val lat = it.getDouble(1)
                        val lng = it.getDouble(2)
                        val radius = it.getFloat(3)
                        val address = if (it.isNull(4)) null else it.getString(4)
                        val displayName = nameByType[type] ?: type
                        val values = android.content.ContentValues().apply {
                            put("name", displayName)
                            put("lat", lat)
                            put("lng", lng)
                            put("radius", radius)
                            put("address", address)
                        }
                        val newId = db.insert("places_new", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                        typeToNewId[type] = newId
                    }
                }

                db.execSQL("DROP TABLE places")
                db.execSQL("ALTER TABLE places_new RENAME TO places")

                // memos.placeId verwijst nu naar de nieuwe Long-id (als string opgeslagen).
                for ((oldType, newId) in typeToNewId) {
                    db.execSQL(
                        "UPDATE memos SET placeId = ? WHERE placeId = ?",
                        arrayOf<Any>(newId.toString(), oldType)
                    )
                }
                // Eventuele weesreferenties opruimen.
                db.execSQL("UPDATE memos SET placeId = NULL WHERE placeId IS NOT NULL AND placeId NOT IN (SELECT CAST(id AS TEXT) FROM places)")
            }
        }

        @Volatile
        private var instance: MemoDatabase? = null

        fun get(context: Context): MemoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java,
                    "memos.db"
                ).addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    migration7to8(context.applicationContext)
                ).build().also { instance = it }
            }
    }
}
