package com.fuerz4.assistant.data.remote

import com.fuerz4.assistant.data.remote.dto.AuthResultDto
import com.fuerz4.assistant.data.remote.dto.ChatRequestDto
import com.fuerz4.assistant.data.remote.dto.ChatResultDto
import com.fuerz4.assistant.data.remote.dto.DeviceDto
import com.fuerz4.assistant.data.remote.dto.DeviceHistoryRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceLatestRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceListRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceListResultDto
import com.fuerz4.assistant.data.remote.dto.DeviceReadingResultDto
import com.fuerz4.assistant.data.remote.dto.DeviceReadingsResultDto
import com.fuerz4.assistant.data.remote.dto.DeviceRemoveRequestDto
import com.fuerz4.assistant.data.remote.dto.DeviceResultDto
import com.fuerz4.assistant.data.remote.dto.SimpleResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit contract for NanoServer's mobile-facing `webServices` — port of tsm_android's `TSMEndPoints.kt`.
 * Every call returns [Response] (rather than the bare body) because NanoServer sends its
 * [nano.server.enums.ResponseType]-equivalent codes as the literal HTTP status, so Retrofit
 * would otherwise surface them as thrown exceptions instead of an inspectable result — see
 * [safeApiCall]. `login`/`tokenLogin` additionally need header access for the `LoginToken`.
 */
interface NanoApi {

    @FormUrlEncoded
    @POST("api/services/login/login")
    suspend fun login(@Field("encryptedData") encryptedData: String): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/login/tokenLogin")
    suspend fun tokenLogin(@Field("encryptedData") encryptedData: String): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/login/requestCode")
    suspend fun requestCode(@Field("encryptedData") encryptedData: String): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/login/validateCode")
    suspend fun validateCode(@Field("encryptedData") encryptedData: String): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/login/resetPassword")
    suspend fun resetPassword(@Field("encryptedData") encryptedData: String): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/users/registration")
    suspend fun registerUser(
        @Field("encryptedData") encryptedData: String,
        @Field("images") images: String
    ): Response<AuthResultDto>

    @FormUrlEncoded
    @POST("api/services/users/update")
    suspend fun updateUser(
        @Header("LoginToken") token: String,
        @Field("encryptedData") encryptedData: String,
        @Field("images") images: String
    ): Response<AuthResultDto>

    @POST("api/services/devices/list")
    suspend fun listDevices(
        @Header("LoginToken") token: String,
        @Body body: DeviceListRequestDto
    ): Response<DeviceListResultDto>

    @POST("api/services/devices/create")
    suspend fun createDevice(
        @Header("LoginToken") token: String,
        @Body body: DeviceDto
    ): Response<DeviceResultDto>

    @POST("api/services/devices/update")
    suspend fun updateDevice(
        @Header("LoginToken") token: String,
        @Body body: DeviceDto
    ): Response<DeviceResultDto>

    @POST("api/services/devices/remove")
    suspend fun removeDevice(
        @Header("LoginToken") token: String,
        @Body body: DeviceRemoveRequestDto
    ): Response<SimpleResultDto>

    @POST("api/services/devices/latestValue")
    suspend fun latestDeviceReading(
        @Header("LoginToken") token: String,
        @Body body: DeviceLatestRequestDto
    ): Response<DeviceReadingResultDto>

    @POST("api/services/devices/history")
    suspend fun deviceHistory(
        @Header("LoginToken") token: String,
        @Body body: DeviceHistoryRequestDto
    ): Response<DeviceReadingsResultDto>

    @POST("api/services/chat/converse")
    suspend fun converse(
        @Header("LoginToken") token: String,
        @Body body: ChatRequestDto
    ): Response<ChatResultDto>
}
