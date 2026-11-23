package com.hzk.back2u.domain

data class ItemErrors(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val location: String? = null,
    val date: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(title, description, category, location, date).any { it != null }
}

object ItemValidator {
    const val TITLE_MIN = 3
    const val TITLE_MAX = 60
    const val DESCRIPTION_MAX = 300

    fun validate(input: ItemInput, nowMillis: Long): ItemErrors {
        val title = input.title.trim()
        return ItemErrors(
            title = when {
                title.isEmpty() -> "Title is required"
                title.length < TITLE_MIN -> "Title must be at least $TITLE_MIN characters"
                title.length > TITLE_MAX -> "Title must be at most $TITLE_MAX characters"
                else -> null
            },
            description = if (input.description.trim().length > DESCRIPTION_MAX)
                "Description must be at most $DESCRIPTION_MAX characters" else null,
            category = if (input.categoryId.isBlank()) "Choose a category" else null,
            // cross-field rule: a found item needs a place to collect it
            location = if (input.type == ItemType.FOUND && input.location.isBlank())
                "Location is required for a found item" else null,
            date = when {
                input.dateMillis == null -> "Date is required"
                input.dateMillis > nowMillis -> "Date cannot be in the future"
                else -> null
            }
        )
    }
}