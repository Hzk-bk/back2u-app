package com.hzk.back2u.domain

data class ItemInput(
    val title: String = "",
    val description: String = "",
    val type: String = ItemType.LOST,
    val categoryId: String = "",
    val categoryName: String = "",
    val location: String = "",
    val dateMillis: Long? = null
)