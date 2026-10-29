package com.kishan9565.expensetracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.domain.repository.UserPrefs
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class DataStoreUserPrefsRepository @Inject constructor(
    @ApplicationContext context: Context
) : UserPrefsRepository {

    private val KEY_CATEGORY = longPreferencesKey("last_category_id")
    private val KEY_PAYMENT = stringPreferencesKey("last_payment_method")
    private val KEY_THEME = stringPreferencesKey("theme_mode")
    
    private val store = context.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    override val lastUsed: StateFlow<UserPrefs> = store.data.map { prefs ->
        val catId = prefs[KEY_CATEGORY]
        val payStr = prefs[KEY_PAYMENT] ?: PaymentMethod.UPI.name
        val themeStr = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        
        val payment = runCatching { PaymentMethod.valueOf(payStr) }.getOrDefault(PaymentMethod.UPI)
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        
        UserPrefs(catId, payment, theme)
    }.stateIn(scope, SharingStarted.Eagerly, UserPrefs(null, PaymentMethod.UPI, ThemeMode.SYSTEM))

    override suspend fun saveLastUsed(categoryId: Long, payment: PaymentMethod) {
        store.edit { prefs ->
            prefs[KEY_CATEGORY] = categoryId
            prefs[KEY_PAYMENT] = payment.name
        }
    }

    override suspend fun saveTheme(themeMode: ThemeMode) {
        store.edit { prefs ->
            prefs[KEY_THEME] = themeMode.name
        }
    }
}
