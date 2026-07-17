package com.horizons.core.shell

/**
 * Canonical names/classification for every model + runtime file the app
 * consumes, shared between [com.horizons.ModelImportActivity] (manual
 * Open-With import) and [AutoImport] (automatic Downloads scan — the
 * primary path; operator feedback session 17: "Open with" was never a
 * step they took, so nothing can depend on it alone).
 */
object RuntimeFiles {

    val MODEL_EXTENSIONS = listOf(
        ".serialized.bin",
        ".bin",
        ".onnx",
        ".gguf",
        ".tflite",
        ".dlc",
        ".pte",
        ".qnn",
    )

    /** Media (STT/TTS) daemon binary — CI build output, serves 127.0.0.1:8091. */
    const val MEDIA_DAEMON_BINARY = "media_daemon"

    /** GenieX model daemon binary — CI build output, serves 127.0.0.1:18181/v1. */
    const val GENIEX_DAEMON_BINARY = "geniex_daemon"

    /** Runtime binaries that must be chmod +x after import. */
    val EXECUTABLE_RUNTIMES = setOf(
        DaemonLauncher.ENGINE_BINARY, // "ort_engine"
        MEDIA_DAEMON_BINARY,
        GENIEX_DAEMON_BINARY,
        "geniex",
    )

    /**
     * sherpa-onnx (media_daemon) pins its own ONNX Runtime build (1.24.3, a
     * community mirror) — a DIFFERENT version from ort_engine's official
     * 1.22.0. Both are natively named libonnxruntime.so, so they cannot live
     * in the same directory: whichever imports second would silently
     * overwrite the other, and media_daemon could load the wrong version's
     * ABI. The CI artifact is published under this distinct name and
     * AutoImport routes it into its own subdirectory (see MEDIA_LIBS_SUBDIR)
     * with its real expected filename restored — never into filesDir root.
     */
    const val MEDIA_ONNXRUNTIME_ARTIFACT = "libonnxruntime-media.so"
    const val MEDIA_LIBS_SUBDIR = "media-libs"

    // Native daemon runtime components — CI build outputs from build-apk.yml.
    // libonnxruntime-media.so is intentionally NOT in this set — it needs the
    // MEDIA_LIBS_SUBDIR special case below, not a flat filesDir-root import.
    val RUNTIME_FILES = setOf(
        DaemonLauncher.ENGINE_BINARY, // "ort_engine"
        MEDIA_DAEMON_BINARY,
        GENIEX_DAEMON_BINARY,
        "geniex",
        "libonnxruntime.so",
        "libgeniex.so",
        "libsherpa-onnx-c-api.so",
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnHtpV79Skel.so",
    )

    fun isModelFile(name: String): Boolean {
        val lower = name.lowercase()
        return MODEL_EXTENSIONS.any { lower.endsWith(it) }
    }

    /** Tolerant match: handles download-dedupe suffixes ("ort_engine (1)"), versioned QNN libs, case variations. */
    fun isRuntimeFile(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.startsWith("ort_engine")) return true
        if (lower.startsWith("media_daemon")) return true
        if (lower.startsWith("geniex")) return true
        if (lower.startsWith("libgeniex") && lower.endsWith(".so")) return true
        if (lower.startsWith("libonnxruntime") && lower.endsWith(".so")) return true
        if (lower.startsWith("libsherpa-onnx") && lower.endsWith(".so")) return true
        if (lower.startsWith("libqnn") && lower.endsWith(".so")) return true
        return false
    }

    fun isGenieXPluginsArchive(name: String): Boolean =
        name.lowercase().let { it.startsWith("geniex-plugins") && it.endsWith(".tar.gz") }

    /** Canonical filename to write to disk — strip Android's "(1)" download suffixes. */
    fun canonicalRuntimeName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.startsWith("ort_engine") -> DaemonLauncher.ENGINE_BINARY
            lower.startsWith("media_daemon") -> MEDIA_DAEMON_BINARY
            lower.startsWith("geniex_daemon") -> GENIEX_DAEMON_BINARY
            lower.startsWith("libgeniex") -> "libgeniex.so"
            lower.startsWith("geniex") -> "geniex"
            // MUST come before the generic "libonnxruntime" branch below.
            lower.startsWith("libonnxruntime-media") -> MEDIA_ONNXRUNTIME_ARTIFACT
            lower.startsWith("libonnxruntime") -> "libonnxruntime.so"
            lower.startsWith("libsherpa-onnx-c-api") -> "libsherpa-onnx-c-api.so"
            lower.startsWith("libqnnhtpv79skel") -> "libQnnHtpV79Skel.so"
            lower.startsWith("libqnnhtp") -> "libQnnHtp.so"
            lower.startsWith("libqnnsystem") -> "libQnnSystem.so"
            else -> name
        }
    }
}
