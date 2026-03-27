package com.example.snackinventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StockFilter {
    ALL, LOW_STOCK, EXPIRING, EXPIRED, IN_STOCK
}

class SnackViewModel(application: Application) : AndroidViewModel(application) {
    private val snackDao = SnackDatabase.getDatabase(application).snackDao()
    val allSnacks: Flow<List<Snack>> = snackDao.getAllSnacks()
    val allHistory: Flow<List<SnackHistory>> = snackDao.getAllHistory()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedStockFilter = MutableStateFlow(StockFilter.ALL)
    val selectedStockFilter = _selectedStockFilter.asStateFlow()

    val filteredSnacks = combine(allSnacks, searchQuery, selectedCategory, selectedStockFilter) { snacks, query, category, stockFilter ->
        snacks.filter { snack ->
            val matchesCategory = (category == "All" || snack.category == category)
            val matchesQuery = snack.name.contains(query, ignoreCase = true)
            val matchesStock = when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> snack.isLowStock()
                StockFilter.EXPIRING -> snack.isExpiringSoon()
                StockFilter.EXPIRED -> snack.isExpired()
                StockFilter.IN_STOCK -> !snack.isLowStock() && !snack.isExpired()
            }
            matchesCategory && matchesQuery && matchesStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = allSnacks.map { snacks ->
        listOf("All") + snacks.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val alerts = allSnacks.map { snacks ->
        snacks.filter { it.isLowStock() || it.isExpiringSoon() || it.isExpired() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateStockFilter(filter: StockFilter) {
        _selectedStockFilter.value = filter
    }

    fun addSnack(name: String, quantity: Int, minThreshold: Int, category: String, expirationDate: Long? = null) {
        viewModelScope.launch {
            val id = snackDao.insertSnack(
                Snack(
                    name = name,
                    quantity = quantity,
                    minThreshold = minThreshold,
                    category = if (category.isBlank()) "General" else category,
                    expirationDate = expirationDate
                )
            )
            if (quantity != 0) {
                snackDao.insertHistory(
                    SnackHistory(
                        snackId = id.toInt(),
                        snackName = name,
                        quantityChange = quantity
                    )
                )
            }
        }
    }

    fun updateSnack(snack: Snack) {
        viewModelScope.launch {
            val oldSnack = snackDao.getSnackById(snack.id)
            if (oldSnack != null) {
                val delta = snack.quantity - oldSnack.quantity
                snackDao.updateSnack(snack)
                if (delta != 0) {
                    snackDao.insertHistory(
                        SnackHistory(
                            snackId = snack.id,
                            snackName = snack.name,
                            quantityChange = delta
                        )
                    )
                }
            }
        }
    }

    fun updateQuantity(snack: Snack, delta: Int) {
        if (delta == 0) return
        viewModelScope.launch {
            val newQuantity = (snack.quantity + delta).coerceAtLeast(0)
            val actualDelta = newQuantity - snack.quantity
            if (actualDelta != 0) {
                snackDao.updateSnack(snack.copy(quantity = newQuantity))
                snackDao.insertHistory(
                    SnackHistory(
                        snackId = snack.id,
                        snackName = snack.name,
                        quantityChange = actualDelta
                    )
                )
            }
        }
    }

    fun removeSnack(snack: Snack) {
        viewModelScope.launch {
            snackDao.deleteSnack(snack)
        }
    }
}
