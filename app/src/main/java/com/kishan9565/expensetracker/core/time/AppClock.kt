package com.kishan9565.expensetracker.core.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

interface AppClock {
    fun today(): LocalDate
    fun now(): Instant
    fun zone(): ZoneId
    fun todayFlow(): Flow<LocalDate>
}

class RealAppClock : AppClock {
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
    override fun todayFlow(): Flow<LocalDate> = flow {
        while (true) {
            val currentNow = now().atZone(zone())
            emit(currentNow.toLocalDate())
            val next = currentNow.toLocalDate().plusDays(1).atStartOfDay(zone())
            delay(Duration.between(currentNow, next).toMillis() + 500)
        }
    }.distinctUntilChanged()
}
