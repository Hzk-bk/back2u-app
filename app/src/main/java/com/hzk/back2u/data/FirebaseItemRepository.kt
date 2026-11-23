package com.hzk.back2u.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hzk.back2u.domain.ItemInput
import com.hzk.back2u.domain.ItemStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseItemRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ItemRepository {

    private val items = db.collection("items")

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

    // The query itself filters by owner; the listener is removed when the flow is cancelled.
    override fun observeMyItems(): Flow<List<Item>> = callbackFlow {
        val registration = items
            .whereEqualTo("ownerId", requireUid())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot.toObjects(Item::class.java))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getItem(id: String): Result<Item> = runCatching {
        items.document(id).get().await().toObject(Item::class.java)
            ?: throw IllegalStateException("Item not found")
    }

    override suspend fun getCategories(): Result<List<Category>> = runCatching {
        db.collection("categories").orderBy("sortOrder").get().await()
            .toObjects(Category::class.java)
    }

    override suspend fun addItem(input: ItemInput): Result<String> = runCatching {
        val data = mapOf(
            "ownerId" to requireUid(),
            "title" to input.title.trim(),
            "description" to input.description.trim(),
            "type" to input.type,
            "categoryId" to input.categoryId,
            "categoryName" to input.categoryName,
            "location" to input.location.trim(),
            "dateOccurred" to Date(requireNotNull(input.dateMillis)),
            "status" to ItemStatus.OPEN,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        items.add(data).await().id
    }

    override suspend fun updateItem(id: String, input: ItemInput): Result<Unit> = runCatching {
        val data = mapOf(
            "title" to input.title.trim(),
            "description" to input.description.trim(),
            "type" to input.type,
            "categoryId" to input.categoryId,
            "categoryName" to input.categoryName,
            "location" to input.location.trim(),
            "dateOccurred" to Date(requireNotNull(input.dateMillis)),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        items.document(id).update(data).await()
        Unit
    }

    override suspend fun deleteItem(id: String): Result<Unit> = runCatching {
        items.document(id).delete().await()
        Unit
    }
}