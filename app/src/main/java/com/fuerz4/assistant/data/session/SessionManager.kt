package com.fuerz4.assistant.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the `LoginToken` (encrypted at rest) so the user isn't forced to re-login every launch. */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _loginToken = MutableStateFlow(readToken())
    val loginToken: StateFlow<String?> = _loginToken.asStateFlow()

    val isLoggedIn: Boolean get() = _loginToken.value != null

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        _loginToken.value = token
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
        _loginToken.value = null
    }

    private fun readToken(): String? = prefs.getString(KEY_TOKEN, null)

    private companion object {
        const val PREFS_NAME = "fuerz4_session"
        const val KEY_TOKEN = "login_token"
    }
}
