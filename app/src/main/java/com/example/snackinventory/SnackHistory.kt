package com.example.snackinventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "snack_history",
    foreignKeys = [
        ForeignKey(
            entity = Snack::class,
            parentColumns = ["id"],
            childColumns = ["snackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SnackHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val snackId: Int,
    val snackName: String,
    val quantityChange: Int,
    val timestamp: Long = System.currentTimeMillis()
)
