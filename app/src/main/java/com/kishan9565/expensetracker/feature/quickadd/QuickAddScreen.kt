package com.kishan9565.expensetracker.feature.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.R
import com.kishan9565.expensetracker.core.time.pickerMillisToLocalDate
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import com.kishan9565.expensetracker.data.db.CategoryEntity
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TxType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun QuickAddRoot(
    onDone: () -> Unit,
    onLaunchUpi: () -> Unit,
    vm: QuickAddViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    ObserveAsEvents(vm.events) { e ->
        when (e) {
            QuickAddEvent.Saved -> onDone()
            is QuickAddEvent.Snack -> {
                scope.launch {
                    snackbar.showSnackbar(e.message.asString(context))
                }
            }
        }
    }

    val speechLauncher = rememberSpeechLauncher(
        onText = { vm.onAction(QuickAddAction.OnVoiceText(it)) },
        onUnavailable = { scope.launch { snackbar.showSnackbar("Speech recognition not available") } }
    )

    QuickAddScreen(state, vm::onAction, snackbar, speechLauncher, onLaunchUpi = onLaunchUpi)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    state: QuickAddState,
    onAction: (QuickAddAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onMicClick: () -> Unit,
    onLaunchUpi: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Type Toggle
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                TypeChip(
                    text = "Expense",
                    selected = state.type == TxType.EXPENSE,
                    onClick = { onAction(QuickAddAction.OnType(TxType.EXPENSE)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                TypeChip(
                    text = "Income",
                    selected = state.type == TxType.INCOME,
                    onClick = { onAction(QuickAddAction.OnType(TxType.INCOME)) }
                )
            }

            // Amount Display
            Text(
                text = "₹ ${state.amountText.ifEmpty { "0" }}",
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(vertical = 16.dp)
            )
            state.amountError?.let {
                Text(
                    text = it.name,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(count = state.categories.size, key = { state.categories[it].id }) { index ->
                    val cat = state.categories[index]
                    CategoryChip(
                        cat = cat,
                        selected = state.categoryId == cat.id,
                        onClick = { onAction(QuickAddAction.OnCategory(cat.id)) }
                    )
                }
            }

            // Payment Methods and Date
            val paymentMethods = PaymentMethod.entries.toList()
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item(key = "date_chip") {
                    DateChip(
                        date = state.date, 
                        today = LocalDate.now(), // Real clock today visually
                        onDate = { onAction(QuickAddAction.OnDate(it)) }
                    )
                }
                items(count = paymentMethods.size, key = { paymentMethods[it].name }) { index ->
                    val method = paymentMethods[index]
                    PaymentMethodChip(
                        method = method,
                        selected = state.payment == method,
                        onClick = { onAction(QuickAddAction.OnPayment(method)) }
                    )
                }
            }
            
            // Note Field
            OutlinedTextField(
                value = state.note,
                onValueChange = { onAction(QuickAddAction.OnNote(it)) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Keypad
            Keypad(
                onKey = { onAction(QuickAddAction.OnKey(it)) },
                onSave = { onAction(QuickAddAction.OnSave) },
                isSaving = state.isSaving,
                onMicClick = onMicClick,
                onLaunchUpi = onLaunchUpi
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateChip(date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FilterChip(selected = true, onClick = { open = true }, label = { Text(dateLabel(date, today)) })
    if (open) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis.pickerMillisToLocalDate() <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onDate(it.pickerMillisToLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
fun dateLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}

@Composable
fun TypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CategoryChip(cat: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(cat.name) }
    )
}

@Composable
fun PaymentMethodChip(method: PaymentMethod, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(method.name) }
    )
}

@Composable
fun Keypad(
    onKey: (Char) -> Unit, 
    onSave: () -> Unit, 
    isSaving: Boolean, 
    onMicClick: () -> Unit,
    onLaunchUpi: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (keyStr in row) {
                    TextButton(
                        onClick = { onKey(keyStr.first()) },
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text(keyStr, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onMicClick,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Mic")
            }
            Button(
                onClick = onLaunchUpi,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("UPI Pay")
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(2f).height(56.dp)
            ) {
                Text("Save")
            }
        }
    }
}
