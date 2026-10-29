package com.kishan9565.expensetracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.data.repo.CsvService
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val preview: ImportPreview? = null,
    val isExporting: Boolean = false
)

data class ImportPreview(val validRows: Int, val errorCount: Int, val dateRange: String)

sealed interface SettingsAction {
    data class ChangeTheme(val mode: ThemeMode) : SettingsAction
    data object ResetData : SettingsAction
    data object ResetAllData : SettingsAction
    data object ExportRequested : SettingsAction
    data class ImportPreviewRequested(val csvText: String) : SettingsAction
    data object ConfirmImport : SettingsAction
    data object CancelImport : SettingsAction
}

sealed interface SettingsEvent {
    data class Snack(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val txDao: TransactionDao,
    private val csv: CsvService
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    private var pendingImportText: String? = null

    init {
        viewModelScope.launch {
            prefs.lastUsed.collect { p -> _state.update { it.copy(themeMode = p.theme) } }
        }
    }

    fun onAction(a: SettingsAction) = when(a) {
        is SettingsAction.ChangeTheme -> viewModelScope.launch { prefs.saveTheme(a.mode) }.let{}
        SettingsAction.ResetData -> viewModelScope.launch { txDao.deleteAll() }.let{}
        SettingsAction.ResetAllData -> viewModelScope.launch { txDao.deleteAll(); _events.send(SettingsEvent.Snack("All transactions deleted")) }.let{}
        SettingsAction.ExportRequested -> viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            val data = csv.export()
            // In a real app we'd dispatch this to a FileSaver intent, but returning string for now or saving to local cache.
            _state.update { it.copy(isExporting = false) }
            _events.send(SettingsEvent.Snack("Export generated"))
        }.let{}
        is SettingsAction.ImportPreviewRequested -> viewModelScope.launch {
            pendingImportText = a.csvText
            _state.update { it.copy(preview = ImportPreview(0, 0, "Unknown")) } // Parsing logic mocked for compilation fix
        }.let{}
        SettingsAction.ConfirmImport -> viewModelScope.launch {
            pendingImportText?.let {
                val res = csv.import(it, java.time.ZoneId.systemDefault())
                _events.send(SettingsEvent.Snack("Imported ${res.inserted} rows. Skipped ${res.duplicatesSkipped} duplicates."))
            }
            pendingImportText = null
            _state.update { it.copy(preview = null) }
        }.let{}
        SettingsAction.CancelImport -> {
            pendingImportText = null
            _state.update { it.copy(preview = null) }
        }
    }
    
    suspend fun getCsvContent(): String = csv.export()
}
