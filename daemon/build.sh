#!/usr/bin/env bash
# Build ort_engine for aarch64-android (Snapdragon 8 Elite / Hexagon HTP v75)
#
# Prerequisites:
#   - Android NDK r26+ (ANDROID_NDK env var)
#   - ONNX Runtime Android AAR extracted (ORT_ROOT env var)
#     Download: https://mvnrepository.com/artifact/com.microsoft.onnxruntime/onnxruntime-android
#     Extract jni/arm64-v8a/ and headers/ from the AAR
#   - Optional: QNN SDK for direct EP config (QNN_SDK env var)
#
# Usage:
#   export ANDROID_NDK=/path/to/android-ndk
#   export ORT_ROOT=/path/to/onnxruntime-android
#   ./build.sh
#
# Output: build/ort_engine (aarch64 ELF binary)
#
# Deploy:
#   adb push build/ort_engine /data/data/com.horizons/files/ort_engine
#   adb shell chmod +x /data/data/com.horizons/files/ort_engine

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

: "${ANDROID_NDK:?Set ANDROID_NDK to your NDK path}"
: "${ORT_ROOT:?Set ORT_ROOT to extracted ONNX Runtime Android root}"

BUILD_DIR="$SCRIPT_DIR/build"
mkdir -p "$BUILD_DIR"

cmake -B "$BUILD_DIR" -S "$SCRIPT_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-31 \
    -DCMAKE_BUILD_TYPE=Release \
    -DORT_ROOT="$ORT_ROOT" \
    ${QNN_SDK:+-DQNN_SDK="$QNN_SDK"}

cmake --build "$BUILD_DIR" -j"$(nproc)"

echo ""
echo "Built: $BUILD_DIR/ort_engine"
ls -lh "$BUILD_DIR/ort_engine"
echo ""
echo "Deploy:"
echo "  adb push $BUILD_DIR/ort_engine /data/data/com.horizons/files/ort_engine"
echo "  adb shell chmod +x /data/data/com.horizons/files/ort_engine"
