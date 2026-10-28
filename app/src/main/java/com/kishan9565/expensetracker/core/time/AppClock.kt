package com.kishan9565.expensetracker.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun today(): LocalDate
    fun now(): Instant
    fun zone(): ZoneId
}

class RealAppClock : AppClock {
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
