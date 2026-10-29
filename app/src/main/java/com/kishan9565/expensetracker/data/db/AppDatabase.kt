package com.kishan9565.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        PayeeEntity::class,
        SplitGroupEntity::class,
        SplitMemberEntity::class,
        SplitExpenseEntity::class,
        SplitShareEntity::class,
        SettlementEntity::class,
        PendingPaymentEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun payeeDao(): PayeeDao
    abstract fun splitDao(): SplitDao
    abstract fun pendingDao(): PendingDao
}
