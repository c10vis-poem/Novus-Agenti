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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.horizons.ui.panels.ChatPane
import com.horizons.ui.panels.LibraryPane
import com.horizons.ui.panels.ModelsPane
import com.horizons.ui.panels.RouterPane
import com.horizons.ui.panels.SettingsPane
import com.horizons.ui.panels.TerminalPanel

enum class Panel { Chat, Router, Library, Diagnostics, Settings, Terminal }

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    /** Foreground-service mic + screen-share types hard-require RECORD_AUDIO granted,
     *  or startForeground() throws SecurityException and crashes the app. Request the
     *  runtime-dangerous permissions up front. */
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
        handleModelFileIntent(intent)
    }

    private fun handleModelFileIntent(intent: Intent?) {
        // Model file intent handling removed -- ort_engine daemon loads
        // qnn_context_binary from a known path, no user file-picking needed.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestRuntimePermissions()
        if (!Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        handleModelFileIntent(intent)
        val initialTab = when (intent?.getStringExtra(EXTRA_LAUNCH_TAB)) {
            TAB_TERMINAL -> Panel.Terminal
            else -> Panel.Chat
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize().imePadding()) {
                    var selectedPanel by remember { mutableStateOf(initialTab) }
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                Panel.entries.forEach { panel ->
                                    NavigationBarItem(
                                        selected = selectedPanel == panel,
                                        onClick = { selectedPanel = panel },
                                        label = { Text(panel.name) },
                                        icon = {},
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        when (selectedPanel) {
                            Panel.Chat        -> ChatPane(modifier = Modifier.fillMaxSize().padding(padding))
                            Panel.Router      -> ModelsPane(modifier = Modifier.fillMaxSize().padding(padding))
                            Panel.Library     -> LibraryPane(modifier = Modifier.fillMaxSize().padding(padding))
                            Panel.Diagnostics -> RouterPane(modifier = Modifier.fillMaxSize().padding(padding))
                            Panel.Settings    -> SettingsPane(modifier = Modifier.fillMaxSize().padding(padding))
                            Panel.Terminal    -> TerminalPanel(modifier = Modifier.fillMaxSize().padding(padding))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_LAUNCH_TAB = "launch_tab"
        const val TAB_TERMINAL = "terminal"
    }
}
