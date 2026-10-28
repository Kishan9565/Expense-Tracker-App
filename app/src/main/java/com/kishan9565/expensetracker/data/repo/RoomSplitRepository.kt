package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.data.db.SettlementEntity
import com.kishan9565.expensetracker.data.db.SplitDao
import com.kishan9565.expensetracker.data.db.SplitExpenseEntity
import com.kishan9565.expensetracker.data.db.SplitExpenseWithShares
import com.kishan9565.expensetracker.data.db.SplitGroupEntity
import com.kishan9565.expensetracker.data.db.SplitMemberEntity
import com.kishan9565.expensetracker.data.db.SplitShareEntity
import com.kishan9565.expensetracker.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomSplitRepository @Inject constructor(
    private val dao: SplitDao
) : SplitRepository {
    override fun observeActiveGroups(): Flow<List<SplitGroupEntity>> = dao.observeActiveGroups()
    override fun observeGroup(id: Long): Flow<SplitGroupEntity?> = dao.observeGroup(id)
    override fun observeMembers(groupId: Long): Flow<List<SplitMemberEntity>> = dao.observeMembers(groupId)
    override fun observeExpenses(groupId: Long): Flow<List<SplitExpenseEntity>> = dao.observeExpenses(groupId)
    override fun observeExpensesWithShares(groupId: Long): Flow<List<SplitExpenseWithShares>> = dao.observeExpensesWithShares(groupId)
    override fun observeSettlements(groupId: Long): Flow<List<SettlementEntity>> = dao.observeSettlements(groupId)
    override suspend fun getShares(expenseId: Long): List<SplitShareEntity> = dao.getSharesForExpense(expenseId)
    
    override suspend fun createGroup(group: SplitGroupEntity, initialMembers: List<SplitMemberEntity>) {
        val groupId = dao.insertGroup(group)
        initialMembers.forEach { dao.insertMember(it.copy(groupId = groupId)) }
    }
    
    override suspend fun addMember(member: SplitMemberEntity) {
        dao.insertMember(member)
    }

    override suspend fun addExpense(expense: SplitExpenseEntity, shares: List<SplitShareEntity>) {
        dao.addExpenseWithShares(expense, shares)
    }

    override suspend fun markExpenseDeleted(expenseId: Long, time: Long) {
        dao.markExpenseDeleted(expenseId, time)
    }

    override suspend fun addSettlement(settlement: SettlementEntity) {
        dao.insertSettlement(settlement)
    }
}
