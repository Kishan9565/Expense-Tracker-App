package com.kishan9565.expensetracker.feature.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    vm: BudgetsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val budgets by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Budgets") })
        }
    ) { paddingValues ->
        BudgetsContent(
            padding = paddingValues,
            budgetCount = budgets.size,
            onBack = onBack
        )
    }
}

@Composable
fun BudgetsContent(
    padding: PaddingValues,
    budgetCount: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Budgets Feature Coming Soon")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Active Budgets: $budgetCount")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}
