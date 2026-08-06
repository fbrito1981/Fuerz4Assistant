package com.fuerz4.assistant.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.fuerz4.assistant.presentation.theme.Blanco
import com.fuerz4.assistant.presentation.theme.NaranjaOscuro

/**
 * Single-select "combo": a pill-shaped, non-editable field showing the current pick + chevron;
 * tapping expands a [RoundedSelectableList] below it. Same visual language and expand/collapse
 * mechanics as [RoundedSelectableList]'s existing usage in `WifiSsidField` — used instead of
 * [SegmentedOptions] where the option count/label length would make a pill row wrap or overflow.
 */
@Composable
fun <T> DropdownSelector(
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = options.map { optionLabel(it) }
    val selectedLabel = optionLabel(selected)

    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Blanco,
            border = BorderStroke(1.5.dp, NaranjaOscuro),
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = selectedLabel,
                    color = NaranjaOscuro,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "dropdownChevron")
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = NaranjaOscuro,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        if (expanded) {
            RoundedSelectableList(
                items = labels,
                onItemSelected = { pickedLabel ->
                    val index = labels.indexOf(pickedLabel)
                    if (index >= 0) onSelect(options[index])
                    expanded = false
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
