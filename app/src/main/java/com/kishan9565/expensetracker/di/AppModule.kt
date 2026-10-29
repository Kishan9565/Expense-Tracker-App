package com.kishan9565.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.RealAppClock
import com.kishan9565.expensetracker.data.db.AppDatabase
import com.kishan9565.expensetracker.data.db.CategoryDao
import com.kishan9565.expensetracker.data.db.MIGRATION_1_2
import com.kishan9565.expensetracker.data.db.MIGRATION_2_3
import com.kishan9565.expensetracker.data.db.MIGRATION_3_4
import com.kishan9565.expensetracker.data.db.MIGRATION_4_5
import com.kishan9565.expensetracker.data.db.PayeeDao
import com.kishan9565.expensetracker.data.db.PendingDao
import com.kishan9565.expensetracker.data.db.SeedDefaultCategories
import com.kishan9565.expensetracker.data.db.SplitDao
import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.data.prefs.DataStoreUserPrefsRepository
import com.kishan9565.expensetracker.data.repo.RoomBudgetRepository
import com.kishan9565.expensetracker.data.repo.RoomCategoryRepository
import com.kishan9565.expensetracker.data.repo.RoomRecurringRepository
import com.kishan9565.expensetracker.data.repo.RoomSplitRepository
import com.kishan9565.expensetracker.data.repo.RoomTransactionRepository
import com.kishan9565.expensetracker.domain.repository.BudgetRepository
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import com.kishan9565.expensetracker.domain.repository.RecurringRepository
import com.kishan9565.expensetracker.domain.repository.SplitRepository
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppClock(): AppClock = RealAppClock()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expenses.db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .addCallback(SeedDefaultCategories())
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    
    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun providePayeeDao(db: AppDatabase): PayeeDao = db.payeeDao()

    @Provides
    @Singleton
    fun providePendingDao(db: AppDatabase): PendingDao = db.pendingDao()
    
    @Provides
    @Singleton
    fun provideSplitDao(db: AppDatabase): SplitDao = db.splitDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: RoomTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository
    
    @Binds
    @Singleton
    abstract fun bindUserPrefsRepository(impl: DataStoreUserPrefsRepository): UserPrefsRepository

    @Binds
    @Singleton
    abstract fun bindSplitRepository(impl: RoomSplitRepository): SplitRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: RoomBudgetRepository): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: RoomRecurringRepository): RecurringRepository
}
