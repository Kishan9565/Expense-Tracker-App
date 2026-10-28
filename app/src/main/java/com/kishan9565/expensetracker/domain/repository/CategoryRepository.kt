package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.data.db.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    suspend fun add(category: CategoryEntity)
    suspend fun archive(id: Long)
}
