package com.kishan9565.expensetracker.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kishan9565.expensetracker.feature.activity.ActivityRoot
import com.kishan9565.expensetracker.feature.home.HomeRoot
import com.kishan9565.expensetracker.feature.quickadd.QuickAddRoot
import com.kishan9565.expensetracker.feature.quickadd.QuickAddViewModel
import com.kishan9565.expensetracker.feature.settings.SettingsScreen
import com.kishan9565.expensetracker.feature.budgets.BudgetsScreen
import com.kishan9565.expensetracker.feature.recurring.RecurringScreen
import com.kishan9565.expensetracker.feature.payee.UpiPayRoot
import com.kishan9565.expensetracker.feature.split.SplitRoot
import com.kishan9565.expensetracker.feature.split.SplitDetailRoot
import com.kishan9565.expensetracker.feature.insights.InsightsRoot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showQuickAdd by remember { mutableStateOf(false) }
    var editTransactionId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == HomeRoute::class.qualifiedName,
                    onClick = {
                        navController.navigate(HomeRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == ActivityRoute::class.qualifiedName,
                    onClick = {
                        navController.navigate(ActivityRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Activity") },
                    label = { Text("Activity") }
                )
                NavigationBarItem(
                    selected = currentRoute?.startsWith(SplitRoute::class.qualifiedName ?: "") == true,
                    onClick = {
                        navController.navigate(SplitRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Split") },
                    label = { Text("Split") }
                )
                NavigationBarItem(
                    selected = currentRoute == MoreRoute::class.qualifiedName,
                    onClick = {
                        navController.navigate(MoreRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == HomeRoute::class.qualifiedName || currentRoute == ActivityRoute::class.qualifiedName) {
                FloatingActionButton(onClick = { 
                    editTransactionId = null
                    showQuickAdd = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(220, easing = LinearEasing)) },
            exitTransition = { fadeOut(animationSpec = tween(150, easing = LinearEasing)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220, easing = LinearEasing)) },
            popExitTransition = { fadeOut(animationSpec = tween(150, easing = LinearEasing)) }
        ) {
            composable<HomeRoute> {
                HomeRoot(
                    onEditTransaction = { txId -> 
                        editTransactionId = txId
                        showQuickAdd = true
                    }
                )
            }
            composable<ActivityRoute> {
                ActivityRoot(
                    onEditTransaction = { txId -> 
                        editTransactionId = txId
                        showQuickAdd = true
                    }
                )
            }
            composable<SplitRoute> {
                SplitRoot(
                    onGroupClick = { groupId ->
                        navController.navigate(SplitDetailRoute(groupId))
                    }
                )
            }
            composable<SplitDetailRoute> {
                SplitDetailRoot(onBack = { navController.popBackStack() })
            }
            composable<MoreRoute> {
                SettingsScreen(
                    onNavigateToBudgets = { navController.navigate(BudgetsRoute) },
                    onNavigateToRecurring = { navController.navigate(RecurringRoute) },
                    onNavigateToInsights = { navController.navigate(InsightsRoute) }
                )
            }
            composable<BudgetsRoute> {
                BudgetsScreen(onBack = { navController.popBackStack() })
            }
            composable<RecurringRoute> {
                RecurringScreen(onBack = { navController.popBackStack() })
            }
            composable<UpiPayRoute> {
                UpiPayRoot(onBack = { navController.popBackStack() })
            }
            composable<InsightsRoute> {
                InsightsRoot(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showQuickAdd) {
        ModalBottomSheet(
            onDismissRequest = { 
                showQuickAdd = false
                editTransactionId = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val quickAddViewModel: QuickAddViewModel = hiltViewModel()
            LaunchedEffect(editTransactionId) {
                quickAddViewModel.loadTransaction(editTransactionId)
            }
            
            QuickAddRoot(
                vm = quickAddViewModel,
                onDone = { 
                    showQuickAdd = false
                    editTransactionId = null
                },
                onLaunchUpi = { 
                    showQuickAdd = false
                    editTransactionId = null
                    navController.navigate(UpiPayRoute)
                }
            )
        }
    }
}
