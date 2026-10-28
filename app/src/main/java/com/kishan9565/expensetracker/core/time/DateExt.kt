package com.kishan9565.expensetracker.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

enum class TimePeriod { DAY, WEEK, MONTH, YEAR }

fun TimePeriod.adjust(date: LocalDate, amount: Long): LocalDate = when (this) {
    TimePeriod.DAY -> date.plusDays(amount)
    TimePeriod.WEEK -> date.plusWeeks(amount)
    TimePeriod.MONTH -> date.plusMonths(amount)
    TimePeriod.YEAR -> date.plusYears(amount)
}

fun TimePeriod.bounds(date: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    TimePeriod.DAY -> date to date
    TimePeriod.WEEK -> {
        val mon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        mon to mon.plusDays(6)
    }
    TimePeriod.MONTH -> date.withDayOfMonth(1) to date.withDayOfMonth(date.lengthOfMonth())
    TimePeriod.YEAR -> date.withDayOfYear(1) to date.withDayOfYear(date.lengthOfYear())
}

/** The calendar day in the user's own zone, as epoch day. Group and filter by THIS, never by millis. */
fun LocalDate.epochDay(): Long = toEpochDay()

/** Exact instant for an entry made on [this] day, keeping the current wall-clock time. */
fun LocalDate.toEntryMillis(zone: ZoneId, now: Instant): Long =
    atTime(now.atZone(zone).toLocalTime()).atZone(zone).toInstant().toEpochMilli()

/** Material3 DatePicker returns UTC-midnight millis for the chosen day. Convert with UTC, not the device zone. */
fun Long.pickerMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun YearMonth.epochRange(): LongRange = atDay(1).toEpochDay()..atEndOfMonth().toEpochDay()
