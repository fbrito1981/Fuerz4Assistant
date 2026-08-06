package com.fuerz4.assistant.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fuerz4.assistant.presentation.theme.Blanco
import com.fuerz4.assistant.presentation.theme.NaranjaOscuro

/** Pill-shaped single-choice toggle row, same visual language as [RoundedSelectableList] (white/orange). */
@Composable
fun <T> SegmentedOptions(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) NaranjaOscuro else Blanco,
                border = BorderStroke(1.5.dp, NaranjaOscuro),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(option) }
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) Blanco else NaranjaOscuro,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                )
            }
        }
    }
}
