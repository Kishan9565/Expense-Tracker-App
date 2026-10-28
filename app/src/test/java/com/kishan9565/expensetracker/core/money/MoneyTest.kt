package com.kishan9565.expensetracker.core.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun parse_ok() {
        assertEquals(Outcome.Ok(150050L), Money.parse("1,500.50"))
    }

    @Test
    fun parse_zero() {
        assertEquals(Outcome.Err(AmountError.ZERO), Money.parse("0"))
    }

    @Test
    fun parse_garbage() {
        assertEquals(Outcome.Err(AmountError.INVALID), Money.parse("12.345"))
    }

    @Test
    fun format_whole() {
        // Indian grouping
        assertEquals("₹1,50,000", Money.format(15_000_000L))
    }
}
