package com.kishan9565.expensetracker.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToInsights: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    ObserveAsEvents(vm.events) { e ->
        when (e) {
            is SettingsEvent.Snack -> scope.launch { snackbar.showSnackbar(e.message) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        topBar = { TopAppBar(title = { Text("Settings & More") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { vm.onAction(SettingsAction.ChangeTheme(mode)) },
                            label = { Text(mode.name) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            item {
                Text("Features", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                ListItem(
                    headlineContent = { Text("Budgets") },
                    modifier = Modifier.padding(horizontal = 16.dp).clickable { onNavigateToBudgets() },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Recurring Expenses") },
                    modifier = Modifier.padding(horizontal = 16.dp).clickable { onNavigateToRecurring() },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Insights") },
                    modifier = Modifier.padding(horizontal = 16.dp).clickable { onNavigateToInsights() },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            item {
                Text("Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                Button(
                    onClick = { vm.onAction(SettingsAction.ExportRequested) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    enabled = !state.isExporting
                ) { Text("Export CSV") }
                
                Button(
                    onClick = { vm.onAction(SettingsAction.ResetAllData) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset All Data") }
            }
        }
        
        if (state.preview != null) {
            AlertDialog(
                onDismissRequest = { vm.onAction(SettingsAction.CancelImport) },
                title = { Text("Import Preview") },
                text = { Text("Found ${state.preview!!.validRows} rows.\nErrors: ${state.preview!!.errorCount}") },
                confirmButton = { Button(onClick = { vm.onAction(SettingsAction.ConfirmImport) }) { Text("Import") } },
                dismissButton = { TextButton(onClick = { vm.onAction(SettingsAction.CancelImport) }) { Text("Cancel") } }
            )
        }
    }
}
