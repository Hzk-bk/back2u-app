package com.hzk.back2u.domain

import com.hzk.back2u.data.Item

/** Returns the items that match [query] (title, description, location or category
 *  name, ignoring case). With [hideReturned] set, returned items are left out. */
fun filterItems(items: List<Item>, query: String, hideReturned: Boolean = false): List<Item> {
    val q = query.trim()
    return items.filter { item ->
        (!hideReturned || item.status != ItemStatus.RETURNED) &&
                (q.isEmpty() ||
                        item.title.contains(q, ignoreCase = true) ||
                        item.description.contains(q, ignoreCase = true) ||
                        item.location.contains(q, ignoreCase = true) ||
                        item.categoryName.contains(q, ignoreCase = true))
    }
}

/** The status an item gets when the user taps the toggle. */
fun toggledStatus(current: String): String =
    if (current == ItemStatus.RETURNED) ItemStatus.OPEN else ItemStatus.RETURNED