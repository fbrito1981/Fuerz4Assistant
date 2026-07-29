package com.fuerz4.assistant.presentation.common

import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.remote.NanoApiError

/** Maps NanoServer's ResponseType-equivalent codes (sent as the `message` field) to localized copy. */
fun NanoApiError.toUiText(): UiText = when (this) {
    is NanoApiError.Network -> UiText.Resource(R.string.common_offline_message)
    is NanoApiError.Unknown -> UiText.Resource(R.string.common_error_general)
    is NanoApiError.ServerCode -> when (code) {
        "401" -> UiText.Resource(R.string.error_unauthorized)
        "500" -> UiText.Resource(R.string.common_error_general)
        "800" -> UiText.Resource(R.string.error_invalid_credentials)
        "801" -> UiText.Resource(R.string.error_inactive_user)
        "802" -> UiText.Resource(R.string.error_invalid_recovery_code)
        "803" -> UiText.Resource(R.string.error_expired_recovery_code)
        "804" -> UiText.Resource(R.string.error_invalid_user)
        "805" -> UiText.Resource(R.string.error_invalid_device)
        "806" -> UiText.Resource(R.string.error_invalid_params)
        "807" -> UiText.Resource(R.string.error_duplicate_device)
        else -> UiText.Resource(R.string.common_error_general)
    }
}
