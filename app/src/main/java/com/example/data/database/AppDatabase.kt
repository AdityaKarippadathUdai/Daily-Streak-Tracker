package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Challenge
import com.example.data.model.CompletionRecord

@Database(entities = [Challenge::class, CompletionRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun completionRecordDao(): CompletionRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_challenge_database"
                )
                .fallbackToDestructiveMigration() // Simple for prototypes to prevent crashes during model updates
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
