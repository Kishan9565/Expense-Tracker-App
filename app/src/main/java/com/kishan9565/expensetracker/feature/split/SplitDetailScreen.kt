package com.kishan9565.expensetracker.feature.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.core.designsystem.AmountText
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun SplitDetailRoot(
    vm: SplitDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    ObserveAsEvents(vm.events) { e ->
        when (e) {
            is SplitDetailEvent.Snack -> scope.launch { snackbar.showSnackbar(e.message) }
        }
    }

    SplitDetailScreen(state, vm::onAction, onBack, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitDetailScreen(
    state: SplitDetailState,
    onAction: (SplitDetailAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMember by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(state.group?.name ?: "Group Details") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Expenses") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Balances") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Members") })
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && state.members.size >= 2) {
                FloatingActionButton(onClick = { showAddExpense = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(onClick = { showAddMember = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> ExpensesTab(state.expenses, onAction)
                1 -> BalancesTab(state.transfers, onAction)
                2 -> MembersTab(state.members)
            }
        }
    }

    if (showAddMember) {
        var memberName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Member") },
            text = {
                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(onClick = { 
                    if (memberName.isNotBlank()) {
                        onAction(SplitDetailAction.AddMember(memberName))
                        showAddMember = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddExpense) {
        AddExpenseDialog(
            members = state.members,
            onDismiss = { showAddExpense = false },
            onConfirm = { amount, note, payer, included ->
                onAction(SplitDetailAction.AddExpense(amount, note, payer, included))
                showAddExpense = false
            }
        )
    }
}

@Composable
fun ExpensesTab(expenses: List<ExpenseWithSharesUi>, onAction: (SplitDetailAction) -> Unit) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No expenses yet") }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(expenses, key = { it.expense.id }) { eUi ->
                ListItem(
                    headlineContent = { Text(eUi.expense.note.ifBlank { "Expense" }) },
                    supportingContent = { Text("${eUi.payerName} paid • ${eUi.sharesSummary}") },
                    trailingContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AmountText(paise = eUi.expense.amountPaise)
                            IconButton(onClick = { onAction(SplitDetailAction.DeleteExpense(eUi.expense.id)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun BalancesTab(transfers: List<TransferUi>, onAction: (SplitDetailAction) -> Unit) {
    if (transfers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("All settled up!") }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(transfers) { t ->
                ListItem(
                    headlineContent = { Text("${t.fromName} owes ${t.toName}") },
                    trailingContent = { AmountText(paise = t.amountPaise, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onAction(SplitDetailAction.Settle(t, false)) }) { Text("Mark Paid (Cash)") }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MembersTab(members: List<com.kishan9565.expensetracker.data.db.SplitMemberEntity>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(members, key = { it.id }) { m ->
            ListItem(
                headlineContent = { Text(m.name) },
                supportingContent = { if (m.isSelf) Text("You") else null }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun AddExpenseDialog(
    members: List<com.kishan9565.expensetracker.data.db.SplitMemberEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, Long, List<Long>) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(members.firstOrNull()?.id ?: 0L) }
    var includedMembers by remember { mutableStateOf(members.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Paid by:", style = MaterialTheme.typography.labelMedium)
                // Simple segmented/chip select for payer
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.take(3).forEach { m ->
                        FilterChip(selected = paidBy == m.id, onClick = { paidBy = m.id }, label = { Text(m.name) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Split between:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.take(3).forEach { m ->
                        FilterChip(
                            selected = includedMembers.contains(m.id),
                            onClick = { 
                                if (includedMembers.contains(m.id) && includedMembers.size > 1) {
                                    includedMembers = includedMembers - m.id
                                } else {
                                    includedMembers = includedMembers + m.id
                                }
                            },
                            label = { Text(m.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = com.kishan9565.expensetracker.core.money.Money.parse(amountStr)
                if (amt is com.kishan9565.expensetracker.core.money.Outcome.Ok) {
                    onConfirm(amt.value, note, paidBy, includedMembers.toList())
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
