package com.doomly.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*

/** A quiet section marker shared by scorekeeping and account views. */
@Composable
fun EditorialSection(number: String, title: String) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Frost)
            Text(number, style = MaterialTheme.typography.labelSmall, color = Mint)
        }
    }
}
