package com.fuerz4.assistant.data.crypto

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Kotlin port of NanoServer's `WSSecurityUtils.java` (and tsm_android's `WSSecurityUtil.kt`).
 * Uses `java.util.Base64` (available since API 26, well under our minSdk 30) instead of
 * `android.util.Base64` so this class is a plain, Robolectric-free JVM unit test target,
 * and matches the server's own `java.util.Base64` usage exactly.
 */
object WsseUtil {
    const val HEADER_KEY = "X-WSSE"

    private const val CUSTOMER_TOKEN_KEY = "UsernameToken Username"
    private const val NONCE_KEY = "Nonce"
    private const val CREATED_KEY = "Created"
    private const val PASSWORD_DIGEST_KEY = "PasswordDigest"
    private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    private fun generatePasswordDigest(nonce: ByteArray, created: ByteArray, password: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        digest.update(nonce)
        digest.update(created)
        digest.update(password)

        return Base64.getEncoder().encodeToString(digest.digest())
    }

    fun getAuthorizationHeader(customerToken: String, secretToken: String, now: Date = Date()): String {
        val randomBytes = SecureUtil.getCode().toString().toByteArray(Charsets.UTF_8)
        val nonceEncoded = Base64.getEncoder().encodeToString(randomBytes)
        // The 'Z' in DATE_FORMAT is a literal char, not a real offset directive — SimpleDateFormat
        // renders in the JVM/device's default timezone unless told otherwise, so without this the
        // server (which parses assuming true UTC) sees a bogus offset and rejects the nonce as
        // stale for any device outside UTC. See CLAUDE.md.
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val created = dateFormat.format(now)
        val passwordDigest = generatePasswordDigest(
            randomBytes,
            created.toByteArray(Charsets.UTF_8),
            secretToken.toByteArray(Charsets.UTF_8)
        )

        return "$CUSTOMER_TOKEN_KEY=\"$customerToken\", " +
            "$PASSWORD_DIGEST_KEY=\"$passwordDigest\", " +
            "$NONCE_KEY=\"$nonceEncoded\", " +
            "$CREATED_KEY=\"$created\""
    }
}
