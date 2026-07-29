package com.fuerz4.assistant.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WsseUtilTest {

    private val headerRegex = Regex(
        "^UsernameToken Username=\"([^\"]*)\", " +
            "PasswordDigest=\"([^\"]*)\", " +
            "Nonce=\"([^\"]*)\", " +
            "Created=\"([^\"]*)\"$"
    )

    @Test
    fun `header matches the expected WSSE shape`() {
        val header = WsseUtil.getAuthorizationHeader("myKey", "mySecret")

        val match = headerRegex.matchEntire(header)
        assertTrue("header did not match expected shape: $header", match != null)
        assertEquals("myKey", match!!.groupValues[1])
    }

    @Test
    fun `password digest matches a manually computed SHA-1 digest for a fixed instant`() {
        val fixedDate = Date(0L)

        val header = WsseUtil.getAuthorizationHeader("key", "secret", fixedDate)
        val match = headerRegex.matchEntire(header)!!

        val passwordDigest = match.groupValues[2]
        val nonceEncoded = match.groupValues[3]
        val created = match.groupValues[4]

        val nonceBytes = Base64.getDecoder().decode(nonceEncoded)
        val expectedDigest = MessageDigest.getInstance("SHA-1").run {
            update(nonceBytes)
            update(created.toByteArray(Charsets.UTF_8))
            update("secret".toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(digest())
        }

        assertEquals(expectedDigest, passwordDigest)
    }

    @Test
    fun `created field is always rendered in UTC regardless of the device's default timezone`() {
        // Fixed instant with a known UTC wall-clock time, verified against a UTC-anchored
        // formatter — this must hold no matter what timezone the JVM/device defaults to,
        // otherwise the server rejects the nonce as stale for any non-UTC client.
        val fixedDate = Date(1_700_000_000_000L)
        val expectedUtcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val expected = expectedUtcFormat.format(fixedDate)
        assertEquals("2023-11-14T22:13:20Z", expected)

        val header = WsseUtil.getAuthorizationHeader("key", "secret", fixedDate)
        val match = headerRegex.matchEntire(header)!!

        assertEquals(expected, match.groupValues[4])
    }

    @Test
    fun `created field does not shift when the default timezone is not UTC`() {
        val originalDefault = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
            val fixedDate = Date(1_700_000_000_000L)

            val header = WsseUtil.getAuthorizationHeader("key", "secret", fixedDate)
            val match = headerRegex.matchEntire(header)!!

            assertEquals("2023-11-14T22:13:20Z", match.groupValues[4])
        } finally {
            TimeZone.setDefault(originalDefault)
        }
    }
}
