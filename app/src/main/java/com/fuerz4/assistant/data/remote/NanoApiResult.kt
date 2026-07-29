package com.fuerz4.assistant.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/**
 * NanoServer sends its [ResponseType]-equivalent codes (e.g. "800", "806") both as the literal
 * HTTP status and as the `message` field of the JSON body. [NanoApi] therefore returns
 * [Response] rather than a bare body so non-2xx statuses show up as `!isSuccessful` instead of
 * a thrown [retrofit2.HttpException] — this is the shared envelope shape every failing response
 * has in common (`data` is always null on failure regardless of the endpoint's success type).
 */
@Serializable
data class ErrorEnvelope(val success: Boolean = false, val message: String? = null)

sealed class NanoApiError : Exception() {
    data class ServerCode(val code: String) : NanoApiError()
    data class Network(override val cause: Throwable) : NanoApiError()
    data class Unknown(override val cause: Throwable? = null) : NanoApiError()
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Runs an API call and unwraps it into a [Result], parsing NanoServer's error envelope on failure. */
suspend fun <T> safeApiCall(block: suspend () -> Response<T>): Result<Response<T>> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Result.success(response)
        } else {
            val code = response.errorBody()?.string()?.let { body ->
                runCatching { errorJson.decodeFromString<ErrorEnvelope>(body).message }.getOrNull()
            }
            Result.failure(if (code != null) NanoApiError.ServerCode(code) else NanoApiError.Unknown())
        }
    } catch (e: IOException) {
        Result.failure(NanoApiError.Network(e))
    } catch (e: Exception) {
        Result.failure(NanoApiError.Unknown(e))
    }
}

/** Convenience for call sites that only need the parsed body, not headers/status. */
suspend fun <T> safeApiCallBody(block: suspend () -> Response<T>): Result<T> =
    safeApiCall(block).mapCatching { it.body() ?: throw NanoApiError.Unknown() }
