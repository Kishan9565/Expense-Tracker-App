package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.data.groupModels.Converters
import com.example.expensetracker.data.groupModels.EventEntity
import com.example.expensetracker.data.groupModels.EventMemberEntity
import com.example.expensetracker.data.groupModels.GroupDao
import com.example.expensetracker.data.groupModels.GroupExpenseEntity
import com.example.expensetracker.data.groupModels.GroupExpenseSplitEntity


@Database(
    entities = [
        Transaction::class,
        EventEntity::class,
        EventMemberEntity::class,
        GroupExpenseEntity::class,
        GroupExpenseSplitEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
