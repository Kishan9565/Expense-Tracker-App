package com.example.expensetracker.data


object DummyDataProvider {
    fun getDummyDailyExpenses(): List<DailyExpense> {
        return listOf(
            DailyExpense(dayTimestamp = 1L, totalAmount = 20.0),
            DailyExpense(dayTimestamp = 2L, totalAmount = 450.0),
            DailyExpense(dayTimestamp = 3L, totalAmount = 160.0),
            DailyExpense(dayTimestamp = 4L, totalAmount = 600.0),
            DailyExpense(dayTimestamp = 5L, totalAmount = 250.0),
            DailyExpense(dayTimestamp = 6L, totalAmount = 700.0),
            DailyExpense(dayTimestamp = 7L, totalAmount = 400.0),
            DailyExpense(dayTimestamp = 8L, totalAmount = 20.0),
            DailyExpense(dayTimestamp = 9L, totalAmount = 100.0),
            DailyExpense(dayTimestamp = 10L, totalAmount = 1000.0),
            DailyExpense(dayTimestamp = 11L, totalAmount = 300.0),
            DailyExpense(dayTimestamp = 12L, totalAmount = 150.0),
            DailyExpense(dayTimestamp = 13L, totalAmount = 800.0),
            DailyExpense(dayTimestamp = 14L, totalAmount = 500.0),
            DailyExpense(dayTimestamp = 15L, totalAmount = 60.0),
            DailyExpense(dayTimestamp = 16L, totalAmount = 900.0),
            DailyExpense(dayTimestamp = 17L, totalAmount = 750.0),
        )
    }
}
