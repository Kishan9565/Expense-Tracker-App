package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import androidx.room.withTransaction
import com.kishan9565.expensetracker.data.db.AppDatabase
import com.kishan9565.expensetracker.core.time.epochDay

object CsvParser {
    /** RFC-4180 style: quotes, "" escapes, commas/newlines inside quotes, CRLF, BOM. */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        val s = text.removePrefix("\uFEFF")
        
        fun endField() { 
            row.add(field.toString())
            field.clear() 
        }
        fun endRow() { 
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = mutableListOf() 
        }
        
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes && c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && c == '\n' -> endRow()
                !inQuotes && c == '\r' -> Unit
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}

fun String.unguardCsv(): String = if (length > 1 && this[0] == '\'' && this[1] in "=+-@") substring(1) else this

fun importKey(localDate: Long, type: TxType, amountPaise: Long, category: String, note: String) =
    listOf(localDate, type.name, amountPaise, category.trim().lowercase(), note.trim().lowercase()).joinToString("|")

class CsvService @Inject constructor(
    private val db: AppDatabase,
    private val transactionDao: TransactionDao,
    private val categoryRepo: CategoryRepository
) {
    suspend fun export(): String {
        val txs = transactionDao.getAllSync()
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return buildString {
            append("Date,Type,AmountPaise,Category,Payment,Note,Source\n")
            for (t in txs) {
                val e = t.transaction
                val date = LocalDate.ofEpochDay(e.localDate).format(dtf)
                val cat = t.category?.name ?: "Other"
                // Guard formula injection on export
                val note = e.note.replace("\"", "\"\"").let { if (it.isNotEmpty() && it[0] in "=+-@") "'$it" else it }
                append("""$date,${e.type.name},${e.amountPaise},"$cat",${e.payment.name},"$note",${e.source.name}""")
                append("\n")
            }
        }
    }

    suspend fun import(csvText: String, zone: ZoneId): ImportResult {
        val rows = CsvParser.parse(csvText)
        if (rows.isEmpty()) return ImportResult(0, 0, 0)
        
        val dataRows = rows.drop(1)
        
        val cats = categoryRepo.observeAll().first()
        val catMap = cats.associateBy { it.name.lowercase() }
        
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        // Grab current exact rows to dedupe against
        val existing = transactionDao.getAllSync()
        val existingKeys = existing.map { 
            importKey(it.transaction.localDate, it.transaction.type, it.transaction.amountPaise, it.category?.name ?: "Other", it.transaction.note)
        }.toSet()
        
        val toInsert = mutableListOf<TransactionEntity>()
        var duplicateCount = 0
        var mappedToOtherCount = 0

        for (row in dataRows) {
            if (row.size < 6) continue // skip invalid
            
            try {
                val dateStr = row[0]
                val type = TxType.valueOf(row[1])
                val amountPaise = row[2].toLong()
                val catName = row[3]
                val payment = PaymentMethod.valueOf(row[4])
                val note = row[5].unguardCsv()
                val sourceStr = row.getOrNull(6)
                val source = if (sourceStr != null) TxSource.valueOf(sourceStr) else TxSource.MANUAL
                
                val localDateObj = LocalDate.parse(dateStr, dtf)
                val localDate = localDateObj.epochDay()
                
                // Dedupe check
                val key = importKey(localDate, type, amountPaise, catName, note)
                if (existingKeys.contains(key)) {
                    duplicateCount++
                    continue
                }

                // Category mapping
                val resolvedCat = catMap[catName.lowercase()]
                val finalCategoryId = if (resolvedCat != null && resolvedCat.type == type) {
                    resolvedCat.id
                } else {
                    mappedToOtherCount++
                    cats.firstOrNull { it.type == type && it.name.lowercase() == "other" }?.id 
                        ?: cats.firstOrNull { it.type == type }?.id
                        ?: 1L
                }

                val occurredAt = localDateObj.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                val now = Instant.now().toEpochMilli()

                toInsert.add(
                    TransactionEntity(
                        type = type,
                        amountPaise = amountPaise,
                        categoryId = finalCategoryId,
                        payment = payment,
                        note = note,
                        localDate = localDate,
                        occurredAtMillis = occurredAt,
                        source = source,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
            } catch (e: Exception) {
                // skip malformed row silently for now
                continue
            }
        }
        
        db.withTransaction {
            transactionDao.insertAll(toInsert)
        }
        
        return ImportResult(
            inserted = toInsert.size,
            duplicatesSkipped = duplicateCount,
            mappedToOther = mappedToOtherCount
        )
    }
}

data class ImportResult(val inserted: Int, val duplicatesSkipped: Int, val mappedToOther: Int)
