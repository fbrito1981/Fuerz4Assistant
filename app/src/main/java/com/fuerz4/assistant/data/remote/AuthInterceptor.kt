package com.fuerz4.assistant.data.remote

import com.fuerz4.assistant.BuildConfig
import com.fuerz4.assistant.data.crypto.WsseUtil
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds the WSSE gateway credential NanoServer requires on every webServices call —
 * port of tsm_android's `ServiceBuilder.kt` interceptor. This is a shared machine credential
 * (not per-user); per-user identity travels separately via the `LoginToken` header added by
 * call sites that need it (see [NanoApi]).
 */
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val header = WsseUtil.getAuthorizationHeader(BuildConfig.SECURITY_KEY, BuildConfig.SECURITY_SECRET)
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "WSSE profile=\"UsernameToken\"")
            .addHeader(WsseUtil.HEADER_KEY, header)
            .build()

        return chain.proceed(request)
    }
}
