package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.data.db.SettlementEntity
import com.kishan9565.expensetracker.data.db.SplitExpenseEntity
import com.kishan9565.expensetracker.data.db.SplitExpenseWithShares
import com.kishan9565.expensetracker.data.db.SplitGroupEntity
import com.kishan9565.expensetracker.data.db.SplitMemberEntity
import com.kishan9565.expensetracker.data.db.SplitShareEntity
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    fun observeActiveGroups(): Flow<List<SplitGroupEntity>>
    fun observeGroup(id: Long): Flow<SplitGroupEntity?>
    fun observeMembers(groupId: Long): Flow<List<SplitMemberEntity>>
    fun observeExpenses(groupId: Long): Flow<List<SplitExpenseEntity>>
    fun observeExpensesWithShares(groupId: Long): Flow<List<SplitExpenseWithShares>>
    fun observeSettlements(groupId: Long): Flow<List<SettlementEntity>>
    suspend fun getShares(expenseId: Long): List<SplitShareEntity>
    suspend fun createGroup(group: SplitGroupEntity, initialMembers: List<SplitMemberEntity>)
    suspend fun addMember(member: SplitMemberEntity)
    suspend fun addExpense(expense: SplitExpenseEntity, shares: List<SplitShareEntity>)
    suspend fun markExpenseDeleted(expenseId: Long, time: Long)
    suspend fun addSettlement(settlement: SettlementEntity)
}
