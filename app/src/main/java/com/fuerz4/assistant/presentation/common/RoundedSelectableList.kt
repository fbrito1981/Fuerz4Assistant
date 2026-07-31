package com.fuerz4.assistant.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fuerz4.assistant.presentation.theme.Blanco
import com.fuerz4.assistant.presentation.theme.NaranjaIndicador
import com.fuerz4.assistant.presentation.theme.NaranjaOscuro

/**
 * Minimalist rounded picker list — white background, orange border/text — for lightweight inline
 * selection (e.g. nearby WiFi networks). Rendered inline by the caller (not a floating popup),
 * since Material's ExposedDropdownMenu popup clips badly against a custom item background inside
 * a scrollable form.
 */
@Composable
fun RoundedSelectableList(
    items: List<String>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Blanco,
        border = BorderStroke(1.5.dp, NaranjaOscuro),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Text(
                    text = item,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemSelected(item) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(color = NaranjaIndicador)
                }
            }
        }
    }
}
