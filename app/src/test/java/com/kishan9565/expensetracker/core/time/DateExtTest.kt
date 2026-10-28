package com.kishan9565.expensetracker.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateExtTest {
    @Test
    fun epochDay_is_correct() {
        val date = LocalDate.of(2026, 10, 25)
        assertEquals(20751L, date.epochDay())
    }
}
