package com.fuerz4.assistant.presentation.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Forces the hosting Activity to [orientation] for as long as this is composed, restoring the previous value on dispose. */
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = orientation
        onDispose {
            if (original != null) {
                activity?.requestedOrientation = original
            }
        }
    }
}
