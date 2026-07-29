package com.fuerz4.assistant.data.crypto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * Kotlin port of NanoServer's `SecureUtils.java` (and tsm_android's `SecureUtil.kt`).
 * Byte-for-byte compatible XOR cipher with a date-rotated key — NOT real cryptography,
 * it only needs to match the server's reversible scheme exactly.
 */
object SecureUtil {
    private val keys = arrayOf(
        "ZrYd+p^kg4jpeKSb", "-hZJSdUtUJ2Ys_fK", "MT?=Lt&p!P!bBG5^", "\$sXjYC6vvU5NEF-@",
        "mz5FUuA8%hqYPj6*", "WHsHDQsthCTrw!9t", "WLma58Jbq?cp5gXd", "Vb66E+t2DDsE&TnB", "cb\$54xtc^WLA\$zg"
    )
    private const val MIN_RANDOM_NUMBER = 10000000
    private const val MAX_RANDOM_NUMBER = 99999999

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /** Exposed with a default arg (rather than calling `Calendar.getInstance()` internally) so tests can pin the clock. */
    internal fun pickKeyIndex(calendar: Calendar = Calendar.getInstance()): Int {
        var date = "${calendar.get(Calendar.YEAR)}"
        date += "${calendar.get(Calendar.MONTH) + 1}"
        date += "${calendar.get(Calendar.DAY_OF_MONTH)}"
        date += "${calendar.get(Calendar.HOUR_OF_DAY)}"
        date += "${calendar.get(Calendar.MINUTE)}"

        while (date.length > 1) {
            var total = 0
            for (element in date) {
                total += "$element".toInt()
            }
            date = total.toString()
        }

        return 9 - date.toInt()
    }

    fun encrypt(value: String, calendar: Calendar = Calendar.getInstance()): String {
        val keyIndex = pickKeyIndex(calendar)
        val key = keys[keyIndex]
        var k = 0
        val encrypted = IntArray(value.length)
        for (i in value.indices) {
            if (k == key.length) {
                k = 0
            }
            encrypted[i] = value[i].code xor key[k].code
            k++
        }

        val reversed = encrypted.reversedArray()
        var result = ""
        for (element in reversed) {
            if (result.isNotEmpty()) {
                result += "${key[1]}"
            }
            result += "$element"
        }

        return "$keyIndex$result"
    }

    fun decrypt(value: String): String {
        var decrypted = ""
        if (value.isNotEmpty()) {
            var k = 0
            val keyIndex = "${value[0]}".toInt()
            val key = keys[keyIndex]
            val realValue = value.substring(1)
            val valueParts = realValue.split(key[1])
            val encrypted = IntArray(valueParts.size) { valueParts[it].toInt() }

            val reversed = encrypted.reversedArray()
            for (element in reversed) {
                if (k == key.length) {
                    k = 0
                }
                decrypted += (element xor key[k].code).toChar()
                k++
            }
        }

        return decrypted
    }

    inline fun <reified T> encrypt(entity: T, calendar: Calendar = Calendar.getInstance()): String {
        return encrypt(json.encodeToString(entity), calendar)
    }

    inline fun <reified T> decrypt(value: String): T {
        return json.decodeFromString(decrypt(value))
    }

    fun getCode(): Int {
        return ((Math.random() * ((MAX_RANDOM_NUMBER - MIN_RANDOM_NUMBER) + 1)) + MIN_RANDOM_NUMBER).toInt()
    }
}
