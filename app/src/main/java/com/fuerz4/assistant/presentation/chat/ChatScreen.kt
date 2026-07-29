package com.fuerz4.assistant.presentation.chat

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R
import com.fuerz4.assistant.presentation.common.OfflineBanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startVoiceInput()
    }
    val wakeWordMicPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setWakeWordEnabled(true)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val context = LocalContext.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(uiState.messages.lastIndex) }
        }
    }

    Scaffold(
        // This Scaffold nests inside NavGraph's outer one, which already owns the bottom nav bar
        // and reserves space for it (and the system gesture-nav inset) via its own innerPadding.
        // Without this override, Scaffold's default contentWindowInsets makes this inner instance
        // ALSO reserve the bottom system-bar inset for having no bottomBar of its own — double
        // spacing the content away from the real bottom nav bar below.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = stringResource(R.string.chat_title),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearConversation) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.chat_clear))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(isOffline = uiState.isOffline)
            if (uiState.isDownloadingModel) {
                Text(
                    stringResource(R.string.chat_wake_word_downloading),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    Text(
                        stringResource(R.string.chat_empty_state),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    shape = RoundedCornerShape(50),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                VoiceInputButton(
                    isListening = uiState.isListening,
                    onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                if (uiState.isSending) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                } else {
                    IconButton(onClick = {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            ) {
                Switch(
                    checked = uiState.wakeWordEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (isRecordAudioGranted(context)) {
                                viewModel.setWakeWordEnabled(true)
                            } else {
                                wakeWordMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            viewModel.setWakeWordEnabled(false)
                        }
                    }
                )

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(R.string.chat_wake_word_toggle),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(R.string.chat_wake_word_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun isRecordAudioGranted(context: android.content.Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
