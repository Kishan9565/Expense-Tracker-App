package com.kishan9565.expensetracker.domain.model

import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import com.kishan9565.expensetracker.data.db.TxType
import java.math.BigDecimal
import java.util.Locale

data class CategoryKeywords(val id: Long, val type: TxType, val keywords: List<String>)
data class VoiceDraft(val amountPaise: Long?, val categoryId: Long?, val type: TxType, val note: String)

class VoiceEntryParser(private val categories: List<CategoryKeywords>) {
    private val amountRegex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)\s*(sau|hundred|hazaar|hajar|thousand|k)?\b""")
    private val incomeWords = listOf("salary", "received", "credited", "income", "aaya", "mila")
    private val filler = Regex("""\b(rupees|rupaye|rupaya|rs|inr|ka|ki|ke|for|on|paid|spent)\b|₹""")

    fun parse(raw: String): VoiceDraft {
        val text = raw.lowercase(Locale.ROOT).trim()
        val m = amountRegex.find(text)
        val paise = m?.let {
            val base = BigDecimal(it.groupValues[1].replace(",", ""))
            val mult = when (it.groupValues[2]) {
                "sau", "hundred" -> 100
                "hazaar", "hajar", "thousand", "k" -> 1000
                else -> 1
            }
            (Money.parse((base * BigDecimal(mult)).toPlainString()) as? Outcome.Ok)?.value
        }
        val type = if (incomeWords.any { it in text }) TxType.INCOME else TxType.EXPENSE
        val cat = categories.firstOrNull { c -> c.type == type && c.keywords.any { it in text } }
        val note = text.replace(amountRegex, " ").replace(filler, " ").replace(Regex("\\s+"), " ").trim()
        return VoiceDraft(paise, cat?.id, type, note)
    }
}
