package com.kishan9565.expensetracker.feature.split

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun SplitRoot(
    vm: SplitViewModel = hiltViewModel(),
    onGroupClick: (Long) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    ObserveAsEvents(vm.events) { e ->
        when (e) {
            is SplitEvent.Snack -> scope.launch { snackbar.showSnackbar(e.message) }
        }
    }
    
    SplitScreen(state, vm::onAction, snackbar, onGroupClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    state: SplitState,
    onAction: (SplitAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onGroupClick: (Long) -> Unit
) {
    var showNewGroupDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Split Bills") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewGroupDialog = true }) {
                Text("New Group", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (state.groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No groups yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = state.groups.size, key = { state.groups[it].id }) { index ->
                        val group = state.groups[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onGroupClick(group.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(group.name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showNewGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("New Group") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onAction(SplitAction.CreateGroup(groupName.trim()))
                            showNewGroupDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}
