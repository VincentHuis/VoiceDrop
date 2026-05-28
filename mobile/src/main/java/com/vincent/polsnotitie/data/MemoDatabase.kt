package com.vincent.polsnotitie.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Memo::class, Place::class], version = 7, exportSchema = true)
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

        @Volatile
        private var instance: MemoDatabase? = null

        fun get(context: Context): MemoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java,
                    "memos.db"
                ).addMigrations(MIGRATION_5_6, MIGRATION_6_7).build().also { instance = it }
            }
    }
}
