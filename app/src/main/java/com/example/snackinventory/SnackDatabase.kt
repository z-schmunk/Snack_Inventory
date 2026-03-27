package com.example.snackinventory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Snack::class, SnackHistory::class], version = 2, exportSchema = false)
abstract class SnackDatabase : RoomDatabase() {
    abstract fun snackDao(): SnackDao

    companion object {
        @Volatile
        private var Instance: SnackDatabase? = null

        fun getDatabase(context: Context): SnackDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, SnackDatabase::class.java, "snack_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
