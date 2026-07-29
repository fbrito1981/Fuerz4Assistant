package com.fuerz4.assistant.data.voice.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.fuerz4.assistant.MainActivity
import com.fuerz4.assistant.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import javax.inject.Inject

/**
 * Foreground service ("microphone" type) that keeps Vosk listening offline for "Che fuerza",
 * the same shape as "Hey Google" wake-word detection. Only runs while the user has opted in
 * (see the toggle in [com.fuerz4.assistant.presentation.chat.ChatScreen]) — see CLAUDE.md for
 * the full lifecycle and mic-arbitration rules this service follows.
 */
@AndroidEntryPoint
class VoskWakeWordService : Service() {

    @Inject
    lateinit var modelProvisioner: VoskModelProvisioner

    @Inject
    lateinit var micArbiter: MicArbiter

    @Inject
    lateinit var eventBus: WakeWordEventBus

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var speechService: SpeechService? = null
    private var model: Model? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // minSdk 30 > Q(29), so the typed startForeground overload is always available here.
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        micArbiter.owner
            .onEach { owner -> speechService?.setPause(owner != MicOwner.WAKE_WORD) }
            .launchIn(serviceScope)

        serviceScope.launch { startListeningIfPossible() }
    }

    private suspend fun startListeningIfPossible() {
        if (!modelProvisioner.isModelReady()) {
            stopSelf()
            return
        }
        if (!micArbiter.tryAcquire(MicOwner.WAKE_WORD)) {
            stopSelf()
            return
        }

        runCatching {
            val loadedModel = Model(modelProvisioner.modelPath())
            val recognizer = Recognizer(loadedModel, SAMPLE_RATE, WakeWordGrammar.GRAMMAR_JSON)
            model = loadedModel

            speechService = SpeechService(recognizer, SAMPLE_RATE).also {
                it.startListening(object : RecognitionListener {
                    override fun onResult(hypothesis: String?) = handleHypothesis(hypothesis)
                    override fun onFinalResult(hypothesis: String?) = handleHypothesis(hypothesis)
                    override fun onPartialResult(hypothesis: String?) = Unit
                    override fun onError(exception: Exception?) = Unit
                    override fun onTimeout() = Unit
                })
            }
        }.onFailure {
            micArbiter.release(MicOwner.WAKE_WORD)
            stopSelf()
        }
    }

    private fun handleHypothesis(hypothesisJson: String?) {
        val text = hypothesisJson
            ?.let { runCatching { JSONObject(it).optString("text") }.getOrNull() }
            ?.lowercase()
            .orEmpty()

        if (text.contains(WakeWordGrammar.PHRASE)) {
            eventBus.notifyDetected()
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "wake_word_channel"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.wake_word_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        model?.close()
        micArbiter.release(MicOwner.WAKE_WORD)
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val NOTIFICATION_ID = 4200
        const val SAMPLE_RATE = 16000.0f
    }
}
