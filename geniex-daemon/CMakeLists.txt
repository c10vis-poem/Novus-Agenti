# geniex_daemon — GenieX LLM runtime behind the OpenAI wire on :18181.
#
# GENIEX_ROOT must point at a built GenieX SDK tree containing:
#   include/geniex.h            (from qualcomm/GenieX sdk/include, or an install)
#   lib/libgeniex.so            (the SDK bridge lib, arm64-v8a)
# Plugins (libgeniex_plugin_llama_cpp.so / ..._qairt.so + their backend libs)
# are dlopen'd at runtime from GENIEX_PLUGIN_PATH — not linked here.
#
# Cross-compile (mirrors media-daemon/):
#   cmake -S geniex-daemon -B geniex-daemon/build \
#     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
#     -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 \
#     -DCMAKE_BUILD_TYPE=Release -DGENIEX_ROOT=/path/to/geniex-install
#
# Device layout (via ModelImportActivity):
#   filesDir/geniex_daemon            this binary
#   filesDir/libgeniex.so             SDK bridge
#   filesDir/geniex-plugins/          plugins + backend libs (GENIEX_PLUGIN_PATH)
# Launch: LD_LIBRARY_PATH=filesDir geniex_daemon --port 18181 \
#           --model <gguf-or-bundle> --plugin llama_cpp --device npu \
#           --plugin-dir filesDir/geniex-plugins

cmake_minimum_required(VERSION 3.13)
project(geniex_daemon CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT DEFINED GENIEX_ROOT)
  message(FATAL_ERROR "Pass -DGENIEX_ROOT=<GenieX SDK build/install dir>")
endif()

add_executable(geniex_daemon src/main.cpp)

target_include_directories(geniex_daemon PRIVATE ${GENIEX_ROOT}/include)
target_link_directories(geniex_daemon PRIVATE ${GENIEX_ROOT}/lib)
target_link_libraries(geniex_daemon geniex)

set_target_properties(geniex_daemon PROPERTIES
  BUILD_WITH_INSTALL_RPATH TRUE
  INSTALL_RPATH ""
)
