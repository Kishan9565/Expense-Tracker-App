package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.data.db.RecurringDao
import com.kishan9565.expensetracker.data.db.RecurringRuleEntity
import com.kishan9565.expensetracker.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomRecurringRepository @Inject constructor(
    private val dao: RecurringDao
) : RecurringRepository {
    override fun observeAll(): Flow<List<RecurringRuleEntity>> = dao.observeAll()
    override suspend fun getDueOn(epochDay: Long): List<RecurringRuleEntity> = dao.getDueOn(epochDay)
    override suspend fun save(rule: RecurringRuleEntity) { dao.insert(rule) }
    override suspend fun update(rule: RecurringRuleEntity) = dao.update(rule)
    override suspend fun delete(rule: RecurringRuleEntity) = dao.delete(rule)
}