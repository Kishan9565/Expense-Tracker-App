package com.kishan9565.expensetracker.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.core.ui.UiText
import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val preview: ImportPreview? = null,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false
)

sealed interface SettingsAction {
    data class ChangeTheme(val mode: ThemeMode) : SettingsAction
    data object ResetAllData : SettingsAction
    data class ExportRequested(val uri: Uri) : SettingsAction
    data class ImportPreviewRequested(val uri: Uri) : SettingsAction
    data object ConfirmImport : SettingsAction
    data object CancelImport : SettingsAction
}

sealed interface SettingsEvent {
    data class Snack(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPrefsRepository,
    private val txDao: TransactionDao,
    private val csvService: CsvService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            prefsRepo.lastUsed.collect { prefs ->
                _state.update { it.copy(themeMode = prefs.themeMode) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ChangeTheme -> {
                viewModelScope.launch {
                    prefsRepo.saveTheme(action.mode)
                }
            }
            is SettingsAction.ResetAllData -> {
                viewModelScope.launch {
                    txDao.deleteAll()
                    _events.send(SettingsEvent.Snack("All transactions deleted"))
                }
            }
            is SettingsAction.ExportRequested -> {
                viewModelScope.launch {
                    _state.update { it.copy(isExporting = true) }
                    val txs = txDao.getAllSync()
                    csvService.export(action.uri, txs)
                        .onSuccess { _events.send(SettingsEvent.Snack("Exported successfully")) }
                        .onFailure { _events.send(SettingsEvent.Snack("Export failed: ${it.message}")) }
                    _state.update { it.copy(isExporting = false) }
                }
            }
            is SettingsAction.ImportPreviewRequested -> {
                viewModelScope.launch {
                    csvService.previewImport(action.uri)
                        .onSuccess { p -> _state.update { it.copy(preview = p) } }
                        .onFailure { _events.send(SettingsEvent.Snack("Import parse failed: ${it.message}")) }
                }
            }
            SettingsAction.ConfirmImport -> {
                viewModelScope.launch {
                    val p = _state.value.preview ?: return@launch
                    _state.update { it.copy(isImporting = true) }
                    txDao.insertAll(p.validRows)
                    _state.update { it.copy(preview = null, isImporting = false) }
                    _events.send(SettingsEvent.Snack("Imported ${p.validRows.size} transactions"))
                }
            }
            SettingsAction.CancelImport -> {
                _state.update { it.copy(preview = null) }
            }
        }
    }
}
