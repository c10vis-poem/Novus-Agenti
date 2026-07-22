package com.horizons.uilocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.horizons.ui.theme.HorizonsColors

/**
 * Novus Agenti — Local UI (session 16, first fork on the new architecture).
 *
 * Deliberately boots straight to [LocalHomeScreen] with no gate on the model
 * daemon: loading/compiling the on-device model is a follow-up step once it's
 * verified on real Hexagon HTP hardware, not a boot requirement here. This
 * mirrors the daemon-side "serve-first" fix (CliffordService/main.cpp) at the
 * UI layer — the app is usable the instant it launches, and the two daemon
 * status rows (model+vision, media) fill in independently as they come up.
 *
 * This activity is additive — MainActivity/HomeGrid are untouched. It exists
 * to iterate on the daemon architecture (model+vision daemon via NpuClient;
 * voice runs in-process — Moonshine STT + Kokoro TTS) without disturbing the working UI.
 */
class LocalHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = HorizonsColors.Background,
                surface = HorizonsColors.Surface,
                primary = HorizonsColors.PrimaryTeal,
                onBackground = Color.White,
                onSurface = Color.White,
                onPrimary = Color.Black,
            )) {
                Surface(modifier = Modifier.fillMaxSize().imePadding()) {
                    LocalHomeScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
