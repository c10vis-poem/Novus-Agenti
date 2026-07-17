package com.horizons

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.horizons.ui.HomeGrid
import com.horizons.ui.panels.ArtifactsPane
import com.horizons.ui.panels.ChatPane
import com.horizons.ui.panels.HorizonsPane
import com.horizons.ui.panels.MonitorPane
import com.horizons.ui.panels.RouterPane
import com.horizons.ui.panels.SettingsPane
import com.horizons.ui.panels.TerminalPanel
import com.horizons.ui.theme.HorizonsColors

enum class Panel { Horizons, Monitor, Chat, Router, Artifacts, Terminal, Settings }

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private fun requestRuntimePermissions() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.horizons.core.diag.Breadcrumb.drop("MainActivity_onCreate_enter")
        super.onCreate(savedInstanceState)
        com.horizons.core.diag.Breadcrumb.drop("MainActivity_after_super")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestRuntimePermissions()
        com.horizons.core.diag.Breadcrumb.drop("MainActivity_runtime_perms_requested")
        // NOTE: previously auto-launched ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        // on every cold start, which kicked the user out to Settings before the UI
        // composed. SettingsPane now exposes that grant on demand instead.
        val launchDirectTo = when (intent?.getStringExtra(EXTRA_LAUNCH_TAB)) {
            TAB_TERMINAL -> Panel.Terminal
            TAB_CHAT -> Panel.Chat
            else -> null
        }
        com.horizons.core.diag.Breadcrumb.drop("MainActivity_before_setContent")
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = HorizonsColors.Background,
                surface = HorizonsColors.Surface,
                primary = HorizonsColors.PrimaryTeal,
                onBackground = Color.White,
                onSurface = Color.White,
                onPrimary = Color.Black,
            )) {
                // systemBarsPadding keeps content clear of the status bar (top
                // tabs/clock) and the gesture/nav bar (bottom input row) — the
                // app draws edge-to-edge (setDecorFitsSystemWindows(false) in
                // onCreate) but was never padding content away from those bars
                // (operator-reported: chat input buried behind the nav bar).
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                    var activePanel by remember { mutableStateOf(launchDirectTo) }

                    BackHandler(enabled = activePanel != null) {
                        activePanel = null
                    }

                    AnimatedContent(
                        targetState = activePanel,
                        transitionSpec = {
                            if (targetState != null) {
                                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 3 } + fadeOut())
                            } else {
                                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it / 3 } + fadeOut())
                            }
                        },
                        label = "panel_nav",
                    ) { panel ->
                        when (panel) {
                            null -> HomeGrid(
                                onTileClick = { activePanel = it },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Horizons  -> HorizonsPane(
                                onBack = { activePanel = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Monitor   -> MonitorPane(
                                onBack = { activePanel = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Chat      -> ChatPane(
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Router    -> RouterPane(
                                onBack = { activePanel = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Artifacts -> ArtifactsPane(
                                onBack = { activePanel = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Terminal  -> TerminalPanel(
                                onBack = { activePanel = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Panel.Settings  -> SettingsPane(
                                onBack = { activePanel = null },
                                onNavigate = { activePanel = it },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_LAUNCH_TAB = "launch_tab"
        const val TAB_TERMINAL = "terminal"
        const val TAB_CHAT = "chat"
    }
}
