package com.fuerz4.assistant.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

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
    fun `created field is formatted as an ISO-8601 UTC instant`() {
        val fixedDate = Date(1_700_000_000_000L)
        val expected = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(fixedDate)

        val header = WsseUtil.getAuthorizationHeader("key", "secret", fixedDate)
        val match = headerRegex.matchEntire(header)!!

        assertEquals(expected, match.groupValues[4])
    }
}
