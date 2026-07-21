package com.horizons.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Per-panel uploadable wallpapers ─────────────────────────────────────────
//
// The user picks a photo; it is copied into app-internal storage and its path
// persisted under the panel's key (AppStateStore.KEY_WALLPAPER_*). Copying
// (rather than holding a content:// URI) means the wallpaper survives app
// restarts with no persistable-permission bookkeeping.
//
// When a wallpaper is set it FULLY REPLACES the panel's procedural background;
// a scrim sits on top so overlaid tiles/content stay legible. When none is set
// the panel falls back to its procedural Canvas background.

class WallpaperController internal constructor(
    val isSet: Boolean,
    val launchPicker: () -> Unit,
    val clear: () -> Unit,
)

private fun wallpaperFile(app: HorizonsApplication, wallpaperKey: String): File {
    val dir = File(app.filesDir, "wallpapers").apply { mkdirs() }
    return File(dir, wallpaperKey.substringAfterLast('.') + ".img")
}

@Composable
fun rememberWallpaperController(wallpaperKey: String): WallpaperController {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val snapshot by app.appState.snapshot.collectAsState()
    val currentPath = snapshot[wallpaperKey]

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val dest = wallpaperFile(app, wallpaperKey)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
            app.appState.put(wallpaperKey, dest.absolutePath)
        } catch (_: Exception) {
            // Bad pick / unreadable stream — leave the existing background as-is.
        }
    }

    return WallpaperController(
        isSet = !currentPath.isNullOrBlank(),
        launchPicker = { picker.launch("image/*") },
        clear = {
            currentPath?.let { runCatching { File(it).delete() } }
            app.appState.remove(wallpaperKey)
        },
    )
}

/**
 * Header control: a picture button to set/replace the panel's wallpaper, and
 * an x to clear it (shown only when one is set). Call inside a Row; [tint]
 * should be the panel's accent color.
 */
@Composable
fun WallpaperPickerButtons(wallpaperKey: String, tint: Color) {
    val controller = rememberWallpaperController(wallpaperKey)
    IconButton(onClick = { controller.launchPicker() }) {
        Text("🖼", fontSize = 15.sp, color = tint)
    }
    if (controller.isSet) {
        IconButton(onClick = { controller.clear() }) {
            Text("✕", fontSize = 15.sp, color = tint.copy(alpha = 0.7f))
        }
    }
}

/**
 * Renders [wallpaperKey]'s uploaded image full-bleed with a legibility scrim,
 * or [procedural] when no wallpaper is set (or the file can't be read).
 */
@Composable
fun PanelWallpaperBackground(
    wallpaperKey: String,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.40f,
    procedural: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val snapshot by app.appState.snapshot.collectAsState()
    val path = snapshot[wallpaperKey]

    if (path.isNullOrBlank()) {
        procedural()
        return
    }

    var image by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        image = withContext(Dispatchers.IO) {
            try {
                val f = File(path)
                if (f.exists() && f.canRead()) {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val img = image
    if (img == null) {
        // Path set but not yet decoded / unreadable — never show blank.
        procedural()
    } else {
        Box(modifier.fillMaxSize()) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }
    }
}
