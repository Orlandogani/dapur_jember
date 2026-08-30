package com.leanecorps.dapurjember.core.designsystem.component

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Blocks screenshots and keeps this screen out of the recents thumbnail while it is shown
 * (arch §7). Used on the screens that put money and takings on display — a POS is often
 * mounted in view of the dining room.
 *
 * The flag is cleared again on dispose, so it never leaks to the rest of the app.
 */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
