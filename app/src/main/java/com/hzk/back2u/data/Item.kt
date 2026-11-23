package com.hzk.back2u.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.hzk.back2u.domain.ItemStatus
import com.hzk.back2u.domain.ItemType
import java.util.Date

data class Item(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = ItemType.LOST,
    val categoryId: String = "",
    val categoryName: String = "",
    val location: String = "",
    val dateOccurred: Date? = null,
    val status: String = ItemStatus.OPEN,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)