package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Insert
    suspend fun insertGroup(group: SplitGroupEntity): Long
    
    @Query("SELECT * FROM split_groups WHERE id = :id")
    fun observeGroup(id: Long): Flow<SplitGroupEntity?>

    @Insert
    suspend fun insertMember(member: SplitMemberEntity): Long

    @Query("SELECT * FROM split_groups WHERE archivedAtMillis IS NULL")
    fun observeActiveGroups(): Flow<List<SplitGroupEntity>>

    @Query("SELECT * FROM split_members WHERE groupId = :groupId")
    fun observeMembers(groupId: Long): Flow<List<SplitMemberEntity>>

    @Insert
    suspend fun insertExpense(expense: SplitExpenseEntity): Long

    @Insert
    suspend fun insertShares(shares: List<SplitShareEntity>)

    @Transaction
    suspend fun addExpenseWithShares(expense: SplitExpenseEntity, shares: List<SplitShareEntity>) {
        val expenseId = insertExpense(expense)
        insertShares(shares.map { it.copy(expenseId = expenseId) })
    }

    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId AND deletedAtMillis IS NULL")
    fun observeExpenses(groupId: Long): Flow<List<SplitExpenseEntity>>
    
    @Transaction
    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId AND deletedAtMillis IS NULL ORDER BY occurredAtMillis DESC")
    fun observeExpensesWithShares(groupId: Long): Flow<List<SplitExpenseWithShares>>

    @Query("SELECT * FROM split_shares WHERE expenseId = :expenseId")
    suspend fun getSharesForExpense(expenseId: Long): List<SplitShareEntity>

    @Query("UPDATE split_expenses SET deletedAtMillis = :time WHERE id = :expenseId")
    suspend fun markExpenseDeleted(expenseId: Long, time: Long)

    @Insert
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM split_settlements WHERE groupId = :groupId ORDER BY occurredAtMillis DESC")
    fun observeSettlements(groupId: Long): Flow<List<SettlementEntity>>
}
