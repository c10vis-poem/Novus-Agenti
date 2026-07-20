package com.horizons.core.voice

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

sealed class KokoroSetupState {
    object Idle : KokoroSetupState()
    object Extracting : KokoroSetupState()
    object Ready : KokoroSetupState()
    data class Error(val message: String) : KokoroSetupState()
}

/**
 * Locates the Kokoro multi-lang v1.0 TTS model on device. NEVER downloads —
 * "the user is the loader": the model archive (kokoro-multi-lang-v1_0.tar.bz2)
 * is imported from device storage via ModelImportActivity → [importArchive],
 * which extracts it under filesDir. After that, [ensureReady] finds it on
 * every boot with zero network.
 */
class KokoroModelManager(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<KokoroSetupState>(KokoroSetupState.Idle)
    val state: StateFlow<KokoroSetupState> = _state.asStateFlow()

    val modelDir: String
        get() = File(context.filesDir, "sherpa_tts/kokoro-multi-lang-v1_0").absolutePath

    /** Check disk for an already-imported model. No side effects, no network. */
    fun ensureReady() {
        val cur = _state.value
        if (cur is KokoroSetupState.Ready || cur is KokoroSetupState.Extracting) return
        scope.launch(Dispatchers.IO) {
            _state.value = if (isComplete()) KokoroSetupState.Ready
            else KokoroSetupState.Error("Kokoro voice model not imported — open the archive from Files/Downloads to import it")
        }
    }

    private fun isComplete(): Boolean {
        val dir = File(modelDir)
        return dir.isDirectory
            && File(dir, "model.onnx").exists()
            && File(dir, "voices.bin").exists()
            && File(dir, "tokens.txt").exists()
            && File(dir, "espeak-ng-data").isDirectory
    }

    /**
     * Extract a user-picked kokoro .tar.bz2 archive (SAF/content Uri) from
     * device storage into filesDir. Called by ModelImportActivity. Blocking —
     * call from an IO dispatcher. Returns true when the model is complete.
     */
    fun importArchive(uri: Uri): Boolean {
        return try {
            _state.value = KokoroSetupState.Extracting
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open archive stream")
            input.use { extractTarBz2(it, File(context.filesDir, "sherpa_tts")) }
            if (isComplete()) {
                Log.i(TAG, "Kokoro model imported to $modelDir")
                _state.value = KokoroSetupState.Ready
                true
            } else {
                _state.value = KokoroSetupState.Error("Archive extracted but expected files missing")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Kokoro import failed", e)
            _state.value = KokoroSetupState.Error(e.message ?: "Unknown error")
            false
        }
    }

    companion object {
        const val TAG = "KokoroModelManager"

        /** Shared tar.bz2 extractor — also used for the Moonshine STT archive. */
        fun extractTarBz2(src: InputStream, destDir: File) {
            destDir.mkdirs()
            BZip2CompressorInputStream(src.buffered()).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val dest = File(destDir, entry.name)
                        // Guard against path traversal in archive entries.
                        if (!dest.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                            throw IOException("Illegal archive entry path: ${entry.name}")
                        }
                        if (entry.isDirectory) dest.mkdirs()
                        else { dest.parentFile?.mkdirs(); dest.outputStream().use { tar.copyTo(it) } }
                        entry = tar.nextEntry
                    }
                }
            }
        }
    }
}
