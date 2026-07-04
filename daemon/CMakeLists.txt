cmake_minimum_required(VERSION 3.22)
project(ort_engine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---------- NDK / cross-compile defaults ----------
# Build with:
#   cmake -B build \
#     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
#     -DANDROID_ABI=arm64-v8a \
#     -DANDROID_PLATFORM=android-31 \
#     -DORT_ROOT=/path/to/onnxruntime-android \
#     -DQNN_SDK=/path/to/qnn-sdk
#   cmake --build build

# ---------- ONNX Runtime ----------
set(ORT_ROOT "" CACHE PATH "ONNX Runtime Android pre-built root")
if(ORT_ROOT)
    set(ORT_INCLUDE ${ORT_ROOT}/headers)
    set(ORT_LIB_DIR ${ORT_ROOT}/jni/${ANDROID_ABI})
    include_directories(${ORT_INCLUDE})
    link_directories(${ORT_LIB_DIR})
endif()

# ---------- QNN SDK (optional — for direct QNN EP registration) ----------
set(QNN_SDK "" CACHE PATH "Qualcomm QNN SDK root")
if(QNN_SDK)
    include_directories(${QNN_SDK}/include)
    link_directories(${QNN_SDK}/lib/aarch64-android)
endif()

# ---------- Sources ----------
add_executable(ort_engine
    src/main.cpp
    src/http_server.cpp
    src/engine.cpp
    src/tokenizer.cpp
    src/sampler.cpp
)

target_include_directories(ort_engine PRIVATE src)

# ---------- Link ----------
target_link_libraries(ort_engine PRIVATE
    onnxruntime
    log
    z
)

# Strip for release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_custom_command(TARGET ort_engine POST_BUILD
        COMMAND ${CMAKE_STRIP} $<TARGET_FILE:ort_engine>)
endif()
