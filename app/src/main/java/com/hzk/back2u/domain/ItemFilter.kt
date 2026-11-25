package com.hzk.back2u.domain

import com.hzk.back2u.data.Item

/** Returns the items whose title, description, location or category name
 *  contain [query]. Ignores case; a blank query returns everything. */
fun filterItems(items: List<Item>, query: String): List<Item> {
    val q = query.trim()
    if (q.isEmpty()) return items
    return items.filter { item ->
        item.title.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true) ||
                item.location.contains(q, ignoreCase = true) ||
                item.categoryName.contains(q, ignoreCase = true)
    }
}