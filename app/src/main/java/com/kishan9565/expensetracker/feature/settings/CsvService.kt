package com.kishan9565.expensetracker.feature.settings

import android.content.Context
import android.net.Uri
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.epochDay
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.Csv
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ImportPreview(
    val validRows: List<TransactionEntity>,
    val errorCount: Int,
    val duplicateCount: Int,
    val dateRange: String
)

class CsvService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepo: CategoryRepository,
    private val clock: AppClock
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun export(uri: Uri, txs: List<TransactionWithCategory>): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write("date,type,amount,category,payment,note\n")
                    for (item in txs) {
                        val tx = item.transaction
                        val dateStr = LocalDate.ofEpochDay(tx.localDate).format(dateFormatter)
                        val typeStr = tx.type.name
                        // Amount: convert paise to decimal string (e.g. 1500.50)
                        val amountStr = java.math.BigDecimal(tx.amountPaise).movePointLeft(2).toPlainString()
                        val catStr = item.category?.name ?: "Unknown"
                        val paymentStr = tx.payment.name
                        val noteStr = tx.note
                        writer.write(Csv.row(dateStr, typeStr, amountStr, catStr, paymentStr, noteStr) + "\n")
                    }
                }
            } ?: throw IllegalStateException("Cannot open output stream")
        }
    }

    suspend fun previewImport(uri: Uri): Result<ImportPreview> {
        return runCatching {
            val categories = categoryRepo.observeAll().first()
            val valid = mutableListOf<TransactionEntity>()
            var errors = 0
            
            context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins)).use { reader ->
                    val header = reader.readLine() // skip header
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.isNotBlank()) {
                            val parts = line.split(",").map { it.removeSurrounding("\"") }
                            if (parts.size >= 6) {
                                try {
                                    val date = LocalDate.parse(parts[0], dateFormatter)
                                    val type = TxType.valueOf(parts[1])
                                    val amtOutcome = Money.parse(parts[2])
                                    val amtPaise = if (amtOutcome is Outcome.Ok) amtOutcome.value else throw IllegalArgumentException("Bad amount")
                                    val catName = parts[3]
                                    val payment = PaymentMethod.valueOf(parts[4])
                                    val note = parts[5]

                                    val catId = categories.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.id ?: categories.first().id
                                    
                                    valid.add(
                                        TransactionEntity(
                                            type = type,
                                            amountPaise = amtPaise,
                                            categoryId = catId,
                                            payment = payment,
                                            note = note,
                                            occurredAtMillis = clock.now().toEpochMilli(),
                                            localDate = date.epochDay(),
                                            source = TxSource.CSV,
                                            createdAtMillis = clock.now().toEpochMilli(),
                                            updatedAtMillis = clock.now().toEpochMilli()
                                        )
                                    )
                                } catch (e: Exception) {
                                    errors++
                                }
                            } else {
                                errors++
                            }
                        }
                        line = reader.readLine()
                    }
                }
            } ?: throw IllegalStateException("Cannot open input stream")

            val rangeStr = if (valid.isNotEmpty()) {
                val sorted = valid.map { it.localDate }.sorted()
                val start = LocalDate.ofEpochDay(sorted.first()).format(dateFormatter)
                val end = LocalDate.ofEpochDay(sorted.last()).format(dateFormatter)
                if (start == end) start else "$start to $end"
            } else "N/A"

            ImportPreview(valid, errors, 0, rangeStr)
        }
    }
}
