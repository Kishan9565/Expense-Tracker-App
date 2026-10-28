package com.kishan9565.expensetracker.domain.model

object Csv {
    fun escape(field: String): String {
        // CSV-injection guard: spreadsheets treat a leading = + - @ as a formula
        val safe = if (field.firstOrNull() in listOf('=', '+', '-', '@')) "'$field" else field
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
    
    fun row(vararg f: String) = f.joinToString(",") { escape(it) }
}