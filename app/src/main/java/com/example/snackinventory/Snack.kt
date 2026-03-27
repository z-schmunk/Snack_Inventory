package com.example.snackinventory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snacks")
data class Snack(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int,
    val minThreshold: Int,
    val category: String,
    val expirationDate: Long? = null
) {
    fun isLowStock(): Boolean = quantity <= minThreshold
    
    fun isExpiringSoon(days: Int = 7): Boolean {
        if (expirationDate == null) return false
        val sevenDaysMillis = days * 24 * 60 * 60 * 1000L
        return expirationDate - System.currentTimeMillis() < sevenDaysMillis
    }

    fun isExpired(): Boolean {
        if (expirationDate == null) return false
        return expirationDate < System.currentTimeMillis()
    }
}
