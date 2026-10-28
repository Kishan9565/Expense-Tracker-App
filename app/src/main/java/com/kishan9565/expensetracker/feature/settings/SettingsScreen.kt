package com.kishan9565.expensetracker.feature.settings

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.onAction(SettingsAction.ExportRequested(it)) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.onAction(SettingsAction.ImportPreviewRequested(it)) }
    }

    ObserveAsEvents(vm.events) { e ->
        when (e) {
            is SettingsEvent.Snack -> scope.launch { snackbar.showSnackbar(e.message) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        topBar = { TopAppBar(title = { Text("More") }) }
    ) { paddingValues ->
        SettingsContent(
            padding = paddingValues,
            state = state,
            onAction = vm::onAction,
            exportLauncher = exportLauncher,
            importLauncher = importLauncher,
            onResetClick = { showResetDialog = true },
            onNavigateToBudgets = onNavigateToBudgets,
            onNavigateToRecurring = onNavigateToRecurring,
            onNavigateToInsights = onNavigateToInsights
        )
    }

    if (showResetDialog) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Data") },
            text = {
                Column {
                    Text("This will delete all transactions. This action cannot be undone. Type 'DELETE' to confirm.")
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        vm.onAction(SettingsAction.ResetAllData)
                        showResetDialog = false
                    },
                    enabled = confirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    val preview = state.preview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { vm.onAction(SettingsAction.CancelImport) },
            title = { Text("Import Preview") },
            text = {
                Text("Found ${preview.validRows.size} valid rows from ${preview.dateRange}.\nErrors: ${preview.errorCount}")
            },
            confirmButton = {
                Button(onClick = { vm.onAction(SettingsAction.ConfirmImport) }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { vm.onAction(SettingsAction.CancelImport) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsContent(
    padding: PaddingValues,
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    exportLauncher: ManagedActivityResultLauncher<String, Uri?>,
    importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    onResetClick: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.themeMode == mode,
                    onClick = { onAction(SettingsAction.ChangeTheme(mode)) },
                    label = { Text(mode.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Features", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToInsights, modifier = Modifier.fillMaxWidth()) {
            Text("Insights & Analytics")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToBudgets, modifier = Modifier.fillMaxWidth()) {
            Text("Budgets")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToRecurring, modifier = Modifier.fillMaxWidth()) {
            Text("Recurring Reminders")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Data Backup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { exportLauncher.launch("expenses.csv") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isExporting) "Exporting..." else "Export CSV")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/csv", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import CSV")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onResetClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset All Data")
        }
    }
}
