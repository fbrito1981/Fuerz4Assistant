package com.fuerz4.assistant.data.crypto

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

@Serializable
private data class Sample(val email: String, val count: Int)

class SecureUtilTest {

    private fun fixedCalendar(): Calendar = GregorianCalendar(2026, Calendar.MARCH, 15, 10, 30)

    @Test
    fun `encrypt then decrypt returns the original ASCII string`() {
        val original = "hola@fuerz4.com:secret123"

        val encrypted = SecureUtil.encrypt(original, fixedCalendar())
        val decrypted = SecureUtil.decrypt(encrypted)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt then decrypt returns the original unicode string`() {
        val original = "ñañé áéíóú che fuerza!"

        val encrypted = SecureUtil.encrypt(original, fixedCalendar())
        val decrypted = SecureUtil.decrypt(encrypted)

        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypted output differs from the original plaintext`() {
        val original = "no deberia verse en claro"

        val encrypted = SecureUtil.encrypt(original, fixedCalendar())

        assertNotEquals(original, encrypted)
    }

    @Test
    fun `reified encrypt and decrypt round trips a serializable object`() {
        val original = Sample(email = "user@fuerz4.com", count = 42)

        val encrypted = SecureUtil.encrypt(original, fixedCalendar())
        val decrypted = SecureUtil.decrypt<Sample>(encrypted)

        assertEquals(original, decrypted)
    }

    @Test
    fun `pickKeyIndex is deterministic for a fixed clock`() {
        val first = SecureUtil.pickKeyIndex(fixedCalendar())
        val second = SecureUtil.pickKeyIndex(fixedCalendar())

        assertEquals(first, second)
    }

    @Test
    fun `pickKeyIndex stays within the valid key range across many clock values`() {
        for (minute in 0..59 step 7) {
            for (hour in 0..23 step 5) {
                val calendar = GregorianCalendar(2026, Calendar.JANUARY, 1, hour, minute)
                val index = SecureUtil.pickKeyIndex(calendar)

                assert(index in 0..8) { "index $index out of range for hour=$hour minute=$minute" }
            }
        }
    }
}
