package com.horizons.core.shell

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File

/**
 * Copies runtime binaries/libs straight from the public Downloads folder
 * into the app's private filesDir, with zero user interaction — no "Open
 * with → Horizons" share-sheet step.
 *
 * That flow existed as the only import path through session 17, but the
 * operator never used it (didn't know it was required, or it just never
 * came up naturally) — files sat in Downloads, visibly downloaded, while
 * the app kept reporting "no backend" because nothing had actually been
 * copied into filesDir (the only place Android will let this app *execute*
 * a binary from — external storage is mounted noexec). Since the app
 * already holds MANAGE_EXTERNAL_STORAGE (granted, confirmed in Settings),
 * there's no reason to route this through Android's Intent/ContentResolver
 * machinery at all — plain java.io.File access to /Download works, and
 * this can just run automatically.
 *
 * Called from CliffordService's CRS tick. Cheap when there's nothing new:
 * every target is skipped once its filesDir copy already exists.
 */
object AutoImport {

    private const val TAG = "AutoImport"
    private val DOWNLOAD_DIR = File("/storage/emulated/0/Download")

    fun sync(context: Context) {
        if (!DOWNLOAD_DIR.isDirectory) return
        val downloadFiles = DOWNLOAD_DIR.listFiles() ?: return

        // Runtime binaries + shared libs (flat, filesDir root).
        for (canonical in RuntimeFiles.RUNTIME_FILES) {
            val dest = File(context.filesDir, canonical)
            if (dest.exists()) continue
            val match = downloadFiles.firstOrNull {
                it.isFile && RuntimeFiles.isRuntimeFile(it.name) &&
                    RuntimeFiles.canonicalRuntimeName(it.name) == canonical
            } ?: continue
            runCatching {
                match.copyTo(dest, overwrite = true)
                if (canonical in RuntimeFiles.EXECUTABLE_RUNTIMES) dest.setExecutable(true, true)
                Log.i(TAG, "Auto-imported ${match.name} -> ${dest.absolutePath}")
            }.onFailure { Log.w(TAG, "Auto-import failed for ${match.name}: ${it.message}") }
        }

        // media_daemon's own ONNX Runtime (version-pinned differently from
        // ort_engine's — see RuntimeFiles.MEDIA_ONNXRUNTIME_ARTIFACT) goes in
        // its own subdirectory under its REAL filename, never filesDir root.
        val mediaLibsDir = File(context.filesDir, RuntimeFiles.MEDIA_LIBS_SUBDIR).apply { mkdirs() }
        val mediaOrtDest = File(mediaLibsDir, "libonnxruntime.so")
        if (!mediaOrtDest.exists()) {
            val match = downloadFiles.firstOrNull {
                it.isFile && it.name.lowercase().let { n ->
                    n.startsWith("libonnxruntime-media") && n.endsWith(".so")
                }
            }
            if (match != null) {
                runCatching {
                    match.copyTo(mediaOrtDest, overwrite = true)
                    Log.i(TAG, "Auto-imported ${match.name} -> ${mediaOrtDest.absolutePath}")
                }.onFailure { Log.w(TAG, "Auto-import failed for ${match.name}: ${it.message}") }
            }
        }

        // GenieX plugins archive — extracted once, not re-checked every tick
        // beyond the directory-exists guard (cheap stat, no work if present).
        val pluginsDir = File(File(context.filesDir, "geniex-plugins"), "plugins")
        if (!pluginsDir.isDirectory) {
            val archive = downloadFiles.firstOrNull {
                it.isFile && RuntimeFiles.isGenieXPluginsArchive(it.name)
            }
            if (archive != null) extractTarGz(archive, File(context.filesDir, "geniex-plugins"))
        }
    }

    private fun extractTarGz(archive: File, destRoot: File) {
        runCatching {
            destRoot.mkdirs()
            var count = 0
            archive.inputStream().use { raw ->
                GzipCompressorInputStream(raw).use { gz ->
                    TarArchiveInputStream(gz).use { tar ->
                        var entry = tar.nextEntry
                        while (entry != null) {
                            val out = File(destRoot, entry.name)
                            if (entry.isDirectory) {
                                out.mkdirs()
                            } else {
                                out.parentFile?.mkdirs()
                                out.outputStream().use { dst -> tar.copyTo(dst) }
                                count++
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
            Log.i(TAG, "Auto-extracted $count GenieX plugin files from ${archive.name} -> ${destRoot.absolutePath}")
        }.onFailure { Log.w(TAG, "GenieX plugin auto-extract failed: ${it.message}") }
    }
}
