package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.data.db.CategoryDao
import com.kishan9565.expensetracker.data.db.CategoryEntity
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = dao.observeActive()

    override suspend fun add(category: CategoryEntity) {
        dao.insert(category)
    }

    override suspend fun archive(id: Long) {
        dao.archive(id)
    }
}
