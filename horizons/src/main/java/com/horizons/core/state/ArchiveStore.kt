package com.horizons.core.state

import android.content.Context
import java.io.File

/**
 * The Archives file store — a real directory tree under filesDir/archive.
 *
 * Anything the app "pushes to artifacts" lands here as an actual file the
 * user named, inside folders they control: saved harnesses, exported
 * terminal scripts, logs, configs. ArtifactsPane renders this as a file
 * manager (browse / new folder / new file / view / delete).
 */
class ArchiveStore(context: Context) {

    val root: File = File(context.filesDir, "archive").apply { mkdirs() }

    /** A relative path like "" (root), "terminal", "harnesses/voice". */
    private fun dir(relPath: String): File =
        if (relPath.isBlank()) root else File(root, relPath)

    data class Entry(
        val name: String,
        val isFolder: Boolean,
        val sizeBytes: Long,
        val modifiedAt: Long,
    )

    fun list(relPath: String = ""): List<Entry> {
        val d = dir(relPath)
        if (!d.exists() || !d.isDirectory) return emptyList()
        return d.listFiles().orEmpty()
            .map { f ->
                Entry(
                    name = f.name,
                    isFolder = f.isDirectory,
                    sizeBytes = if (f.isFile) f.length() else 0L,
                    modifiedAt = f.lastModified(),
                )
            }
            .sortedWith(compareByDescending<Entry> { it.isFolder }.thenBy { it.name.lowercase() })
    }

    fun mkdir(relPath: String, name: String): Boolean {
        val safe = sanitize(name) ?: return false
        return File(dir(relPath), safe).mkdirs()
    }

    fun writeText(relPath: String, name: String, content: String): Boolean {
        val safe = sanitize(name) ?: return false
        return try {
            val d = dir(relPath).apply { mkdirs() }
            File(d, safe).writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readText(relPath: String, name: String): String? = try {
        val f = File(dir(relPath), name)
        if (f.isFile) f.readText() else null
    } catch (_: Exception) {
        null
    }

    fun delete(relPath: String, name: String): Boolean {
        val f = File(dir(relPath), name)
        return if (f.isDirectory) f.deleteRecursively() else f.delete()
    }

    fun rename(relPath: String, from: String, to: String): Boolean {
        val safe = sanitize(to) ?: return false
        return File(dir(relPath), from).renameTo(File(dir(relPath), safe))
    }

    /** Reject path traversal and blank names; keep everything inside root. */
    private fun sanitize(name: String): String? {
        val n = name.trim()
        if (n.isEmpty() || n == "." || n == ".." || n.contains('/') || n.contains('\\')) return null
        return n
    }
}
