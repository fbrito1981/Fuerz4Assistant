package com.fuerz4.assistant.data.voice.wakeword

import com.fuerz4.assistant.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

/**
 * Downloads and unpacks the Vosk Spanish model into app-private storage on first use of the
 * wake-word feature. The model is deliberately NOT bundled in the APK/AAB or committed to the
 * (public) repo — see CLAUDE.md for the size/repo-bloat rationale and the recommended model URL.
 */
@Singleton
class VoskModelProvisioner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val modelDir = File(context.filesDir, "vosk-model")

    fun isModelReady(): Boolean = modelDir.exists() && modelDir.listFiles()?.isNotEmpty() == true

    fun modelPath(): String = modelDir.absolutePath

    suspend fun downloadAndUnpackIfNeeded(onProgress: (Float) -> Unit = {}): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (isModelReady()) return@withContext Result.success(Unit)

            if (BuildConfig.VOSK_MODEL_URL.isBlank()) {
                return@withContext Result.failure(IllegalStateException("VOSK_MODEL_URL no configurada"))
            }

            runCatching {
                modelDir.mkdirs()
                downloadAndUnzip(onProgress)
                flattenSingleTopLevelDirIfNeeded()
            }.onFailure {
                modelDir.deleteRecursively()
            }
        }

    private fun downloadAndUnzip(onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(BuildConfig.VOSK_MODEL_URL).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Respuesta vacía")
            val total = body.contentLength()
            var readSoFar = 0L

            ZipInputStream(BufferedInputStream(body.byteStream())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(modelDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out ->
                            val buffer = ByteArray(8192)
                            var len = zip.read(buffer)
                            while (len > 0) {
                                out.write(buffer, 0, len)
                                readSoFar += len
                                if (total > 0) onProgress(readSoFar.toFloat() / total)
                                len = zip.read(buffer)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    /** Vosk model archives typically contain one top-level folder; flatten it into [modelDir]. */
    private fun flattenSingleTopLevelDirIfNeeded() {
        val children = modelDir.listFiles() ?: return
        if (children.size == 1 && children[0].isDirectory) {
            val nested = children[0]
            nested.listFiles()?.forEach { child ->
                child.copyRecursively(File(modelDir, child.name), overwrite = true)
            }
            nested.deleteRecursively()
        }
    }
}
