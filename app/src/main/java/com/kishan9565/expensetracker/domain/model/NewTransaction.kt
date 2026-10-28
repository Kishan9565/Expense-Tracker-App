package com.kishan9565.expensetracker.domain.model

import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.TxType
import java.time.LocalDate

data class NewTransaction(
    val type: TxType,
    val amountPaise: Long,
    val categoryId: Long,
    val payment: PaymentMethod,
    val note: String,
    val date: LocalDate,
    val source: TxSource = TxSource.MANUAL,
    val payeeId: Long? = null
)
