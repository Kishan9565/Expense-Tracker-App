package com.kishan9565.expensetracker.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HomeScreenLogicTest {

    @Test
    fun calculatePreviousSpendVirtualToday_handlesEndOfMonthSafely() {
        val selectedMonth = YearMonth.of(2026, 4) // April
        val today = LocalDate.of(2026, 5, 31) // May 31

        val prevMonth = selectedMonth.minusMonths(1) // March (31 days)
        val clampedDay = minOf(today.dayOfMonth, prevMonth.lengthOfMonth())
        val virtualTodayForPrevMonth = prevMonth.atDay(clampedDay)

        // March 31 is valid
        assertEquals(LocalDate.of(2026, 3, 31), virtualTodayForPrevMonth)
    }

    @Test
    fun calculatePreviousSpendVirtualToday_handlesLeapYearSafely() {
        val selectedMonth = YearMonth.of(2024, 3) // March 2024
        val today = LocalDate.of(2024, 3, 31) // March 31

        val prevMonth = selectedMonth.minusMonths(1) // Feb 2024 (Leap year, 29 days)
        val clampedDay = minOf(today.dayOfMonth, prevMonth.lengthOfMonth())
        val virtualTodayForPrevMonth = prevMonth.atDay(clampedDay)

        // Should clamp to Feb 29 safely without crashing
        assertEquals(LocalDate.of(2024, 2, 29), virtualTodayForPrevMonth)
    }

    @Test
    fun calculatePreviousSpendVirtualToday_handlesNonLeapYearSafely() {
        val selectedMonth = YearMonth.of(2023, 3) // March 2023
        val today = LocalDate.of(2023, 3, 31) // March 31

        val prevMonth = selectedMonth.minusMonths(1) // Feb 2023 (28 days)
        val clampedDay = minOf(today.dayOfMonth, prevMonth.lengthOfMonth())
        val virtualTodayForPrevMonth = prevMonth.atDay(clampedDay)

        // Should clamp to Feb 28 safely without crashing
        assertEquals(LocalDate.of(2023, 2, 28), virtualTodayForPrevMonth)
    }
}
