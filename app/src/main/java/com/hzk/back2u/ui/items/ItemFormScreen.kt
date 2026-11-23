package com.hzk.back2u.ui.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder: R6 replaces this with the real create/edit form.
@Composable
fun ItemFormScreen(itemId: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (itemId == null) "New item (form comes in R6)" else "Edit item $itemId (form comes in R6)")
    }
}