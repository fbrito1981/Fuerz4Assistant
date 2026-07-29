package com.fuerz4.assistant.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fuerz4.assistant.R

/** Shared banner shown at the top of any screen while [isOffline] is true — reused so offline copy/styling stays consistent. */
@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = isOffline) {
        Text(
            text = stringResource(R.string.common_offline_message),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
