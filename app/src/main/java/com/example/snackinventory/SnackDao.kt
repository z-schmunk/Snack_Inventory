package com.example.snackinventory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SnackDao {
    @Query("SELECT * FROM snacks")
    fun getAllSnacks(): Flow<List<Snack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnack(snack: Snack): Long

    @Update
    suspend fun updateSnack(snack: Snack)

    @Delete
    suspend fun deleteSnack(snack: Snack)

    @Query("SELECT * FROM snacks WHERE id = :id LIMIT 1")
    suspend fun getSnackById(id: Int): Snack?

    @Insert
    suspend fun insertHistory(history: SnackHistory)

    @Query("SELECT * FROM snack_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SnackHistory>>
}
