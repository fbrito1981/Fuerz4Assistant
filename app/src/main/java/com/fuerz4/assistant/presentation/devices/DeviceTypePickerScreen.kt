package com.fuerz4.assistant.presentation.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fuerz4.assistant.R
import com.fuerz4.assistant.presentation.theme.NaranjaClaro

@Composable
fun DeviceTypePickerScreen(onTypeSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.device_type_picker_title), style = MaterialTheme.typography.titleLarge)

        DeviceTypeCard(
            title = stringResource(R.string.device_type_energy),
            description = stringResource(R.string.device_type_energy_description),
            onClick = { onTypeSelected("energy") }
        )
        DeviceTypeCard(
            title = stringResource(R.string.device_type_environment),
            description = stringResource(R.string.device_type_environment_description),
            onClick = { onTypeSelected("environment") }
        )
    }
}

@Composable
private fun DeviceTypeCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = NaranjaClaro),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
