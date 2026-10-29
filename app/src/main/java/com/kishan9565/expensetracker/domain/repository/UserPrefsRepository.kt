package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.data.db.PaymentMethod
import kotlinx.coroutines.flow.StateFlow

data class UserPrefs(
    val categoryId: Long?,
    val payment: PaymentMethod,
    val theme: ThemeMode = ThemeMode.SYSTEM
)

interface UserPrefsRepository {
    val lastUsed: StateFlow<UserPrefs>
    suspend fun saveLastUsed(categoryId: Long, payment: PaymentMethod)
    suspend fun saveTheme(themeMode: ThemeMode)
}
