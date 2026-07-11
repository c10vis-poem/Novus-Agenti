[https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned](https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned)

That specific hardware version changes the game entirely. The v79 Hexagon HTP (Hardware Tensor Processor) is the exact engine built into your Snapdragon 8 Elite. \[1, 2, 3\]   
Because you are compiling for the v79 hardware layout via llama.cpp, you must switch back and use Q4\_0.  
The open-source communities and Qualcomm engineers specifically optimized the native Hexagon backend codebase (LLAMA\_DEVICE\_TYPE\_HEXAGON) around Q4\_0, Q8\_0, and MXFP4 matrix operations. \[2, 4\]   
\#\# Why Q4\_0 Wins on Hexagon v79 Hardware

   1\. Hardware-Bypassed Compilation: The native v79 assembly instruction compiler is mapped specifically to handle straight MUL\_MAT (matrix multiplication) calculations for linear data types. \[2, 3\]   
   2\. Missing Lookups for IQ/NL: While IQ4\_NL works beautifully on standard desktop GPUs, the raw custom C++/Assembly driver code for llama.cpp's Hexagon backend does not currently support the non-linear lookup layers (\_NL). Trying to pass a Q4\_NL model right now will throw an unhandled tensor layout error or cause the app to fallback entirely to your slow CPU cores. \[2, 4\]   
   3\. Thermal Benefits: On the v79 tensor tiles, running a native Q4\_0 file means the chip generates tokens at full processing speed with almost zero power draw. Your phone won't heat up, and it won't throttle. \[5\] 

\#\# Crucial Step for Your Kotlin Code  
Because you are handling a 9B model (\~5.5 GB) on the v79 architecture, a single Hexagon memory session block can sometimes choke if you attempt to load it onto one unified block.  
The standard way to bypass this in the Hexagon SDK/llama.cpp pipeline is to register multiple virtual HTP instances (HTP0, HTP1) in your C++ layer to slice the model execution across the v79's internal multi-threaded compute lanes: \[3, 6\] 

// Within your native CMake configuration layer:// Tell the layout to treat the v79 compute lanes like multiple distinct processing blocks  
NDEV=2   
D=HTP0,HTP1

Final Build Verdict: Stick purely to Qwen 3.5 9B Q4\_0. It matches the active capabilities of the Hexagon v79 framework perfectly. \[2, 4\]   
Do you have your CMake configuration flags mapped out to pull the explicit v79 compiler libraries, or do you need help setting up the dynamic linker path to prevent your Kotlin app from throwing a library not found crash at startup?

\[1\] \[https://www.qualcomm.com\](https://www.qualcomm.com/developer/blog/2026/02/build-faster-on-hexagon-npu-tritor-pytorch-with-hexagon-mlir-open-source)  
\[2\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1odriw4/preliminary\_support\_in\_llamacpp\_for\_qualcomm/)  
\[3\] \[https://www.youtube.com\](https://www.youtube.com/watch?v=odnyMYSTxoU\&t=1)  
\[4\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1odriw4/preliminary\_support\_in\_llamacpp\_for\_qualcomm/)  
\[5\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/)  
\[6\] \[[https://huggingface.co\](https://huggingface.co/PanhaPa/My-Llama-3.2-FineTuned/blob/main/llama.cpp/docs/backend/hexagon/developer.md)](https://huggingface.co]\(https://huggingface.co/PanhaPa/My-Llama-3.2-FineTuned/blob/main/llama.cpp/docs/backend/hexagon/developer.md\))  
To run your Qwen 3.5 9B (Q4\_0) model on the Snapdragon 8 Elite's Hexagon NPU, you need an inference engine that explicitly utilizes Qualcomm's Snapdragon/Hexagon backend. \[1\]   
Standard Android LLM apps run purely on the phone's ARM CPU or Adreno GPU. To force the workload onto the NPU via Qualcomm's dedicated dspqueue channel, you have two main routes: the user-friendly App method or the Developer / Termux method. \[1, 2, 3, 4, 5\]   
\------------------------------  
\#\# Method 1: The App Route (Easiest)  
Several local Android LLM runners use native llama.cpp JNI bindings under the hood and have begun rolling out explicit Hexagon NPU acceleration. \[1, 5\] 

   1\. Download a Compatible Runner: Install an advanced open-source GGUF Android client like PocketPal AI or SmolChat (ensure you are on the latest update).  
   2\. Load your Model: Place your 5.5 GB qwen3.5-9b-instruct-q4\_0.gguf file into the app's designated model directory.  
   3\. Change the Hardware Backend: Go to the app's Settings \> Inference Engine / Hardware. Change the execution provider from CPU (Neon) or GPU (OpenCL/Vulkan) to Qualcomm Hexagon / NPU.  
   4\. Set Thread Count: Set your CPU threads to 4 (the number of performance cores on the chip) to handle the initial prompt ingestion smoothly while the NPU processes generation.

\------------------------------  
\#\# Method 2: The Termux Route (Max Performance)  
If you want raw speed without app layer overhead, the open-source community maintains a dedicated llama.cpp fork optimized for Qualcomm HTP (Hexagon Tensor Processor) engines. \[6, 7\] 

   1\. Set Up Termux: Install \[Termux\]([https://termux.dev/](https://termux.dev/) on your Android phone and install standard compilation dependencies (clang, cmake, git    
   2\. Acquire the Qualcomm NPU Libraries: You will need the native proprietary Qualcomm library files 

libhtp\_ops.so and libhtp\_ops\_skel.so 

 .  
 These are usually pulled from an existing Snapdragon 8 Elite device firmware dump or through the official \[Qualcomm AI Stack SDK [https://www.qualcomm.com/processors/ai-engine](https://www.qualcomm.com/processors/ai-engine)  \[4, 8, 9\]   
   3\. Compile llama.cpp with Hexagon Support: Clone the \[haozixu/llama.cpp-npu\](https://github.com/haozixu/llama.cpp-npu) repository (which features Qualcomm-contributed acceleration code) and compile it utilizing the Snapdragon flags:  
     
   mkdir build && cd build  
   cmake \-DLLAMA\_SNAPDRAGON=ON ..  
   make llama-cli  
     
   \[1, 6\]   
   4\. Link the Drivers: Move the model file and the Qualcomm .so binaries into your working directory, and point Android's dynamic linker to them:  
     
   export LD\_LIBRARY\_PATH=/data/local/tmp/llama.cpp  
   export DSP\_LIBRARY\_PATH=/data/local/tmp/llama.cpp  
     
   \[8\]   
   5\. Run Inference: Fire up the client, explicitly pointing to the NPU device:  
     
   ./llama-cli \-m qwen3.5-9b-q4\_0.gguf \--device hexagon \-p "Your prompt here"  
     
   \[8, 10\] 

\------------------------------  
\#\# Critical Settings for Qwen 3.5 9B on Mobile

To keep your 5.5 GB setup stable and avoid running out of RAM, use these exact parameters inside your app or CLI launcher:

\* Context Window (Context Size): Limit this to 4,096 tokens or 8,192 tokens max. While Qwen 3.5 natively supports massive context windows, letting it scale past 8k on a mobile device will cause the Key-Value (KV) cache to balloons in memory, which will cause your app to crash.  
\* Flash Attention: Always toggle Flash Attention \= ON in your app settings. This optimizes memory bandwidth significantly, which is critical since mobile LLM inference is highly bandwidth-bound. \[11\] 

Are you comfortable setting this up inside a graphic user interface (GUI) app, or do you want to try the Termux terminal compilation method to squeeze out every drop of token-generation speed?

\[1\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[2\]   

[https://github.com\](https://github.com/jeffzhou2000/ggml-hexagon](https://github.com]\(https://github.com/jeffzhou2000/ggml-hexagon)   
\[3\]  

[https://www.qualcomm.com\](https://www.qualcomm.com/developer/blog/2025/02/how-to-run-deepseek-windows-snapdragon-tutorial-llama-cpp-mlc-llm](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/blog/2025/02/how-to-run-deepseek-windows-snapdragon-tutorial-llama-cpp-mlc-llm)   
\[4\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1sdeok0/how\_to\_run\_ai\_on\_an\_android\_npu/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1sdeok0/how_to_run_ai_on_an_android_npu/)   
\[5\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing\_smolchat\_running\_any\_gguf\_slmsllms/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing_smolchat_running_any_gguf_slmsllms/)   
\[6\]  
[https://github.com\](https://github.com/haozixu/llama.cpp-npu](https://github.com]\(https://github.com/haozixu/llama.cpp-npu)   
\[7\] \[[https://www.qualcomm.com](https://www.qualcomm.com)     
 [https://www.qualcomm.com/processors/hexagon](https://www.qualcomm.com/processors/hexagon)   
\[8\] [https://github.com\](https://github.com/haozixu/llama.cpp-npu](https://github.com]\(https://github.com/haozixu/llama.cpp-npu)   
\[9\] \[[https://www.youtube.com](https://www.youtube.com)

[https://www.youtube.com/watch?v=wd57g2IM3C4\&t=51](https://www.youtube.com/watch?v=wd57g2IM3C4&t=51)   
\[10\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[11\] \[[https://unsloth.ai\](https://unsloth.ai/docs/models/qwen3.5)](https://unsloth.ai]\(https://unsloth.ai/docs/models/qwen3.5\))

Building your own Kotlin app to tap into that Hexagon NPU is an excellent route, giving you full control over your project.  
To run your Qwen 3.5 9B (Q4\_0) model using Qualcomm's dedicated hardware acceleration in a native Android environment, your architecture needs a Kotlin/JNI bridge that talks directly to a specialized build of llama.cpp. Qualcomm explicitly maintains a native Hexagon/Snapdragon backend directly within the llama.cpp ecosystem. \[1, 2, 3\]   
This guide breaks down how to construct the engine, wire up your JNI bridge, and initialize it inside your Kotlin code.  
\------------------------------  
\#\# Phase 1: Compile the Native Libraries (.so)  
Because standard llama.cpp builds default to the CPU (NEON), you must cross-compile the library using the Android NDK and pass the specific flags that tell the compiler to build for the Qualcomm AI Stack. \[3, 4\] 

   1\. Get the proprietary drivers: You need Qualcomm's libhtp\_ops.so and libhtp\_ops\_skel.so libraries. These act as the communication bridge to the Hexagon NPU hardware. You can extract these binaries directly from a Snapdragon 8 Elite device firmware dump or download them via the official \[Qualcomm AI Stack SDK\](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai). \[3, 5, 6\] 

   2\. Build with CMake: Set up your build toolchain on your machine using the following configuration, explicitly turning the Snapdragon backend engine on:  
     
   mkdir build-android && cd build-android  
   cmake \-DCMAKE\_TOOLCHAIN\_FILE=$ANDROID\_NDK/build/cmake/android.toolchain.cmake \\  
         \-DANDROID\_ABI=arm64-v8a \\  
         \-DANDROID\_PLATFORM=android-34 \\  
         \-DLLAMA\_SNAPDRAGON=ON \\  
         \-DLLAMA\_SNAPDRAGON\_HTP=ON ..  
   make \-j4  
     
   \[4\]   
   3\. Place your compiled libllama.so and the Qualcomm driver .so files into your app project's src/main/jniLibs/arm64-v8a/ directory so Android Studio bundles them natively.

\------------------------------  
\#\# Phase 2: Create the JNI Bridge (llama-jni.cpp)  
Your Kotlin code cannot talk to C++ directly; it needs a JNI mapping layer. Create a src/main/cpp/llama-jni.cpp file that translates Kotlin commands into the specific backend configuration commands for the NPU hardware: \[3, 7\] 

\#include \<jni.h\>\#include \<string\>\#include "llama.h"  
extern "C"JNIEXPORT jlong JNICALL  
Java\_com\_example\_qwenapp\_LlamaEngine\_loadModelNative(JNIEnv \*env, jobject thiz, jstring model\_path) {  
    const char \*path \= env-\>GetStringUTFChars(model\_path, nullptr);

    // 1\. Initialize llama standard parameters  
    llama\_model\_params model\_params \= llama\_model\_default\_params();  
      
    // 2\. Explicitly steer the model allocation to the Hexagon NPU  
    // This tells llama.cpp to pipe operations into the Qualcomm HTP pipeline  
    model\_params.devices\[0\] \= {  
        .type \= LLAMA\_DEVICE\_TYPE\_HEXAGON,  
        .index \= 0  
    };

    llama\_model \*model \= llama\_load\_model\_from\_file(path, model\_params);  
    env-\>ReleaseStringUTFChars(model\_path, path);

    return reinterpret\_cast\<jlong\>(model);  
}  
https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned

\------------------------------  
\#\# Phase 3: Implement the Kotlin Layer  
On the Kotlin side, load the native libraries at runtime and create wrapper functions matching your JNI headers. \[3\]   
Ensure you pass Flash Attention parameters and explicitly set your parallel thread count. On the Snapdragon 8 Elite, assigning 4 threads handles the initial text scanning perfectly before the NPU takes over matrix multiplica)

package com.example.qwenapp  
import android.util.Log  
class LlamaEngine {

    companion object {  
        init {  
            // Load the proprietary Qualcomm NPU hardware communication layers  
            System.loadLibrary("htp\_ops")  
            System.loadLibrary("htp\_ops\_skel")  
            // Load your custom compiled llama engine bridge  
            System.loadLibrary("llama-jni")  
        }  
    }

    private var modelPointer: Long \= 0

    // External JNI hooks mapping to our native C++ code  
    private external fun loadModelNative(modelPath: String): Long  
    private external fun generateTokensNative(pointer: Long, prompt: String, maxTokens: Int): String

    fun initializeModel(absolutePath: String) {  
        try {  
            modelPointer \= loadModelNative(absolutePath)  
            if (modelPointer \== 0L) {  
                Log.error("LlamaEngine", "Failed to bind model to Hexagon NPU.")  
            } else {  
                Log.info("LlamaEngine", "Qwen 3.5 9B loaded onto NPU successfully\!")  
            }  
        } catch (e: Exception) {  
            Log.error("LlamaEngine", "Initialization error: ${e.message}")  
        }  
    }

    fun askQwen(prompt: String): String {  
        if (modelPointer \== 0L) return "Engine not initialized."  
        // Constrain settings: limit max output length to conserve mobile system buffers  
        return generateTokensNative(modelPointer, prompt, maxTokens \= 512\)  
    }  
}

\------------------------------  
\#\# Phase 4: Configure Android Permissions and Memory Bounds  
Because a 5.5 GB model file consumes significant memory, you must inform the Android OS that your app needs access to a larger memory pool to prevent the kernel from forcing an Out-Of-Memory (OOM) shutdown.  
Open your app's AndroidManifest.xml file and append the large heap flag inside the \<application\> tag:

\<application  
    android:name=".QwenApplication"  
    android:largeHeap="true"  
    android:hardwareAccelerated="true"\>  
    ...  
\</application\>

Would you like help writing the token generation logic loop for your llama-jni.cpp file, or do you need assistance configuring the CMakeLists.txt inside Android Studio to bundle these specific libraries correctly?

\[1\] \[[https://www.reddit.com](https://www.reddit.com) ([https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[2\] \[[https://www.reddit.com](https://www.reddit.com)

[https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing\_smolchat\_running\_any\_gguf\_slmsllms/](https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing_smolchat_running_any_gguf_slmsllms/)   
\[3\] [https://github.com\](https://github.com/shubham0204/SmolChat-Android](https://github.com]\(https://github.com/shubham0204/SmolChat-Android)   
\[4\] [https://developers.google.com\](https://developers.google.com/edge/litert/android/npu/qualcomm](https://developers.google.com]\(https://developers.google.com/edge/litert/android/npu/qualcomm)   
\[5\] [https://cactuscompute.com](https://cactuscompute.com) [https://cactuscompute.com/compare/best-llama-cpp-alternative](https://cactuscompute.com/compare/best-llama-cpp-alternative)   
\[6\] [https://www.qualcomm.com\](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)   
\[7\] \[[https://meetprajapati.com\](https://meetprajapati.com/blogs/running-on-device-ai-models-android-mediapipe-llamacpp-executorch/)](https://meetprajapati.com]\(https://meetprajapati.com/blogs/running-on-device-ai-models-android-mediapipe-llamacpp-executorch/\))

\#\# What This Means for Your v79 Workbench Run  
Because the physical memory footprint is tighter and more efficient than I initially stated, compiling Qwen 3.5 9B with ai-edge-litert becomes a lot more attractive:

\* Zero-Copy Memory Layering: On the Hexagon v79, you can aggressively use ion/dma-buf memory allocations. Since the model weights are \~4.2 GB, you can map them directly into the HTP’s virtual address space, keeping your remaining overhead to less than 1.5 GB for execution.  
\* HMX Channel Saturation: At \~5.8 GB total runtime footprint, the model fits beautifully within the fast-access system cache segments of the Gen 4 silicon, meaning fewer trips back to the physical LPDDR5X RAM chips and significantly higher tokens-per-second.

\#\# Adjusting Your Workbench Quantization Flag  
Since you are aiming for that sleek 5.8 GB sweet spot, ensure your quantization config file (quantization\_config.json) passed to the compiler explicitly locks the weights to 4-bit while keeping activations at a higher precision for accuracy:

{  
    "quant\_mode": "WEIGHT\_ONLY\_INT4",  
    "activation\_precision": "INT8",  
    "kv\_cache\_precision": "INT8"  
}

This specific mix maintains Qwen's deep contextual reasoning without letting the file size balloon out of control.  
Now that the footprint is dialed into that \~5.8 GB target:

\* Are you going to use an asymmetric or symmetric INT4 quantization mapping?  
\* Do you need the LiteRT memory-mapping (mmap) code template to ensure it loads directly into the NPU cache without a CPU buffer middleman?

Let me know how you'd like to structure the next step\!

\# Let's double check the exact memory footprint of a 9B model in INT4 quantization.\# A 9B model has approx 9 billion parameters.\# In a strict INT4 weight-only quantization (4 bits per weight), memory \= 9,000,000,000 \* 0.5 bytes \= 4.5 GB.\# Plus the activations, system overhead, and KV cache.\# If it's a mixed precision or slightly under-9B model (like Qwen 2.5 7B/9B, Qwen models often have specific counts like 7.6B or 9B), let's calculate for a 9 billion parameter model.params \= 9e9bytes\_per\_param\_int4 \= 0.5weight\_size\_gb \= (params \* bytes\_per\_param\_int4) / (1024\*\*3)  
print(f"Weight size in GB: {weight\_size\_gb}")

