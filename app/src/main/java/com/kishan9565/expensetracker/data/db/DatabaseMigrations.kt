package com.kishan9565.expensetracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new table with NOT NULL constraints
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_payments_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `payeeId` INTEGER NOT NULL, 
                `vpa` TEXT NOT NULL, 
                `amountPaise` INTEGER NOT NULL, 
                `categoryId` INTEGER NOT NULL, 
                `note` TEXT NOT NULL, 
                `startedAtMillis` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Copy the data from the old table to the new table, replacing NULLs with 0
        db.execSQL(
            """
            INSERT INTO `pending_payments_new` (`id`, `payeeId`, `vpa`, `amountPaise`, `categoryId`, `note`, `startedAtMillis`)
            SELECT `id`, COALESCE(`payeeId`, 0), `vpa`, `amountPaise`, COALESCE(`categoryId`, 0), `note`, `startedAtMillis` FROM `pending_payments`
            """.trimIndent()
        )

        // Drop the old table
        db.execSQL("DROP TABLE `pending_payments`")

        // Rename the new table to the old table's name
        db.execSQL("ALTER TABLE `pending_payments_new` RENAME TO `pending_payments`")
    }
}
