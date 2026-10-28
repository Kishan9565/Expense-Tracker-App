package com.kishan9565.expensetracker.core.money

import java.math.BigDecimal
import java.util.Locale

interface AppError
enum class AmountError : AppError { EMPTY, INVALID, ZERO, TOO_LARGE }

sealed interface Outcome<out D, out E : AppError> {
    data class Ok<out D>(val value: D) : Outcome<D, Nothing>
    data class Err<out E : AppError>(val error: E) : Outcome<Nothing, E>
}

object Money {
    private const val MAX_PAISE = 10_000_000_000L          // ₹10 crore
    private val AMOUNT = Regex("""\d{1,10}(\.\d{1,2})?""")

    fun parse(input: String): Outcome<Long, AmountError> {
        val s = input.trim().replace(",", "").removeSuffix(".")
        if (s.isEmpty()) return Outcome.Err(AmountError.EMPTY)
        if (!AMOUNT.matches(s)) return Outcome.Err(AmountError.INVALID)
        val paise = BigDecimal(s).movePointRight(2).longValueExact()
        return when {
            paise <= 0 -> Outcome.Err(AmountError.ZERO)
            paise > MAX_PAISE -> Outcome.Err(AmountError.TOO_LARGE)
            else -> Outcome.Ok(paise)
        }
    }

    /** Robust Indian digit grouping avoiding JDK locale inconsistencies. */
    fun format(paise: Long): String {
        val rs = paise / 100
        val p = paise % 100
        val rsStr = rs.toString()
        val result = if (rsStr.length > 3) {
            val last3 = rsStr.substring(rsStr.length - 3)
            val rest = rsStr.substring(0, rsStr.length - 3)
            val chunked = rest.reversed().chunked(2).joinToString(",").reversed()
            "$chunked,$last3"
        } else {
            rsStr
        }
        return if (p == 0L) "₹$result" else "₹$result.${String.format(Locale.US, "%02d", p)}"
    }
}
