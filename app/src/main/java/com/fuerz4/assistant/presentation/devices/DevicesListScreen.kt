package com.fuerz4.assistant.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R
import com.fuerz4.assistant.domain.model.Device
import com.fuerz4.assistant.presentation.theme.NaranjaClaro

@Composable
fun DevicesListScreen(
    onAddDevice: () -> Unit,
    onOpenDetail: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    viewModel: DevicesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDevice) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.devices_add))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.devices.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiState.error?.asString() ?: stringResource(R.string.devices_empty)
                    )
                }
                else -> LazyColumn {
                    items(uiState.devices, key = { it.uuid }) { device ->
                        DeviceRow(
                            device = device,
                            onClick = { onOpenDetail(device) },
                            onEdit = { onEditDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit, onEdit: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = NaranjaClaro),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column {
                Text(device.name, style = MaterialTheme.typography.titleLarge)
                device.model?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit))
            }
        }
    }
}
