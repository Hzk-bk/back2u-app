package com.hzk.back2u.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object ItemType {
    const val LOST = "lost"
    const val FOUND = "found"
}

object ItemStatus {
    const val OPEN = "open"
    const val RETURNED = "returned"
}

data class Item(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = ItemType.LOST,
    val categoryId: String = "",
    val categoryName: String = "",
    val location: String = "",
    val dateOccurred: Timestamp? = null,
    val status: String = ItemStatus.OPEN,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)