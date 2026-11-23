package com.hzk.back2u.data

import com.hzk.back2u.domain.ItemInput
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun observeMyItems(): Flow<List<Item>>
    suspend fun getItem(id: String): Result<Item>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun addItem(input: ItemInput): Result<String>
    suspend fun updateItem(id: String, input: ItemInput): Result<Unit>
    suspend fun deleteItem(id: String): Result<Unit>
}