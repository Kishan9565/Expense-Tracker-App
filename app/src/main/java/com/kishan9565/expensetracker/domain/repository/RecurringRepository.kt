package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.data.db.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringRuleEntity>>
    suspend fun getDueOn(epochDay: Long): List<RecurringRuleEntity>
    suspend fun save(rule: RecurringRuleEntity)
    suspend fun update(rule: RecurringRuleEntity)
    suspend fun delete(rule: RecurringRuleEntity)
}