package com.msebenzi.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local offline-first database. The job feed reads from here first so the
 * app is usable without connectivity, then refreshes from the network
 * when it's available (see JobRepository).
 */
@Database(entities = [Job::class, SyncAction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun jobDao(): JobDao
    abstract fun syncActionDao(): SyncActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "msebenzi.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
