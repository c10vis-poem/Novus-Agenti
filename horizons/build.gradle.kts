plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val nexaEnabled: Boolean = (project.findProperty("nexaEnabled") as? String)?.toBoolean() ?: false

val gitSha: String = try {
    val proc = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    proc.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
} catch (e: Exception) {
    "unknown"
}

android {
    namespace = "com.horizons"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.horizons"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0-phase1"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
        // Razr Ultra is arm64-v8a only.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("release/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES")
        jniLibs {
            // Nexa SDK's plugin loader calls dlopen() with real file paths —
            // useLegacyPackaging=true ensures .so files are extracted at install time.
            useLegacyPackaging = true
            excludes += setOf(
                "lib/armeabi-v7a/**", "lib/armeabi/**",
                "lib/x86/**", "lib/x86_64/**",
                "lib/mips/**", "lib/mips64/**"
            )
            // ORT ships its own .so; deduplicate if Nexa AAR also pulls it in.
            pickFirst("**/libonnxruntime.so")
            pickFirst("**/libonnxruntime4j_jni.so")
            pickFirst("**/libsherpa-onnx-jni.so")
            pickFirst("**/libsherpa_onnx_jni.so")
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.documentfile)

    if (nexaEnabled) {
        implementation(libs.nexa.core)
    }

    // Silero VAD — lightweight ONNX signal-processing model, not STT/TTS.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")

    // Sherpa-ONNX — on-device TTS runtime for Kokoro multi-lang v1.0 (28 English voices).
    // AAR downloaded by CI (see build-apk.yml); excluded from git via horizons/libs/.gitignore.
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    // Apache Commons Compress — bzip2 extraction for the Kokoro model archive.
    implementation("org.apache.commons:commons-compress:1.27.1")
}
