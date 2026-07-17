# media_daemon — Moonshine STT + Kokoro TTS over HTTP :8091 (sherpa-onnx C API).
#
# Cross-compile for arm64-android (mirrors daemon/'s ort_engine build):
#   SHERPA_ONNX_ROOT must point at a sherpa-onnx android install tree
#   (build-android-arm64-v8a/install — produced by sherpa-onnx's
#   build-android-arm64-v8a.sh with SHERPA_ONNX_ENABLE_C_API=ON,
#   SHERPA_ONNX_ENABLE_JNI=OFF, BUILD_SHARED_LIBS=ON), containing:
#     include/sherpa-onnx/c-api/c-api.h
#     lib/libsherpa-onnx-c-api.so
#     lib/libonnxruntime.so
#
#   cmake -S media-daemon -B media-daemon/build \
#     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
#     -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 \
#     -DCMAKE_BUILD_TYPE=Release -DSHERPA_ONNX_ROOT=/path/to/install
#
# Runtime layout on device (all via ModelImportActivity, same as ort_engine):
#   filesDir/media_daemon                (this binary)
#   filesDir/libsherpa-onnx-c-api.so     (runtime lib)
#   filesDir/libonnxruntime.so           (shared with ort_engine — same file)
# Launch: LD_LIBRARY_PATH=filesDir media_daemon --port 8091 \
#           --stt-dir <moonshine dir> --tts-dir <kokoro dir>

cmake_minimum_required(VERSION 3.13)
project(media_daemon CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT DEFINED SHERPA_ONNX_ROOT)
  message(FATAL_ERROR "Pass -DSHERPA_ONNX_ROOT=<sherpa-onnx android install dir>")
endif()

add_executable(media_daemon src/main.cpp)

target_include_directories(media_daemon PRIVATE ${SHERPA_ONNX_ROOT}/include)
target_link_directories(media_daemon PRIVATE ${SHERPA_ONNX_ROOT}/lib)
target_link_libraries(media_daemon sherpa-onnx-c-api)

# The binary is launched with LD_LIBRARY_PATH=filesDir; don't bake build paths.
set_target_properties(media_daemon PROPERTIES
  BUILD_WITH_INSTALL_RPATH TRUE
  INSTALL_RPATH ""
)
