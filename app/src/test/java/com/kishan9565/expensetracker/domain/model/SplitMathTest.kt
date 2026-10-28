package com.kishan9565.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitMathTest {

    @Test
    fun allocate_keeps_every_paisa() {
        assertEquals(listOf(3334L, 3333L, 3333L), allocate(10_000, listOf(1, 1, 1)))
    }

    @Test
    fun transfers_settle_everyone() {
        val net = mapOf(1L to 30_000L, 2L to -10_000L, 3L to -20_000L)
        val t = minimizeTransfers(net)
        assertEquals(30_000L, t.sumOf { it.amountPaise })
        assertTrue(t.all { it.to == 1L })
    }
}
