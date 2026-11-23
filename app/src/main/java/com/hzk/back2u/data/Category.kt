package com.hzk.back2u.data

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortOrder: Int = 0
)