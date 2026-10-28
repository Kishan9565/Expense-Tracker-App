package com.kishan9565.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class, 
        TransactionEntity::class, 
        BudgetEntity::class, 
        RecurringRuleEntity::class,
        PayeeEntity::class,
        PendingPaymentEntity::class,
        SplitGroupEntity::class,
        SplitMemberEntity::class,
        SplitExpenseEntity::class,
        SplitShareEntity::class,
        SettlementEntity::class
    ], 
    version = 4, 
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun payeeDao(): PayeeDao
    abstract fun pendingPaymentDao(): PendingPaymentDao
    abstract fun splitDao(): SplitDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`categoryId` INTEGER NOT NULL, `monthlyLimitPaise` INTEGER NOT NULL, PRIMARY KEY(`categoryId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amountPaise` INTEGER, `categoryId` INTEGER NOT NULL, `payment` TEXT NOT NULL, `frequency` TEXT NOT NULL, `dayOfMonth` INTEGER, `nextDueEpochDay` INTEGER NOT NULL, `active` INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `payees` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, `vpa` TEXT NOT NULL, `lastUsedMillis` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payees_vpa` ON `payees` (`vpa`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `payeeId` INTEGER, `vpa` TEXT NOT NULL, `amountPaise` INTEGER NOT NULL, `categoryId` INTEGER, `note` TEXT NOT NULL, `startedAtMillis` INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `archivedAtMillis` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_members` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `name` TEXT NOT NULL, `nameKey` TEXT NOT NULL, `isSelf` INTEGER NOT NULL, `upiVpa` TEXT, FOREIGN KEY(`groupId`) REFERENCES `split_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_members_groupId` ON `split_members` (`groupId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_split_members_groupId_nameKey` ON `split_members` (`groupId`, `nameKey`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `paidByMemberId` INTEGER NOT NULL, `amountPaise` INTEGER NOT NULL, `note` TEXT NOT NULL, `splitType` TEXT NOT NULL, `localDate` INTEGER NOT NULL, `occurredAtMillis` INTEGER NOT NULL, `deletedAtMillis` INTEGER)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_expenses_groupId` ON `split_expenses` (`groupId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_shares` (`expenseId` INTEGER NOT NULL, `memberId` INTEGER NOT NULL, `sharePaise` INTEGER NOT NULL, PRIMARY KEY(`expenseId`, `memberId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `split_settlements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `fromMemberId` INTEGER NOT NULL, `toMemberId` INTEGER NOT NULL, `amountPaise` INTEGER NOT NULL, `occurredAtMillis` INTEGER NOT NULL, `viaUpi` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_settlements_groupId` ON `split_settlements` (`groupId`)")
    }
}
