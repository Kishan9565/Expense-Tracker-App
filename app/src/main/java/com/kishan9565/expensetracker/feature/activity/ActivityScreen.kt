package com.kishan9565.expensetracker.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.core.designsystem.AmountText
import com.kishan9565.expensetracker.core.designsystem.LocalFinanceColors
import com.kishan9565.expensetracker.core.time.TimePeriod
import com.kishan9565.expensetracker.core.time.bounds
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun ActivityRoot(
    onEditTransaction: (Long) -> Unit,
    vm: ActivityViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(vm.events) { event ->
        when (event) {
            is ActivityEvent.ShowUndo -> {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = event.message,
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.onAction(ActivityAction.UndoDelete(event.tx))
                    }
                }
            }
        }
    }

    ActivityScreen(
        state = state,
        onAction = vm::onAction,
        onEdit = onEditTransaction,
        snackbarHostState = snackbar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    state: ActivityState,
    onAction: (ActivityAction) -> Unit,
    onEdit: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val bounds = remember(state.period, state.date) { state.period.bounds(state.date) }
    val dateDisplay = remember(state.period, bounds) {
        when (state.period) {
            TimePeriod.DAY -> bounds.first.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            TimePeriod.WEEK -> "${bounds.first.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${bounds.second.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
            TimePeriod.MONTH -> bounds.first.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimePeriod.YEAR -> bounds.first.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Activity") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Period / Date Navigation
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TimePeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { onAction(ActivityAction.ChangePeriod(period)) },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onAction(ActivityAction.PreviousPeriod) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
                Text(dateDisplay, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onAction(ActivityAction.NextPeriod) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
            }

            // Filters & Sort
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = state.filterType == TxType.EXPENSE, onClick = { onAction(ActivityAction.FilterType(if (state.filterType == TxType.EXPENSE) null else TxType.EXPENSE)) }, label = { Text("Expense") })
                }
                item {
                    FilterChip(selected = state.filterType == TxType.INCOME, onClick = { onAction(ActivityAction.FilterType(if (state.filterType == TxType.INCOME) null else TxType.INCOME)) }, label = { Text("Income") })
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(selected = state.sortOrder != SortOrder.NEWEST, onClick = { expanded = true }, label = { Text("Sort: ${state.sortOrder.name}") })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(text = { Text(order.name) }, onClick = { onAction(ActivityAction.Sort(order)); expanded = false })
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (state.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.transactions, key = { it.transaction.id }) { txWithCat ->
                        val tx = txWithCat.transaction
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onAction(ActivityAction.Delete(tx))
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.error
                                } else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            },
                            content = {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    TransactionRow(
                                        txWithCategory = txWithCat,
                                        onClick = { onEdit(tx.id) },
                                        onDelete = { onAction(ActivityAction.Delete(tx)) },
                                        showDeleteIcon = true
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    txWithCategory: TransactionWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    showDeleteIcon: Boolean = false
) {
    val tx = txWithCategory.transaction
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(txWithCategory.displayTitle(), style = MaterialTheme.typography.bodyMedium)
            Text(tx.payment.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val sign = if (tx.type == TxType.INCOME) "+" else "-"
        val color = if (tx.type == TxType.INCOME) LocalFinanceColors.current.income else LocalFinanceColors.current.expense
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sign,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = color
            )
            AmountText(
                paise = tx.amountPaise,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = color
            )
            if (showDeleteIcon) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
