package com.kishan9565.expensetracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.R
import com.kishan9565.expensetracker.core.designsystem.AmountText
import com.kishan9565.expensetracker.core.designsystem.LocalFinanceColors
import com.kishan9565.expensetracker.core.time.TimePeriod
import com.kishan9565.expensetracker.core.time.bounds
import com.kishan9565.expensetracker.feature.activity.TransactionRow
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeRoot(
    onEditTransaction: (Long) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(state, vm::onAction, onEditTransaction)
}

@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    var scrubbedDay by remember { mutableStateOf<Int?>(null) }
    
    val bounds = remember(state.selectedPeriod, state.selectedDate) {
        state.selectedPeriod.bounds(state.selectedDate)
    }
    
    val dateDisplay = remember(state.selectedPeriod, bounds) {
        when (state.selectedPeriod) {
            TimePeriod.DAY -> bounds.first.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            TimePeriod.WEEK -> "${bounds.first.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${bounds.second.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
            TimePeriod.MONTH -> bounds.first.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimePeriod.YEAR -> bounds.first.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
    
    val currentSpend = remember(state.currentPeriodDaily, bounds, state.today) {
        cumulativeSpend(state.currentPeriodDaily, bounds.first, bounds.second, state.today)
    }
    
    val displayedTotal = remember(currentSpend, scrubbedDay, state.spentThisPeriod) {
        if (scrubbedDay != null && currentSpend.isNotEmpty()) {
            currentSpend[scrubbedDay!!]
        } else {
            state.spentThisPeriod
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            // Period Tabs
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimePeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { onAction(HomeAction.ChangePeriod(period)) },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            // Date Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onAction(HomeAction.PreviousPeriod) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onAction(HomeAction.NextPeriod) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            if (scrubbedDay != null) {
                val dayOffset = bounds.first.plusDays(scrubbedDay!!.toLong())
                val dayStr = dayOffset.format(DateTimeFormatter.ofPattern("dd MMM"))
                Text(
                    text = dayStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AmountText(paise = displayedTotal)
                Text(
                    text = "Cumulative spend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AmountText(paise = state.balance)

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Income Card
                    Card(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AmountText(
                                paise = state.incomeThisPeriod, 
                                style = MaterialTheme.typography.titleMedium, 
                                color = LocalFinanceColors.current.income
                            )
                            if (state.incomeLastPeriod > 0) {
                                val diff = state.incomeThisPeriod - state.incomeLastPeriod
                                val pct = (diff * 100 / state.incomeLastPeriod)
                                val diffText = if (diff >= 0) "up $pct%" else "down ${-pct}%"
                                Text(diffText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    // Expense Card
                    Card(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AmountText(
                                paise = state.spentThisPeriod, 
                                style = MaterialTheme.typography.titleMedium, 
                                color = LocalFinanceColors.current.expense
                            )
                            if (state.spentLastPeriod > 0) {
                                val diff = state.spentThisPeriod - state.spentLastPeriod
                                val pct = (diff * 100 / state.spentLastPeriod)
                                val diffText = if (diff >= 0) "up $pct%" else "down ${-pct}%"
                                Text(diffText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            if (currentSpend.size >= 2) {
                val daysInPeriod = (ChronoUnit.DAYS.between(bounds.first, bounds.second) + 1).toInt()
                SpendCurveChart(
                    current = currentSpend,
                    previous = null, // Can map previous curve later if desired
                    daysInPeriod = daysInPeriod,
                    modifier = Modifier.fillMaxWidth(),
                    onScrub = { scrubbedDay = it }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.your_month_will_appear_here),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (state.recentTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            state.recentTransactions.forEach { txWithCat ->
                item(key = txWithCat.transaction.id) {
                    TransactionRow(
                        txWithCategory = txWithCat,
                        onClick = { onEditTransaction(txWithCat.transaction.id) },
                        onDelete = { /* Recent items on home don't have delete button directly to keep UI clean, or we can hide it */ }
                    )
                }
            }
        }
    }
}
