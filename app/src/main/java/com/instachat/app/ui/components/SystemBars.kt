package com.instachat.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.instachat.app.ui.theme.LocalAppDarkTheme
import com.instachat.app.util.findActivity

/**
 * Applies status/navigation bar icon contrast for the current screen. Chat
 * screens can override using their per-conversation theme brightness.
 */
@Composable
fun AppSystemBars(darkTheme: Boolean = LocalAppDarkTheme.current) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }
    SideEffect {
        if (!view.isInEditMode) {
            val window = activity?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
