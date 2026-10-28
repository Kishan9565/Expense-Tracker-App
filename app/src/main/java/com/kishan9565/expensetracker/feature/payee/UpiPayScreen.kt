package com.kishan9565.expensetracker.feature.payee

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan9565.expensetracker.core.ui.ObserveAsEvents
import com.kishan9565.expensetracker.domain.model.UpiLink
import com.kishan9565.expensetracker.domain.model.launchUpi
import kotlinx.coroutines.launch

@Composable
fun UpiPayRoot(
    vm: UpiPayViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onAction(UpiPayAction.UpiAppReturned)
    }

    // Mock QR Scanner launch
    val qrScanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Here GMS code scanner result would be parsed. We're mocking this action for now.
    }

    ObserveAsEvents(vm.events) { e ->
        when (e) {
            is UpiPayEvent.LaunchUpi -> {
                val launched = launchUpi(upiLauncher, e.request)
                if (!launched) scope.launch { snackbar.showSnackbar("No UPI app found") }
            }
            is UpiPayEvent.Snack -> scope.launch { snackbar.showSnackbar(e.message) }
            UpiPayEvent.PaymentRecorded -> onBack()
        }
    }

    UpiPayScreen(
        state = state,
        onAction = vm::onAction,
        onQrScan = { /* qrScanner.launch(...) */ },
        onBack = onBack,
        snackbarHostState = snackbar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiPayScreen(
    state: UpiPayState,
    onAction: (UpiPayAction) -> Unit,
    onQrScan: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Pay UPI") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            })
        }
    ) { paddingValues ->
        val pending = state.pendingPayment
        if (pending != null) {
            // Confirmation dialog after returning from UPI App
            AlertDialog(
                onDismissRequest = { onAction(UpiPayAction.PaymentStatus(false)) },
                title = { Text("Did the payment go through?") },
                text = { Text("Did you successfully pay ₹${java.math.BigDecimal(pending.amountPaise).movePointLeft(2).toPlainString()} to ${pending.vpa}?") },
                confirmButton = {
                    Button(onClick = { onAction(UpiPayAction.PaymentStatus(true)) }) {
                        Text("Yes, record it")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UpiPayAction.PaymentStatus(false)) }) {
                        Text("No / Check later")
                    }
                }
            )
        }

        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Button(onClick = onQrScan, modifier = Modifier.fillMaxWidth()) {
                Text("Scan QR Code")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.vpa,
                onValueChange = { onAction(UpiPayAction.UpdateVpa(it)) },
                label = { Text("UPI ID (VPA)") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.vpa.isNotBlank() && !UpiLink.isValidVpa(state.vpa)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(UpiPayAction.UpdateName(it)) },
                label = { Text("Payee Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.amountText,
                onValueChange = { onAction(UpiPayAction.UpdateAmount(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = { onAction(UpiPayAction.UpdateNote(it)) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onAction(UpiPayAction.Pay) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.vpa.isNotBlank() && state.amountText.isNotBlank() && UpiLink.isValidVpa(state.vpa)
            ) {
                Text("Pay via UPI")
            }
        }
    }
}
