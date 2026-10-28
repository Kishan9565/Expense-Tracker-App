package com.kishan9565.expensetracker.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.domain.repository.UserPrefs
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreUserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPrefsRepository {

    private val KEY_LAST_CATEGORY = longPreferencesKey("last_category_id")
    private val KEY_LAST_PAYMENT = stringPreferencesKey("last_payment_method")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    override val lastUsed: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        val catId = prefs[KEY_LAST_CATEGORY]
        val paymentStr = prefs[KEY_LAST_PAYMENT] ?: PaymentMethod.UPI.name
        val themeStr = prefs[KEY_THEME] ?: ThemeMode.DARK.name
        val payment = runCatching { PaymentMethod.valueOf(paymentStr) }.getOrDefault(PaymentMethod.UPI)
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        UserPrefs(catId, payment, theme)
    }

    override suspend fun saveLastUsed(categoryId: Long, payment: PaymentMethod) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_CATEGORY] = categoryId
            prefs[KEY_LAST_PAYMENT] = payment.name
        }
    }

    override suspend fun saveTheme(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = themeMode.name
        }
    }
}