package com.horizons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.horizons.core.shell.RuntimeFiles
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class ModelImportActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val statusText = mutableStateOf("Preparing import…")
    private val importing = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF222C34),
                surface = Color(0xFF35414A),
                primary = Color(0xFF2DD4D9),
            )) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (importing.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                        Text(
                            statusText.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return finishWithError("No intent received")

        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        if (uri == null) return finishWithError("No file URI in intent")

        val fileName = resolveFileName(uri)
        Log.i(TAG, "handleIntent: action=${intent.action} uri=$uri resolved fileName='$fileName'")
        when {
            RuntimeFiles.isGenieXPluginsArchive(fileName) -> {
                statusText.value = "Extracting GenieX plugins…"
                scope.launch { extractGenieXPlugins(uri) }
            }
            fileName.lowercase().let { it.startsWith("libonnxruntime-media") && it.endsWith(".so") } -> {
                // media_daemon's own ONNX Runtime — must NOT land in filesDir
                // root (would collide with ort_engine's differently-versioned
                // libonnxruntime.so). See RuntimeFiles.MEDIA_ONNXRUNTIME_ARTIFACT.
                statusText.value = "Installing media daemon's ONNX Runtime…"
                scope.launch {
                    importFile(
                        uri, "libonnxruntime.so",
                        destDir = File(filesDir, RuntimeFiles.MEDIA_LIBS_SUBDIR),
                        executable = false,
                        label = "Runtime component",
                    )
                }
            }
            RuntimeFiles.isModelFile(fileName) -> {
                statusText.value = "Importing $fileName…"
                scope.launch {
                    importFile(
                        uri, fileName,
                        destDir = File(filesDir, "models"),
                        executable = false,
                        label = "Model",
                    )
                }
            }
            RuntimeFiles.isRuntimeFile(fileName) -> {
                val canonical = RuntimeFiles.canonicalRuntimeName(fileName)
                statusText.value = "Installing $canonical…"
                scope.launch {
                    importFile(
                        uri, canonical,
                        destDir = filesDir,
                        executable = canonical in RuntimeFiles.EXECUTABLE_RUNTIMES,
                        label = "Runtime component",
                    )
                }
            }
            else -> finishWithError("Unsupported file type: $fileName")
        }
    }

    /**
     * Unpacks geniex-plugins-arm64.tar.gz (CI output — see build-apk.yml)
     * into filesDir/geniex-plugins/, preserving the tar's own layout
     * (one directory per plugin id, each holding its .so files) so it
     * matches GENIEX_PLUGIN_PATH's expected shape. geniex_daemon dlopen's
     * these from its own process — no chmod +x needed, .so files just
     * need to be readable.
     */
    private suspend fun extractGenieXPlugins(uri: Uri) {
        try {
            val count = withContext(Dispatchers.IO) {
                val destRoot = File(filesDir, "geniex-plugins").apply { mkdirs() }
                var n = 0
                contentResolver.openInputStream(uri)?.use { raw ->
                    org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(raw).use { gz ->
                        org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gz).use { tar ->
                            var entry = tar.nextEntry
                            while (entry != null) {
                                val out = File(destRoot, entry.name)
                                if (entry.isDirectory) {
                                    out.mkdirs()
                                } else {
                                    out.parentFile?.mkdirs()
                                    out.outputStream().use { dst -> tar.copyTo(dst) }
                                    n++
                                }
                                entry = tar.nextEntry
                            }
                        }
                    }
                } ?: throw IllegalStateException("Cannot open input stream")
                Log.i(TAG, "Extracted $n GenieX plugin files -> ${destRoot.absolutePath}")
                n
            }

            importing.value = false
            statusText.value = "GenieX plugins installed ($count files)"
            Toast.makeText(this@ModelImportActivity, "GenieX plugins installed", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@ModelImportActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "GenieX plugin extraction failed", e)
            finishWithError("Plugin extraction failed: ${e.message}")
        }
    }

    private suspend fun importFile(uri: Uri, fileName: String, destDir: File, executable: Boolean, label: String) {
        try {
            destDir.mkdirs()
            val dest = File(destDir, fileName)

            withContext(Dispatchers.IO) {
                val input: InputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream")
                input.use { src ->
                    dest.outputStream().use { out ->
                        val buf = ByteArray(256 * 1024)
                        var total = 0L
                        var read: Int
                        while (src.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            total += read
                            val mb = total / (1024 * 1024)
                            withContext(Dispatchers.Main) {
                                statusText.value = "Importing $fileName… ${mb} MB"
                            }
                        }
                    }
                }
                if (executable) dest.setExecutable(true, true)
            }

            Log.i(TAG, "Imported $fileName → ${dest.absolutePath} (${dest.length()} bytes)")
            importing.value = false
            statusText.value = "Imported $fileName (${dest.length() / (1024 * 1024)} MB)"
            Toast.makeText(this, "$label imported: $fileName", Toast.LENGTH_LONG).show()

            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            finishWithError("Import failed: ${e.message}")
        }
    }

    private fun resolveFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "unknown_model"
    }

    private fun finishWithError(msg: String) {
        Log.w(TAG, msg)
        importing.value = false
        statusText.value = msg
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "ModelImport"

        /** @deprecated use [RuntimeFiles.MODEL_EXTENSIONS] — kept for external references. */
        val MODEL_EXTENSIONS get() = RuntimeFiles.MODEL_EXTENSIONS
    }
}
