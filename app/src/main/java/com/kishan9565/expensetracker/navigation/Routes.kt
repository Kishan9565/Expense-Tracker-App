package com.kishan9565.expensetracker.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ActivityRoute

@Serializable
data object SplitRoute

@Serializable
data class SplitDetailRoute(val groupId: Long)

@Serializable
data object MoreRoute

@Serializable
data object BudgetsRoute

@Serializable
data object RecurringRoute

@Serializable
data object UpiPayRoute

@Serializable
data object InsightsRoute
