# HTP

> Extracted from the Qualcomm QAIRT SDK manual (`#QAIRT/` Drive capture). Official Qualcomm documentation — text extracted from the saved doc page; nav/boilerplate stripped.

Developer Workspace

Loading...

Bring your ideas to life by saving your favorite products, comparing specifications and sharing with your team to work collaboratively. 
0 Projects

Sort

You do not have any projects yet. Start building your Workspace.

Documentation

Qualcomm AI Runtime (QAIRT) SDK 

Overview
QAIRT API

Overview
How to Use the QAIRT Sample App

Linux
Windows
Migration Guide
C API

QairtInterface
QairtSystem
QairtContext
QairtGraph
QairtTensor
QairtBackend
QairtDevice
QairtMem
QairtProfile
QairtLog
QairtSignal
QairtOpConfig
QairtQuantizeParams
Common Types
C++ API

QairtApi
QairtContext
QairtGraph
QairtTensor
QairtBackend
QairtDevice
QairtBuffer
QairtMem
QairtProfile
QairtLog
QairtSignal
QairtOpConfig
QairtInfo
QairtProperty
QairtTypeTraits
QairtSystem (C++)
QNN

Introduction
Overview
Setup

Linux Setup
Windows Setup
Backend

DSP
HTP

HTP API Usage Guidelines
Qualcomm Hexagon Plugin Interface
QNN HTP Op Package - Common Default Package Ops Usage Examples
QNN HTP Optimization Utility Functions Usage Examples
HTP Core Headers for Op Packages
Implementing Ops
QNN HTP Op Package API Revision History
Optimization Grammar
QNN HTP Op Package - Relu Op Example
QNN HTP-FP16 Op Package - Relu Op Example
Scheduling and Allocation
Allocate Memory for Scratch Buffers
Tensors and Memory Layout
Writing QNN HTP Op Package
General OpPackage Central Migration Guidance
Op Package Migration Guide
Avoid Low Depth Activations
Avoid Low Depth Activations (more examples)
Use Space-to-depth Transformation where possible
Reducing TCM Requirements for Performance and Functionality
Choice of Activation Functions
Number of Channels
Quantized 16 bit activations (A16) vs FP16 and Activation Fusion: Performance and power differences
INT4 encodings for weights
Other Performance and Energy Guidelines
HTP Yielding
HTP Parallel Graph Execution
HTP VTCM Sharing
VTCM Windowing
QNN HTP SSR
Asynchronous graph execution for HTP backend
QNN HTP Qmem Graph
Multi-SoC DLC with Reference Weight Sharing
HTA
LPAI
CPU
GPU

QNN GPU QnnMem API Tutorial
QNN GPU Tuning Mode Tutorial
Saver
Op Packages

Generating Op Packages

QNN Op Package Code Generation
XML OpDef Schema Reference
Example XML OpDef Configs
QNN Converter Op Package Code Generation
Tools
Converters
Quantization
QAIRT Quantization Specification
Tutorials

Tutorial: Converting and executing a CNN or ONNX model with QNN

CNN to QNN for Linux Host

Convert to QNN for Linux Host on Linux / Android / QNX Target

Convert to QNN for Linux Host on CPU Backend
Convert to QNN for Linux Host on GPU Backend
Convert to QNN for Linux Host on DSP Backend
Convert to QNN for Linux Host on HTP Backend
Convert to QNN for Linux Host on LPAI Backend
CNN to QNN for Linux Host on Windows Target
ONNX to QNN for Linux Host

Convert to QNN for Linux Host on Linux / Android / QNX Target

Convert to QNN for Linux Host on CPU Backend
Convert to QNN for Linux Host on GPU Backend
Convert to QNN for Linux Host on DSP Backend
Convert to QNN for Linux Host on HTP Backend
Convert to QNN for Linux Host on LPAI Backend
CNN to QNN for Linux Host on Windows Target
CNN to QNN for Windows Host

CNN to QNN for Windows Host on Linux Target
CNN to QNN for Windows Host on Windows Target
How to Use the Sample App
Multicore Device Creation and NSP Selection
Sample App Tutorial
Saver Tutorial: Save execution sequence with Saver and replay on a backend
Auto Platform Overview
Tutorial: Running QNN HTP on SA-series

Tutorial: HTP Backend on SA-series — LA GVM (Android)
Tutorial: HTP Backend on SA-series — LV GVM (Rich OS)
Tutorial: Running QNN GPU on SA-series

Tutorial: GPU Backend on SA-series — LV (GVM or PVM)
Tutorial: Running QNN on SA8797 QC Linux PVM (HTP and LPAI Backends)
Tutorial: Running QNN HTP on x86 Using QEMU Emulation
Tutorial: Running QNN HTP with Custom Ops on SA-series

Tutorial: HTP with Custom Ops on SA-series — LA GVM (Android)
Tutorial: HTP with Custom Ops on SA-series — LV GVM (Rich OS)
Control which HTP device executes a model
Tutorial: Turning on various optimization on HTP and HTP MCP Backends
QNN HTP Shared Buffer Tutorial
Tutorial: Executing a shallow model using custom op package
Tutorial: Converting and executing a CNN model with custom operations
Tutorial: Running QNN HTP with Custom Ops on SA-series
Migrating from Hexagon-nn
Auto Platform Overview

Tutorial: Converting and executing a CNN or ONNX model with QNN

CNN to QNN for Linux Host

Convert to QNN for Linux Host on Linux / Android / QNX Target

Convert to QNN for Linux Host on CPU Backend
Convert to QNN for Linux Host on GPU Backend
Convert to QNN for Linux Host on DSP Backend
Convert to QNN for Linux Host on HTP Backend
Convert to QNN for Linux Host on LPAI Backend
CNN to QNN for Linux Host on Windows Target
ONNX to QNN for Linux Host

Convert to QNN for Linux Host on Linux / Android / QNX Target

Convert to QNN for Linux Host on CPU Backend
Convert to QNN for Linux Host on GPU Backend
Convert to QNN for Linux Host on DSP Backend
Convert to QNN for Linux Host on HTP Backend
Convert to QNN for Linux Host on LPAI Backend
CNN to QNN for Linux Host on Windows Target
CNN to QNN for Windows Host

CNN to QNN for Windows Host on Linux Target
CNN to QNN for Windows Host on Windows Target
How to Use the Sample App
Multicore Device Creation and NSP Selection
Sample App Tutorial
Saver Tutorial: Save execution sequence with Saver and replay on a backend
Auto Platform Overview
Tutorial: Running QNN HTP on SA-series

Tutorial: HTP Backend on SA-series — LA GVM (Android)
Tutorial: HTP Backend on SA-series — LV GVM (Rich OS)
Tutorial: Running QNN GPU on SA-series

Tutorial: GPU Backend on SA-series — LV (GVM or PVM)
Tutorial: Running QNN on SA8797 QC Linux PVM (HTP and LPAI Backends)
Tutorial: Running QNN HTP on x86 Using QEMU Emulation
Tutorial: Running QNN HTP with Custom Ops on SA-series

Tutorial: HTP with Custom Ops on SA-series — LA GVM (Android)
Tutorial: HTP with Custom Ops on SA-series — LV GVM (Rich OS)
Control which HTP device executes a model
Tutorial: Turning on various optimization on HTP and HTP MCP Backends
QNN HTP Shared Buffer Tutorial
Tutorial: Executing a shallow model using custom op package
Tutorial: Converting and executing a CNN model with custom operations
Tutorial: Running QNN HTP with Custom Ops on SA-series
Migrating from Hexagon-nn
Benchmarking
Operations

Supported Operations
Op Definition Revision History
Backend Specific Revision History

QNN CPU Op Definition Revision History
QNN DSP Op Definition Revision History
QNN GPU Op Definition Revision History
QNN HTA Op Definition Revision History
QNN HTP Op Definition Revision History
QNN LPAI Op Definition Revision History
Common Terminology
Master Definitions
Block Op Overview
Block Op Usage

Block Ops ONNX Usage
Buffer Usage
MaskedSoftmax Usage
StatefulGru Usage
StatefulLstm Usage
Block Op Definitions
CPU
DSP
GPU
HTP
HTA
LPAI
Functional Safety
API

Overview
Usage Guidelines
QNN API Revision History
Backend Specific Revision History

CPU Version History
DSP Version History
GPU Version History
HTA Version History
HTP Version History
LPAI Version History
QNN System API Revision History
Supported APIs
Supported Capabilities
Error codes
Glossary
Revision History
SNPE

Overview
Setup

Linux Setup
Windows Setup
Tutorials and Examples

SNPE Building and Executing Your Model Tutorial

SNPE Building and Executing a Model Tutorial for Linux Host

SNPE Tutorial for Linux Target Device from Linux Host
In Summary
SNPE Tutorial for Windows Target Device from Linux Host
SNPE Building and Executing Your Model for Windows Host

SNPE Tutorial for Windows Target Device from Windows Host
In Summary
Tutorials Setup
SNPE1 to SNPE2 Migration Guide
Running Nets

Running the Inception v3 Model
Running the Inception v3 Model in Windows
Running the Word-RNN Model
Running the Spoken Digit Recognition Model
Running a VGG Model
Code Examples

C++ Tutorial - Build the Sample
C Tutorial - Build the Sample
C API Guidelines
Android Tutorial
Windows Tutorial
UDO Tutorial
UDO DSP tutorial for Quantized DLC
UDO DSP tutorial on Windows for Quantized DLC
UDO Tutorial With Weights
PSNPE Introduction
PSNPE C Tutorial
PSNPE C++ Tutorial
PSNPE Android Tutorial
DSP Runtime Environment
Network Resizing
Input Image Batch
Init Caching
Windows DSP Library Loading Tutorial
Signed PD and Unsigned PD at Runtime
Enabling Dual‑NPU Execution in SNPE (HTP Device Selection)
Application Tips

Application Integration Tips
Performance Tips
Burst Mode on DSP and AIP
Logging
CPU Fixed Point Mode
Windows ARM64X Support
Hexagon NPU Runtime Driver (Windows Only)
Windows Error Reporting
Low Level Perf APIs
Network Models

Supported Network Layers
Supported ONNX Ops
Quantized vs Non-Quantized Models
User-defined Operations

Overview of UDO
Defining a UDO
Defining a UDO Package
Creating a UDO Package
Compiling a UDO package
Compiling a UDO package for Windows
Preparing a model with UDO
Running a model with UDO
Model Conversion

TensorFlow Model Conversion
Tensorflow Graph Compatibility
TFLite Model Conversion
PyTorch Model Conversion
ONNX Model Conversion
Quantizing a Model
Offline Graph Caching for DSP Runtime on HTP
Qairt Converter
Qairt Quantizer
Model Tips

Using MobilenetSSD
Using DeepLabv3
Input Data and Preprocessing

Input Image Formatting
Benchmarking and Accuracy

Benchmarking

Linting Profile
QHAS Profiling
MobilenetSSD Benchmarking
Inference Accuracy
Tools
Debug Tools

Architecture Checker (Experimental)
Accuracy Debugger (Experimental)
API
Limitations
Revision History
References
Genie

Introduction

Conventions
Revision History
Tutorials

Setup
How to Use Genie
Source Code Examples

libGenie.so and Genie.dll
genie-t2t-run
genie-t2e-run
genie-app
Gen AI Transformer Model Library
Genie Dialog

Llama 2 7B

QNN HTP

Android

Basic

Yes
No
SSD Q1
SPD
LADE
Windows
QNN Gen AI Transformer

Linux

Yes

Linux
Android
No

Linux
Android
Windows
QNN GPU
KV Share Dialog
Llama 3 3B

QNN HTP

Android

Eaglet

Eaglet Dialog - LoRA & Draft Switching
Eaglet Dialog - LoRA Switching
No
Query Cancellation
Dialog Pause/Resume
KV$ Rewind
Updating the Stop Sequence
Token Query
Updating the Sampler Parameters
Engine Sharing
Prefix Quant
batchQuery
Genie Embedding

BGE-Large

QNN HTP
QNN Gen AI Transformer
Genie Engine
Genie Log
Genie Profile
Genie Pipeline

GLM-4v
facebook/wt19-en-de
Genie Accuracy
Genie DLC
Library

Genie C API
GeniePipeline
GenieNode

Genie Node JSON Configuration
GenieDialog

Genie Dialog JSON Configuration
GenieEmbedding

Genie Embedding JSON Configuration
GenieProfile

Genie Profile JSON Configuration
GenieSampler
GenieEngine

Genie Engine JSON Configuration
GenieTokenizer
GenieAccuracy

Genie Accuracy JSON Configuration
Tools

genie-t2t-run
genie-t2e-run
genie-app
qnn-genai-transformer-composer
TFLite Delegate

Overview
Tutorials

Tutorial - Preparing and Executing a Model with TFLite Delegate
Tutorial - Using TFLite Delegate With a C/C++ Application
Tutorial - Using TFLite Delegate With a Java Application
Tutorial - Running Inference Using the Qualcomm® AI Engine Direct Delegate
Tutorial - Skip Delegation Ops Using the Qualcomm® AI Engine Direct Delegate
Tutorial - Benchmarking the Qualcomm® AI Engine Direct Delegate
Tutorial - Running Inference Using Shared Memory
Tutorial - Use Mix-Precision Model with Qualcomm® AI Engine Direct Delegate
Tutorial - Profile Custom Models using Qualcomm® AI Engine Direct Delegate
Tutorial - Use IR Backend by Using the Qualcomm® AI Engine Direct Delegate
Acceleration Support
Tools
Custom Operator Support
Frequently Asked Questions
API

C Interface
Java Interface
External Delegate Options
Qualcomm® AI Engine Direct Backend Library
Glossary
API Version History
Release Notes

Legal notice

- 

- QNN
- Backend
- HTP

# HTP

Updated: Jul 02, 2026 80-63442-10 Rev: AL 

On this page

- API Specializations
- API Usage Guidelines
- Usage Expectations
- QNN HTP Supported Operations
- QNN HTP Supported Core Types
- QNN HTP Variable Batch
- QNN HTP Backend API
- QNN HTP Performance Infrastructure API
- QNN HTP Precision
- QNN HTP FP16 output difference between SM8550 and SM8650
- QNN HTP Deep Learning Bandwidth Compression (DLBC)
- Limitations
- QNN HTP Sparse Weights Compression
- QNN UBWC (Universal Bandwidth Compression)
- QNN HTP - Setting Number of HVX Threads
- QNN HTP - Enabling the system level cache allocator
- QNN HTP Backend Extensions
- Why LLM Decode Modes?
- Available Modes
- Configuration
- QNN HTP Profiling
- QNN HTP Optrace Profiling
- QNN HTP Hextimate Profiling
- QNN HTP Analysis Summary (QHAS)
- QNN Context Binary size
- Op Writing Guidelines
- Choosing Between QHPI and Legacy HTP Operator APIs
- Qualcomm Hexagon Plugin Interface (QHPI)
- Legacy HTP operator APIs
- Recommendations for Network Design
- Yielding and Pre-Emption
- Parallel Graph Execution
- VTCM Sharing
- SubSystem Restart (SSR)
- Running Model on Different HTP Devices (Auto)
- With qnn-net-run on HTP Backend
- With QNN API on HTP Backend
- HTP Optimization (Auto)
- Optimization levels
- P points
- HTP Forced Preemption
- Asynchronous Execution
- Qmem Graph (shared_buffer only graph)
- HTP Session & Artifact Usage Guidelines
- Supported Library Use
- Unsupported Library Use
- Graph Switching (Beta)
- Multi-Graph Switching (Beta)
- Benefits of batch inference and multi-threaded inference
- Hexagon NPU Runtime Driver (Windows Only)
- QnnContext_createFromBinaryListAsync API
- Multiple cDSP Sessions
- Enabling Async Init on older Context bins
- Init and Execute Cancellation
- Multicore
- Graph Priority
- Setting Graph Priority
- LLM native KVcache
- MaskedSoftmax
- QnnContext_createFromBinaryWithCallback API
- Linux/Android code example:
- QNN HTP Monolithic LSTM
- Recommended Use Cases
- Multi-SoC DLC with Reference Weight Sharing

HTP Qualcomm® AI Runtime (QAIRT) SDKs 

# HTP

This section provides information specific to QNN HTP backend. 

- 
API Specializations 
- 
API Usage Guidelines 
- 
Usage Expectations 
- 
QNN HTP Supported Operations 
- 
QNN HTP Supported Core Types 
- 
QNN HTP Backend API 
- 
QNN HTP Performance Infrastructure API 
- 
QNN HTP Precision 
- 
QNN HTP FP16 output difference between SM8550 and SM8650 
- 
QNN HTP Deep Learning Bandwidth Compression (DLBC) 
- 
QNN HTP Sparse Weights Compression 
- 
QNN UBWC (Universal Bandwidth Compression) 
- 
QNN HTP - Setting Number of HVX Threads 
- 
QNN HTP - Enabling the system level cache allocator 
- 
QNN HTP Backend Extensions 
- 
QNN HTP Profiling 
- 
QNN HTP Optrace Profiling 
- 
QNN HTP Hextimate Profiling 
- 
QNN HTP Analysis Summary (QHAS) 
- 
QNN Context Binary size 
- 
Op Writing Guidelines 
- 
Recommendations for Network Design 
- 
Yielding and Pre-Emption 
- 
Parallel Graph Execution 
- 
VTCM Sharing 
- 
SubSystem Restart (SSR) 
- 
Qmem Graph (shared_buffer only graph) 
- 
Graph Switching (Beta) 
- 
Multi-Graph Switching (Beta) 
- 
Benefits of batch inference and multi-threaded inference 
- 
Running Model on Different HTP Devices (Auto) 
- 
HTP Optimization (Auto) 
- 
HTP Performance Estimates 
- 
HTP Forced Preemption 
- 
Asynchronous Execution 
- 
HTP Session & Artifact Usage Guidelines 
- 
Enabling Async Init on older Context bins 
- 
Hexagon NPU Runtime Driver (Windows Only) 
- 
Init and Execute Cancellation 
- 
Multicore 
- 
Graph Priority 
- 
Setting Graph Priority 
- 
LLM native KVcache 
- 
MaskedSoftmax 
- 
QNN HTP Monolithic LSTM 
- 
Multi-SoC DLC with Reference Weight Sharing 

## API Specializations

This section contains information related to API specialization for the HTP backend. All QNN HTP backend specialization is available under `<QNN_SDK_ROOT>/include/QNN/HTP/` directory. 
The current version of the QNN HTP backend API is: 

Warning 
doxygendefine: Cannot find define “QNN_HTP_API_VERSION_MAJOR” in doxygen xml output for project “QairtCApi” from directory: /local/mnt/workspace/mlg_user_admin/ci.docker.tmp/37_32544/build/x86_64-linux-clang/FirstParty/QNN/Doc/qairt-api-docs/c-api-docs/xml 

Warning 
doxygendefine: Cannot find define “QNN_HTP_API_VERSION_MINOR” in doxygen xml output for project “QairtCApi” from directory: /local/mnt/workspace/mlg_user_admin/ci.docker.tmp/37_32544/build/x86_64-linux-clang/FirstParty/QNN/Doc/qairt-api-docs/c-api-docs/xml 

Warning 
doxygendefine: Cannot find define “QNN_HTP_API_VERSION_PATCH” in doxygen xml output for project “QairtCApi” from directory: /local/mnt/workspace/mlg_user_admin/ci.docker.tmp/37_32544/build/x86_64-linux-clang/FirstParty/QNN/Doc/qairt-api-docs/c-api-docs/xml 

## API Usage Guidelines

The following page refers to both the minimum expected and expanded QNN HTP backend API callflows: HTP API Usage Guidelines 

## Usage Expectations

1. The sequence of calls to QnnGraph_addNode() to build the QNN model should be done in the node dependency order. 
2. The QnnBackend_registerOpPackage() takes in an optional parameter called ‘target’. 

Given below are the acceptable values for ‘target’ 

- 
“CPU” - for both linux x86 and ARM op packages 
- 
“HTP” - for op package that gets loaded on the HTP. 
- 
nullptr 

- 
For loading context binary on ARM - Loads registered HTP op package 
- 
For linux x86 - Registers the linux x86 op package 

- 
Loading a context binary generated for a different HTP arch may give indeterminate result. 

## QNN HTP Supported Operations

QNN HTP supports running quantized 8-bit and quantized 16-bit networks on all Qualcomm SoCs. List of operations supported by QNN HTP Quant runtime can be seen under Backend Support HTP column in Supported Operations 
QNN HTP supports running float32 networks using float16 math on select Qualcomm SoCs. If the QNN SDK supports QNN HTP Float, the list of operations supported by HTP Float runtime can be seen under Backend Support HTP FP16 column in Supported Operations 
QNN HTP supports running bfloat16 networks on Qualcomm Windows V81 based platforms and newer. If the QNN SDK supports QNN HTP BFloat16, the list of operations supported by HTP BF16 runtime can be seen under Backend Support HTP BF16 column in Supported Operations 

## QNN HTP Supported Core Types

QNN HTP supports multiple core types, such as NSPSS and HPASS, on select SOCs. To determine the supported core type, clients can query the platform’s capabilities using the QnnDevice_getPlatformInfo() API. This API returns a exhale_struct_structQnnDevice__PlatformInfo__t containing information about the platform’s core type, which is represented by the QnnHtpDevice_t. 
To create a device handle with the desired core type and its respective core ID, QNN clients can use the retrieved the platform’s capabilities and customize their own QnnDevice_Config_t struct per device to match the platform’s configuration. By doing so, clients can optimize their application for the specific core type and its associated capabilities. 
Example below shows how the client can create a device handle based on the retrieved platform information, using their customized exhale_struct_structQnnDevice__Config__t. 

```
// retrieve platform capabilities
QnnDevice_PlatformInfo_t* platformInfo{nullptr};
QnnDevice_PlatformInfo_t* platformInfoCustomized{nullptr};
QnnDevice_getPlatformInfo(&platformInfo);

// check platformInfo for hwDeviceId, coreId, and coreType
// populate platformInfoCustomized based on the coreType
for (auto& device : platformInfo->hwDevices) {
for (auto& core : device.cores) {
if (core.coreType == QNN_HTP_CORE_TYPE_HPASS) {
*platformInfoCustomized = device;
break;
}
}
}

// use the customized platformInfo in the deviceConfig.
QnnDevice_Config_t deviceConfig;
deviceConfig.option = QNN_DEVICE_CONFIG_OPTION_PLATFORM_INFO;
deviceConfig.hardwareInfo = platformInfoCustomized;
const QnnDevice_Config_t* deviceConfigs[] = {&deviceConfig, nullptr};

// create a device handle based on the information populated in deviceConfig
Qnn_DeviceHandle_t device;
QnnDevice_create(deviceConfigs, &device);

```

To configure an HTP Backend extension with core type HPASS, refer to the following example configuration. More documentation on Backend extension can be found under QNN HTP Backend Extensions. 

```
{
"devices": [
{
// Selection of the device [optional] [default: 0]
"device_id": 0,
// Type of core to be used [optional: 0 for NSP, 1 for HPASS] [default: 0]
"core_type": 1,
//core Id of the selected core
"core_id":0
}
]
}

```

Note 
HPASS cores have specific limitations when it comes to certain features, such as the number of HVX, HMX threads, VTCM sizes, or FP16 support. When running QNN HTP BE on HPASS, the available feature set may differ from what’s supported on NSPSS, depending on the underlying core capabilities. 

## QNN HTP Variable Batch

QNN HTP supports variable batch dimension in a limited manner. The batch dimension at graph execute can be an integer mulitple of the respective dimension provided at graph prepare. All inputs and outputs tensors must have the same batch multiple. For example, if the tensor dimensions provided at graph prepare are [b,h,w,d] then graph can be executed with tensor having dimensions as [n*b,h,w,d], where n is a positive integer. 

## QNN HTP Backend API

file_include_QNN_HTP_QnnHtpDevice.h is the backend specialization header that goes along with file_include_QNN_QnnDevice.h. This header file allows clients to configure the QnnDevice to cater to specific use-cases. 
exhale_struct_structQnnHtpGraph__CustomConfig__t is defined in file_include_QNN_HTP_QnnHtpGraph.h and is the backend specialization header that goes with file_include_QNN_QnnGraph.h 
QNN HTP Device Config Options (QnnHtpDevice_CustomConfig_t)

Option Name 
Option Description 
Default 
When to use 

QNN_HTP_DEVICE_CONFIG_OPTION_SOC 
Integer value used to identify SoC model 
QNN_SOC_MODEL_SM8350 
Client can provide socModel to indicate which SoC is targeted 

QNN_HTP_DEVICE_CONFIG_OPTION_ARCH 
Data structure to configure a device to set the HTP Arch. The driver will use ops that are compatible to this HTP Arch 
QNN_HTP_DEVICE_ARCH_NONE 
Client can provide as part of the custom config when there are multiple devices in use 

QNN_HTP_DEVICE_CONFIG_OPTION_SIGNEDPD 
Enables signed process domain. In order to use this flag, client also needs to push a signed dsp image to target 
False (Unsigned Process Domain) 
Client use signed process domain. Check Hexagon SDK document for more detail. 
Client can set SocModel as shown below: Refer Qnn_SocModel_t for setting Soc Model. 
Note that Qnn_SocModel_t will be deprecated, For setting Soc Model refer to the Supported Snapdragon Devices 

```
1QnnHtpDevice_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_DEVICE_CONFIG_OPTION_SOC;
3customConfig.socModel = QNN_SOC_MODEL_SM8550;
4QnnDevice_Config_t devConfig;
5devConfig.option = QNN_DEVICE_CONFIG_OPTION_CUSTOM;
6devConfig.customConfig = &customConfig;
7const QnnDevice_Config_t* pDeviceConfig[] = {&devConfig, NULL};

```

Client can set Htp arch as shown below: Refer QnnHtpDevice_Arch_t for setting Htp Arch. 

```
1QnnHtpDevice_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_DEVICE_CONFIG_OPTION_ARCH;
3customConfig.arch.arch = QNN_HTP_DEVICE_ARCH_V73;
4customConfig.arch.deviceId = 0; // Id of device to be used. If single device is used by default 0.
5QnnDevice_Config_t devConfig;
6devConfig.option = QNN_DEVICE_CONFIG_OPTION_CUSTOM;
7devConfig.customConfig = &customConfig;
8const QnnDevice_Config_t* pDeviceConfig[] = {&devConfig, NULL};

```

Client can set signed PD as shown below: 

```
1QnnHtpDevice_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_DEVICE_CONFIG_OPTION_SIGNEDPD;
3customConfig.useSignedProcessDomain.useSignedProcessDomain = true;
4customConfig.useSignedProcessDomain.deviceId = 0; // Id of device to be used. If single device is used by default 0.
5QnnDevice_Config_t devConfig;
6devConfig.option = QNN_DEVICE_CONFIG_OPTION_CUSTOM;
7devConfig.customConfig = &customConfig;
8const QnnDevice_Config_t* pDeviceConfig[] = {&devConfig, NULL};

```

QNN HTP Context Config Options (QnnHtpContext_CustomConfig_t)
Clients can enable weight sharing as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_WEIGHT_SHARING_ENABLED;
3customConfig.weightSharingEnabled = true; // set to false to disable weight sharing
4QnnContext_Config_t contextConfig;
5contextConfig.option = QNN_CONTEXT_CONFIG_OPTION_CUSTOM;
6contextConfig.customConfig = &customConfig;
7const QnnContext_Config_t* pContextConfig[] = {&contextConfig, NULL};

```

Note 
The Weight Sharing feature has certain requirements and limitations: 

- 
Only supports offline prepare on x86_Linux platform. Online prepare and other platforms (ARM/x86_Windows) offline prepare are not supported. 
- 
Only supports Hexagon v73 and onward architectures. 
- 
Only supports within a single PD. Sharing cross PD or cross different VTCM size and SoC is not supported. 
- 
Only supports sharing for a maximum of 64 graphs at the same time per context. 
- 
Any previously generated binaries will not automatically benefit from Weight Sharing. Users are required to regenerate new serialized binary to benefit from Weight Sharing. Old serialized binaries will still work without the weight sharing feature. 
Clients can set shared spill-fill buffer details for multiple contexts as follows: 

Note 
This feature is only enabled for an offline prepare usecase. Information regarding spill fill size is written as part of exhale_struct_structQnnSystemContext__BinaryInfo__t defined in QnnSystemContext.h. The hwInfoBlob field within the struct contains information regarding the index to the graph and respective spill fill buffer size utilized by that graph as defined in QnnHtpSystemContext.h. 
Users should figure out the maximum spill fill buffer size needed across all the contexts before proceeding to deserialize. There are two ways to achieve this: 

- 
Use qnn-context-binary-utility to output binary details in a JSON file. It essentially prints the content of exhale_struct_structQnnSystemContext__BinaryInfo__t, along with HTP specific content as defined in QnnHtpSystemContext.h. Search for the “spillFillBufferSize” key to figure out the spill fill buffer size required for each of the graphs. 
- 
Add checks at runtime. Users can parse the content of the binary from exhale_struct_structQnnSystemContext__BinaryInfo__t struct along with HTP specific information from QnnHtpSystemContext.h. 

```
1// ===== FIRST CONTEXT =====
2QnnHtpContext_CustomConfig_t customConfig;
3customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_MULTI_CONTEXTS;
4QnnHtpContext_GroupRegistration_t groupInfo;
5groupInfo.firstGroupHandle = 0x0; // New group
6groupInfo.maxSpillFillBuffer = 30081024; // Max spill-fill buffer across contexts. Must be >0
7customConfig.groupRegistration = groupInfo;
8QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
9QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);
10
11// ===== SECOND CONTEXT =====
12QnnHtpContext_CustomConfig_t customConfig2;
13customConfig2.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_MULTI_CONTEXTS;
14QnnHtpContext_GroupRegistration_t groupInfo2;
15groupInfo2.firstGroupHandle = contextHandle; // associated to above contextHandle
16groupInfo2.maxSpillFillBuffer = 30081024; // same value as above OR don't set this now
17customConfig2.groupRegistration = groupInfo2;
18QnnContext_Config_t* cfgs2[] = {&customConfig2, NULL};
19QnnContext_createFromBinary(..., cfgs2, ..., &contextHandle2, ...);
20
21// ===== THIRD CONTEXT =====
22QnnHtpContext_CustomConfig_t customConfig3;
23customConfig3.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_MULTI_CONTEXTS;
24QnnHtpContext_GroupRegistration_t groupInfo3;
25groupInfo3.firstGroupHandle = contextHandle; // associated to above contextHandle
26groupInfo3.maxSpillFillBuffer = 30081024; // same value as above or don't set this
27customConfig3.groupRegistration = groupInfo3;
28QnnContext_Config_t* cfgs3[] = {&customConfig3, NULL};
29QnnContext_createFromBinary(..., cfgs3, ..., &contextHandle3, ...);

```

Clients can set shared spill-fill and VTCM backup buffers for concurrent resource sharing as follows: 

Note 
This feature is only supported on Android for the V81 Hexagon architecture. This feature is only enabled for an offline prepare usecase. Information regarding spill fill size is written as part of exhale_struct_structQnnSystemContext__BinaryInfo__t defined in QnnSystemContext.h. The hwInfoBlob field within the struct contains information regarding the index to the graph and respective spill fill buffer size utilized by that graph as defined in QnnHtpSystemContext.h. 
Users should figure out the maximum spill fill buffer size needed across the contexts for each priority before proceeding to deserialize. There are two ways to achieve this: 

- 
Use qnn-context-binary-utility to output binary details in a JSON file. It essentially prints the content of exhale_struct_structQnnSystemContext__BinaryInfo__t, along with HTP specific content as defined in QnnHtpSystemContext.h. Search for the “spillFillBufferSize” key to figure out the spill fill buffer size required for each of the graphs. 
- 
Add checks at runtime. Users can parse the content of the binary from exhale_struct_structQnnSystemContext__BinaryInfo__t struct along with HTP specific information from QnnHtpSystemContext.h. 
In addition, the context/graph priorities within each group must be the same, and the priorities cannot be modified by either `QnnContext_setConfig()` or `QnnGraph_setConfig()` on the fly. However, if this concurrent feature is not enabled, you may have graphs with different priorities within the same context. 

```
1// ===== CONTEXT #1: NEW GROUP =====
2QnnHtpContext_CustomConfig_t customConfig;
3customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_CONCURRENT_RESOURCE_SHARING;
4QnnHtpContext_GroupRegistration_t groupInfo;
5groupInfo.firstGroupHandle = 0x0; // New group, can be any priority
6groupInfo.maxSpillFillBuffer = 30081024; // Max spill-fill buffer across contexts. Must be > 0
7customConfig.concurrentGroupRegistration = groupInfo;
8QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
9QnnContext_createFromBinary(..., cfgs, ..., &contextHandle1, ...);
10
11// ===== CONTEXT #2: SAME GROUP AS CONTEXT #1 =====
12QnnHtpContext_CustomConfig_t customConfig2;
13customConfig2.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_CONCURRENT_RESOURCE_SHARING;
14QnnHtpContext_GroupRegistration_t groupInfo2;
15// Must be the same priority as CONTEXT #1
16groupInfo2.firstGroupHandle = contextHandle1; // associated with the above contextHandle1
17groupInfo2.maxSpillFillBuffer = 30081024; // same value as above OR don't set this now
18customConfig2.concurrentGroupRegistration = groupInfo2;
19QnnContext_Config_t* cfgs2[] = {&customConfig2, NULL};
20QnnContext_createFromBinary(..., cfgs2, ..., &contextHandle2, ...);
21
22// ===== CONTEXT #3: SAME GROUP AS CONTEXT #1 =====
23QnnHtpContext_CustomConfig_t customConfig3;
24customConfig3.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_CONCURRENT_RESOURCE_SHARING;
25QnnHtpContext_GroupRegistration_t groupInfo3;
26// Must be the same priority as CONTEXT #1
27groupInfo3.firstGroupHandle = contextHandle1; // associated with the above contextHandle1
28groupInfo3.maxSpillFillBuffer = 30081024; // same value as above or don't set this
29customConfig3.concurrentGroupRegistration = groupInfo3;
30QnnContext_Config_t* cfgs3[] = {&customConfig3, NULL};
31QnnContext_createFromBinary(..., cfgs3, ..., &contextHandle3, ...);
32
33// ===== CONTEXT #4: NEW GROUP WITH ONLY ONE CONTEXT =====
34QnnHtpContext_CustomConfig_t customConfig4;
35customConfig4.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_CONCURRENT_RESOURCE_SHARING;
36QnnHtpContext_GroupRegistration_t groupInfo4;
37groupInfo4.firstGroupHandle = 0x0; // New group, can be any priority
38groupInfo4.maxSpillFillBuffer = 30081024; // Max spill-fill buffer across contexts. Must be > 0
39customConfig4.concurrentGroupRegistration = groupInfo4;
40QnnContext_Config_t* cfgs4[] = {&customConfig4, NULL};
41QnnContext_createFromBinary(..., cfgs4, ..., &contextHandle4, ...);

```

Clients can configure read memory budget of serialized binary as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_FILE_READ_MEMORY_BUDGET;
3customConfig.fileReadMemoryBudgetInMb = 25;
4QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
5QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

In the example above, 25 MB chucks are loaded to memory at a time. If user sets the value to greater than the file size, min(fileSize, fileReadMemoryBudgetInMb) is used. The value should be greater than 0 and less than or equal to the file size. If a value of 0 is passed, this feature is turned off. 
Clients can configure I/O memory estimation as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_IO_MEM_ESTIMATION;
3customConfig.ioMemoryEstimation = true;
4QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
5QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

Clients can configure share resources and share resource optimization type as follows: 

Note 
This custom config needs to be set and passed as a group configuration and not as individual context configuration. `QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES_OPTIMIZATION_TYPE` is applied only when `QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES` is true; otherwise, it is ignored. 
The following table lists available configuration options for `QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES_OPTIMIZATION_TYPE`. 

Option Name 
Option Description 

SEQUENTIAL_WITH_VA_OPTIMIZATION 

- 
Graphs have to be executed sequentially, otherwise unexpected system behavior may be observed. 
- 
Optimizes both HTP Virtual Address (VA) space and runtime memory usage. 
- 
Ideal for large generative AI workloads with multiple splits. 
- 
VA optimization is supported only on specific SoCs. If used on an unsupported SoC, the API will return `QNN_CONTEXT_ERROR_UNSUPPORTED_FEATURE`. 

SEQUENTIAL_WITHOUT_VA_OPTIMIZATION 

- 
Graphs have to be executed sequentially, otherwise unexpected system behavior may be observed. 
- 
Optimizes runtime memory usage, but without explicit HTP VA space optimization. 
- 
Suitable for smaller generative AI workloads and traditional AI models. 

CONCURRENT_OPTIMIZATION 

- 
Designed for concurrent graph execution with runtime memory optimization. 
- 
When enabled, spill-fill and VTCM backup buffers are shared by contexts with the same priorities. 
- 
This feature is only supported on Android for the V81 Hexagon architecture. 

```
1QnnHtpContext_CustomConfig_t customListConfig[2];
2customListConfig[0].option = QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES;
3customListConfig[0].shareResources = true;
4QnnHtpContext_CustomConfig_t shResOptConfig;
5customListConfig[1].option = QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES_OPTIMIZATION_TYPE;
6customListConfig[1].shareResOptType = SEQUENTIAL_WITH_VA_OPTIMIZATION;
7QnnContext_Config_t* cfgs[] = {&customListConfig[0], &customListConfig[1], NULL};
8QnnContext_createFromBinaryListAsync(..., &contextParams, cfgs, ...);

```

Clients can configure init acceleration as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_INIT_ACCELERATION;
3customConfig.initAcceleration = true;
4QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
5QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

Clients can configure concurrent deserialization patch as follows: 

```
1int patchFD = open("/path/to/patch/file", O_RDWR | O_CREAT, 0644);
2QnnHtpContext_CustomConfig_t customConfig;
3customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_CONCURRENT_DESERIALIZATION_PATCH;
4customConfig.concurrentDeserializationPatchFd = patchFD;
5QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
6QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

Clients can configure skip validation on binary section as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_SKIP_VALIDATION_ON_BINARY_SECTION;
3customConfig.skipValidationOnBinarySection = true;
4QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
5QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

Clients can enable lora weight sharing as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_LORA_WEIGHT_SHARING_ENABLED;
3customConfig.loraWeightSharingEnabled = true; // set to false to disable lora weight sharing
4QnnContext_Config_t contextConfig;
5contextConfig.option = QNN_CONTEXT_CONFIG_OPTION_CUSTOM;
6contextConfig.customConfig = &customConfig;
7const QnnContext_Config_t* pContextConfig[] = {&contextConfig, NULL};

```

Note 
The Lora Weight Sharing feature has certain requirements and limitations: 

- 
Only supports offline prepare on x86_Linux platform. Online prepare and other platforms (ARM/x86_Windows) offline prepare are not supported. 
- 
Any previously generated binaries will not automatically benefit from Lora Weight Sharing. Users are required to regenerate new serialized binary to benefit from Lora Weight Sharing. Old serialized binaries will still work without the lora weight sharing feature. 
Clients can configure reused I/O size as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_REUSED_IO_LIMIT;
3customConfig.reusedIoLimitMb = 1024; /* IO size in MBs */
4QnnContext_Config_t* cfgs[] = {&customConfig, NULL};
5QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```

Clients can enable graph splitting as follows: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_GRAPH_SPLITTING_ENABLED;
3customConfig.graphSplittingEnabled = true;
4QnnContext_Config_t contextConfig;
5contextConfig.option = QNN_CONTEXT_CONFIG_OPTION_CUSTOM;
6contextConfig.customConfig = &customConfig;
7const QnnContext_Config_t* pContextConfig[] = {&contextConfig, NULL};
8QnnContext_create(..., pContextConfig, ..., &contextHandle);

```

Note 
The Graph Splitting feature has certain requirements and limitations: 

- 
Only supports offline prepare on x86_Linux platform. Online prepare and other platforms (ARM/x86_Windows) offline prepare are not supported. 
- 
Models must be re-prepared to benefit from graph splitting. Previously generated context binaries are not affected and will continue to work without it. 
QNN HTP Graph Config Options (QnnHtpGraph_CustomConfig_t)

Option Name 
Option Description 
Default 
When to use 

QNN_HTP_GRAPH_CONFIG_OPTION_OPTIMIZATION 
This enum provides different HTP graph optimization options that can be used to finalize the graph for optimum performance. 
QNN_HTP_GRAPH_OPTIMIZATION_TYPE_UNKNOWN 
Client can provide this option when an optimization is desired for the graph finalize process 

QNN_HTP_GRAPH_CONFIG_OPTION_PRECISION 
An enum which defines the different precision modes supported by QNN backends 
QNN_PRECISION_FLOAT32 
Client provides when they desire to use a specific math type in the implementation of an operation 

QNN_HTP_GRAPH_CONFIG_OPTION_VTCM_SIZE_IN_MB/QNN_HTP_GRAPH_CONFIG_OPTION_VTCM_SIZE 
Used to define the amount of VTCM memory (in MB) to reserve and utilize 
4 When a client wants to use a specific (<= MAX_SOC_VTCM) or the maximum VTCM amount

- 
To use the maximum VTCM amount, set the value to QNN_HTP_GRAPH_CONFIG_OPTION_MAX and specify the target SoC (QNN_HTP_DEVICE_CONFIG_OPTION_SOC). 
- 
When loading a context binary generated with `VTCM = QNN_HTP_GRAPH_CONFIG_OPTION_MAX`, if QnnContext_BinaryCompatibilityType_t is set to `STRICT`, QNN performs the optimality check for the graph VTCM size. 

QNN_HTP_GRAPH_CONFIG_OPTION_FOLD_RELU_ACTIVATION_INTO_CONV_OFF 
For any graph where a Convolution or Convolution like operation is followed by Relu or ReluMinMax, the relu is folded into the Convolution operation 
always fold 
Clients that cannot guarantee that the quantization parameters of the Relu output exactly reflect the range of the data after the Relu should set this flag. This will come at the cost of performance, but perserve the requested quantization encodings 

QNN_HTP_GRAPH_CONFIG_OPTION_SHORT_DEPTH_CONV_ON_HMX_OFF 
Run all Convolution operations using HMX instructions 
always use HMX instructions 
Clients that have graphs where weights are not symmetric and have Convolution with short depths should set this flag to guarantee accurate results 

QNN_HTP_GRAPH_CONFIG_OPTION_HIGH_PRECISION_SIGMOID 
Use FP16 high precision HVX kernel for Sigmoid 
false 
High precision sigmoid HVX implementation gives better accuracy with some performance degradation. 

QNN_HTP_GRAPH_CONFIG_OPTION_NUM_HVX_THREADS 
Used to define number of HVX threads to reserve and utilize for a particular graph 
4 
When a client wants to keep aside specific number of HVX threads for other parallel work loads 

QNN_HTP_GRAPH_CONFIG_OPTION_WEIGHTS_PACKING 
Used to enable weights packing for a particular graph 
false 
This feature is currently in experimental beta release, any proposed method of usage and behavior may change in future releases. At graph prepare, enabling this feature will cause 8-bit weights that are in the 4-bit range to be stored in the context binary as packed 4-bit, potentially reducing the context binary size. However, please note that while this may reduce the size of a context binary, it does not guarantee any performance improvements. 

QNN_HTP_GRAPH_CONFIG_OPTION_FINALIZE_CONFIG 
This option sets the graph finalize level settings. 
No explicit setting. 
It is used to configure the graph finalize level. More details will be provided in a separate supplementary note. 

QNN_HTP_GRAPH_CONFIG_OPTION_MONOLITHIC_LSTM 
Enables execution of LSTM operations as single, monolithic units rather than decomposing them into multiple smaller sub-operations. 
false (LSTM operations are expanded into multiple smaller sub-operations by default) 
This option is to execute LSTM operations in a monolithic style, to avoid expanding an LSTM operations into multiple small operations, potentially reducing context binary size and graph compile time. 

QNN_HTP_GRAPH_CONFIG_OPTION_PRECISION_COMPENSATION 
Improves numerical precision for models quantized with 16-bit activations on Hexagon v81+ targets. Requires min_arch >= 81. 
false 
Higher numerical precision for 16-bit activation models when better accuracy is needed. 
QNN_HTP_GRAPH_OPTIMIZATION_TYPE_FINALIZE_OPTIMIZATION_FLAG = 3 configuration will take into account QNN_HTP_DEVICE_CONFIG_OPTION_SOC configuration when possible. When SOC information is taken into account, O3 configuration is expected to provide more optimal graph in most cases, but may result in less optimal graph in some cases. Also, it may yield possible larger context binary size and hence possible degradation on graph loading time. 

Note 
Its recommended to refer Hexagon SDK documentation prior to following section as significant functionality described here, inherently uses Hexagon SDK APIs. 
If the user specifies both QNN_HTP_DEVICE_CONFIG_OPTION_SOC and QNN_HTP_DEVICE_CONFIG_OPTION_ARCH , the HTP backend driver uses the QNN_HTP_DEVICE_CONFIG_OPTION_SOC configuration and ignores the QNN_HTP_DEVICE_CONFIG_OPTION_ARCH configuration. We recommend using QNN_HTP_DEVICE_CONFIG_OPTION_SOC instead of QNN_HTP_DEVICE_CONFIG_OPTION_ARCH. 
exhale_typedef_QnnHtpDevice_8h_1a1b95c51336e38afa44dff4da80c21c76 is a backend specific subcomponent of exhale_struct_structQnnDevice__HardwareDeviceInfo__t. Information for these structs are provided by the client for offline operation, and can be populated by a call to QnnDevice_getPlatformInfo() 
QNN HTP Device Info Extension Options (QnnHtpDevice_DeviceInfoExtension_t)

Option Name 
Option Description 
Default 
When to use 

QNN_HTP_DEVICE_CONFIG_OPTION_PCIE_DEVICE_INFO_EXTENSION 
This structure provides info about the device that connect with PCIe bus: VTCM size (in MB), socModel, number of NSPs on the PCIe device, signed PD support, DLBC support, device architecture 
NULL 
For online operation, caller should get these info from QnnDevice_getPlatformInfo. For offline operation, caller need to create this structure and filling the correct information for QnnDevice_create 

QNN_HTP_DEVICE_CONFIG_OPTION_ON_CHIP_DEVICE_INFO_EXTENSION 
This structure provides info about the NSP device inside SoC: VTCM size (in MB), socModel, signed PD support, DLBC support, device architecture 
NULL 
For online operation, caller should get these info from QnnDevice_getPlatformInfo. For offline operation, caller need to create this structure and filling the correct information for QnnDevice_create 

## QNN HTP Performance Infrastructure API

Clients can invoke QnnDevice_getInfrastructure after loading the QNN HTP library and then invoke methods that are available in QnnHtpPerfInfrastructure.h. These APIs allow a client to control CPU and HTP accelerator’s system settings for performance purpose. A few use-cases are: 

- 
Set up a voting policy by controlling the core clocks and voltage corners. 
- 
Set up DCVS modes to achieve different performance settings as applicable to a use-case. 
- 
Set up a specific RPC Control Latency per session to control CPU low power modes and reduce CPU wake-up latency impact on FastRPC. Latency critical applications recommended to vote for greater than 0 and less than 200 us; moderate latency requirement can vote for more than 200 us or by default 0 us without specific setting needed. 

Note 
Setting up number of threads for accelerator is not supported. QNN Perf Infrastructure maps directly to fast RPC features on CPU side, and on HTP side maps to HAP_Power DCVS v3. For detailed configuration of voltage corners and DCVS modes, please refer to Hexagon SDK documentation on HAP_power_set API. 
QNN provides interface through APIs to control DSP core and bus clocks based on power and performance needs. These APIs allow programmers to adjust the DSP power usage as per the application’s power requirement, thereby providing a good balance between power consumption and performance. Performance Parameters table shows settings for various user defined performance profiles. These performance parameters are listed in QnnHtpPerfInfrastructure.h and can be used to control performance settings. Usage of these performance parameters are shown below. 

Note 

Terminology — the DCVS_V3 vote. A `DCVS_V3` (or `DCVS_V3_EXP`) configuration controls both the QDSP6 core voltage corner and the HVX clock corner — the two are programmed together by the same struct and cannot be voted independently. This document refers to that combined vote as the “DCVS_V3 vote” (or “DCVS_V3 / DCVS_V3_EXP vote” when both variants apply). Older material and existing client integrations may refer to it as the “core vote” or “HVX vote” interchangeably; in this document those terms are equivalent to a DCVS_V3 vote. 
Clock Corner Settings - It is used to set Bus and Core operating corners for performance setting. 

```
QnnHtpPerfInfrastructure_DcvsV3_t dcvsV3Config;

```

Bus Parameters - Bus params is used to set the bus clock parameters. 

```
dcvsV3Config.setBusParams = 1; //True to consider Bus parameter otherwise False.
dcvsV3Config.busVoltageCornerMin = DCVS_VOLTAGE_VCORNER_TURBO;
dcvsV3Config.busVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_TURBO;
dcvsV3Config.busVoltageCornerMax = DCVS_VOLTAGE_VCORNER_TURBO;

```

Core Parameters - Core params is used to set the core clock parameters. 

```
dcvsV3Config.setCoreParams = 1; //True to consider Core parameter otherwise False.
dcvsV3Config.coreVoltageCornerMin = DCVS_VOLTAGE_VCORNER_TURBO;
dcvsV3Config.coreVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_TURBO;
dcvsV3Config.coreVoltageCornerMax = DCVS_VOLTAGE_VCORNER_TURBO;

```

DCVS Enable - setDcvsEnable and dcvsEnable parameters enables user to vote for DCVS participation. 

```
dcvsV3Config.setDcvsEnable = 1;
dcvsV3Config.dcvsEnable = 0; // zero value means to disable dcvs

```

Sleep Latency - setSleepLatency and sleepLatency parameters can be used to request for a sleep latency in micro seconds. 

```
dcvsV3Config.setSleepLatency = 1;
dcvsV3Config.sleepLatency = 100; // give sleep latency value, ranges 10-65535 us

```

Sleep Disable - setSleepDisable and sleepDisable parameters enables user to disable sleep (all LPM modes) in HTP. 

```
dcvsV3Config.setSleepDisable = 1;
dcvsV3Config.sleepDisable = 1; // non zero value means disable sleep

```

Power Mode - powerMode parameter enables user to request for a particular DCVS mode when set_dcvs_enable and dcvs_enable both are set to TRUE. 

```
dcvsV3Config.powerMode = QNN_HTP_PERF_INFRASTRUCTURE_POWERMODE_PERFORMANCE_MODE;

```

QNN HTP Performance Infrastructure APIs provides interface to the client to control the performance and system settings of the QNN HTP Accelerator. 

Create Power Config ID - This API is used to associate unique client context so that subsequent APIs can refer to the same context using created ID. 

```
Qnn_ErrorHandle_t createPowerConfigId(uint32_t deviceId, uint32_t coreId, uint32_t* powerConfigId);

//Usage
uint32_t powerConfigId; // Below Api creates the power config id.
uint32_t deviceId = 0;
uint32_t coreId = 0;
sample_app::StatusCode sample_app::QnnSampleApp::createPowerConfigId() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;
Qnn_ErrorHandle_t perfInfraErr = perfInfra.createPowerConfigId(deviceId, coreId, &powerConfigId);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("createPowerConfigId failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Set Power Config - This API allows client to set up system power configuration that will enable different performance modes. This API uses HAP_power_dcvs_v3_payload struct to config HAP power parameters. Detailed HAP power parameters description please refer to Hexagon SDK HAP_power_dcvs_v3_payload documentation. setPowerConfig API below has settings which gives high performance, users can experiment with different settings according to their requirements. 

```
Qnn_ErrorHandle_t setPowerConfig(uint32_t powerConfigId, const QnnHtpPerfInfrastructure_PowerConfig_t** config);

//Usage
sample_app::StatusCode sample_app::QnnSampleApp::setPowerConfig() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;

QnnHtpPerfInfrastructure_PowerConfig_t powerConfig;
memset(&powerConfig, 0, sizeof(powerConfig));
powerConfig.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DCVS_V3;
powerConfig.dcvsV3Config.dcvsEnable = 0; //1- To enable Dcvs and consider dcvs power mode, 0- To disable dcvs
powerConfig.dcvsV3Config.setDcvsEnable = 1;
powerConfig.dcvsV3Config.contextId = powerConfigId; //use the power config id created

// refer QnnHtpPerfInfrastructure.h
powerConfig.dcvsV3Config.powerMode = QNN_HTP_PERF_INFRASTRUCTURE_POWERMODE_PERFORMANCE_MODE;
powerConfig.dcvsV3Config.setSleepLatency = 1; //True to consider Latency parameter otherwise False
powerConfig.dcvsV3Config.setBusParams = 1; //True to consider Bus parameter otherwise False
powerConfig.dcvsV3Config.setCoreParams = 1; //True to consider Core parameter otherwise False
powerConfig.dcvsV3Config.sleepDisable = 1; //True to disable sleep, False to re-enable sleep
powerConfig.dcvsV3Config.setSleepDisable = 1; //True to consider sleep disable/enable parameter otherwise False

//Set Sleep latency parameter
powerConfig.dcvsV3Config.sleepLatency = 40; // set dsp sleep latency ranges 10-65535 micro sec, refer hexagon sdk

//set Bus Clock Parameters (refer QnnHtpPerfInfrastructure.h)
powerConfig.dcvsV3Config.busVoltageCornerMin = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.busVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.busVoltageCornerMax = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;

//set Core Clock Parameters (refer QnnHtpPerfInfrastructure.h)
powerConfig.dcvsV3Config.coreVoltageCornerMin = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.coreVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.coreVoltageCornerMax = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;

// Set power config with different performance parameters
const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] = {&powerConfig, NULL};

Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Destroy Power Config ID - This API allows client to destroy power configuration ID which was created earlier. 

```
Qnn_ErrorHandle_t destroyPowerConfigId(uint32_t powerConfigId);

//Usage
sample_app::StatusCode sample_app::QnnSampleApp::destroyPowerConfigId() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;

Qnn_ErrorHandle_t perfInfraErr = perfInfra.destroyPowerConfigId(powerConfigId);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("destroyPowerConfigId failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Apart from the above APIs, the user can use RPC polling and control latency for better performance in high performance modes. 
RPC Polling and Latency Settings - rpcPollingTimeConfig parameter can be used to request for a rpc polling time in micro seconds, rpcControlLatencyConfig parameter can be used to reduce CPU wakeup delays. 

```
sample_app::StatusCode sample_app::QnnSampleApp::setRpcLatencyAndPolling() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;

// set RPC Control Latency
QnnHtpPerfInfrastructure_PowerConfig_t rpcControlLatency; // refer QnnHtpPerfInfrastructure.h
memset(&rpcControlLatency, 0, sizeof(rpcControlLatency));
rpcControlLatency.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_RPC_CONTROL_LATENCY;
rpcControlLatency.rpcControlLatencyConfig = 100; // use rpc control latency recommended 100 us, refer hexagon sdk
const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs1[] = {&rpcControlLatency, NULL};

Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs1); // set RPC latency config on power config ID created
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}

// set RPC Polling
QnnHtpPerfInfrastructure_PowerConfig_t rpcPollingTime; // refer QnnHtpPerfInfrastructure.h
memset(&rpcPollingTime, 0, sizeof(rpcPollingTime));
rpcPollingTime.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_RPC_POLLING_TIME;
rpcPollingTime.rpcPollingTimeConfig = 9999; // use rpc polling time recommended 0-10000 us
const QnnHtpPerfInfrastructure_PowerConfig_t* powerConfigs2[] = {&rpcPollingTime, NULL};

Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs2); // set RPC polling config on power config ID created
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Note 

- 
RPC Latency and Polling is not supported on QNX platforms 
- 
For detailed information on all the above performance setting parameters refer hexagon sdk documentation. 
When RPC polling is enabled, the user may further enable adaptive polling for better performance, especially for large models. 
Adaptive Polling Time - adaptivePollingTimeConfig parameter allows users to set the minimum threshold for inference time to determine if adaptive polling should be activated. adaptivePollingTimeConfig parameter can be used to save CPU power by skipping unnecessary RPC polling, and saves RPC time by waking up the CPU just in time to poll for a very short period of time. 

```
sample_app::StatusCode sample_app::QnnSampleApp::setAdaptivePollingTime() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;

// set adaptive polling time
QnnHtpPerfInfrastructure_PowerConfig_t adaptivePollingTime; // refer to QnnHtpPerfInfrastructure.h
memset(&adaptivePollingTime, 0, sizeof(adaptivePollingTime));
adaptivePollingTime.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_ADAPTIVE_POLLING_TIME;
adaptivePollingTime.adaptivePollingTimeConfig = 1000;
const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] = {&adaptivePollingTime, NULL};

// set adaptive polling time config on power config ID created
Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Note 

- 
Adaptive Polling can only be activated if RPC Polling has already been enabled 
- 
It is not recommended to enable adaptive polling for small models (e.g., < 1 ms inference time) 
These performance APIs can be used to boost the performance. Example application of using these APIs for performance improvement in graph execution is shown below. 

```
#include <HTP/QnnHtpPerfInfrastructure.h>
#include <QnnInterface.h>
#include <HTP/QnnHtpDevice.h>

void example_application () {

-----
std::unique_ptr<sample_app::QnnSampleApp> app;
-----
-----

app->createPowerConfigId(); // Create power config ID before voting
app->setRpcLatencyAndPolling(); // Use RPC polling and latency for high performing modes
app->setPowerConfig(); // Set the different configurations for performance settings

-----
app->executeGraphs(); // Execute the graphs
-----

app->destroyPowerConfigId(); // Destroy the power config id
-----
-----
}

```

The above example app shown is purely for usage purpose. Clients can use their own settings for performance in these APIs and use them according to their requirements. 
HMX Power Settings QnnHtpPerfInfrastructure.h allows setting HMX votes manually. To vote manually for both HVX and HMX, user can send different power configurations (PowerConfig’s) as shown below. The API design allows for one single call to set all power parameters. 
CENG Power Settings QnnHtpPerfInfrastructure.h allows setting CENG (Compression Engine) votes manually. The Compression Engine handles data compression and decompression for weights and activations on the HTP. To vote manually for CENG alongside HVX and HMX, the user can include a CENG power configuration in the same `setPowerConfig` call as shown in the HMX Power Settings example above. The API design allows for one single call to set all power parameters (DCVS_V3, HMX, CENG, DDR). 

```
Qnn_ErrorHandle_t setPowerConfig(uint32_t powerConfigId, const QnnHtpPerfInfrastructure_PowerConfig_t** config);

//Usage
sample_app::StatusCode sample_app::QnnSampleApp::setPowerConfig() {
QnnDevice_Infrastructure_t deviceInfra = nullptr;
QnnInterface_t qnnInterface;
Qnn_ErrorHandle_t devErr = qnnInterface.QNN_INTERFACE_VER_NAME.deviceGetInfrastructure(&deviceInfra);
if (devErr != QNN_SUCCESS) {
QNN_ERROR("device error");
return StatusCode::FAILURE;
}
QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;

-------

// Initialize the power config and select the voltage corner values for the performance settings
QnnHtpPerfInfrastructure_PowerConfig_t powerConfig;
memset(&powerConfig, 0, sizeof(powerConfig));

powerConfig.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DCVS_V3;
powerConfig.dcvsV3Config.dcvsEnable = 1; //1- To enable Dcvs and consider dcvs power mode, 0- To disable dcvs
powerConfig.dcvsV3Config.setDcvsEnable = 1;
powerConfig.dcvsV3Config.contextId = powerConfigId; //use the power config ID created

// refer QnnHtpPerfInfrastructure.h
powerConfig.dcvsV3Config.powerMode = QNN_HTP_PERF_INFRASTRUCTURE_POWERMODE_PERFORMANCE_MODE;
powerConfig.dcvsV3Config.setSleepLatency = 1; //True to consider Latency parameter
powerConfig.dcvsV3Config.setBusParams = 1; //True to consider Bus parameter
powerConfig.dcvsV3Config.setCoreParams = 1; //True to consider Core parameter
powerConfig.dcvsV3Config.sleepDisable = 1; //True to disable sleep, False to re-enable sleep
powerConfig.dcvsV3Config.setSleepDisable = 1; //True to consider sleep disable/enable parameter

//Set Sleep latency parameter
powerConfig.dcvsV3Config.sleepLatency = 40; // set dsp sleep latency ranges 10-65535 micro sec, refer hexagon sdk

//set Bus Clock Parameters (refer QnnHtpPerfInfrastructure.h)
powerConfig.dcvsV3Config.busVoltageCornerMin = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.busVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.busVoltageCornerMax = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;

//set Core Clock Parameters (refer QnnHtpPerfInfrastructure.h)
powerConfig.dcvsV3Config.coreVoltageCornerMin = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.coreVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfig.dcvsV3Config.coreVoltageCornerMax = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;

--------

QnnHtpPerfInfrastructure_PowerConfig_t powerConfigHMX;
memset(&powerConfigHMX, 0, sizeof(powerConfigHMX));

powerConfigHMX.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_HMX_V2;
powerConfigHMX.hmxV2Config.hmxPickDefault = 0; // 1- HMX vote will scale with Dcvs Corner, 0- HMX vote needs to specified manually
powerConfigHMX.hmxV2Config.hmxPerfMode = QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH; //select max freq at target voltage corner, refer QnnHtpPerfInfrastructure.h

//set HMX clock parameters (refer QnnHtpPerfInfrastructure.h)
powerConfigHMX.hmxV2Config.hmxVoltageCornerMin = DCVS_EXP_VCORNER_TUR;
powerConfigHMX.hmxV2Config.hmxVoltageCornerTarget = DCVS_EXP_VCORNER_TUR;
powerConfigHMX.hmxV2Config.hmxVoltageCornerMax = DCVS_EXP_VCORNER_TUR;

// Initialize the CENG power config (Compression Engine clock settings)
QnnHtpPerfInfrastructure_PowerConfig_t powerConfigCENG;
memset(&powerConfigCENG, 0, sizeof(powerConfigCENG));

powerConfigCENG.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_CENG;
powerConfigCENG.cengConfig.cengPerfMode = QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH; //select max freq at target voltage corner, refer QnnHtpPerfInfrastructure.h

//set CENG clock parameters (refer QnnHtpPerfInfrastructure.h)
powerConfigCENG.cengConfig.cengVoltageCornerMin = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfigCENG.cengConfig.cengVoltageCornerTarget = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;
powerConfigCENG.cengConfig.cengVoltageCornerMax = DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER;

const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] = {&powerConfig, &powerConfigHMX, &powerConfigCENG, NULL};
Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}
return StatusCode::SUCCESS;
}

```

Note 
The API for HMX power setting has few limitations as listed below: 

- 
Only supports Hexagon v75 and later architectures. 
- 
To set the HMX vote, the client must create a DcvsV3 Context ID (powerConfig ID) first by calling createPowerConfigId() and use this powerConfig ID to set the DCVS_V3 vote. The client can then use the same powerConfig ID to request the HMX vote either in the same call or different call. 
- 
No independent HMX vote will be allowed from QNN API; the client will only be able to vote for the HMX when there is an active DCVS_V3 vote (legacy `DCVS_V3` or expanded `DCVS_V3_EXP`) on the same powerConfig ID. 
- 
If no DCVS_V3 vote is detected for a powerConfig ID, the HMX vote will be denied with error INVALID_INPUT. 
- 
If no HMX vote is provided for a powerConfig ID, the default HMX vote will be applied (see the table below). 
- 
Once the client places an explicit HMX vote, it is the client’s responsibility to set hmxPickDefault and make another call to setPowerConfig() if default behavior is desired. 
- 
HMX vote change or revert to default can be applied, provided the context ID has a valid DCVS_V3 vote. 
- 
When destroyPowerConfigId() is called with powerConfig ID, all votes associated with that context ID will be removed. 

Note 
The API for CENG power setting has the following limitations: 

- 
Only supports Hexagon v75 and later architectures. 
- 
To set the CENG vote, the client must create a DcvsV3 Context ID (powerConfig ID) first by calling `createPowerConfigId()` and use this powerConfig ID to set the CENG vote. The client can then use the same powerConfig ID to request the CENG vote either in the same call or a different call. 
- 
No independent CENG vote will be allowed from QNN API; the client will only be able to vote for CENG when there is an active DCVS_V3 vote (legacy `DCVS_V3` or expanded `DCVS_V3_EXP`) on the same powerConfig ID. 
- 
If no DCVS_V3 vote is detected for a powerConfig ID, the CENG vote will be denied with error INVALID_INPUT. 
- 
If no CENG vote is provided for a powerConfig ID, the default CENG vote will be applied (`QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH` with `DCVS_VOLTAGE_VCORNER_MIN_VOLTAGE_CORNER`). 
- 
When `destroyPowerConfigId()` is called with powerConfig ID, all votes associated with that context ID, including CENG votes, will be removed. 

DCVS_V3 
HMX 
HMX Pick Default 
CENG 
CENG Pick Default 
Operational Validity 

Vote Applied 
Vote Applied 
No 
Vote Applied 
No 
Valid 

No Vote 
Vote Applied 
N/A 
Vote Applied 
N/A 
Invalid 

Vote Applied 
No vote 
Yes 
No vote 
Yes 
Valid 

Vote Removed 
Vote Removed(Automatic) 
N/A 
Vote Removed(Automatic) 
N/A 
Valid 
Expanded Voltage Corners (V85+) 
Hexagon v85 introduces an expanded set of voltage corners and clock perf modes that give finer control over the power-performance curve. The expanded path adds two new `setPowerConfig` options: 

- 
`QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DCVS_V3_EXP` — expanded core/bus voltage corners and an expanded core/bus perf mode (HIGH, D1–D15, LOW). 
- 
`QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_CENG_EXP` — expanded CENG voltage corners and an expanded CENG perf mode. 
When to use expanded corners. Legacy `DCVS_V3` offers a small set of coarse voltage levels (SVS, NOM, TURBO, etc.) that are sufficient for most workloads. Expanded corners are useful when: 

- 
The application runs sustained workloads (e.g. continuous inference in an LLM pipeline) and needs to find an operating point that balances throughput against thermal headroom — the finer granularity lets you dial in a sweet spot that coarse legacy corners cannot reach. 
- 
The application is battery-sensitive and wants to run at the lowest voltage corner that still meets a latency target — intermediate corners (NOM_L0, TUR_L1, etc.) can reduce power without sacrificing performance. 
- 
The platform SKU maps a single voltage corner to multiple physical frequencies — the `corePerfMode` / `busPerfMode` / `cengPerfMode` fields let you select among those frequencies (see below). 
If none of these apply, legacy `DCVS_V3` / `CENG` remain a simpler choice with wider device compatibility and no fallback logic required. 
Two axes are exposed: 

- 
Voltage corner — `ExpVoltageCorner_t` values (`DCVS_EXP_VCORNER_*`, all `>= 0x100`) select the voltage rail point. Examples: `DCVS_EXP_VCORNER_NOM`, `DCVS_EXP_VCORNER_NOM_L0`, `DCVS_EXP_VCORNER_TUR_L3`. 
- 
Perf mode — `ClkPerfMode_t` values (`QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH`, `..._HIGH_D1` through `..._HIGH_D15`, `..._LOW`) select the target frequency within the chosen corner. 
Understanding perf mode (D1–D15). On some platform SKUs a single voltage corner maps to more than one physical clock frequency. The perf mode selects among those frequencies: `HIGH` requests the highest frequency available at the corner, `HIGH_D1` requests one step below, `HIGH_D2` two steps below, and so on through `HIGH_D15`, with `LOW` being the minimum frequency. Not all levels are necessarily physically distinct on every SKU — the hardware clamps to the nearest supported frequency. If a corner has only one frequency, all perf mode values produce the same result. Treat perf mode as a relative selector, not an absolute frequency guarantee. 
Discovery and fallback. Expanded corners require Hexagon v85 or later and a CDSP image that exposes the underlying HAP support. There is no separate “is expanded corners supported?” query API by design — the client discovers support by sending a `DCVS_V3_EXP` (or `CENG_EXP`) option and checking the return value: 

- 
The pipeline returns `QNN_HTP_PERF_INFRASTRUCTURE_ERROR_UNSUPPORTED` on pre-v85 architectures (host-side arch gate, no FastRPC traffic). 
- 
On v85+ with an older CDSP image, the DSP-side capability query returns `ERROR_UNSUPPORTED` once per Process Domain (the result is cached for the rest of the PD’s lifetime). 
Clients should treat `ERROR_UNSUPPORTED` as the signal to retry with the legacy `DCVS_V3` / `CENG` options. There is no transparent remapping; the fallback is the client’s responsibility. 

```
// Recommended fallback pattern: try expanded, fall back to legacy if unsupported.
const QnnHtpPerfInfrastructure_PowerConfig_t *expConfigs[] = {&powerConfigExp, NULL};
Qnn_ErrorHandle_t err = perfInfra.setPowerConfig(powerConfigId, expConfigs);
if (err == QNN_HTP_PERF_INFRASTRUCTURE_ERROR_UNSUPPORTED) {
// Device or CDSP does not support expanded corners — use legacy DCVS_V3
const QnnHtpPerfInfrastructure_PowerConfig_t *legacyConfigs[] = {&powerConfigLegacy, NULL};
err = perfInfra.setPowerConfig(powerConfigId, legacyConfigs);
}
if (err != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
}

```

DCVS_V3_EXP Power Settings 
`DCVS_V3_EXP` mirrors `DCVS_V3` but uses `ExpVoltageCorner_t` for the six corner fields and adds explicit `corePerfMode` / `busPerfMode` fields. `DCVS` is implicitly enabled when `DCVS_V3_EXP` is voted — the `setDcvsEnable` / `dcvsEnable` fields are not present in this struct. 

```
QnnHtpPerfInfrastructure_PowerConfig_t powerConfigExp;
memset(&powerConfigExp, 0, sizeof(powerConfigExp));

powerConfigExp.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DCVS_V3_EXP;
powerConfigExp.dcvsV3ExpConfig.contextId = powerConfigId; // use the power config ID created
powerConfigExp.dcvsV3ExpConfig.powerMode = QNN_HTP_PERF_INFRASTRUCTURE_POWERMODE_PERFORMANCE_MODE;
powerConfigExp.dcvsV3ExpConfig.setSleepLatency = 1;
powerConfigExp.dcvsV3ExpConfig.sleepLatency = 40;
powerConfigExp.dcvsV3ExpConfig.setSleepDisable = 1;
powerConfigExp.dcvsV3ExpConfig.sleepDisable = 1;
powerConfigExp.dcvsV3ExpConfig.setBusParams = 1;
powerConfigExp.dcvsV3ExpConfig.setCoreParams = 1;

// Expanded bus corners — finer-grained voltage points than legacy DCVS_V3
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerMin = DCVS_EXP_VCORNER_NOM;
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerTarget = DCVS_EXP_VCORNER_TUR_L1;
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerMax = DCVS_EXP_VCORNER_TUR_L3;

// Expanded core corners
powerConfigExp.dcvsV3ExpConfig.coreVoltageCornerMin = DCVS_EXP_VCORNER_SVS;
powerConfigExp.dcvsV3ExpConfig.coreVoltageCornerTarget = DCVS_EXP_VCORNER_NOM_L0;
powerConfigExp.dcvsV3ExpConfig.coreVoltageCornerMax = DCVS_EXP_VCORNER_TUR;

// Per-rail perf mode within the chosen corner — pick from HIGH, HIGH_D1 .. HIGH_D15, LOW
powerConfigExp.dcvsV3ExpConfig.corePerfMode = QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH_D3;
powerConfigExp.dcvsV3ExpConfig.busPerfMode = QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH_D1;

const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] = {&powerConfigExp, NULL};
Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}

```

Note 
The API for `DCVS_V3_EXP` power setting has the following limitations: 

- 
Only supports Hexagon v85 and later architectures. Pre-v85 architectures return `QNN_HTP_PERF_INFRASTRUCTURE_ERROR_UNSUPPORTED` from the host-side arch gate. 
- 
Additionally requires a CDSP image that exposes `HAP_dcvs_config` support. On a v85+ device running an older CDSP image, the call returns `ERROR_UNSUPPORTED` after a one-time runtime capability query (the result is cached for the lifetime of the Process Domain). 
- 
To set a `DCVS_V3_EXP` vote the client must first call `createPowerConfigId()` and pass the returned ID via `contextId` — the same flow as `DCVS_V3`. The `contextId` field in `DcvsV3Exp_t` must match the `powerConfigId` passed as the first argument to `setPowerConfig()`. 
- 
`DCVS` is implicitly enabled when `DCVS_V3_EXP` is voted; the `setDcvsEnable` and `dcvsEnable` fields are not present in `DcvsV3Exp_t`. Clients that need DCVS off must use the legacy `DCVS_V3` option. 
- 
`DCVS_V3_EXP` cannot be combined with legacy `DCVS_V3` in the same `setPowerConfig()` call — such a combination returns `INVALID_INPUT`. Use one option or the other within a given call. Across separate calls on the same powerConfig ID, the most recently voted variant supersedes the other (mutual exclusion: the cache for the older variant is erased). 
- 
All six corner fields (`busVoltageCornerMin/Target/Max`, `coreVoltageCornerMin/Target/Max`) must be either `0` (`DCVS_EXP_VCORNER_DISABLE`) or an `ExpVoltageCorner_t` value (`>= 0x100`). Passing a legacy `VoltageCorner_t` value (`0x01`–`0xFF`) returns `INVALID_INPUT` at the pipeline. 
- 
`corePerfMode` and `busPerfMode` must be in the range `[0, QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_MAX]` (i.e. `<= 0xFF`). Out-of-range values return `INVALID_INPUT`. 
- 
`DCVS_V3_EXP` may be combined freely with `HMX_V2`, `DDR_PERF_MODE`, legacy `CENG`, and `CENG_EXP` on the same powerConfig ID. `HMX_V2`, `DDR_PERF_MODE`, `CENG`, and `CENG_EXP` accept either a legacy `DCVS_V3` or an expanded `DCVS_V3_EXP` vote as their prerequisite. 
- 
`destroyPowerConfigId()` removes both legacy `DCVS_V3` and `DCVS_V3_EXP` votes for the ID. 
- 
Automatic recovery after CDSP SubSystem Restart (SSR): QNN automatically replays the most recently applied `DCVS_V3_EXP` vote when the CDSP recovers from a crash, provided the architecture still supports expanded corners. No client action is required for normal SSR recovery. If the recovered CDSP image has downgraded capability (rare — requires firmware rollback across SSR), the replay is skipped and the client receives `ERROR_UNSUPPORTED` on the next `setPowerConfig()` call. In this case the client must re-issue power configuration using legacy corners. 
CENG_EXP Power Settings 
`CENG_EXP` mirrors legacy `CENG` but uses `ExpVoltageCorner_t` for the three CENG corner fields and extends `cengPerfMode` to the full HIGH / HIGH_D1–HIGH_D15 / LOW range. 
A `DCVS_V3` or `DCVS_V3_EXP` vote must be active on the same powerConfig ID before `CENG_EXP` can be set. The DCVS vote may be placed in the same `setPowerConfig()` call or in a prior call. 

```
// Prerequisite: a DCVS_V3 or DCVS_V3_EXP vote must already be active for this
// powerConfigId (either from a prior setPowerConfig call or included in this array).

QnnHtpPerfInfrastructure_PowerConfig_t powerConfigCengExp;
memset(&powerConfigCengExp, 0, sizeof(powerConfigCengExp));

powerConfigCengExp.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_CENG_EXP;
powerConfigCengExp.cengExpConfig.cengVoltageCornerMin = DCVS_EXP_VCORNER_SVS;
powerConfigCengExp.cengExpConfig.cengVoltageCornerTarget = DCVS_EXP_VCORNER_NOM;
powerConfigCengExp.cengExpConfig.cengVoltageCornerMax = DCVS_EXP_VCORNER_TUR;
powerConfigCengExp.cengExpConfig.cengPerfMode = QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_HIGH_D2;

// Example: DCVS_V3_EXP + CENG_EXP in one call (DCVS_V3_EXP satisfies the prerequisite)
const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] =
{&powerConfigExp, &powerConfigCengExp, NULL};
Qnn_ErrorHandle_t perfInfraErr = perfInfra.setPowerConfig(powerConfigId, powerConfigs);
if (perfInfraErr != QNN_SUCCESS) {
QNN_ERROR("setPowerConfig failed");
return StatusCode::FAILURE;
}

```

Note 
The API for `CENG_EXP` power setting has the following limitations: 

- 
Only supports Hexagon v85 and later architectures. 
- 
Requires the same CDSP capability as `DCVS_V3_EXP`; the runtime capability query is shared between the two options. 
- 
To set a `CENG_EXP` vote, the client must first place an active DCVS_V3 vote on the same powerConfig ID — either legacy ``DCVS_V3`` or expanded ``DCVS_V3_EXP`` is accepted. If neither is present the call returns `INVALID_INPUT`. 
- 
`CENG_EXP` cannot be combined with legacy `CENG` in the same `setPowerConfig()` call — `INVALID_INPUT` is returned. Across separate calls on the same powerConfig ID, the most recently voted variant supersedes the other. 
- 
All three corner fields must be `0` (`DCVS_EXP_VCORNER_DISABLE`) or an `ExpVoltageCorner_t` value (`>= 0x100`). Legacy values return `INVALID_INPUT`. 
- 
`cengPerfMode` must be in the range `[0, QNN_HTP_PERF_INFRASTRUCTURE_CLK_PERF_MAX]`. 
- 
`destroyPowerConfigId()` removes both legacy `CENG` and `CENG_EXP` votes for the ID. 
DDR_PERF_MODE — interaction with DCVS_V3 and DCVS_V3_EXP 
The `DDR_PERF_MODE` option controls DDR clock frequency selection. It has prerequisites that apply to both the legacy and expanded DCVS paths. 

```
// DDR_PERF_MODE requires a DCVS_V3 or DCVS_V3_EXP vote with all bus corners at MAX.
// Example with DCVS_V3_EXP:

QnnHtpPerfInfrastructure_PowerConfig_t powerConfigExp;
memset(&powerConfigExp, 0, sizeof(powerConfigExp));
powerConfigExp.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DCVS_V3_EXP;
powerConfigExp.dcvsV3ExpConfig.contextId = powerConfigId;
powerConfigExp.dcvsV3ExpConfig.setBusParams = 1;
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerMin = DCVS_EXP_VCORNER_MAX;
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerTarget = DCVS_EXP_VCORNER_MAX;
powerConfigExp.dcvsV3ExpConfig.busVoltageCornerMax = DCVS_EXP_VCORNER_MAX;

QnnHtpPerfInfrastructure_PowerConfig_t ddrConfig;
memset(&ddrConfig, 0, sizeof(ddrConfig));
ddrConfig.option = QNN_HTP_PERF_INFRASTRUCTURE_POWER_CONFIGOPTION_DDR_PERF_MODE;
ddrConfig.ddrPerfModeConfig = 1; // DDR perf mode value

const QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] =
{&powerConfigExp, &ddrConfig, NULL};
perfInfra.setPowerConfig(powerConfigId, powerConfigs);

```

Note 
The API for `DDR_PERF_MODE` power setting has the following limitations: 

- 
Requires an active DCVS_V3 vote on the same powerConfig ID — either legacy `DCVS_V3` or expanded `DCVS_V3_EXP` is accepted. If neither is present the call returns `INVALID_INPUT`. 
- 
Requires the bus voltage corner to be at maximum on all three of `busVoltageCornerMin`, `busVoltageCornerTarget`, and `busVoltageCornerMax` of the underlying DCVS_V3 vote. The “maximum” value depends on which DCVS variant supplied the prerequisite: 

- 
With legacy `DCVS_V3`: `DCVS_VOLTAGE_VCORNER_MAX_VOLTAGE_CORNER`. 
- 
With expanded `DCVS_V3_EXP`: `DCVS_EXP_VCORNER_MAX`. 
- 
`destroyPowerConfigId()` removes `DDR_PERF_MODE` votes alongside the rest of the powerConfig ID’s state. 
Operational Validity (V85+, Expanded Path) 
The table below mirrors the legacy validity table for the expanded path. The DCVS_V3 column accepts either `DCVS_V3` or `DCVS_V3_EXP` as a satisfying vote; the HMX and CENG columns refer to `HMX_V2` and `CENG_EXP` respectively. 

DCVS_V3 / DCVS_V3_EXP 
HMX_V2 
HMX Pick Default 
CENG_EXP 
CENG Pick Default 
Operational Validity 

Vote Applied (DCVS_V3_EXP) 
Vote Applied 
No 
Vote Applied 
No 
Valid 

Vote Applied (DCVS_V3 legacy) 
Vote Applied 
No 
Vote Applied (CENG_EXP) 
No 
Valid (cross-mix allowed) 

No Vote 
Vote Applied 
N/A 
Vote Applied 
N/A 
Invalid 

Vote Applied (either) 
No vote 
Yes 
No vote 
Yes 
Valid 

Vote Applied (DCVS_V3_EXP) 
Vote Applied 
No 
Vote Applied (legacy CENG) 
No 
Valid (cross-mix allowed) 

Vote Removed 
Vote Removed (Automatic) 
N/A 
Vote Removed (Automatic) 
N/A 
Valid 
Use Case Examples 

`Voting at Every Inference` - This case depicts simple use case of setting certain performance setting (possibly higher performance configuration) before executing inference request, followed by another performance setting (possibly lower performance configuration). Figure below shows the call flow of setting performance setting at every inference. 

`Sustain Setting for Multiple Inference` - This case depicts the sustenance of performance setting (possibly higher performance configuration) for multiple inferences. This can be achieved using system timer. Client can start a timer for certain duration (higher than expected time between successive inferences) after setting performance vote (possibly higher performance). This vote gets reset (possibly with lower performance) either when timer expires or when client requests to change the performance settings. Figure below shows the call flow of sustaining performance setting for multiple inferences. 

## QNN HTP Precision

QNN HTP supports running graphs having a mix of floating-point and fixed-point data types. 
QNN HTP can support running float32 graphs using float16 math on select Qualcomm SoCs. The client is expected to set up the QNN graph with float32 tensors and QNN HTP accelerator will finalize and execute the QNN graph using float16 math. 

Note 
QNN_HTP_GRAPH_CONFIG_OPTION_PRECISION is deprecated starting from 2.35 release. If you are using and SDK version >=2.35, there is no need to set this option. 
QNN HTP backend will convert user provided float32 inputs in QnnGraph_execute() to float16 and execute the graph with float16 math. The final output is provided to user as float32 outputs. 

Note 
Please note that, float32 math is not supported by QNN HTP. 

## QNN HTP FP16 output difference between SM8550 and SM8650

The outputs of floating point models on HTP backend will be slightly different between SM8550 and SM8650. This may lead to slight accuracy difference between these two although one is not more accurate than the other. This is because of the changes in the hardware which changed the associativity of some of the computations to achieve higher efficiency. 

Note 
This same point can also be found in 2.9.1 release notes on the “HTP Float16” slide. 

## QNN HTP Deep Learning Bandwidth Compression (DLBC)

Deep Learning Bandwidth Compression is a feature that allows inputs to be compressed so the processing bandwidth can be lowered. QNN HTP provides a configuration option for users to turn ON or OFF DLBC through client usage like below: 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_OPTIMIZATION;
3 customConfig.optimizationOption.type = QNN_HTP_GRAPH_OPTIMIZATION_TYPE_ENABLE_DLBC;
4 customConfig.optimizationOption.floatValue = 1.0; // set to 0 to turn off
5
6 QnnGraph_Config_t graphConfig;
7 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
8 graphConfig.customConfig = &customConfig;
9
10 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

For offline preparation with DLBC, the backend-specific config file should specify the following option along with any other desired options: 

```
{
"graphs": [
{
"vtcm_mb": ...,
"graph_names": ['...'],
"dlbc": 1 // set to 1 to turn on
...
}
],
"devices": [
{
...
...
}
]
}

```

Value of 0 will turn OFF the feature and any positive floating point value greater than or equal to 1.0 will turn ON the feature. By default DLBC will be in disabled state i.e. when configuration option is not provided. 
DLBC allows weight data to be compressed to lower processing bandwidth. QNN HTP provides a configuration option for clients to turn enable or disable DLBC weights. 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_OPTIMIZATION;
3 customConfig.optimizationOption.type = QNN_HTP_GRAPH_OPTIMIZATION_TYPE_ENABLE_DLBC_WEIGHTS;
4 customConfig.optimizationOption.floatValue = 1.0; // set to 0 to turn off
5
6 QnnGraph_Config_t graphConfig;
7 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
8 graphConfig.customConfig = &customConfig;
9
10 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

For offline preparation with DLBC, the backend-specific configuration should specify the `dlbc_weights` option along with any other options. 

- 
0 – Disables DLBC weights; default when option is not provided 
- 
>= 1 – Enables DLBC weights 

```
{
"graphs": [
{
"vtcm_mb": ...,
"graph_names":['...'],
"dlbc_weights": 1 // set to 0 to turn off, dlbc for weights
...
}
],
"devices": [
{
...
...
}
]
}

```

Compression for inputs and weights can be independently set. 

### Limitations

- 
Only supported for offline preparation. 
- 
Not supported with weight sharing. 
- 
Not supported with spillfill buffer sharing. 
- 
The number of graphs supported depends on whether compression is enabled for inputs and weights; graphs supported: 

- 
32 – Input OR weight compression 
- 
16 – Input AND weight compression 

Note 
The DLBC Weights with Weight Sharing feature is not supported. Starting with release 2.36, creating binaries with both DLBC Weights and Weight Sharing enabled will not be supported. Any binaries prepared before 2.36 with both features enabled will have DLBC WTS turned off regardless of the settings. To use DLBC WTS with such binaries, re-prepare without Weight Sharing. 
Target-specific support: DLBC Weights with Weight Sharing is only supported on select targets where hardware capability is present. On these targets, binaries may be prepared with both DLBC Weights and Weight Sharing enabled. Note that enabling DLBC weights on any graph which shares weights will enable DLBC on shared weights across all graphs. 

## QNN HTP Sparse Weights Compression

Sparse Weights Compression allows sparse weights data to be compressed which results in a reduced memory storage footprint in DDR and VTCM. QNN HTP provides a configuration option for clients to turn enable or disable sparse weights compression 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_OPTIMIZATION;
3 customConfig.optimizationOption.type = QNN_HTP_GRAPH_OPTIMIZATION_TYPE_ENABLE_SPARSE_WEIGHTS_COMPRESSION;
4 customConfig.optimizationOption.floatValue = 1.0; // set to 0 to turn off
5
6 QnnGraph_Config_t graphConfig;
7 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
8 graphConfig.customConfig = &customConfig;
9
10 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

For offline preparation with Sparse Weights Compression, the backend-specific configuration should specify the `sparse_weights_compression` option along with any other options. 

- 
0 – Disables Sparse Weights Compression; default when option is not provided 
- 
>= 1 – Enables Sparse Weights Compression 

```
{
"graphs": {
"vtcm_mb":...,
"graph_names":[...],
"sparse_weights_compression": 1 // set to 0 to turn off
...
},
"devices": [
{
...
...
}
]
}

```

Note that the above config structure will be deprecated SDK 2.20 release onwards, the new config supported is shown below: 

```
{
"graphs": [
{
"vtcm_mb":...,
"graph_names":[...],
"sparse_weights_compression": 1 // set to 0 to turn off
...
}
],
"devices": [
{
...
...
}
]
}

```

At model preparation time, the amount of memory saved from compression can be seen in the qnn-context-binary-generator output as: 

```
spare weights compression bytes saved: ....

```

Note 
The Sparse Weights Compression feature has certain limitations: 

- 
Only supported for automotive Snapdragon devices SA8255, SA8620P 
- 
Only supports offline prepare. 

## QNN UBWC (Universal Bandwidth Compression)

Universal Bandwidth Compression (UBWC) is a bandwidth compression scheme which improves effective throughput to system memory. UBWC is supported only on image data that are input or output tensors of the network. 
Preparation (Model Conversion) 
There are no changes to model conversion required to handle UBWC. The graph is converted and quantized with uncompressed data frames. 
Preparation (Context Binary Generation) 
For preparation, clients specify the UBWC pixel format of the graph tensors that are compressed as follows using the data format configuration parameter to qnn-context-binary-generator. UBWC can be enabled independently on input and output tensors. In this example, a model takes compressed RGBA8888 data as input and outputs compressed RGBBA8888. 

Another example configuration is shown below where the model takes in NV12 frame as two separate tensors consisting of the Y and UV planes. 

```
{
"graphs": [
"graph_name":"Test_graph",
"tensors": [
{
"tensor_name": "Input1",
"dataFormat": "QNN_TENSOR_DATA_FORMAT_UBWC_NV12_Y"
}
{
"tensor_name": "Input2",
"dataFormat": "QNN_TENSOR_DATA_FORMAT_UBWC_NV12_UV"
}
}
]
}

```

Execution 
The following figure illustrates the call flow for a model that takes a single RGBA8888 frame. 

Here is another example of a call flow for a model that takes a single NV12 frame as 2 separate input tensors consisting of the Y and UV planes. 

Note 
The UBWC feature has the following limitations: 

- 
Only supported on select automotive SOCs. 
- 
Only supports offline prepare. 
- 
Only supported with shared buffers. 

## QNN HTP - Setting Number of HVX Threads

This option allows user to set number of HVX thread(s) for a particular graph. The inference time depends on the number of HVX threads utilized. If more threads are used, the execution time of a graph will be lower (i.e. faster). 
Number of HVX threads can be configured for both online and offline prepare cases. The value passed in the config during binary blob creation is what gets written in the serialized blob. Number of HVX threads can be re-configured by passing a new config to `QnnGraph_setConfig` QNN API. 
It is important to note that number of threads can not be configured/re-configured after the first execution of that particular graph; it has to be prior to it. 
Users can set the custom option as such: 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_NUM_HVX_THREADS;
3 customConfig.numHvxThreads = 3; // set a number. MAX = number of HVX HW blocks for that SoC
4
5 QnnGraph_Config_t graphConfig;
6 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
7 graphConfig.customConfig = &customConfig;
8
9 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

The backend-specific config file should specify the following option along with any other desired options. In the case of offline prepare, If “hvx_threads” option is not provided, a default value of 4 is written to the binary blob. In the case of online prepare, if a config does not set any number of hvx thread(s), max supported value for that SoC is used during an inference. 
Config can be used to set number of HVX threads as such: 

```
{
"graphs": [
{
"vtcm_mb": ...,
"graph_names":['...'],
"hvx_threads": 3 // set a number. MAX = number of HVX HW blocks for that SoC
...
}
],
"devices": [
{
...
...
}
]
}

```

## QNN HTP - Enabling the system level cache allocator

This option allows user to enable the usage of the System Level Cache Allocator for a given graph. It will help the by saving overall bandwith on the use case. 
Users can set the custom option as such: 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_OPTIMIZATION;
3 customConfig.optimizationOption.type = QNN_HTP_GRAPH_OPTIMIZATION_TYPE_ENABLE_SLC_ALLOCATOR;
4 customConfig.optimizationOption.floatValue = 1;
5
6 QnnGraph_Config_t graphConfig;
7 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
8 graphConfig.customConfig = &customConfig;
9
10 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

The feature is only supported by specific SOCs. By default the option is turned off 
Config can be used to set the option as such: 

```
{
"graphs": [
{
"vtcm_mb": ...,
"graph_names":['...'],
"slc_alloc_enable": 1
...
}
],
"devices": [
{
"soc_id": 69, //representing the soc
...
...
}
]
}

```

Note 
This option can be configured for offline prepare cases. This option can’t be modified during inference. 
However, it is possible to force the disablement of the feature during execution Users can set the custom option as such: 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_OPTIMIZATION_TYPE_ENABLE_SLC_ALLOCATOR;
3 customConfig.optimizationOption.floatValue = 0;
4
5 QnnGraph_Config_t graphConfig;
6 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
7 graphConfig.customConfig = &customConfig;
8
9 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

Note 
To restore the previous state, the same sequence should be called with 1 as a value. 

## QNN HTP Backend Extensions

The qnn-net-run utility is backend agnostic, meaning it can only use generic QNN APIs. The backend extension feature facilitates usage of the backend specific APIs, namely custom configurations. More documentation on backend extensions can be found under qnn-net-run. Note that the scope of QNN backend extensions is limited to qnn-net-run and qnn-context-binary-generator. HTP Backend Extensions is an interface to provide custom options to HTP Backend. It is also required to enable different performance modes. These options and performance modes can be exercised by providing an extension shared library `libQnnHtpNetRunExtensions.so` and a config file, if necessary. 
To use backend extension related parameters with qnn-net-run, use `--config_file` argument and give path to JSON file. 

```
$ qnn-net-run --model <qnn_model_name.so> \
--backend <path_to_model_library>/libQnnHtp.so \
--output_dir <output_dir_for_result> \
--input_list <path_to_input_list.txt>
--config_file <path to JSON of backend extensions>

```

The above config file with minimum parameters to use backend extensions config is shown below: 

```
{
"backend_extensions" :
{
"shared_library_path" : "path_to_shared_library", // give path to shared extensions library (.so)
"config_file_path" : "path_to_config_file" // give path to backend config
}
}

```

Users can set the custom options and different performance modes to HTP Backend through the backend config. The various options available in the config are shown below: 

```
{
"type": "object", "properties": {
"graphs": {
"type": "array", "items": {
"type": "object", "properties": {

// Corresponds to the graph name provided to QnnGraph_create
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"graph_names": {"type": "array", "items": {"type": "string"}},

// Provides performance infrastructure configuration options that are memory specific [optional]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
// To use a device's maximum VTCM amount, set the value to 0 (QNN_HTP_GRAPH_CONFIG_OPTION_MAX)
// and specify the target SoC through the device config.
"vtcm_mb": {"type": "integer"},

// Corresponds to the number of HVX threads to use for a particular graph during an inference.
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"hvx_threads": {"type": "integer"},

// Set Graph optimization value. Valid values are 2 and 3 [optional] [default: 2]
// Higher optimization levels incur longer offline prepare time but yield more optimal graph and hence faster execution time for most graphs
// Note: Optimization level 1 is reserved for internal use only
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"O": {"type": "number", "multipleOf": 1},

// Provide deep learning bandwidth compression value 0 or 1 [optional] [default: 0]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"dlbc": {"type": "number", "multipleOf": 1},

// Specifies whether to enable weights packing [optional] [default: false]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"weights_packing": {"type": "boolean"},

// Specifies the number of cores the graph will use for execution [optional] [default: 1]
// Used by qnn-context-binary-generator during offline preparation
"num_cores": {"type": "integer"},

// Specifies whether to configure short depth convolution for the graph [optional] [default: false]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"short_depth_conv_on_hmx_off": {"type": "boolean"},

// Specifies whether to configure fold relu activation for the graph [optional] [default: false]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"fold_relu_activation_into_conv_off": {"type": "boolean"},

// Specifies whether to enable or disable fusion of convolution operations with advanced activation functions
// such as sigmoid, tanh, gelu, and swish. [optional] [default: true]
// When enabled, it may improve performance in floating-point models by reducing computational overhead.
// When disabled, it may improve accuracy in floating-point models at the cost of performance.
// Note that this option has no effect on quantized graphs.
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"advanced_activation_fusion": {"type": "boolean"},

// Specifies whether to configure use high precision fp16 sigmoid for the graph [optional] [default: false]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"use_high_precision_fp16_sigmoid": {"type": "boolean"},

// Specifies whether to configure monolithic lstm for the graph [optional] [default: false]
// Used by qnn-net-run during online prepare and qnn-context-binary-generator uses it during offline preparation
"monolithic_lstm": {"type": "boolean"}
}
}
},
"devices": {
"type": "array", "items": {
"type": "object", "properties": {

// Selection of the device [optional] [default: 0]
// Used by qnn-net-run
"device_id": {"type": "integer"},

// Select the core [optional] [default: 0]
// Used by qnn-net-run to select among the cores available in a device
"core_id": {"type": "array", "items": {"type": "integer"}},

// Select the available core type [optional] [default: 0]
// Used by qnn-net-run, 0 - NSP, 1 - HPASS
"core_type": {"type": "integer"},

// Selection of the SoC [optional] [default: 0]
// Used by qnn-net-run and qnn-context-binary-generator
"soc_id": {"type": "integer"},

// Selection of the SoC model [optional] [default: 0]
// Used by qnn-net-run and qnn-context-binary-generator
"soc_model": {"type": "integer"},

// Set dsp architecture value [optional] [default: NONE]
// Used by qnn-net-run and qnn-context-binary-generator
"dsp_arch": {"type": "string"},

// Specifies the user pd attribute [optional] [default: "unsigned"]
// Used by qnn-net-run and qnn-context-binary-generator
"pd_session": {"type": "string"},

// Used for linting profiling level [optional] [default: not set]
// Used by qnn-net-run and qnn-context-binary-generator
"profiling_level": {"type": "string"},

// Specifies whether to use null context or not. true means using a unique power context id, and false means using null context.
// NOTE: This parameter is not supported for v68 onwards
// Used by qnn-net-run
"use_client_context": {"type": "boolean"},
"cores": {
"type": "array", "items": {
"type": "object", "properties": {

// Provide performance profile [optional] [default: "high_performance"]
// Used by qnn-net-run
// Valid values: burst, sustained_high_performance, high_performance, balanced,
// low_balanced, high_power_saver, power_saver, low_power_saver, extreme_power_saver,
// system_settings, llm_decode_burst, llm_decode_sustained_high_performance,
// llm_decode_high_performance, llm_decode_balanced, llm_decode_low_balanced,
// llm_decode_high_power_saver, llm_decode_power_saver, llm_decode_low_power_saver,
// llm_decode_extreme_power_saver, llm_decode_default.
// Note: This perf profile will be overridden by any profiles specified via the command line option --perf-profile
"perf_profile": {"type": "string"},

// Rpc control latency value in micro second [optional] [default: 100us]
// Used by qnn-net-run
"rpc_control_latency": {"type": "integer"},

// Rpc polling time value in micro second [optional]
// [default: 9999 us for burst, high_performance & sustained_high_performance, 0 us for other perf profiles]
// Used by qnn-net-run
"rpc_polling_time": {"type": "integer"},

// Hmx timeout value in micro second [optional] [default: 300000us]
// Used by qnn-net-run
"hmx_timeout_us": {"type": "integer"},

// Adaptive polling time value in micro second [optional] [default: 0 us]
// Used by qnn-net-run
"adaptive_polling_time": {"type": "integer"}
}
}
}
}
}
},
"context": {
"type": "object", "properties": {

// Used for enabling Weight Sharing [optional] [default: false]
// Used by qnn-context-binary-generator during offline preparation
"weight_sharing_enabled": {"type": "boolean"},

// Used to associate max spill-fill buffer size across multiple contexts within a group [optional] [default: Not Set]
// Used by qnn-net-run and qnn-throughput-net-run during offline preparation. group_id value must be set to 0 for this option to be used.
"max_spill_fill_buffer_for_group": {"type": "integer"},

// Specifies the group id to which contexts can be associated [optional] [default: None]
// Used by qnn-net-run and qnn-throughput-net-run during offline preparation.
"group_id": {"type": "integer"},

// Used to set read memory budget size in Mb [optional] [default: 0]
// Used by qnn-net-run and qnn-throughput-net-run when using a serialized binary for graph preparation.
"file_read_memory_budget_in_mb": {"type": "integer"}

// Used to enable I/O memory estimation [optional] [default: false]
// Used by qnn-net-run and qnn-throughput-net-run when creating context from a serialized context binary.
"io_memory_estimation": {"type": "boolean"}

// Used to enable init acceleration [optional] [default: false]
// Used by qnn-net-run and qnn-throughput-net-run when creating context from a serialized context binary.
"init_acceleration": {"type": "boolean"}

// Used to provide path to a read&writeable concurrent deserialization patch file [optional] [default: NONE]
// Used by qnn-net-run and qnn-throughput-net-run when creating context from a serialized context binary.
"concurrent_deserialization_patch": {"type": "string"}

// Used for enabling Lora Weight Sharing [optional] [default: false]
// Used by qnn-context-binary-generator during offline preparation
"lora_weight_sharing": {"type": "boolean"}

// Used for enabling Lora Weight Sharing Ram Preload [optional] [default: false]
// Used by qnn-net-run and qnn-throughput-net-run when creating context from a serialized context binary.
"lora_weight_sharing_ram_preload": {"type": "boolean"}

// Used to set reused IO Buffer size in Mb [optional] [default: 0]
// Used by qnn-net-run and qnn-throughput-net-run when creating context from a serialized context binary.
"reused_io_limit_mb": {"type": "integer"}
}
},
"groupContext": {
"type": "object", "properties": {

// Used to enable shared resources across different contexts [optional] [default: false]
// Used by qnn-net-run and qnn-throughput-net-run when creating multiple contexts from a list of serialized context binaries.
"share_resources": { "type": "boolean"}

// Used to set reused IO Buffer size in Mb for the group [optional] [default: 0]
// Used by qnn-net-run and qnn-throughput-net-run when creating multiple contexts from a list of serialized context binaries.
"reused_io_limit_mb": { "type": "integer"}
}
},
"memory": {
"type": "object", "properties": {

// Use multi-tensor shared buffers for input/output [optional] [default: QNN_HTP_MEM_UNDEFINED], Refer QnnHtpMem_Type_t
// Used by qnn-net-run and qnn-throughput-net-run
"mem_type": {"type": "string", "enum": ["shared_buffer"] }
}
}
}
}

```

Note 

- 
soc_id parameter will be deprecated, For setting the Soc use soc_model parameter. 
- 
Qnn_SocModel_t will be deprecated, For setting Soc Model refer to the Supported Snapdragon Devices 
- 
fp16_relaxed_precision is deprecated starting from 2.35.0 release. Moving forward, there is no need to set this parameter for fp functionality and it will be determined based on SoC support. 
Backend extensions performance modes can be enabled using `perf_profile` parameter through backend config as shown above. Valid settings are low_balanced, balanced, high_performance, sustained_high_performance, burst, low_power_saver, power_saver, high_power_saver, extreme_power_saver, system_settings, llm_decode_burst, llm_decode_sustained_high_performance, llm_decode_high_performance, llm_decode_balanced, llm_decode_low_balanced, llm_decode_high_power_saver, llm_decode_power_saver, llm_decode_low_power_saver, llm_decode_extreme_power_saver and llm_decode_default. Note that these performance modes are user defined and customers can choose to define their own performance modes according to their needs using QNN APIs. 
These performance modes use different configurations of core clocks, bus clocks, DCVS participation algorithms and sleep latencies. There are 3 types of voltage corners defined as TURBO, NOM and SVS which further have different voltage levels. Apart from these, there are MAX and MIN voltage corners which sets the frequency to maximum and minimum frequency supported on target. For further details on the performance modes configuration and parameter details, refer hexagon sdk documentation. These settings used by different performance modes defined above are shown in table below: 

BURST 
SUSTAINED_HIGH_PERFORMANCE 
HIGH_PERFORMANCE 
BALANCED 
LOW_BALANCED 
HIGH_POWER_SAVER 
POWER_SAVER 
LOW_POWER_SAVER 
EXTREME_POWER_SAVER 
RELAXED_POWER_STATE2 
RELEASED_POWER_STATE2 

`sleepLatency` 
40 us 
100 us 
100 us 
1000 us 
1000 us 
1000 us 
1000 us 
1000 us 
1000 us 
2000 us 
65535 us 

`dcvsEnable` 
False 
False 
False 
False 
False 
False 
False 
False 
False 
True 
True 

`RPC Polling`1 
ON 
ON 
ON 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 

`busVCornerMin` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS2 
MIN_VOLTAGE_CORNER 

`busVCornerTarget` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS 
MIN_VOLTAGE_CORNER 

`busVCornerMax` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS 
MIN_VOLTAGE_CORNER 

`coreVCornerMin` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS2 
MIN_VOLTAGE_CORNER 

`coreVCornerTarget` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS 
MIN_VOLTAGE_CORNER 

`coreVCornerMax` 
MAX_VOLTAGE_CORNER 
TURBO 
TURBO 
NOM_PLUS 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
SVS 
MIN_VOLTAGE_CORNER 

Note 
1 Default RPC Polling time when switched ON is 10 milli-seconds. 
1 RPC Polling is enabled by default only for Burst, sustained_high_performance and high_performance profiles on non-windows platform. 
1 RPC Polling is enabled by default only for Burst profile on windows platform. 
2 RELAXED_POWER_STATE and RELEASED_POWER_STATE are internally applied based on performance profile to lower the votes. These are not configurable to user. 
Above table is ordered from highest performance (BURST) to lowest performance (EXTREME_POWER_SAVER). BURST and SUSTAINED_HIGH_PERFORMANCE uses a timer during execution which helps in keeping the vote high for all inferences and avoids subsequent up-down of perf votes until timeout. They have low sleep latency, RPC polling is enabled and DCVS is disabled during execution. Note that DCVS if enabled can both increase and decrease the core/bus clock speeds while min_corner and max_corner votes are used as lower and upper limit thresholds for DCVS. BURST has the highest frequency and it sustains high voting, which gives the best performance. 
HIGH_PERFORMANCE mode however do not sustain votes during multiple inferences instead it moves to idle state RELAXED_POWER_STATE in between inferences which reduces CPU power consumption. POWER_SAVER, LOW_POWER_SAVER and HIGH_POWER_SAVER have low frequencies, high sleep latencies and moves to idle state RELEASED_POWER_STATE in between inferences. EXTREME_POWER_SAVER is the lowest performing performance mode and saves the highest power. 
There are 3 stages to graph execution i.e INIT, INFERENCE and DE-INIT. Above defined performance modes will be applied to graph before each stage i.e INIT, INFERENCE and DE-INIT as well. After each stage completion, the lower votes will be applied i.e RELAXED_POWER_STATE or RELEASED_POWER_STATE according to the performance mode selected by the user. 
Below config can be used to set HTP performance profile and rpc polling time: 

```
{
...
"devices": [{
...
"cores":[{
"perf_profile": "burst", // use this to set any of the above performance profile
"rpc_polling_time": 9999, // use this to set rpc polling, ranges 0-9999 us
"rpc_control_latency": 100 // use to set rpc control latency
}]
}]
}

```

New in version 2.48. 

Note 
LLM Decode performance modes are supported on Hexagon V79 and newer architectures (V79, V81, V85, V89). Selecting an `llm_decode_*` mode on an older architecture will be rejected at runtime with a clear error. 

### Why LLM Decode Modes?

Large Language Model inference has two distinct compute phases with fundamentally different hardware utilization patterns: 
Prefill (encode): Processes the input prompt in parallel. This phase is compute-bound — it heavily utilizes HMX (matrix multiply) and HVX (vector) units. Traditional performance modes (BURST, BALANCED, etc.) which scale all clock domains proportionally are well-suited for this phase. 
Decode (token generation): Generates output tokens one at a time, autoregressively. Each decode step processes a single token against the full KV-cache. This phase is memory-bandwidth-bound — performance is gated by how fast weights and KV-cache data can be fetched from DDR, not by compute throughput. HMX utilization is low because the matrix multiplications are narrow (batch=1). 
The `llm_decode_*` modes exploit this insight by decoupling HMX frequency from DDR bandwidth: 

- 
HMX is reduced aggressively across all tiers (since it is underutilized during decode) 
- 
DDR (bus) and HVX (core) are scaled per tier; see the table below for exact votes 
- 
CENG (compute-engine bus) is tuned per tier to avoid becoming a bottleneck 
- 
`DDR Perf Mode` is an additional toggle that unlocks a few extra cycles of DDR performance when used at the higher corners; it is enabled only for `llm_decode_burst` 
This decoupled voting strategy achieves significantly better tokens-per-watt (Tok/W) efficiency compared to traditional modes, because power-hungry compute units are not clocked higher than necessary while memory bandwidth — the actual bottleneck — is preserved. 

Note 
These modes may not represent optimal operating points for: 

- 
LLM prefill/encode phase (compute-bound, requires different frequency profile) 
- 
Large Vision Models (LVM) such as Stable Diffusion 
- 
Other workload classes 
For prefill and LVM workloads, use the traditional performance modes (BURST, BALANCED, etc.) until dedicated profiles are available. 

### Available Modes

Available `llm_decode_*` modes (ordered highest to lowest performance): 

- 
`llm_decode_burst` 
- 
`llm_decode_sustained_high_performance` 
- 
`llm_decode_high_performance` 
- 
`llm_decode_balanced` 
- 
`llm_decode_low_balanced` 
- 
`llm_decode_high_power_saver` 
- 
`llm_decode_power_saver` 
- 
`llm_decode_low_power_saver` 
- 
`llm_decode_extreme_power_saver` 
- 
`llm_decode_default` (alias for llm_decode_balanced) 

### Configuration

Each mode maps to the following set of hardware votes (HVX core, HMX, CENG, DDR bus, sleep latency). These values are fixed per release. Voltage corner vocabulary (TURBO, NOM, SVS, MAX, etc.) follows the same scheme as the traditional modes above; refer to the Hexagon SDK documentation for further details. 

LLM_DECODE_BURST 
LLM_DECODE_SUSTAINED_HIGH_PERFORMANCE 
LLM_DECODE_HIGH_PERFORMANCE 
LLM_DECODE_BALANCED 
LLM_DECODE_LOW_BALANCED 
LLM_DECODE_HIGH_POWER_SAVER 
LLM_DECODE_POWER_SAVER 
LLM_DECODE_LOW_POWER_SAVER 
LLM_DECODE_EXTREME_POWER_SAVER 
LLM_DECODE_DEFAULT 

`sleepLatency` 
40 us 
100 us 
100 us 
1000 us 
1000 us 
1000 us 
1000 us 
1000 us 
1000 us 
1000 us 

`busVoltageCorner` 
MAX_VOLTAGE_CORNER 
TURBO_PLUS 
TURBO 
NOM 
NOM 
NOM 
SVS_PLUS 
SVS2 
DISABLE 
NOM 

`coreVoltageCorner` 
MAX_VOLTAGE_CORNER 
NOM 
SVS_PLUS 
SVS_PLUS 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
DISABLE 
SVS_PLUS 

`hmxVoltageCorner` 
SVS_L1 
LOW_SVS_D1 
LOW_SVS_D1 
DISABLE 
DISABLE 
DISABLE 
DISABLE 
DISABLE 
DISABLE 
DISABLE 

`hmxPerfMode` 
HIGH 
LOW 
LOW 
LOW 
LOW 
LOW 
LOW 
LOW 
LOW 
LOW 

`cengVoltageCorner` 
TURBO_PLUS 
NOM 
NOM 
NOM 
NOM 
SVS_PLUS 
SVS 
SVS2 
DISABLE 
NOM 

`cengPerfMode` 
HIGH 
HIGH 
HIGH 
HIGH 
HIGH 
LOW 
LOW 
LOW 
LOW 
HIGH 

`DDR Perf Mode` 
ON 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
OFF 
The above table is ordered from highest performance (`llm_decode_burst`) to lowest (`llm_decode_extreme_power_saver`). The modes group into three tiers: 

- 
High-performance tier (`llm_decode_burst`, `llm_decode_sustained_high_performance`, `llm_decode_high_performance`): prioritize peak token throughput. `llm_decode_burst` votes the highest corners and enables `DDR Perf Mode` to maximize memory bandwidth; the sustained variants hold votes elevated across multiple inferences without the additional DDR boost. 
- 
Balanced tier (`llm_decode_balanced`, `llm_decode_low_balanced`, `llm_decode_default`): trade some throughput for improved tokens-per-watt by disabling HMX and dropping the bus corner to NOM. `llm_decode_default` is equivalent to `llm_decode_balanced`. 
- 
Power-saver tier (`llm_decode_high_power_saver`, `llm_decode_power_saver`, `llm_decode_low_power_saver`, `llm_decode_extreme_power_saver`): minimize power for battery-bound or background inference at the cost of token throughput; HMX, HVX, and DDR are all dropped progressively across this tier. 
Example usage: 

```
{
...
"devices": [{
...
"cores":[{
"perf_profile": "llm_decode_burst"
}]
}]
}

```

## QNN HTP Profiling

Basic Profiling 
Basic profiling report for execution provides the graph inference summary on both - Host and Accelerator. 
HTP Execute Basic Profiling Events diagram illustrates the basic HTP execute profiling events and how they are measured during the inference. 
HTP Execute Basic Profiling Events

DSP heap profiling is available for `QnnContext_createFromBinary` use-cases for monitoring total memory use. Currently, a total DSP heap usage metric can be retrieved for following scenarios: 

- 
before any contexts are created (when creating the first context), 
- 
after all contexts are freed (when freeing the last context). 
Enabling DSP heap usage profiling for a given context can be achieved by passing the following configuration to the relevant `QnnContext_createFromBinary` call: 

```
1QnnHtpContext_CustomConfig_t customConfig;
2customConfig.option = QNN_HTP_CONTEXT_CONFIG_OPTION_DSP_MEMORY_PROFILING_ENABLED;
3customConfig.dspMemoryProfilingEnabled = true; // set to false to disable DSP heap profiling
4
5QnnContext_Config_t contextConfig;
6contextConfig.option = QNN_CONTEXT_CONFIG_OPTION_CUSTOM;
7contextConfig.customConfig = &customConfig;
8
9const QnnContext_Config_t* pDspMemProfilingContextConfig[] = {&contextConfig, NULL};

```

Total DSP heap usage before any contexts are created: If the aforementioned configuration is enabled for the first context to be created in the relevant `QnnContext_createFromBinary` call, the total DSP heap usage value can be retrieved from the `DSP:before_context_created` event defined in the `Qnn_ProfileHandle_t` instance, as shown below: 

```
1Qnn_ProfileHandle_t profileHandle;
2QnnProfile_create(QNN_PROFILE_LEVEL_BASIC, &profileHandle);
3
4QnnContext_createFromBinary(..., pDspMemProfilingContextConfig, ..., &contextHandle1, &profileHandle); // first context creation
5QnnContext_createFromBinary(..., &contextHandle2, ...);
6QnnContext_createFromBinary(..., pDspMemProfilingContextConfig, ..., &contextHandle3, ...);
7
8const QnnProfile_EventId_t* events;
9uint32_t numEvents;
10QnnProfile_getEvents(profileHandle, &events, &numEvents);
11
12for (uint32_t i = 0u; i < numEvents; ++i) {
13 QnnProfile_EventData_t eventData;
14 QnnProfile_getEventData(events[i], &eventData);
15 if (strcmp(eventData.identifier, "DSP:before_context_created") == 0) {
16 uint64_t totalDspHeapUsageBeforeContextCreated = eventData.value;
17 }
18}

```

Total DSP heap usage after all contexts are freed: If the aforementioned configuration was enabled for the last context to be freed either in the relevant `QnnContext_createFromBinary` call or later on with the use of `QnnContext_setConfig`, the total DSP heap usage value can be retrieved from the `DSP:after_context_freed` event defined in the `Qnn_ProfileHandle_t` instance, as shown below: 

```
1QnnContext_free(&contextHandle1, ...);
2QnnContext_free(&contextHandle2, ...);
3QnnContext_free(&contextHandle3, &profileHandle); // last context free, config was enabled for contextHandle3 during QnnContext_createFromBinary
4
5const QnnProfile_EventId_t* events;
6uint32_t numEvents;
7QnnProfile_getEvents(profileHandle, &events, &numEvents);
8
9for (uint32_t i = 0u; i < numEvents; ++i) {
10 QnnProfile_EventData_t eventData;
11 QnnProfile_getEventData(events[i], &eventData);
12 if (strcmp(eventData.identifier, "DSP:after_context_freed") == 0) {
13 uint64_t dspHeapUsageAfterContextFreed = eventData.value;
14 }
15}

```

Note 
Please note that in case the configuration was not enabled for the given context, `QnnContext_free` will not output the DSP heap usage metric. 

Note 
DSP heap profiling feature has the following requirements and limitations: 
Requirements: 

- 
Profiling should be enabled both for the first context to be created and the last context to be freed. 
Limitations: 

- 
Only supported on Android and QNX platforms. 
- 
By enabling this feature initialization and cleanup time might be impacted. 
Detailed and Linting Profiling 
Detailed profiling report provides per op profiling result by cycle counts instead of time in microsecs. There is no direct conversion method from cycle count to microsecs because of the parallelized execution of Ops. Hence it is recommended to use the per layer cycle timings as a reference to compare/measure the relative performance to know which of them are using lower/higher cycles to finish the execution. 
HTP-specific linting profiling report provides per op cycle count on the main thread along with background execution information. On the main thread, each op has to wait for some cycles since the execution of the last op before the start of its own execution. This wait period can be attributed to various factors such as scheduling or waiting for some background HVX or DMA activity to finish. In linting profiling report, each op has a cycle count associated with it signifying the amount of cycles spent actually executing the op on the main thread. There is also a “Wait” entry associated with each op that correponds to the wait period mentioned before. Aside from these two cycle counts that describe the main thread activity, each op has two more entries to depict background activity. The first of these two entries is the “Overlap” entry denoting the number of cycles spent on at least one background op while the op is executing on the main thread. Next, each op has a “Overlap (wait)” entry that is similar to the “Wait” entry with the exception that the cycles reported in this entry correspond to the “Wait” period (ie. cycles spent on at least one background op while the main thread was waiting). Background ops that are being waited on by main thread ops are not considered as background activity and as such do not contribute to the counts reported by the overlap entries. Each of the overlap entries also has several indented lines (10 maximum) following it indicating the names of the ops that contributed to the respective overlap cycle count. Finally, each op also has a “Resources” entry listing the different resources used by that op. The HTP-specific linting profiling level can be enabled by specifying `--profiling_level=backend` when running qnn-net-run so that the profiling level specified in the backend-specific config file is used. Please refer to the documentation for qnn-net-run to learn more about libQnnHtpNetRunExtensions.so and backend-specific config files. For linting profiling, the backend-specific config file should specify the following option along with any other desired option: 

```
{
....
"devices": [{
...
"profiling_level": "linting",
"cores": [{
...
}]
}]
}

```

The profile outputs generated with this profiling level can be viewed using the qnn-profile-viewer tool with its libQnnHtpProfilingReader.so or libQnnChrometraceProfilingReader.so reader plugin. libQnnHtpProfilingReader.so reader provides raw output of every single run whereas libQnnChrometraceProfilingReader.so provides average output of all the runs. Additionally, a file containing the profiling data in chrometrace format can be generated if an output file is specified with the `--output` option when running the qnn-profile-viewer tool with the libQnnChrometraceProfilingReader.so reader plugin. 
To retrieve linting information from an inference, the following steps are required: 

- 
Set $QNN_SDK_ROOT to your desired QNN version 
- 
Run “source $QNN_SDK_ROOT/bin/envsetup.sh” 
- Push the reqired files to the device

- 
$QNN_SDK_ROOT/lib/aarch64-android/libQnnHtpNetRunExtensions.so 
- 
backend_extension_config.json 
- 
htp_config.json 
- 
Run inference on device, make sure to add the following parameters: “-–profiling_level=backend and –-config_file=backend_extension_config.json” 
- 
Pull output logs to linux 
- 
When using qnn-profile-viewer make sure to specify the following parameter: “–-reader $QNN_SDK_ROOT/lib/x86_64-linux-clang/libQnnHtpProfilingReader.so” 
- 
When generating chromeTrace file, make sure to specify the following parameter: “–output ./chrometrace.json” 
backend_extension_config.json 

```
{
"backend_extensions": {
"shared_library_path" : "./libQnnHtpNetRunExtensions.so",
"config_file_path" : "./htp_config.json"
}
}

```

htp_config.json 

```
{"devices": [ {"profiling_level" : "linting"} ] }

```

Example Inference Command 

```
./qnn-net-run \
--retrieve_context sample_model.bin \
--backend libQnnHtp.so \
--input_list target_raw_list.txt \
--config_file backend_extension_config.json \
--output_dir output_htp \
--profiling_level backend

```

Example Profile Viewer Command 

```
$QNN_SDK_ROOT/bin/x86_64-linux-clang/qnn-profile-viewer \
--reader $QNN_SDK_ROOT/lib/x86_64-linux-clang/libQnnHtpProfilingReader.so \
--input_log ./output/qnn-profiling-data_0.log

$QNN_SDK_ROOT/bin/x86_64-linux-clang/qnn-profile-viewer \
--reader $QNN_SDK_ROOT/lib/x86_64-linux-clang/libQnnChrometraceProfilingReader.so \
--input_log ./output/qnn-profiling-data_0.log \
--output ./chromeTrace.json

```

Showcase Model 1 diagram illustrates a model with two branches each performing a couple of convolutions before their results are used in a sub operation. 
Showcase Model 1

The linting profiling output for this model is given below: 

```
Execute Stats (Average):
------------------------
Total Inference Time:
---------------------
NetRun: 16792 us
Backend (RPC (execute) time): 16242 us
Backend (QNN accelerator (execute) time): 15190 us
Backend (Num times yield occured): 0 count
Backend (Time for initial VTCM acquire): 0 us
Backend (Time for HVX + HMX power on and acquire): 0 us
Backend (Accelerator (critical path execute) time (cycles)): 4327266 cycles
Input OpId_2 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
OpId_0 (cycles): 8036 cycles
Wait (Scheduler) time: 629 cycles
Overlap time: 4770 cycles
Overlap (wait) time: 565 cycles
Resources:
model_convStart_Conv2D:OpId_21 (cycles): 147075 cycles
Wait (Scheduler) time: 32 cycles
Overlap time: 85292 cycles
model_sub_sub:OpId_57
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 32 cycles
model_convStart_Conv2D:OpId_21
Resources: HVX, HMX, DMA
model_tf_op_layer_stride_stride:OpId_24 (cycles): 146494 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 70807 cycles
model_add_add:OpId_58
Output OpId_3
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_convLeft1_Conv2D:OpId_34 (cycles): 288249 cycles
Wait (Scheduler) time: 425 cycles
Overlap time: 195988 cycles
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 304 cycles
Output OpId_3
model_add_add:OpId_58
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight1_Conv2D:OpId_41 (cycles): 220391 cycles
Wait (Scheduler) time: 803 cycles
Overlap time: 135268 cycles
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 557 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight2_Conv2D:OpId_48 (cycles): 181016 cycles
Wait (Scheduler) time: 1090 cycles
Overlap time: 69323 cycles
model_sub_sub:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Overlap (wait) time: 489 cycles
model_sub_sub:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Resources: HMX, DMA
model_convLeft2_Conv2D:OpId_55 (cycles): 233736 cycles
Wait (Scheduler) time: 1059 cycles
Overlap time: 93020 cycles
model_sub_sub:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 464 cycles
model_sub_sub:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Resources: HMX, DMA
model_sub_sub:OpId_57 (cycles): 2165162 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 465046 cycles
model_sub_sub:OpId_57
Output OpId_3
model_add_add:OpId_58
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_add_add:OpId_58 (cycles): 525971 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 481468 cycles
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Overlap (wait) time: 0 cycles
Resources: HVX
Output OpId_3 (cycles): 407091 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 115120 cycles
Overlap (wait) time: 0 cycles
Resources: HVX

```

The linting profiling chrometrace output for this model is given below: 
Showcase Model 1 Chrometrace

From the output, it is evident that the sub op (OpId_57) is the most significant contributor to the total execution time - around 50%. This op also does not have significant parallel op execution - its Overlap time is 465046 cycles which is about 21.5% of its total execution time - indicating that this op is a good bottleneck to optimize. We can design an equivalent model as shown in the Showcase Model 1 Optimized diagram merging the two branches and replacing the sub op with a convolution with weights manually designed such that it performs the same task as a sub op. 
Showcase Model 1 Optimized

The linting profiling output for this optimized model is given below: 

```
Execute Stats (Average):
------------------------
Total Inference Time:
---------------------
NetRun: 11884 us
Backend (RPC (execute) time): 11525 us
Backend (QNN accelerator (execute) time): 10481 us
Backend (Num times yield occured): 0 count
Backend (Time for initial VTCM acquire): 0 us
Backend (Time for HVX + HMX power on and acquire): 0 us
Backend (Accelerator (critical path execute) time (cycles)): 1374349 cycles
Input OpId_2 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
OpId_0 (cycles): 3500 cycles
Wait (Scheduler) time: 1284 cycles
Overlap time: 3221 cycles
Overlap (wait) time: 1268 cycles
Resources:
model_convStart_Conv2D:OpId_21 (cycles): 487448 cycles
Wait (Scheduler) time: 32 cycles
Overlap time: 475888 cycles
Output OpId_3
model_add_add:OpId_50
model_tf_op_layer_stride_1_stride_1:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 32 cycles
model_convStart_Conv2D:OpId_21
Resources: HVX, HMX, DMA
model_tf_op_layer_stride_1_stride_1:OpId_24 (cycles): 10422 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 10075 cycles
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_1_stride_1:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_convCombined1_Conv2D:OpId_34 (cycles): 337711 cycles
Wait (Scheduler) time: 82 cycles
Overlap time: 307394 cycles
Output OpId_3
model_tf_op_layer_stride_1_stride_1:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 50 cycles
Output OpId_3
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convCombined2_Conv2D:OpId_41 (cycles): 295022 cycles
Wait (Scheduler) time: 1184 cycles
Overlap time: 286062 cycles
model_add_add:OpId_50
Output OpId_3
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_1_stride_1:OpId_24
Overlap (wait) time: 1140 cycles
model_add_add:OpId_50
Output OpId_3
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_1_stride_1:OpId_24
Resources: HMX, DMA
model_subConv_Conv2D:OpId_48 (cycles): 48720 cycles
Wait (Scheduler) time: 1186 cycles
Overlap time: 46686 cycles
model_add_add:OpId_50
model_tf_op_layer_stride_1_stride_1:OpId_24
Output OpId_3
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 1142 cycles
model_add_add:OpId_50
Output OpId_3
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_add_add:OpId_50 (cycles): 110698 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 108524 cycles
model_add_add:OpId_50
Output OpId_3
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_1_stride_1:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
Output OpId_3 (cycles): 77054 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 75438 cycles
Overlap (wait) time: 0 cycles
Resources: HVX

```

The total execution time has decreased significantly as a result of removing the sub op. All the ops also have significant amount of parallel op execution - as evidenced by their respective Overlap time numbers - indicating good optimization. Showcase Model 2 diagram illustrates a model that is similar to the one in the Showcase Model 1 diagram. The difference is that there is a div op in place of the problematic sub op. 
Showcase Model 2

The linting profiling output for this model is given below: 

```
Execute Stats (Average):
------------------------
Total Inference Time:
---------------------
NetRun: 19353 us
Backend (RPC (execute) time): 18679 us
Backend (QNN accelerator (execute) time): 17700 us
Backend (Num times yield occured): 0 count
Backend (Time for initial VTCM acquire): 0 us
Backend (Time for HVX + HMX power on and acquire): 0 us
Backend (Accelerator (critical path execute) time (cycles)): 7866535 cycles
Input OpId_2 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
OpId_0 (cycles): 8657 cycles
Wait (Scheduler) time: 782 cycles
Overlap time: 5155 cycles
Overlap (wait) time: 717 cycles
Resources:
model_convStart_Conv2D:OpId_21 (cycles): 148293 cycles
Wait (Scheduler) time: 34 cycles
Overlap time: 86500 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 34 cycles
model_convStart_Conv2D:OpId_21
Resources: HVX, HMX, DMA
model_tf_op_layer_stride_stride:OpId_24 (cycles): 145084 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 70877 cycles
model_convStart_Conv2D:OpId_21
model_add_add:OpId_58
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_convLeft1_Conv2D:OpId_34 (cycles): 285476 cycles
Wait (Scheduler) time: 431 cycles
Overlap time: 196212 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 318 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight1_Conv2D:OpId_41 (cycles): 219298 cycles
Wait (Scheduler) time: 804 cycles
Overlap time: 134711 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 558 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight2_Conv2D:OpId_48 (cycles): 181198 cycles
Wait (Scheduler) time: 1083 cycles
Overlap time: 68306 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 476 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
Resources: HMX, DMA
model_convLeft2_Conv2D:OpId_55 (cycles): 233731 cycles
Wait (Scheduler) time: 1055 cycles
Overlap time: 91960 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 447 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
Resources: HMX, DMA
model_tf_op_layer_RealDiv_RealDiv:OpId_57 (cycles): 5344081 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 528123 cycles
model_tf_op_layer_RealDiv_RealDiv:OpId_57
Output OpId_3
model_add_add:OpId_58
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_add_add:OpId_58 (cycles): 525199 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 481084 cycles
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Output OpId_3
model_add_add:OpId_58
Overlap (wait) time: 0 cycles
Resources: HVX
Output OpId_3 (cycles): 771320 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 115729 cycles
Overlap (wait) time: 0 cycles
Resources: HVX

```

Again, the bottleneck for this graph can be identified by examining the main and background utilization of each op. In this case, the div op is the major contributor to the overall graph execution time with it taking up 5344081 cycles - about 68% of the total execution time. Only about 10% of this op’s execution has some parallel background activity which again indicates a good potential for performance gain through optimization. Replacing the div op with a mul op is a suggested optimization strategy found in the best practices guidelines. The linting profiler output for the graph optimized with a mult op instead of a div op is given below: 

```
Execute Stats (Average):
------------------------
Total Inference Time:
---------------------
NetRun: 15755 us
Backend (RPC (execute) time): 15274 us
Backend (QNN accelerator (execute) time): 14108 us
Backend (Num times yield occured): 0 count
Backend (Time for initial VTCM acquire): 0 us
Backend (Time for HVX + HMX power on and acquire): 0 us
Backend (Accelerator (critical path execute) time (cycles)): 2741387 cycles
Input OpId_2 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
OpId_0 (cycles): 8067 cycles
Wait (Scheduler) time: 735 cycles
Overlap time: 4781 cycles
Overlap (wait) time: 669 cycles
Resources:
model_convStart_Conv2D:OpId_21 (cycles): 147478 cycles
Wait (Scheduler) time: 32 cycles
Overlap time: 86319 cycles
model_multiply_mul:OpId_57
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 32 cycles
model_convStart_Conv2D:OpId_21
Resources: HVX, HMX, DMA
model_tf_op_layer_stride_stride:OpId_24 (cycles): 145396 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 70208 cycles
model_convStart_Conv2D:OpId_21
model_add_add:OpId_58
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_convLeft1_Conv2D:OpId_34 (cycles): 287130 cycles
Wait (Scheduler) time: 430 cycles
Overlap time: 198222 cycles
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 308 cycles
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight1_Conv2D:OpId_41 (cycles): 219409 cycles
Wait (Scheduler) time: 806 cycles
Overlap time: 135286 cycles
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 558 cycles
Output OpId_3
model_tf_op_layer_stride_stride:OpId_24
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_convRight2_Conv2D:OpId_48 (cycles): 181465 cycles
Wait (Scheduler) time: 1068 cycles
Overlap time: 69160 cycles
model_multiply_mul:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 467 cycles
model_multiply_mul:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Resources: HMX, DMA
model_convLeft2_Conv2D:OpId_55 (cycles): 233619 cycles
Wait (Scheduler) time: 1055 cycles
Overlap time: 92740 cycles
model_multiply_mul:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 445 cycles
model_multiply_mul:OpId_57
model_convStart_Conv2D:OpId_21
Output OpId_3
model_add_add:OpId_58
Resources: HMX, DMA
model_multiply_mul:OpId_57 (cycles): 737978 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 437784 cycles
model_multiply_mul:OpId_57
Output OpId_3
model_add_add:OpId_58
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Overlap (wait) time: 0 cycles
Resources: HVX
model_add_add:OpId_58 (cycles): 527450 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 481714 cycles
model_convStart_Conv2D:OpId_21
model_tf_op_layer_stride_stride:OpId_24
Output OpId_3
model_add_add:OpId_58
Overlap (wait) time: 0 cycles
Resources: HVX
Output OpId_3 (cycles): 249264 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 117890 cycles
Overlap (wait) time: 0 cycles
Resources: HVX

```

There is a noticeable reduction in the total graph execute time and the ops also have better background utilization indicating better optimization than before. Next, Showcase Model 3 diagram illustrates a model that is similar to the one in Showcase Model 1 Optimized diagram. The difference is that the ReLU ops have been replaced with PReLU ops. 
Showcase Model 3

The linting profiler output for this model is given below: 

```
Execute Stats (Average):
------------------------
Total Inference Time:
---------------------
NetRun: 15368 us
Backend (RPC (execute) time): 15033 us
Backend (QNN accelerator (execute) time): 13863 us
Backend (Num times yield occured): 0 count
Backend (Time for initial VTCM acquire): 0 us
Backend (Time for HVX + HMX power on and acquire): 0 us
Backend (Accelerator (critical path execute) time (cycles)): 2789467 cycles
Input OpId_2 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
OpId_0 (cycles): 3411 cycles
Wait (Scheduler) time: 1226 cycles
Overlap time: 3173 cycles
Overlap (wait) time: 1194 cycles
Resources:
model_convStart_Conv2D:OpId_21 (cycles): 589431 cycles
Wait (Scheduler) time: 957 cycles
Overlap time: 41199 cycles
Output OpId_3
model_add_add:OpId_54
model_preluCombined1_add:OpId_37
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 72 cycles
Output OpId_3
model_convStart_Conv2D:OpId_21
Resources: HVX, HMX, DMA
model_tf_op_layer_stride_1_stride_1:OpId_24 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources:
model_convCombined1_Conv2D:OpId_34 (cycles): 165119 cycles
Wait (Scheduler) time: 1089 cycles
Overlap time: 155164 cycles
model_preluCombined1_add:OpId_37
Output OpId_3
model_add_add:OpId_54
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 977 cycles
model_preluCombined1_add:OpId_37
Output OpId_3
model_add_add:OpId_54
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_preluCombined1_add:OpId_37 (cycles): 27315 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 9431 cycles
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 0 cycles
Resources: HVX
model_convCombined2_Conv2D:OpId_43 (cycles): 805490 cycles
Wait (Scheduler) time: 81 cycles
Overlap time: 251743 cycles
model_add_add:OpId_54
Output OpId_3
model_preluCombined1_add:OpId_37
model_preluCombined2_add:OpId_46
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 62 cycles
Output OpId_3
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_preluCombined2_add:OpId_46 (cycles): 0 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 0 cycles
Overlap (wait) time: 0 cycles
Resources: HVX
model_subConv_Conv2D:OpId_52 (cycles): 666721 cycles
Wait (Scheduler) time: 34 cycles
Overlap time: 180805 cycles
model_add_add:OpId_54
Output OpId_3
model_convStart_Conv2D:OpId_21
model_preluCombined2_add:OpId_46
Overlap (wait) time: 13 cycles
model_convStart_Conv2D:OpId_21
Resources: HMX, DMA
model_add_add:OpId_54 (cycles): 62806 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 57481 cycles
model_add_add:OpId_54
Output OpId_3
model_preluCombined1_add:OpId_37
model_preluCombined2_add:OpId_46
model_convStart_Conv2D:OpId_21
Overlap (wait) time: 0 cycles
Resources: HVX
Output OpId_3 (cycles): 465781 cycles
Wait (Scheduler) time: 0 cycles
Overlap time: 430560 cycles
Overlap (wait) time: 0 cycles
Resources: HVX

```

The usual sign indicating bottlenecks is present here as well. There are multiple ops with low parallel execution. PReLU ops are some of the background ops that executed for these ops and the best practices guidelines suggest that PReLU ops should be replaced with ReLU ops. Changing the graph by replacing the PReLU ops with ReLU gives us the same model as the one shown in the Showcase Model 1 Optimized diagram which is much better optimized as explained before. 

## QNN HTP Optrace Profiling

Optrace High-level Operation 
HTP Optrace Block Diagram illustrates the high-level operation of Optrace profiling. 
HTP Optrace Block Diagram

HTP Optrace Tooling Call Flow Diagram illustrates the basic Optrace profiling events and how they are captured. 
HTP Optrace Tooling Call Flow Diagram

Detailed Command Line Usage Guide 
To enable and use Optrace profiling, we need to specify additional parameters during model preparation, model execution, and post-processing. The following sections give a detailed usage guide for the command line parameters needed, with examples. 
Converter 
For converter, you can optionally set these two parameters: 

- 
`--export_format dlc` 
- 
`--enable_framework_trace` 
This is an option specific to generate a dlc file, which allows the chrometrace to be aware of framework name, as well as the QNN Op types that those get converted into. It is recommended that you enable these options for additional context whenever possible. 
Preparation (Context Binary Generation) 
For context binary generation, we require two additional parameters: 

- 
`--profiling_level detailed` 
- 
`--profiling_option optrace` 
By using these parameters, the result is an extra file outputted in the current working directory of qnn-context-binary-generator: the schematic binary 
Sample Command Line Below: 

```
qnn-context-binary-generator --profiling_level detailed --profiling_option optrace --backend [SDK_PATH]/lib/x86_64-linux-clang/libQnnHtp.so --model [MODEL].so --config_file HtpConfigFile.json --output_dir [OUTPUT_DIR] --binary_file [CONTEXT_BIN]

```

From the above command here are the following outputs: 

- 
`[CONTEXT_BIN]` - this is the context binary file containing the model 
- 
`[MODEL]_schematic.bin` - this is the schematic file (required for chrometrace generation) 
Execution (Net Run) 
For qnn-net-run, we require two additional parameters: 

- 
`--profiling_level detailed` 
- 
`--profiling_option optrace` 
This will allow for profiling events to be embedded into the output profiling log. 
Sample Command Line Below: 

```
qnn-net-run --profiling_level detailed --profiling_option optrace --output_data_type float_and_native --retrieve_context [CONTEXT_BIN] --backend libQnnHtp.so --input_list ./inputs/input_list.txt --output_dir . --log_level info

```

From the above command here are the following outputs: 

- 
`qnn-profiling-data.log` - this is the log data containing the optrace information that will be further parsed in the post process step (required for chrometrace generation) 
- 
The normal outputs of model execution 
Post Process (Chrometrace Generation) 
During the post process phase, we now have the required data from the above two steps to generate a chrometrace. 

- 
`[MODEL]_schematic.bin` - this is the schematic we recieve from the context binary generation step 
- 
`qnn-profiling-data.log` - this is the detailed profiling data we recieve from the execution step 
To generate the chrometrace, we run qnn-profile-viewer on the host with the libQnnHtpOptraceProfilingReader reader library. 
Sample Command Line Below: 

```
qnn-profile-viewer --config [PATH_TO]/config.json --reader [SDK_PATH]/lib/[TARGET]/libQnnHtpOptraceProfilingReader.so --input_log ./qnn-profiling-data.log --schematic ./[MODEL]_schematic.bin --output ./chrometrace.json

```

The config.json file provides parameters beyond the ones covered in the command line arguments, such as the following: 

- 
`enable_input_output_flow_events` - Adds flow events to the chrometrace.json, showing input-output dependencies between operations. Requires using the legacy UI to open chrometrace. 
- 
`enable_sequencer_flow_events` - Adds flow events to the chrometrace.json, showing ordering dependencies between operations, imposed by the sequencer. Requires using the legacy UI to open chrometrace. 
- 
`htp_json` - Dumps a [NAME]_htp.json file containing the toplogy and op-by-op information about the HTP graph. Default is on. 
- 
`runtrace` - Adds Runtrace execution and preemption events (if available) at the bottom of each core in the output chrometrace. Default is on. 
- 
`memory_info` - Adds memory bandwidth and allocation graphs (if available) at the bottom of each core in the output chrometrace. Default is on. 
- 
`traceback` - Adds trace back to source framework in the output chrometrace. Default is on. 
- 
`qhas_schema` - Dumps a qhas_schema.json that can be used to validate the QHAS json file. Default is off. 
- 
`qhas_json` - Dumps a [model]_qnn_htp_analysis_summary.json. Default is off. 
Sample config.json below, with all available boolean parameters: 

```
{
"features":
{
"enable_input_output_flow_events": true,
"enable_sequencer_flow_events": true,
"htp_json": true,
"runtrace": true,
"memory_info": true,
"traceback": true,
"qhas_schema": true,
"qhas_json": true
}
}

```

From the above command here are the following outputs: 

- 
`chrometrace.json` - the chrometrace output that can be opened with either the Perfetto Trace Visualizer or with `chrome://tracing` 
- 
`chrometrace_qnn_htp_analysis_summary.html` - the QHAS HTML report 

Note 
A number of ops in HTP get classified as “System Service”. These are not ops associated with any specific operation performed in the base neural network. Each System Service category is briefly explained below: 

- 
DramToTcm: Loads data from DRAM into VTCM. 
- 
TcmToDram: Writes data from VTCM into DRAM. 
- 
Sync Op: Used to provide an ordering for HVX ops to resolve dependencies between ops. 
- 
DmaCheckpointSet: A producer writing to memory for a future consumer to use sets this checkpoint when it is finished writing. 
- 
DmaCheckpointWait: A consumer that waits for DmaCheckpointSet. 
- 
BlockZapOp: When a tensor’s data is not a multiple of block-size, this operation “pads” the blocks with a specified zero-value. 
- 
SystemService: Not associated with any specific parent op, it prefetches chunks of data into L2 cache for ops to use. 
If you want additional context such as framework names and QNN Op types, provide the DLC file with the following parameter: 

- 
`--dlc ./[MODEL].dlc` 
This DLC file is generated within the converter as mentioned above. 
Optionally, you can perform a profile submodule chrometrace, where you specify two QNN node names, and the generated chrometrace only represents the subnetwork contained between the two. To use this, you use the following two parameters: 

- 
`--zoom_start` - this is the starting QNN node name for the submodule. 
- 
`--zoom_end` - this is the ending QNN node name for the submodule. 
The parameters `--zoom_start` and `--zoom_end` can accept framework node names (e.g. ONNX op names) if the –dlc parameter is set earlier. If the –dlc parameter is set, the program will automatically detect whether the names provided match a framework name or QNN name, no further context is required. 
HTP Graph Topology and per-Op Information in Netron 
By enabling the `htp_json` parameter in the config.json above, a [NAME]_htp.json file containing the toplogy and op-by-op information about the HTP graph will be dumped. This json file can be viewed directly in Netron. This feature can be used in conjunction with the node zooming feature above. 
HTP Graph Topology in Netron demonstrates viewing [NAME]_htp.json in Netron. 
HTP Graph Topology in Netron

HTP Graph per-Op Information in Netron demonstrates the ability to click on an HTP node and view Op properties. 
HTP Graph per-Op Information in Netron

Memory Bandwidth and Allocation graphs 
By enabling the `memory_info` parameter in the config.json above, memory bandwidth and allocation graphs (if available) will be displayed at the bottom of each core in the output chrometrace. The graphs below titled “VTCM”/”DRAM” “read”/”write” display instantaneous bandwidth, while the “VTCM alloc” graphs display current memory allocation at that point. 
HTP Optrace Multicore Memory Graphs demonstrates the per-core memory bandwidth and allocation graphs. 
HTP Optrace Multicore Memory Graphs

Gzip Compression of Chrometrace 
This feature will add the ability to automatically compress output chrometraces into the gzip format and will save massive amounts of disk space. 
On a sample model, the size of the chrometrace output was ~1300KB before compression and ~60KB after compression (ratio of 0.05). 
There are 3 ways to enable gzip compression: 

- 
Append `.gz` to the output filename command line argument for qnn-profile-viewer 
- 
Add the `--gzip` command line flag for qnn-profile-viewer 
- 
Enable the `gzip` parameter in the optrace config.json file 
Gzip compression will be turned on if any one of these options are set. 

Note 
Command line parameters will be subject to change once multi-graph support is enabled. 
Runtrace Execution and Preemption Events 
By enabling the `runtrace` parameter in the config.json above, Runtrace execution and preemption events (if available) will be displayed at the bottom of each core in the output chrometrace. 
The graphs below titled “<GRAPH_NAME> Runtraces” show the duration of the entire inference as well as other information such as the time it took to acquire physical resources (execution events). The graphs below titled “<GRAPH_NAME> Yields” show the time it took to save, re-acquire, and restore VTCM memory when yielding to a higher priority thread (preemption events). 
HTP Optrace Runtrace Graph demonstrates the Runtrace graph execution events for one graph. 
HTP Optrace Runtrace Graph

## QNN HTP Hextimate Profiling

The QNN HTP Optrace Profiling section describes the use of Optrace profiling, to generate a timeline breakdown of operations during a model’s runtime. Refer to the above section for more details on output files and command line arguments. 
Hextimate profiling is an alternative to Optrace profiling. Rather than requiring a runtime trace of ops, it takes in an estimate of the ops’ performance instead. 
For context binary generation, we require two additional parameters: 

- 
`--profiling_level detailed` 
- 
`--profiling_option optrace` 
Additionally, the –config_file parameter for the context binary generator takes a config.json file. This file must have an soc_model entry with a value of 52 under the devices entry. An example follows: 

```
{
"graphs":[
{
"vtcm_mb":8,
"O": 3,
"graph_names":["<network-name>"]
}
],
"devices":[
{
"dsp_arch":"v73",
"soc_model": 52,
"pd_session":"unsigned",
"device_id":0
}
]
}

```

With the above two parameters and the soc_model set correctly, the context binary generator should output a qnn-profiling-data.log alongside the [MODEL]_schematic.bin file. For Hextimate profiling, you do not need to run the qnn-net-run step. 
To generate the Hextimate chrometrace, we then use the same comamnd as for a regular optrace: qnn-profile-viewer on the host with the libQnnHtpOptraceProfilingReader reader library. 
Sample Command Line Below: 

```
qnn-profile-viewer --reader [SDK_PATH]/lib/[TARGET]/libQnnHtpOptraceProfilingReader.so --input_log ./qnn-profiling-data.log --schematic ./[MODEL]_schematic.bin --output ./chrometrace.json

```

Note 
Hextimate profiling is only available on Auto SDK builds. 

## QNN HTP Analysis Summary (QHAS)

From the steps above for running optrace profiling, we can see that a QHAS HTML Report is generated by `qnn-profile-viewer` as part of the existing flow (no extra parameters required). Unlike the chrometrace which visually depicts the data from the HTP ops, the QHAS HTML Report summarizes this data into a report with analysis. 
QHAS HTML Report Example illustrates the layout of the QHAS Report with the “HTP Overall Summary” section expanded. The other sections will expand once clicked. 
QHAS HTML Report Example

Additionally, a column within a section can be sorted by clicking on the sort icon. And a pie chart can be generated for a column to illustrate the value of each row relative to the total in that column, by clicking on the chart icon. 
In the QHAS HTML Report Sorting and Plotting Example, we are sorting the “Cycles” column under the “QNN Op Types” section in descending order, and are displaying a pie chart for it. This pie chart visually represents the fraction of the total cycle count that each op type uses. 
QHAS HTML Report Sorting and Plotting Example

In the QHAS HTML Report Filtering Example, we are filtering the columns under “HTP Op” and “QNN Op” by the keywords “conv” and “batch”, respectively. Only rows that contain the filter keyword will be shown. Filter keywords in multiple columns can be set simultaneously. 
QHAS HTML Report Filtering Example

Note 
The “Dominant Path” section of QHAS shows a timeline of the highest priority HTP op throughout the timeline. The priority list is as follows: 

- 
HMX Op 
- 
HVX Op 
- 
Ops performing DMA Reads 
- 
Ops performing DMA Writes 
- 
DmaCheckpointSet and DmaCheckpointWait (as explained in System Services above) 
- 
SyncOp (as explained in System Services above) 

Note 
If you enable the profile submodule feature above, QHAS will also only show an analysis for the nodes contained within the subnetwork. 

Note 
The QHAS feature is still in Beta so it is subject to change in future SDK versions. 

## QNN Context Binary size

The QNN Context Binary is used by QNN for execution of the neural network. Post preparation of graph, the ‘QNN Context Binary’ contains the information & optimizations for faster inference of the model. The ‘QNN Context Binary’ has larger size compared to the size of QNN model. This enlarged size results from the following reasons: 

- 
Number of Operations: HTP tries to run as many operations as possible in parallel. To be able to fit into the VTCM, heavy operations are split into smaller operations. This often results in increase in the number of operations which are needed to be present in Context Binary, resulting in increase in its size. For example, if each op takes 40 bytes of Context Binary and if the number of operations before and after the above optimization are 30 and 300,000, then we need 1.2 KB and 12 MB respectively in the Context Binary. The figure below shows a large Conv operation which needs to be broken up into smaller operations. 

- 
Sequencing and Data Paging: As the number of operations increase, the Context Binary must also need to store information about the sequence of operations and the information about data paging (which operation needs to be written to DDR and which needs to be brought back into VTCM during execution). This information also contributes to the size of prepared Context Binary. 
- 
Constant Data in Graph: The QNN Context Binary contains all constant data of the graph. The constant data consists of, among other things, convolution filters. These filters could be padded in the QNN context binary to represent internal HTP format for performance efficiency, causing additional size increase for the QNN context binary. 

## Op Writing Guidelines

This section serves as a guide for user to develop custom operations for graphs that can be run on the HTP backend. It also includes examples which will help user to become familiar with the different aspects of the HTP core software. 
QNN HTP exposes the following two interfaces for custom operations: 

### Choosing Between QHPI and Legacy HTP Operator APIs

General recommendations: 

- 
HTP custom ops should be written using QHPI whenever possible 
- 
QHPI and legacy packages can coexist in the same graph to allow incremental migration. Cross-package optimization rules in legacy packages can reference QHPI operators and vice versa via the `PackageName::OpName` convention. 
Use QHPI when: 

- 
Forward compatibility matters. QHPI op packages built with an older SDK are expected to continue working on newer SDKs without recompilation, thanks to versioned APIs and data structures. Legacy op packages must be recompiled after every QNN SDK release. 
- 
You are writing a new operator from scratch. QHPI provides a cleaner starting point with explicit C data structures (`QHPI_Kernel_vxxx`, `QHPI_Tensor_Signature_vxxx`, `QHPI_OpInfo_vxxx`) instead of C++ macros, reducing the surface area for subtle errors. 
- 
You need multi-threading support. QHPI exposes a first-class `multithreaded` flag and slice APIs (`qhpi_num_slices`, `qhpi_slice_number`) for parallel kernel execution across hardware threads. 
- 
Your tiling needs are standard. QHPI’s tiling callbacks (`shape_required`, `shape_legalized`, `build_tile`) hook into the HTP central tiler, which automatically balances parallelism, TCM residency, and inter-op communication costs. For most operators this is sufficient and simpler than writing manual `AUTOSPLIT` rules. 
- 
Your graph rewrites are straightforward. QHPI’s `early_rewrite` and `late_rewrite` C callbacks replace the `DEF_PACKAGE_OPTIMIZATION` DSL with direct graph manipulation via `qhpi_op_create`, `qhpi_op_slice`, and related APIs. This is easier to debug and maintain than the pattern-match-and-replace grammar, though it covers the same pre-tiling and post-tiling rewrite phases. 
Use the legacy HTP operator APIs when: 

- 
You have a large existing legacy op package and migration cost is not justified. QHPI coexists with legacy packages in the same graph, so there is no requirement to migrate all at once. Incremental migration is supported however compatibility may not be guaranteed. 
- 
You need fine-grained control over optimization pass ordering. The legacy `DEF_PACKAGE_OPTIMIZATION` macro accepts arbitrary numeric priorities (e.g., `EARLY+1`, `MIDDLE`, `LATE+900`), allowing precise interleaving of your rules with the framework’s own optimization passes. QHPI simplifies this to two fixed phases (early and late rewrites). 
- 
You need cost-based kernel selection. In the legacy system, cost functions (`DEF_PACKAGE_OP_AND_COST_AND_FLAGS`, `DEF_PACKAGE_OP_AND_COST_F_AND_FLAGS`) influence which kernel implementation is selected at graph preparation time. In QHPI, cost functions are only used for execution time prediction and have no influence on kernel selection, which is driven solely by tensor signature matching order and optional predicate callbacks. 
- 
You rely on the optimization rule DSL for complex pattern matching. The legacy grammar supports multi-op subgraph matching (`Op`, `OpVarIn`, `LET`), rich constraint expressions (`RANK_OF`, `DIM_OF`, `CONSTVAL_INT`, `SAME_ENCODING`, `EXTERNAL_CONSTRAINT`, etc.), and composable replacement patterns (`AUTOSPLIT`, `TYPICAL_SLICE`, `CHANGEDIM_SLICE`, `OP_ITER`, `SHAPEFN_APPLY`, `EXTERNAL_REPLACE`). If your operator requires matching and rewriting multi-node patterns with detailed constraints, the DSL may be more expressive than QHPI’s callback-based approach. 

### Qualcomm Hexagon Plugin Interface (QHPI)

QHPI introduces a structured approach for creating and registering operators with the QNN HTP backend through a set of well-defined APIs. It is designed to enhance usability and provide robust API and ABI compatibility for customer operator packages—capabilities that are not available with the legacy APIs. For additional details on QHPI, refer to the QHPI Doxygen API and the section below.: 

- Qualcomm Hexagon Plugin Interface 

Note 
Please note that QHPI is still in Beta so it is subject to change in future SDK versions. 

### Legacy HTP operator APIs

Refers to the existing C++ macro based (DEF_PACKAGE_OPT., etc) HTP APIs that the user is familiar with to create and register non-native operators with HTP BE. Additional information can be found below: 

- QNN HTP Op Package - Common Default Package Ops Usage Examples 
- QNN HTP Optimization Utility Functions Usage Examples 
- HTP Core Headers for Op Packages 
- Implementing Ops 
- QNN HTP Op Package API Revision History 
- Optimization Grammar 
- QNN HTP Op Package - Relu Op Example 
- QNN HTP-FP16 Op Package - Relu Op Example 
- Scheduling and Allocation 
- Allocate Memory for Scratch Buffers 
- Tensors and Memory Layout 
- Writing QNN HTP Op Package 
- General OpPackage Central Migration Guidance 
- Op Package Migration Guide 

## Recommendations for Network Design

The HTP supports A8, A16 and FP16 activations. Generally, the accuracy and the power and energy requirements follow the order A8 < A16 < FP16. Therefore, to minimize power, one should first try the A8 mode and check for accuracy of the results. If the accuracy is not sufficient try A16 mode and if even that doesn’t achieve the desired accuracy move to FP16 mode. 
The following sections cover some of the best practices in graph design that allows for the optimal use of HTP hardware from a performance and accuracy perspective. 

- Avoid Low Depth Activations 
- Avoid Low Depth Activations (more examples) 
- Use Space-to-depth Transformation where possible 
- Reducing TCM Requirements for Performance and Functionality 
- Choice of Activation Functions 
- Number of Channels 
- Quantized 16 bit activations (A16) vs FP16 and Activation Fusion: Performance and power differences 
- INT4 encodings for weights 
- Other Performance and Energy Guidelines 
It is recommended to always use symmetrical quantization of weights when quantizing the model to obtain best accuracy on HTP based targets. Activation data is recommended to be asymmetric. 
It is recommended to use quantization aware training as much as possible to improve the accuracy of models especially in the case of high resolution image transformation models. Using quantization aware training may take away the need to use 16bit activations and may allow the use of 8bit activations which will improve both performance and power. When using quantization aware training, keep in mind the following: 

- 
A comparison of outputs in original framework between original float model and model with fakequant nodes helps in determining the quality of the quantization aware trained model 
- 
Ensure there are fakequant nodes for all layers/kernels 
A16W16 (int16 weight along with uint16 activations) is supported for several Convolution type of operations. This is commonly used on image enhancement networks, but available to other type of usecases as well. 
This feature is enabled only on selected SoCs. For further accuracy enhancement purpose, per-channel quantization method will be added in the future. Expectations of comparison between A16W16 models and A16W8 and FP16 models as follows:

- 
A16W16 models are expected to achieve better accuracy than A16W8 models with Post-Training quantization. 
- 
A16W16 models are expected to achieve better power efficiency than FP16 models while maintaining a similar accuracy result. List of Convolution type of operations supported for A16W16:

- 
Conv2d 
- 
DepthConv2d 
- 
TransposeConv2D 
- 
FullyConnected 
- 
Matmul 
- 
Batchnorm 
- 
LayerNorm 
All weights/filters need to be symmetrically quantized. For Matmul, Input A must be asymmetrically quantized, Input B must be symmetrically quantized. Please refer to OpDef/HtpOpDefSupplement:HTP Backend Op Definition Supplement for details. 
Limitation: Due to Hexagon hardware limitation, INT16 weight have a special limitation that the range of weight value will be 0x8000 to 0x7F7F instead of a full 16bit range 0x8000 to 0x7FFF. Please add `--restrict_quantization_steps "-0x8000 0x7F7F"` to quantizer options when using A16W16. 

## Yielding and Pre-Emption

Yielding and Pre-Emption is a cooperative user-based implementation of context switching. The following document aims to help the user understand different concurrency scenarios and their expected behaviour. 

- HTP Yielding 

## Parallel Graph Execution

Parallel Graph Execution, available starting from v81 SoCs, enables more than one graph to execute simultaneously. The following document aims to help the user understand how to use this feature. 

- HTP Parallel Graph Execution 

## VTCM Sharing

Staring from hexagon-v73, it is possible for other threads in the same process to share VTCM resources with QNN HTP using the procedure described in the following pages: 

- HTP VTCM Sharing 
- VTCM Windowing 

## SubSystem Restart (SSR)

A QNN HTP BE specific feature that allows the CDSP subsystem to automatically restart an invalidated connection after crashing. Details are provided in the following page: 

- QNN HTP SSR 

## Running Model on Different HTP Devices (Auto)

### With qnn-net-run on HTP Backend

It is possible to control which HTP device (i.e., CDSP/NSP ID) will execute a model. Execution of models on CDSP1/NSP1 is extended by libQnnHtpNetRunExtensions.so. 
The HTP net extension library (libQnnHtpNetRunExtensions.so) and device_id (CDSP/NSP ID) info can be passed to qnn-net-run through the `--config_file` option via commandline parameter as below: 

```
$ ./qnn-net-run --backend libQnnHtp.so \
--input_list target_raw_list.txt \
--retrieve_context Inception_v3_quantized.serialized.bin \
--profiling_level basic \
--log_level error \
--config_file qnn_v2_config.json

```

An example HTP backend config JSON file is given below: 
qnn_v2_config.json 

```
// For loading the NetRunExtensions / Config JSON
{
"backend_extensions": {
"shared_library_path" : "libQnnHtpNetRunExtensions.so",
"config_file_path" : "qnn_v2_8mb_vtcm_nsp0.json"
},
"context_configs" : {
"context_priority" : "normal"
}
}

```

It refers to a config_file_path paramter which another json config used to speficy the device_id. 
To select CDSP 0, follow the config JSON file below: 
qnn_v2_8mb_vtcm_nsp0.json 

```
{
"graphs": {
"vtcm_mb":8,
"graph_names":["<network-name>"]
},
"devices":[
{
"dsp_arch":"v68",
"pd_session":"unsigned",
"device_id":0
}
]
}

```

Similarly, we can set device_id=1 to select CDSP1: 
qnn_v2_8mb_vtcm_nsp1.json 

```
{
"graphs": {
"vtcm_mb":8,
"graph_names":["<network-name>"]
},
"devices":[
{
"dsp_arch":"v68",
"pd_session":"unsigned",
"device_id":1
}
]
}

```

Please refer to the following table to select the appropriate DSP architecture (“dsp_arch”) value. 
QNN HTP TARGET CONFIG TABLE 

Target Name 
Target dsp_arch 
Target soc_id 

SA8295 
v68 
39 

SA8540 
v68 
62 

SA8650 
v73 
52 

SA8775 
v73 
52 

SA8255 
v73 
52 

SA8620 
v75 
67 

SA7255 
v75 
67 

SA8797 
v81 
72 

Note 
If no config file is used with qnn-net-run, the model execution is run on device_id=0 or CDSP0/NSP0 by default. 

### With QNN API on HTP Backend

A QNN API example is shown below: 

HTP Device Creation example with CDSP/NSP selection 

```
1// QnnInterface_t is defined in ${QNN_SDK_ROOT}/include/QNN/QnnInterface.h
2QnnInterface_t qnnInterface;
3// Init qnn interface ......
4// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code
5// Also, check Sample App Tutorial at ${QNN_SDK_ROOT}/docs/QNN/general/sample_app.html
6
7uint32_t deviceId = 0; // This is where QNN application can select which CDSP/NSP ID or deviceId to
8 // use for device creation for a model inference
9QnnDevice_PlatformInfo_t e;
10const QnnDevice_PlatformInfo_t *platformInfo{nullptr};
11auto qnnStatus = m_qnnFunctionPointers.qnnInterface.deviceGetPlatformInfo(nullptr, &platformInfo);
12if (QNN_SUCCESS != qnnStatus) {
13 // handle errors
14}
15
16//Note a: You need to call <qnnInterface>.deviceFreePlatformInfo() to free up any resources that <qnnInterface>.deviceGetPlatformInfo() might have allocated.
17//Note b: <qnnInterface>.deviceGetPlatformInfo() is unsupported on x86 Linux.
18
19
20if (platformInfo) {
21 e.version = QNN_DEVICE_PLATFORM_INFO_VERSION_1;
22 e.v1.numHwDevices = 1;
23
24 QnnDevice_HardwareDeviceInfo_t *hwDeviceInfo = platformInfo->v1.hwDevices;
25 if (!hwDeviceInfo) {
26 // handle errors
27 }
28
29 QnnDevice_HardwareDeviceInfo_t *hwDeviceInfoTemp{nullptr};
30 for (uint32_t temp = 0; temp < platformInfo->v1.numHwDevices; ++temp) {
31 if (hwDeviceInfo->v1.deviceId == deviceId) {
32 hwDeviceInfoTemp = hwDeviceInfo;
33 break;
34 }
35 if ((temp + 1) < platformInfo->v1.numHwDevices) {
36 hwDeviceInfo++;
37 }
38 }
39 if (!hwDeviceInfoTemp) {
40 // handle errors
41 }
42 e.v1.hwDevices = hwDeviceInfoTemp;
43}
44
45QnnDevice_Config_t devConfig;
46devConfig.option = QNN_DEVICE_CONFIG_OPTION_PLATFORM_INFO;
47devConfig.hardwareInfo = &e;
48
49const QnnDevice_Config_t* devConfigArray[] = {&devConfig, nullptr};
50
51auto qnnStatus = m_qnnFunctionPointers.qnnInterface.deviceCreate(m_logHandle, devConfigArray, &m_deviceHandle);
52if (QNN_SUCCESS != qnnStatus) {
53 // handle errors
54}

```

Note 
If no devConfig is provided with deviceCreate, the model execution is run on device_id=0 or CDSP0/NSP0 by default. 

## HTP Optimization (Auto)

The following tutorial will explain how to turn on and prepare optimized graphs on HTP and HTP MCP Backends. 
The sections of the tutorial are as follows: 

- 
Optimization levels 
- 
P points 
- 
HTP Performance Estimates 

### Optimization levels

For automotive, HTP supports different graph optimization levels. Level 3 optimization (O=3) may yield the most optimal graph. However, experimentation is required as the highest level of optimization is not always guaranteed to give the best performance. 
When creating serialized context binary with qnn-context-binary-generator, backend extension parameters can be specified using in the “–config_file” argument. Its full documentation can be found in QNN HTP Backend Extensions section. 
As shown in the sample HTP backend JSON config below, to enable a graph for O=3, specify the optimization “O” value as 3: 
htp_context.json 

```
{
"graphs": [
{
"vtcm_mb": 8,
"O": 3,
"graph_names": [
"qnn_model"
]
},
],
"devices":[
{
"dsp_arch": "v68",
"soc_id": 62,
"pd_session": "unsigned",
"device_id": 0
}
]
}

```

When preparing a graph using O=3, specifying the correct device “soc_id” matching the target to use could turn on additional algorithm(s) which may further improve inference performance. Please consult the above htp-target-table to select the appropriate DSP architecture (“dsp_arch”) value and SoC ID (“soc_id”) value. 
In terms of C API, the value 3 for “O” is from QNN_HTP_GRAPH_OPTIMIZATION_TYPE_FINALIZE_OPTIMIZATION_FLAG field of exhale_struct_structQnnHtpGraph__OptimizationOption__t. More details can be found in QNN HTP Backend API section. 
To prepare a context binary with HTP optimization related parameters, use qnn-context-binary-generator with –config_file argument and give path to htp_context.json. 

```
${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtp.so
--model model.so
--binary_file model.serialized
--profiling_level basic
--config_file htp_context.json

```

Similarly for HTP MCP backend, to enable a graph for O=3, specify the optimization “O” value as 3: 
graph_prepare (2 files) 

- 
graph_prepare.json: 

```
{
"backend_extensions": {
"shared_library_path": "libQnnHtpMcpNetRunExtensions.so",
"config_file_path" : "graph_prepare.conf"
}
}

```

- 
graph_prepare.conf (linked to from graph_prepare.json above): 

```
{
"graphs": [
{
"graph_names": [
"qnn_model"
],
"O": 3
}
]
}

```

Next, to generate the serialized context binary, specify the graph_prepare.json file using the –config_file flag as follows: 

```
$ cd ${QNN_SDK_ROOT}/examples/Models/InceptionV3
$ cp ${QNN_SDK_ROOT}/lib/hexagon-v68/unsigned/libQnnHtpMcpV68.elf network.elf
$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtpMcp.so \
--model ${QNN_SDK_ROOT}/examples/Models/InceptionV3/model_libs/x86_64-linux-clang/libInception_v3_quantized.so \
--config_file graph_prepare.json \
--binary_file Inception_v3_quantized_qpc.serialized

```

### P points

#### Overview

P points are an advanced O=3 optimization feature that may yield better performance for your model. They are available exclusively when `O=3` (optimization level 3) is enabled and give you a way to experiment with non-default compiler configurations when preparing a context binary. 
Each P value selects a different pre-defined point in the compiler’s internal configuration space, adjusting parameters that affect how the HTP executes operations — for example, tradeoffs between latency and DRAM bandwidth. 
The best P point for a given model would depend on your latency requirements and DDR bandwidth and can be chosen by experimenting across all possible P points. There is no universal “best” P value, as the right choice depends on the characteristics of your network. 
Once a graph compiles successfully with a P point, the execution output is bit-accurate with a graph compiled without P points. 

#### Valid values

Valid values for P point are: 0 (default value that does not change any parameters), 1, 2, 3, 4, 5, 6, 8, 13, 15, 16, 17, 19, 20, 21, 22, 23. 
In other words, the valid values are 0 to 23 with the following values excluded: 7, 9, 10, 11, 12, 14, 18. 

Note 
The set of valid P point values and the behavior of each value may change from release to release. Always re-validate your chosen P value when upgrading to a new SDK version. 

#### Workflow: where P points fit in the lifecycle

P points affect the offline graph preparation step only. The workflow is: 

- 
Convert your model to a QNN model (`.so` or `.bin`) using `qnn-tensorflow-converter`, `qnn-onnx-converter`, or equivalent. (P points have no effect at this stage.) 
- 
Prepare the context binary using `qnn-context-binary-generator` on x86, with `O=3` and your chosen `P` value in the HTP backend extensions config. This is where P points take effect — they change how the compiler generates the serialized context binary. 
- 
Test the prepared graph on your target hardware before deploying to production. Because P point behavior can vary by network, always validate functional correctness and performance after changing P. 
- 
Deploy the context binary to the target device. At runtime, the P value has already been baked into the binary — no runtime flag is needed. 

#### Caveats and warnings

- 
Always test before deploying. When a model is prepared using a P point, the prepared graph must be validated for both performance and functional correctness before production deployment. 

- 
The supported set of P point values may change from release to release. 
- 
The behavior of a given P point value may change from release to release. 
- 
A given network may fail to prepare or execute correctly for a given P point value. 
- 
P points are independent. Unlike O levels, where higher values generally yield better performance at the cost of longer compile time, there is no ordering or relationship between P point values. P=2 is not “better” or “worse” than P=1 in any general sense. 
- 
P only takes effect when O=3. Setting `P` without `O=3` has no effect. 
- 
Specifying more than one P point results in undefined behavior. The outcome is not guaranteed and may change across releases. 
- 
Performance varies by network. A P value that improves one model may have no effect or degrade performance on another. See Sample performance difference between two networks as reference. 
Sample performance difference between two networks

#### Setting P points (HTP backend)

P points are set via the `finalize_config` option in the HTP backend extensions config, which corresponds to `QNN_HTP_GRAPH_CONFIG_OPTION_FINALIZE_CONFIG` in `QnnHtpGraph_CustomConfig_t`. 
The following example enables `P=1` with `O=3` on the HTP backend: 
htp_context.json 

```
{
"graphs": [
{
"graph_names": [
"<network-name>"
],
"O": 3,
"vtcm_mb": 8,
"hvx_threads": 4,
"finalize_config": {"P": 1}
}
],
"devices": [
{
"device_id": 0,
"soc_id": 62,
"dsp_arch": "v68"
}
]
}

```

To prepare a context binary with HTP optimization related parameters, use qnn-context-binary-generator with –config_file argument and give path to htp_context.json. 

```
${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtp.so \
--model model.so \
--binary_file model.serialized \
--profiling_level basic \
--config_file htp_context.json

```

#### Setting P points (HTP MCP backend)

For the HTP MCP (multicore) backend, `finalize_config` is passed through the `graph_prepare.conf` file and corresponds to `QNN_HTP_MCP_GRAPH_CONFIG_OPTION_FINALIZE_CONFIG` in `QnnHtpMcpGraph_CustomConfig_t`. 
graph_prepare.json 

- 
graph_prepare.json: 

```
{
"backend_extensions": {
"shared_library_path": "libQnnHtpMcpNetRunExtensions.so",
"config_file_path" : "graph_prepare.conf"
}
}

```

- 
graph_prepare.conf: 

```
{
"graphs": [
{
"graph_names": [
"qnn_model"
],
"O": 3,
"finalize_config": {"P": 1}
}
]
}

```

Next, to generate the serialized context binary, specify the graph_prepare.json file using the –config_file flag as follows: 

```
$ cd ${QNN_SDK_ROOT}/examples/Models/InceptionV3
$ cp ${QNN_SDK_ROOT}/lib/hexagon-v68/unsigned/libQnnHtpMcpV68.elf network.elf
$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtpMcp.so \
--model ${QNN_SDK_ROOT}/examples/Models/InceptionV3/model_libs/x86_64-linux-clang/libInception_v3_quantized.so \
--config_file graph_prepare.json \
--binary_file Inception_v3_quantized_qpc.serialized

```

#### HTP Performance Estimates

QNN can provide performance estimates for a graph using operator costs (i.e. execution cycle predictions) to simulate the target hardware. These estimates project the HTP performance and a confidence level for a graph. The information provided by the estimates is provided “as is” with no warranty of any kind. Every effort has been made to ensure the accuracy of the information provided by the estimates. 
However, there are no representations being made regarding the use of the estimates provided in terms of its correctness, accuracy vs. silicon, reliability, or otherwise. The estimates may vary and even show regressions on a per-graph basis across QNN releases. The information provided by the estimates is provided for informational purposes only and should not be relied upon for any other purpose. 
The following is a non-exhaustive list of assumptions and approximations made for the performance estimates: 

- 
Performance estimates may vary across SDK versions for a graph. 
- 
Each execution cycle prediction of an op is perturbed by an amount that is reflective of the actual errors we see while training models used for calculating the estimates. The whole graph is then simulated using the lower and upper execution cycles estimate for each op to produce the overall lower and upper estimates respectively. The lower and upper estimates provided by these performance estimates are approximations of the simulation accuracy vs. target silicon; they are not accuracy error bounds. 
- 
Performance estimates may assume kernel operator costs for operators which it currently does not model, which includes all operators derived from ‘custom ops’. 
- 
Performance estimates assumes burst performance mode, the HTP has full bandwidth to the DDR and that no other cores are using the DDR during the execution of the graph being simulated. 

- 
This is may not be true in reality, as the HTP has to share the DDR with other cores and other devices on the SoC (e.g. CPU, GPU, camera, etc.), which may or may not be active during the execution of the graph. 
Generating performance estimates: 
Generation of performance estimates requires the correct soc_id in the HTP backend extensions config. For e.g., the following json file uses soc_id = 52 which is the soc_id for the SA8650, SA8775 and SA8255 targets: 
htp_context.json 

```
{
"graphs": [
{
"graph_names": [
"graph1_name"
],
"vtcm_mb": 8,
"hvx_threads": 4
}
],
"devices": [
{
"device_id": 0,
"soc_id": 52,
"dsp_arch": "v73"
}
]
}

```

When running the qnn-context-binary-generator on HTP backend, specify the profiling level parameter and the htp_context.json (containing the right soc_id). 

```
$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtp.so \
--model model.so \
--binary_file model.serialized \
--profiling_level basic \
--config_file htp_context.json

```

Passing the HTP profiling reader (–reader libQnnHtpProfilingReader.so) to qnn-profile-viewer is important to get the correct layout of the performance estimates. 

```
$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-profile-viewer \
--input_log output/qnn-profiling-data.log \
--reader libQnnHtpProfilingReader.so

```

Similarly, use the following commands for HTP MCP backend: 

```
$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
--backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtpMcp.so \
--model model.so \
--binary_file model.serialized \
--profiling_level basic

$ ${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-profile-viewer \
--input_log output/qnn-profiling-data.log \
--reader libQnnHtpMcpProfilingReader.so

```

Performance estimate output: 

- 
Simulated (Accelerator exec cycles): Simulated execution cycles. 
- 
Simulated (Accelerator exec cycles (lower estimate)): Lower estimate of the simulated execution cycles. 
- 
Simulated (Accelerator exec cycles (upper estimate)): Upper estimate of the simulated execution cycles. 
- 
Bandwidth Stats per HTP in bytes: 

- 
Input Fill: Total reads from DDR for graph input related tensors (weights, bias, activations). This value counts all bytes of operators which do not have predecessors. 
- 
Intermediate Fill : Total reads from DDR for compiler generated data transfer to satisfy VTCM size constraints. This value counts all bytes of compiler generated fill operators which have predecessors and successors and originate on the same HTP. 
- 
Intermediate Spill : Total writes to DDR for compiler generated data transfer to satisfy VTCM size constraints. This value counts all bytes of compiler generated spill operators which have predecessors and successors and originate on the same HTP. 
- 
Inter HTP Fill : Total reads from DDR for fills which were generated by a different HTP core. This value counts all bytes of compiler generated fill operators which do not have a predecessor, but have a successor. 
- 
Inter HTP Spill : Total reads from DDR for spills which were generated by a different HTP core. This value counts all bytes of compiler generated spill operators which do not have a successor, but have a predecessor. 
- 
Output Spill : Total writes to DDR for graph output related tensors. This value counts all bytes of operators which do not have successors. 
Sample profiler output Finalize Stats: 

- 
With performance estimates: 

```
Finalize Stats:
Accelerator (finalize) time : 193364 us
Performance Estimates :
Mode : Burst
Simulated (Accelerator exec cycles) : 991608 cycles
Simulated (Accelerator exec cycles (lower estimate)) : 921188 cycles
Simulated (Accelerator exec cycles (upper estimate)) : 1094620 cycles
Bandwidth Stats :
HTP ID : 0
Input Fill : 24524800 bytes
Intermediate Fill : 0 bytes
Intermediate Spill : 0 bytes
Inter HTP Fill : 0 bytes
Inter HTP Spill : 0 bytes
Output Spill : 2048 bytes

```

- 
Without performance estimates: 

```
Finalize Stats:
Accelerator (finalize) time : 193364 us

```

## HTP Forced Preemption

The preemption feature is used to allow HTP resource sharing between clients on the HTP core. It does so by OS preempting lower priority application, handing over its resources to a higher priority application. When preempted, execution of lower priority graph(s) is (are) paused until execution of all higher priority graphs are completed. 
The primary use case for preemption is to enable higher priority clients to get access to resources on the DSP, taking them from lower priority clients. It specifically hands off control of VTCM and HMX to the incoming client. A typical use case would include a high-priority graph (i.e. a safety feature, crash detection, etc.) needing to run immediately over something low priority (i.e. detecting speed limit). 
Forced preemption is currently supported on SA8650, SA8775, SA8255 and SA8797. 
Assumptions of use: 

- 
Each application shall only execute graphs of a single priority. In other words, graphs of each priority shall be contained by separate arm-side processes. It is the user’s responsibility to ensure graphs of different priorities are not executed by the same application. 
- 
Higher priority threads shall trigger an immediate preemption of lower priority clients. 
Preemption example using qnn-throughput-net-run 
In this example, we shall run two instances of qnn-throughput-net-run, one of the instances shall execute a low priority graph, while the other executes a high priority graph. 

```
$ ./qnn-throughput-net-run --config sampleLowPriority.json&
$ ./qnn-throughput-net-run --config sampleHighPriority.json&

```

where sampleLowPriority.json has the following content: 
sampleLowPriority.json 

```
{
"backends": [
{
"backendName": "backend_1",
"backendPath": "libQnnHtp.so",
"profilingLevel": "BASIC",
"backendExtensions": "libQnnHtpNetRunExtensions.so"
}
],
"models": [
{
"modelName": "model_1",
"modelPath": <serialized_network_binary.bin>,
"loadFromCachedBinary": true,
"inputPath": <filename_for_input_list>,
"inputDataType": "FLOAT",
"outputPath": "model_low_output",
"outputDataType": "FLOAT_ONLY",
"saveOutput": "NATIVE_LAST"
}
],
"contexts": [
{
"contextName": "context_1",
"priority" : "LOW"
}
],
"testCase": {
"logLevel": "error",
"threads": [
{
"threadName": "thread_1",
"backend": "backend_1",
"context": "context_1",
"model": "model_1",
"interval": 0,
"loopUnit": "count",
"loop": 100,
"backendConfig": "thread1_settings.json",
"executeAsynchronous": true
}
],
"iteration": 1
}
}

```

and sampleHighPriority.json has the following content: 
sampleHighPriority.json 

```
{
"backends": [
{
"backendName": "backend_2",
"backendPath": "libQnnHtp.so",
"profilingLevel": "BASIC",
"backendExtensions": "libQnnHtpNetRunExtensions.so"
}
],
"models": [
{
"modelName": "model_2",
"modelPath": <serialized_network_binary.bin>,
"loadFromCachedBinary": true,
"inputPath": <filename_for_input_list>,
"inputDataType": "FLOAT",
"outputPath": "model_high_output",
"outputDataType": "FLOAT_ONLY",
"saveOutput": "NATIVE_LAST"
}
],
"contexts": [
{
"contextName": "context_2",
"priority" : "HIGH"
}
],
"testCase": {
"logLevel": "error",
"threads": [
{
"threadName": "thread_2",
"backend": "backend_2",
"context": "context_2",
"model": "model_2",
"interval": 0,
"loopUnit": "count",
"loop": 100,
"backendConfig": "thread2_settings.json",
"executeAsynchronous": true
}
],
"iteration": 1
}
}

```

Preemption Verification with qnn-profile-viewer 
The output of qnn-profile-viewer shall indicate the number of times a graph was preempted (number of times it yielded) as: “Backend (QNN Num times yield occurred): X count”. 

## Asynchronous Execution

A QNN API implementation that allows asynchronous execution of graphs. Details specific to HTP backend implementation are provided in the following page: 

- Asynchronous graph execution for HTP backend 

## Qmem Graph (shared_buffer only graph)

A QNN HTP BE specific feature that allows users to use data buffers for shared access in between processing domains in QNN HTP backend. Using shared buffers can eliminate data copy in between client code on the host CPU and HTP accelerator. 

- QNN HTP Qmem Graph 

## HTP Session & Artifact Usage Guidelines

### Supported Library Use

QNN only supports one set of libraries per-process. These libraries must be of the same SDK version and have matching Hexagon architecture versions. For illustrative purposes, V73 Hexagon architecture libraries will be used in the diagrams; the same guidelines apply for non-V73 artifacts as well. The supported library layout for QNN is displayed below. 

To prevent incorrect QNN library layouts, Qualcomm recommends the following: 

- 
One copy of each library should be present for a single process (backend, stub, skel, etc). 
- 
The backend library (libQnnHtp.so) should be explicitly loaded with dlopen rather than being dynamically linked as a dependency. 
- 
During the loading of Stub.so and Prepare.so, QNN first searches for them in the path of libQnnHtp.so. If not found, it searches in LD_LIBRARY_PATH. 
- 
Libraries should be in the same directory as one another (the skel is an exception to this so long as the ADSP_LIBRARY_PATH is correctly set to find the library). 
- 
Do not rename libraries to load multiple copies as this is not supported. 

### Unsupported Library Use

QNN does not support multiple copies of the QNN backend library (libQnnHtp.so) being accessed in a single process. Two different layouts are depicted below where multiple backend libraries are present on device. 

For two backend libraries to be loaded, either a second copy of the library is explicitly loaded from a separate directory than where the first library is located, or a duplicate filesystem is created during process execution (adb remount for android targets). In either case, the two artifact layouts shown above are not supported. If these layouts are detected during runtime, QNN_COMMON_ERROR_LOADING_BINARIES will be returned from the following APIs: 

- 
QnnBackend_registerOpPackage 
- 
QnnBackend_validateOpConfig 
- 
QnnContext_create 
- 
QnnDevice_create 
- 
QnnGraph_create 

## Graph Switching (Beta)

Note 
This feature is currently in experimental beta release, any proposed method of usage and behavior may change in future releases. 
This feature is used only when client requires further reduction in RAM at the cost of execution speed. The feature currently has limitations as stated below: 

- 
This feature does not support concurrent graph execution between the switching graphs. 
- 
This feature can not be used together with QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES config. 
- 
Memory is reduced at the cost of slower first execution after graph switching. 
This feature is a way to lazy load models when needing to execute. This allows multiple graphs to be enabled but in unloaded state, and keeping only one graph(ie. graph1) fully loaded and ready to execute in a low memory mode to reduce sustained memory usage. When an unloaded graph(ie. graph2) needs to execute, graph switching (lazy unload graph 1 and load graph2) will take place automatically if this graph switching feature is enabled. The amount of memory saved roughly depends on the total size of the models that are in unloaded state. To enable this feature in code: (Beta)

```
1QnnContext_Config_t* isPersistentBinaryConfig = new QnnContext_Config_t;
2isPersistentBinaryConfig->option = QnnContext_ConfigOption_t::QNN_CONTEXT_CONFIG_PERSISTENT_BINARY;
3isPersistentBinaryConfig->isPersistentBinary = 1;
4
5QnnContext_Config_t* memoryLimitHintConfig = new QnnContext_Config_t;
6memoryLimitHintConfig->option = QnnContext_ConfigOption_t::QNN_CONTEXT_CONFIG_MEMORY_LIMIT_HINT;
7memoryLimitHintConfig->memoryLimitHintConfig = 1; //any non-zero value
8
9QnnContext_Config_t* cfgs[] = {isPersistentBinaryConfig, memoryLimitHintConfig, NULL};
10QnnContext_createFromBinary(..., cfgs, ..., &contextHandle, ...);

```
Example in backend extension config: (Beta)

```
{
"backend_extensions" :{...},
"context_configs" :
{
"is_persistent_binary" : boolean_value,
"memory_limit_hint" : uint64_value,
"enable_graphs" : ["<graph_name_1>", "<graph_name_2>", ...]
}
"graph_configs" : [{...}]
}

```

- 
is_persistent_binary: default false; to use graph switching feature, is_persistent_binary is required. 
- 
memory_limit_hint: default 0, set to any non zero value to enter low memory mode with graph_switching enabled. 
- 
enable_graphs (optional): the name of the graphs that will be enabled. When graph switching feature is enabled, only the first graph in the enable_graphs list is loaded. When this field is left out during graph switching mode, it signals all graphs in the serialized binary are enabled, and only the first graph in the serialized binary is loaded. User can strategically specify the order of graphs in this enable_graphs field to control which graph to load first. 

Note 

- 
When is_persistent_binary is enabled, it is advised for the client to use mmap to map the binary file passed to the QnnContext_createFromBinary API call. Once the API call is finished, it is also advised that the client should use techniques such as madvise() to free up some memory held by mmap. Otherwise, the client may experience a high sustained memory due to holding onto the persistent binary. For the same reason, a non-mmaped implementation strategy is not recommended. 
- 
Client is responsible for keeping this mmapped buffer alive in the lifetime of context so graph reloading could happen. Client must not unmap the fd until QnnContext_free is invoked. Freeing/ummaping the buffer prematurelly will result in undefined behavior. 
- 
Client is also responsible for freeing up the mmapped buffer when destroying context, other wise may introduce memory leak. 
- 
For the memory_limit_hint, any non-zero value will enable graph switching. Values greater than zero, will only indicate the low memory mode. Any specific memory_limit_hint value will not affect the graph switching behaviour. 
- 
Client is responsible for creating and freeing buffer used to store spill fill buffer and weights buffer. 

## Multi-Graph Switching (Beta)

The Multi-Graph Switching feature enhances the existing graph switching, allowing multiple graphs to be loaded and retained in memory at once. It improves the execution speed compared to traditional graph switching, which unloads the current graph before loading a new one. This feature balances memory usage and execution efficiency. 
To enable Multi-Graph Switching, users must configure a “graphs_retention_order” which is an ordered list of graph names that defines their retention priority. Along with this, users must set memory_limit_hint > 0 and is_persistent_binary = true similar to traditional graph switching. Example in backend extension config: (Beta)

```
{
"backend_extensions" :{...},
"context_configs" :
{
"is_persistent_binary" : boolean_value,
"memory_limit_hint" : uint64_value,
"enable_graphs" : ["<graph_name_1>", "<graph_name_2>", ...],
"graph_retention_order": ["<graph_name_1>", "<graph_name_2>", ...]
}
"graph_configs" : [{...}]
}

```

- 
graph_retention_order: This field defines an ordered list of graph names to be retained in memory. Graphs earlier in the list have higher retention priority. When the Multi-Graph Switching feature is enabled, it will preload all of the graphs in this list, provided they all fit into the current PD’s virtual address space. Any other graph that is not part of this list will be lazy loaded at execution. Additionally, if the memory is limited, then the retained graphs may get unloaded, starting with the least important or graphs lower in the retention list. If the graph_retention_order is configured without memory_limit_hint, then the retention feature will be disabled. If other graph switching configurations are set and this field is left empty, traditional graph switching will be triggered. The graph_retention_order can be dynamically updated after the initial deserialization. The new retention list can be modified as follows: 

```
1const char* const new_retention_list[] = {"graph1", "graph2", NULL};
2QnnContext_Config_t* graphRetentionOrderConfig = new QnnContext_Config_t;
3graphRetentionOrderConfig->option = QnnContext_ConfigOption_t::QNN_CONTEXT_CONFIG_GRAPH_RETENTION_ORDER;
4graphRetentionOrderConfig->graphRetentionOrder = const_cast<const char* const* const>(new_retention_list);
5
6QnnContext_Config_t* contextConfig[] = {graphRetentionOrderConfig, NULL};
7QnnContext_setConfig(contextHandle, contextConfig);

```

When a new graph retention list is provided, it overrides the existing one. Example: If the original list at preload time includes graphs [“A”, “B”], then graphs A and B are loaded and retained. Later, if QnnContext_setConfig is called with a new list [“E”, “F”], the retention list is reset and graphs A and B are no longer set for retention. This change takes effect only in subsequent executions. When the next graph execution begins through switching, graphs A and B will be unloaded. Graphs E and F are executed and retained as they come. 
Additionally, if graph_retention_order is set to an empty list, retention is disabled, and will fallback to traditional graph switching. 
- 
is_persistent_binary: This field is required to be set to true for using the Multi-Graph Switching feature. The default is false. 
- 
memory_limit_hint: This field can be set to any non-zero value to enter low memory mode to enable multi-graph switching. The default is 0. 
- 
enable_graphs (optional): This field sets the name of the graphs that will be enabled. In case of Multi-graph switching, the graph names in the retention list should be in the enabled state as these are loaded during deserialization. When this field is left out during graph switching mode, it signals all graphs in the serialized binary as enabled but proceeds with loading graphs that are part of the retention list. 
Example: Multi-Graph Switching with Retention Order 
Assume a binary contains four graphs: A, B, C, and D, each of equal size. The configuration sets graph_retention_order: [“A”, “B”], memory_limit_hint > 0, and is_persistent_binary = true. For these examples, assume the available memory can hold at most two graphs simultaneously. 
Case 1: Execution order A, B, A, B 
In multi-graph switching, all graphs from the retention list are loaded during deserialization as long as they fit into the memory. Here graph A and B can both fit, hence are loaded and ready to execute. 

- 
When graph A comes for execution, it is already loaded and ready to execute. 
- 
When graph B comes for execution, it is already loaded and ready to execute. 
- 
When graph A comes for execution again, it is already loaded and ready to execute. 
- 
When graph B comes for execution again, it is already loaded and ready to execute. 
Because both A and B fit within the memory limit and are in the retention list, they stay loaded throughout all executions — eliminating any switching overhead for this sequence. 
Case 2: Execution order A, B, C, D 
In multi-graph switching, all graphs from the retention list are loaded during deserialization as long as they fit into the memory. Here graph A and B can both fit, hence are loaded and ready to execute. 

- 
When graph A comes for execution, it is already loaded and ready to execute. 
- 
When graph B comes for execution, it is already loaded and ready to execute. 
- 
Graph C is not loaded, so graph switching is triggered. The runtime checks whether A, B, and C can all fit in memory. Because no non-retained graphs are present to free first, and all three cannot fit, the least important retained graph B is unloaded. With only A and C in memory, both fit — so C is loaded and executed. 
- 
Graph D is not loaded, so graph switching is triggered. Graph C is unloaded first, because it is not part of the retention list. With A and D in memory, both fit — so D is loaded and executed. Because D is not part of the retention list, it will be a candidate for unloading at the next switch. 
This example shows that even retained graphs may be unloaded under memory pressure. Non-retained graphs are always the first candidates for eviction when a switch occurs. 
Case 3: Execution order A, B, C, A, B, D 
In multi-graph switching, all graphs from the retention list are loaded during deserialization as long as they fit into the memory. Here graph A and B can both fit, hence are loaded and ready to execute. 

- 
When graph A comes for execution, it is already loaded and ready to execute. 
- 
When graph B comes for execution, it is already loaded and ready to execute. 
- 
Graph C is not loaded, so graph switching is triggered. The runtime checks whether A, B, and C can all fit in memory. Because no non-retained graphs are present to free first, and all three cannot fit, the least important retained graph B is unloaded. With only A and C in memory, both fit — so C is loaded and executed. 
- 
Graph A is already loaded and executes immediately — no switching occurs. Graph C remains in memory because no switch was triggered, but it is a candidate for unloading at the next switch event. 
- 
Graph B is not loaded, so graph switching is triggered. Graph C is unloaded first, because it is not part of the retention list. With A and B in memory, both fit — so B is loaded and executed. Because B is part of the original retention list, it is treated as a retained graph again going forward. 
- 
Graph D is not loaded, so graph switching is triggered. The runtime checks whether A, B, and D can all fit in memory. Because no non-retained graphs are present to free first, and all three cannot fit, the least important retained graph B is unloaded. With A and D in memory, both fit — so D is loaded and executed. 
This example shows that even retained graphs may be unloaded under memory pressure. The number of graphs that can remain loaded at any time depends on the retention list size, individual graph sizes, execution order, and the PD memory limit. 

Note 
This feature is currently in experimental beta release, and currently has following limitations/constraints: 

- 
Graphs from the graph_retention_order list are preloaded based on the PD’s virtual address space limit. Users need to configure this list to ensure all graphs can be preloaded. If all graphs from this list cannot be deserialized on any of the PDs then this list needs to be reconfigured taking into account the graph’s memory usage. 
- 
The number of retained graphs is highly dependent on the number of graphs in the retention order, execution order, PD limit, and the actual memory usage of the graphs. It may not retain all the graphs from the provided list. 
- 
At any point, memory pressures may cause any of the retained graphs to be unloaded. 
- 
During multi-graph switching, graphs not in the retention list and not currently executing will be unloaded during switching. 
- 
If the graph retention order is empty, by default traditional graph switching will be triggered. 
- 
Compared to traditional graph switching, the RAM usage will be higher because more graphs stay loaded. 
- 
For the memory_limit_hint, any non-zero value will enable graph switching. Values greater than zero will only indicate the low memory mode. Any specific memory_limit_hint value will not affect the graph switching behaviour. 
- 
Multi-graph switching does not support concurrent graph execution between the switching graphs. 
- 
Multi-graph switching cannot be used together with the QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES configuration. 
- 
The graph retention list can be updated dynamically, but the change does not take immediate effect. It does not trigger unloading of currently loaded graphs. The updated list will be applied in the next executions. 

## Benefits of batch inference and multi-threaded inference

Multi-threading hides the cost of CPU/HTP communication time (RPC). In a single threaded inference the utilization of HTP hardware is not efficient as shown in the example below. 

Compare this to the multi-threaded inference. Here, the time used by RPC of the first inference is masked by the inference in the second thread. Thus, HTP hardware spends less idle time. Similarly, using multiple inferences per batch improves the reuse of weights memory, thus avoiding reloading the weights. It is to be noted here that the Activation size is a factor which affects the benefit of using batches. 

- 
With low resolution models, weights and activations fit in VTCM. In this case, using batched activations reuses the weights for all the activations, thus increasing the efficiency per batch. 
- 
With large resolution models, as the activations take up more VTCM space, therefore weights are reused for lesser number of activations. This reduces the reuse efficiency of weights per batch. 
Example: 
With SNPE 2.9, SM8550 (Kailua.LA.1.1-00190 GKI.HY11-1) shows approximately 1.8x benefit for resnet50 when using snpe-parallel-run. 

- 
Baseline: Using 1 batch and 1 thread achieves approximately 1500 inf/s 
- 
Batching and multithreading: Using batches of 5 and 2 threads achieves approximately 2800 inf/s 

## Hexagon NPU Runtime Driver (Windows Only)

Hexagon NPU Runtime Driver (HNRD) is available for Windows based Snapdragon® X Series Platforms. HNRD is designed to be forward and backward compatible with Qualcomm AI Stack SDKs. With HNRD in the system, applications have the option to unbundle from QNN HTP platform dependent libraries, and allows these applications to be portable over to older and newer Windows platforms. HNRD is packaged and distributed independently from the QNN SDK and it is currently packaged with the device BSP. OEMs use the BSP to pre-install HNRD on their devices. 
Switching Between Traditional and HNRD Paths 
Pre-driver:

- 
Traditional path only; applications build with the QNN SDK and bundle the QNN HTP platform dependent libraries Post-driver:

- 
Traditional and HNRD paths available; the application can choose which path to use 
- 
Traditional path; applications build with the QNN SDK and bundle the QNN HTP platform dependent libraries with the application (Default – same as pre-driver) 
- 
HNRD path; applications build with the QNN SDK, but utilize the platform dependent libraries installed on the system 
- 
Note: there is no difference in the build step between traditional and HNRD paths. In addition, the bundling and choosing between traditional and HNRD paths is applicable to both QNN and SNPE 
In other words, if an application bundles the QNN HTP platform dependent libraries (i.e., QnnHtpV73Stub.dll and QnnHtpV73Skel.so), it will default to choose the traditional path. Otherwise, if the platform dependent libraries are not bundled, it will fall back to choose the HNRD path. 
The following sample logs illustrate the HNRD path being applied: 

```
1 0.0ms [WARNING] QnnDsp <W> Traditional path not available. Switching to user driver path
2 0.0ms [WARNING] QnnDsp <W> HTP user driver is loaded. Switched to user driver path

```

Compatibility Support 
The minimum QNN and SNPE version is 2.22.2. Applications can be built with older or newer versions of the QNN SDK and they will still work on the device. Depending on the HNRD version installed on the system, new features may not be supported. 
Note that traditional and HNRD paths can co-exist on the platform and each application independently selects whether to use traditional or HNRD paths. 
Context Binary Management 
When utilizing general/api_overview:Context Caching with HNRD, it requires special attention on managing the context binaries. When using HNRD path, online prepare and context binary loading are done by HNRD since they are platform-dependent. A context binary generated by one version of HNRD might not be able to run on HNRD with an older version or might not utilize all software / hardware capabilities on HNRD with a new version. It is required to check the compatibility of saved context binaries with the HNRD every time before loading, since the HNRD installed on a device can be upgraded (or downgraded) at any time. 
QnnContext_createFromBinary() checks the compatibility automatically before loading the context binaries. Setting QnnContext_BinaryCompatibilityType_t can control whether to fail sub-optimal context binaries during compatibility check. QnnContext_validateBinary does a similar check as QnnContext_createFromBinary() except it won’t create the context. 
If a context binary fails to pass QnnContext_createFromBinary() or QnnContext_validateBinary, performing online prepare to create another valid context binary can solve the problem. To reduce the latency impact of online prepare, one can continue execution with the original sub-optimal context binary while doing online prepare in a background thread and switch to the new context binary once online prepare is done. Or one can first do a fast prepare (e.g., by setting QNN_HTP_GRAPH_OPTIMIZATION_TYPE_FINALIZE_OPTIMIZATION_FLAG to 1) to execute with a sub-optimal context, and then do a slow prepare (e.g., by setting QNN_HTP_GRAPH_OPTIMIZATION_TYPE_FINALIZE_OPTIMIZATION_FLAG to 3) in a background thread. 
Note that online prepare may not succeed. This can happen when a model converted by a newer SDK version uses features that are not supported by the HNRD. In such a case, upgrading the HNRD. The following example shows how to handle the context binary management. 

```
1Qnn_ContextHandle_t context; // The context used to do inference
2std::future<Qnn_ContextHandle_t> futureContext; // The context being created in background
3
4// Create from binary and set compatibility mode to strict to check sub-optimality.
5Qnn_ErrorHandle_t result =
6 doCreateFromBinary(QNN_CONTEXT_BINARY_COMPATIBILITY_STRICT, &context);
7
8if (result != QNN_SUCCESS) {
9 if (result == QNN_CONTEXT_ERROR_BINARY_SUBOPTIMAL) {
10 // The context binary is valid but sub-optimal.
11 // Continue execution with this context binary.
12 // Set compatibility mode to permissive to bypass sub-optimality check.
13 doCreateFromBinary(QNN_CONTEXT_BINARY_COMPATIBILITY_PERMISSIVE, &context);
14 } else if (result = QNN_CONTEXT_ERROR_CREATE_FROM_BINARY) {
15 // The context binary cannot run.
16 // Do fast prepare with optimization level 1 without saving the context binary to file.
17 context = doOnlinePrepare(HTP_OPTIMIZATION_LEVEL_1, NO_SAVE_CONTEXT);
18
19 // Here assumes nullptr will be returned if online prepare fails.
20 if (context == nullptr) {
21 // If online prepare fails, one possible reason could be HNRD is too old for the
22 // graph. In such case, prompt the users to upgrade HNRD.
23 message(ERROR, "The HNRD is too old. Please install latest HNRD.");
24 }
25 } else {
26 throw std::runtime_error("Skip handling of other errors.");
27 }
28
29 // Now we have a sub-optimal context in variable "context".
30 // Do online prepare in another thread to produce the optimal context and save it.
31 futureContext = std::async(std::launch::async, doOnlinePrepare, HTP_OPTIMIZATION_LEVEL_3,
32 SAVE_CONTEXT);
33}
34
35// Do inference.
36while (waitInputData()) {
37 // Switch to optimal context if prepare is done.
38 if (futureContext.valid() &&
39 futureContext.wait_for(std::chrono::seconds(0)) == std::future_status::ready) {
40 context = futureContext.get();
41 }
42
43 doInference(context);
44}
45
46// Clean up.
47doFreeContext(context);

```

## QnnContext_createFromBinaryListAsync API

This API provides a method for asynchronously initializing multiple context. Currently only supported on Mobile and Windows platforms. binaries (models) in a single API call. 
It offers two primary features: 

- 
Initialization Time Optimization: Optimizes the time taken to initialize the context binaries. 
- 
Resource Sharing: Optimizes runtime memory usage and/or HTP virtual address space based on the HTP custom context configurations QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES and QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES_OPTIMIZATION_TYPE 
Notifications and Handles: 

- 
For each graph within a context, and for all contexts, a notification will be sent after they are initialized along with the initialization status. 
- 
These notifications may arrive before or after the API returns. 
- 
The order of the notification depends on the initialization time of each context and is not guaranteed to follow any sequence. 
- 
For a context with multiple graphs, there will be a notification for each graph and an additional one for the context. 
- 
Valid graph and context handles will be sent back to the client through these notifications. Clients can then freely use these handles. 
Guidelines and Limitations of using this API 

- 
It is highly advisable to use a single thread to call this API and no other QNN API should be called in parallel. 

- 
Multiple application threads trying to initialize multiple use cases in parallel is not fully supported and can result in Input/Output Memory allocation or mapping failures during graph execution or during `QnnMem_register()` call. 
- 
We internally parallelize model initialization (the driver decides at runtime whether to do it on CPU, HTP, or both), fully utilizing the backend to minimize the initialization time. Therefore, calling any other QNN API in parallel can result in over-subscription and degrade performance, which is counterproductive. 
- 
Initialization time optimization may be impacted if the runtime SDK version and the SDK version used to prepare context binaries are mismatched, due to backward compatibility requirements. 

- 
Following below is an example in terms of initialization time: .. list-table: 

```
:header-rows: 0
:widths: auto

* - serialized.bin
- SDK 2.42 Runtime
- SDK 2.43 Runtime
* - Prepared on SDK 2.42
- Optimized
- Impacted
* - Prepared on SDK 2.43
- Not Supported
- Optimized

```

- 
Clients should not use the context configuration option `QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_MULTI_CONTEXTS` or `QNN_HTP_CONTEXT_CONFIG_OPTION_REGISTER_CONCURRENT_RESOURCE_SHARING` to register multiple contexts, as they are explicitly designed for the `QnnContext_createFromBinary()` API. 
- 
When the HTP custom context configuration (QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES) is enabled, it is recommended that clients use I/O buffers that utilize the Multi-Tensor shared buffer. This method maps a group of tensors to a single shared buffer, optimizing both space and initialization time. Additionally, when using this option, the batch size of the graph should be set to 1 to prevent reallocation of the shared buffer. Data Callback:
During context creation from binary, QNN requires certain sections of the context binary, such as weights, etc to be available in buffers that will eventually be mapped onto HTP. By default, QNN allocates memory and copies the required data from the context binary. Alternatively, callers can register callbacks using the `callback` parameter in `QnnContext_ParamsV2_t`. When QNN needs data from the context binary, it will invoke these callbacks. The callback implementation is responsible for: 

- 
Allocating memory 
- 
Populating memory with the requested data 
This approach allows users to apply platform-specific strategies for efficient memory allocation and data retrieval, resulting in improved overall performance. If callbacks are not provided, QNN will handle data retrieval internally. For details on callback usage and platform-specific considerations, see (QnnContext_createFromBinaryWithCallback). Note:

- 
Using data callbacks is applicable only for scenarios where the context binary is present on the disk. In other scenarios, the API returns `QNN_CONTEXT_ERROR_UNSUPPORTED_FEATURE`. 
- 
Using data callbacks when `QNN_HTP_CONTEXT_CONFIG_OPTION_SHARE_RESOURCES_OPTIMIZATION_TYPE` is set to `SEQUENTIAL_WITH_VA_OPTIMIZATION` is not supported. The API returns `QNN_CONTEXT_ERROR_UNSUPPORTED_FEATURE`. Optimizing HTP VA space for buffers allocated using callbacks is not supported. 
Usage Diagram: 
Refer to the following diagram for a visual representation of the API usage. 

## Multiple cDSP Sessions

Note 

- 
This feature is often referred to as Multi-PD, a term that can be misleading as PD (process domain) technically denotes a different transport session. 
- 
Please refer to Hexagon SDK Documentation for more information. 

- 
Certain Snapdragon SoCs are capable of creating and opening multiple sessions within cDSP from a single CPU application. The number of concurrent sessions that can be supported per CPU application process, as well as the total number of sessions that can be supported within cDSP, are dependent on the hardware configuration of the SoC. 
- 
cDSP is a 32-bit processor, and each session supports a virtual address space of 4 GB; however, only 3.75 GB of this space is usable. 
- 
The maximum amount of memory per CPU process is constrained by either the total free RAM available, or the product of the number of sessions supported per CPU process and 3.75 GB, depending on which of the two is lesser. 
Model Loading Across Multiple Sessions 
Here are some key aspects: 

- 
When loading the offline-prepared model (via `QnnContext_createFromBinary()`, `QnnContext_createFromBinaryWithSignal()`, or `QnnContext_createFromBinaryListAsync()`), the backend will attempt to deserialize the model on the first available session. If the attempt fails due to memory availability, the backend will attempt to deserialize the model on the next available session. 
- 
The initialization time for a model, which will be deserialized on sessions other than the first one, will be slightly higher. This is because the backend will try to load the model on all of the available sessions sequentially. 
- 
A single context binary cannot be split across multiple sessions. Therefore, it is expected that the client will split the model before running QNN converter tools. Optimum split points are decided based on the network topology, precision used (a8w4, a16w4, a8w8, a16w8, etc.), and the maximum virtual address space per session. 
- 
Although the maximum usable virtual address space available per session is 3.75 GB, it is recommended that clients limit the heap required for a single context binary to be under 3 GB. 
- 
The decision for which session to use for a context binary is determined by the backend. There is no API to specify the session while loading a context. The sessions used are transparent to the client application. 
The following example will help understand the workings of multi-sessions. The algorithm may vary slightly based on the API used to initialize the models; however, the backend will generally try to efficiently pack the models on a single session. 

- 
Consider an ideal scenario with no fragmentation and the memory required for the model deserialization is exactly equal to the context binary size. Assume we have five (5) models with sizes 1.5 GB, 1.5 GB, 1 GB, 1 GB, and 250 MB respectively, with the models loaded sequentially in this order. 
- 
The first model’s deserialization attempt will happen on session #0. Assuming session #0 has nothing loaded right now, it will get deserialized successfully. 
- 
The second model will also get deserialized on session #0 as there is still space available on it. At this point, session #0 now has 3 GB of virtual space occupied. 
- 
The third model’s deserialization attempt will happen on session #0 first. This will be unsuccessful as there is not enough space available on it. Then backend will create a new sesion (say session #1). Session #1 has 3.75 GB available and hence the third model will be deserialized successfully on it. 
- 
The fourth model’s deserialization attempt will be made on session #0 first. This will be unsuccessful for the same reason as the third model. The next deserialization attempt will be made on session #1. This will be successful as only 1 GB was occupied on session #1. 
- 
The fifth model’s deserialization attempt will be made on session #0. As the model requires 250 MB and we have that much space available on session #0, the model will be successfully deserialized on it. 
Please refer to the diagram below for an illustration of this example: 

Guidelines and Limitations for I/O Buffer Allocation/Mapping Failures: 
As described earlier, QNN HTP runtime uses an Efficient Packing mechanism to deserialize context binaries on different sessions. This means that if a context binary can fit on the current session, then the backend will deserialize the context binary on that session. If it does not fit, then it will try on the next session. 
Typically in use-cases where shared buffers are used for inputs and outputs (I/O), clients register externally allocated memory with the backend through the `QnnMem_register` API (avoids time spent on memory copy). When the models are large (LLMs/LVMs), their I/O also tend to be larger. In those cases, clients might encounter memory registration failures. This happens due to the virtual address space being occupied enough that there is no space available for the I/O to be mapped. Although this is predominant in shared buffer use cases, it can happen without those as well. Note that the I/O buffers specific to a context need to be mapped to the same session where that context is deserialized. 
Consider the following example: 

- 
There are five (5) context binaries out of which the first four (4) context binaries can fit into the first session (session #0), and for the fifth context binary, the backend spawns a new session (say session #1) and deserializes it there. 
- 
After deserializing all the context binaries, the client application is trying to register I/O buffers for each of the contexts and one of the mappings fails due to unavailability of the space on session #0. 
- 
If context 4 would not have been deserialized on session #0, and provided the fifth context is on session #2, session #0 would have been less packed leaving space for those buffers. 
Please refer to the diagram below for an illustration of this scenario: 

Recommendations to address the issue: 

- 
Use the custom context configuration (QNN_HTP_CONTEXT_CONFIG_OPTION_IO_MEM_ESTIMATION) to make sure the space is available on the session for I/O buffers. This does have a limitation that models can only be initialized sequentially, and right after the model is initialized, the I/O buffers for that context should be registered. This also increases peak RAM during model initialization; however, sustained RAM usage would be identical to the case where this option is disabled. 
- 
Another potential way to alleviate the issue is to introduce a dummy memory registration call. If a client runs into a memory registration failure issue, they can register a dummy buffer that can force the context to deserialize on to the next session, thereby bypassing the issue. Clients would be expected to free the dummy registered memory after the initialization of all contexts is completed. 
- 
Use the `QnnContext_createFromBinaryListAsync` API to initialize the large model use cases. This API can be enabled with virtual address space optimization with which the backend internally takes care of making sure enough space is available for I/O to be mapped later. So after all models are initialized, which can be done through a single API call, I/O can be safely mapped to the PD. 

## Enabling Async Init on older Context bins

To allow async inits on a particular context binary, the following condition must be met: * Use either the `QnnContext_createFromBinaryListAsync` API, or pass the QNN_HTP_CONTEXT_CONFIG_OPTION_INIT_ACCELERATION 

config option to the `QnnContext_createFromBinary` API. 

- 
The version of the context binary must match the version of the runtime libraries from the SDK. 
- 
If there is a version mismatch, a read-write accessible path must be provided via the QNN_HTP_CONTEXT_CONFIG_OPTION_CONCURRENT_DESERIALIZATION_PATCH config option. 
For cases when the context binaries are old here’s an example of what the backend extension file would look like: Example in backend extension config:

```
{
...
"context":
{
...
"init_acceleration": true,
"concurrent_deserialization_patch": "/path/to/patch_file",
...
}
...
}

```

- 
init_acceleration: Required only when using `QnnContext_createFromBinary`. If using `QnnContext_createFromBinaryListAsync`, this flag is not needed. Defaults to false. 
- 
concurrent_deserialization_patch: Must be a read-writeable path. On the first run (when versions mismatch), patch data is measured and stored in this file. Subsequent runs will use this patch data, enabling async initialization. 
NOTE : The first run will still involve patch measurement, so performance improvements from async init will not be noticeable initially. However, all subsequent runs will benefit from the improved initialization performance. 

## Init and Execute Cancellation

- 
Abort Signal It enables users to interrupt the API based on their requirement. 
- 
Timeout Signal It will automatically interrupt the API after a predefined timeout period. 
Init Cancellation The QnnContext_createFromBinaryWithSignal API allows users to interrupt the context creation process using either of the two signal types mentioned above. 

Execute Cancellation The QnnGraph_execute API can be interrupted by either of the two signal types mentioned above. 

For more details regarding signal config refer QnnSignal.h 

## Multicore

QNN HTP enables multicore graph execution on supported SoCs. Using this feature, customers can use QNN APIs and tools to split graphs for execution across multiple cores. 
Preparing graph offline for multicore 
During offline graph preparation, QNN user can set the number of HTP cores option to enable multicore graph compilation. The HTP backend defaults to single core compilation otherwise. 
To configure an HTP Backend extension for multicore, refer to the following example configuration. More documentation on HTP Backend extension can be found under QNN HTP Backend Extensions. 

```
{
"graphs": {"type": "array", "items": {
"type": "object", "properties": {
// Corresponds to the graph name provided to QnnGraph_create
// Used by qnn-context-binary-generator during offline preparation
"graph_name": {"type": "string"},

// Specifies the number of cores (2 cores in this example) will be used for graph_name compilation [optional] [default: 1]
// Used by qnn-context-binary-generator during offline preparation
"num_cores": 2
}
}
}

```

Doing multicore inferences on target device processor 
Multicore device handle can be created at runtime on device using QnnDevice_create() with option QNN_DEVICE_CONFIG_OPTION_PLATFORM_INFO in exhale_struct_structQnnDevice__Config__t. QNN clients can invoke QnnDevice_getPlatformInfo() to retrieve platform’s capabilities via exhale_struct_structQnnDevice__PlatformInfo__t. Based on the retrieved platform information, clients can customize their own exhale_struct_structQnnDevice__Config__t per device with the desired number of cores and its respective core ID list to create a multicore device handle. 

```
1// retrieve platform capabilities
2QnnDevice_PlatformInfo_t* platformInfoPtr{nullptr};
3QnnDevice_getPlatformInfo(&platformInfoPtr);
4
5// check platformInfoPtr for hardware device ID, num of cores supported per device ID and core types
6// populate deviceConfig based on the retrieved platform information
7
8QnnDevice_Config_t deviceConfig;
9deviceConfig.option = QNN_DEVICE_CONFIG_OPTION_PLATFORM_INFO;
10deviceConfig.hardwareInfo = platformInfoPtr;
11const QnnDevice_Config_t* deviceConfigs[] = {&deviceConfig, nullptr};
12
13// create multicore device handle based on the information populated in deviceConfig
14Qnn_DeviceHandle_t device;
15QnnDevice_create(deviceConfigs, &device);

```

Refer to the following diagram for a visual representation of the API usage. 

Multicore PerfInfrastructure requires the client to vote for each core involved in the use case. 

```
1// Multicore perf infrastructure
2QnnDevice_Infrastructure_t deviceInfra = nullptr;
3QnnDevice_getInfrastructure(&deviceInfra);
4
5QnnHtpDevice_Infrastructure_t *htpInfra = static_cast<QnnHtpDevice_Infrastructure_t *>(deviceInfra);
6QnnHtpDevice_PerfInfrastructure_t perfInfra = htpInfra->perfInfra;
7
8uint32_t powerConfigId[hwDeviceInfo.v1.numCores];
9for(uint32_t i=0; i < hwDeviceInfo.v1.numCores; i++) {
10 // Create unique power client for each core
11 perfInfra.createPowerConfigId(hwDeviceInfo.v1.deviceId, coreInfo[i].v1.coreId, &powerConfigId[i]);
12}
13
14QnnHtpPerfInfrastructure_PowerConfig_t powerConfig;
15/*
16 Please refer to "Qnn HTP Performance Infrastructure" description at QNN SDK doc
17 on how to create vote request: Qualcomm AI Engine Direct
18*/
19
20// Set power config with different performance parameters
21QnnHtpPerfInfrastructure_PowerConfig_t *powerConfigs[] = {&powerConfig, NULL};
22for(uint32_t i=0; i < hwDeviceInfo.v1.numCores; i++) {
23 // Vote for each core using unique power client
24 perfInfra.setPowerConfig(powerConfigId[i], powerConfigs);
25}

```

An example of HTP Backend extension configuration (for 2 cores with core IDs of 1 and 2 respectively) to be used for multicore device creation on target, specified through JSON is given below. More documentation on HTP Backend extension can be found under QNN HTP Backend Extensions. 

```
{
"devices": [
{
// Selection of the device [optional] [default: 0]
"device_id": 0,
// Array of cores to be used in multicore use case
"core_id": [1, 2],
// Type of core to be used in multicore use case [optional: 0 for NSP, 1 for HPASS] [default: 0]
"core_type": 0,
"cores":[
{
// Provide performance profile [optional] [default: "high_performance"]
"perf_profile": "burst"
}
]
}
]

```

} 
Limitations 
The following features are not supported for multicore graphs: 

- 
Multicore concurrency 
- 
Linting profile 
- 
Cancellation of context create from binary 
- 
Graph switching/selection 
- 
Spill fill buffer sharing 
- 
Weight sharing 

## Graph Priority

Setting Graph Priority involves assigning priorities to both HMX and HVX threads during graph processing. 
Priority Behavior 
When a priority level X is specified: (Assuming Y is the corresponding HTP Thread Priority, refer below table for mapping) 

- 
HMX thread(s) are assigned priority Y 
- 
HVX thread(s) are assigned priority Y + 1. 
NOTE : A higher numeric value indicates a lower execution priority. 
QNN to HTP Thread Priority Mapping 
The table below outlines how QNN priority levels map to HTP thread priorities: 
HTP Thread Priority Derivation 

QNN Priority Level 
HMX Thread(s) Priority 
HVX Thread(s) Priority 

QNN_PRIORITY_LOW / QNN_PRIORITY_LOWEST (0) 
0xC5 
0xC6 

QNN_PRIORITY_NORMAL_LOW (50) 
0xC3 
0xC4 

QNN_PRIORITY_NORMAL / QNN_PRIORITY_DEFAULT (100) 
0xC0 
0xC1 

QNN_PRIORITY_NORMAL_HIGH (150) 
0xBD 
0xBE 

QNN_PRIORITY_HIGH (200) 
0xBB 
0xBC 

QNN_PRIORITY_HIGH_PLUS (300) 
0xB6 
0xB7 

QNN_PRIORITY_CRITICAL (400) 
0xB1 
0xB2 

QNN_PRIORITY_CRITICAL_PLUS / QNN_PRIORITY_HIGHEST (500) 
0xAC 
0xAD 
For more details on priority levels, refer to the Hexagon SDK documentation. 

## Setting Graph Priority

HTP backend supports setting graph priorities by given parameter exhale_struct_structQnnGraph__Config__t with `QNN_GRAPH_CONFIG_OPTION_PRIORITY` when calling QnnGraph_create. 
The graph priority is set to `QNN_PRIORITY_DEFAULT` by default if no configuration options are provided. 
Clients may also modify the priority of an existing graph using QnnGraph_setConfig. Some priority levels may be restricted for general developers depending on the end device. 
HTP backend allows clients to modify graph priorities using QnnContext_setConfig by passing exhale_struct_structQnnContext__Config__t with `QNN_CONTEXT_CONFIG_OPTION_PRIORITY`. 
When calling this all graph priorities in this context are updated. 

## LLM native KVcache

The native KVcache feature is designed to optimize the LLMs performance executed on HTP backend. 
HTP backend transforms the input KV tensors to HMX format for HMX operations. This transformation process is expensive, and the overhead increases with longer context lengths. Native KVcache avoids this costly transformation by keeping Input KV tensors in HMX layout. 
This feature is typically integrated into an LLM pipeline, where the KV management module is responsible for updating the KVcache in the HMX layout format. 
Benefits 

- 
Improved TTFT and Token Rate, especially for large context lengths 
- 
Reduced Power Consumption, particularly beneficial for large context lengths. 
Constraints 

- 
Applicability to only GenAI LLM Models with KVcache mode 
- 
The KVcache tensors must be uint8 and symmetrically quantized 
- 
KVcache must be either fully in native format or not at all. Partial use, where some decoder layers use native KVcache while others do not, is not supported. 
- 
Context lengths should be a multiple of 256, attention head_dim should be a multiple of 64 
- 
The operator for concatenating new and old KV in attention must be ScatterElement instead of Concat, the supported single-head structures are illustrated in below graph. 
- 
For ARN where N is multiple of 32, the KV input must be in native format. The KV output can be either native or flat. Using native KV output can accelerate kvupdate during TTFT, compared to using flat KV output. 
- 
For ARN where N is not multiple of 32, AR1/4/8/16 are typically used. Please ensure 1) use native KV input 2) use flat KV output 3) scatter_index <= (context length - roundup(ARN,32)). In this case, the client is responsible for the conversion of flat-output to native-input 

How to enable 
To enable and use native KVcache, please ensure the input model meets all the constraints. Users can refer to the QC notebook to export a model that supports native KVcache. Then users need to specify the dataFormat for KV cache I/O tensors as QNN_TENSOR_DATA_FORMAT_HMX_WEIGHT_LAYOUT during qnn-context-binary-generator, use `--data_format_config` argument and give path to JSON file 

```
$ qnn-context-binary-generator --data_format_config DataFormatFile.json \
--backend [SDK_PATH]/lib/x86_64-linux-clang/libQnnHtp.so \
--dlc_path [MODEL].dlc \
--model libQnnModelDlc.so \
--output_dir [OUTPUT_DIR] \
--binary_file [CONTEXT_BIN] \
--config_file HtpConfigFile.json

```

A sample of DataFormatFile.json to enable both native KV input and native KV output. All KV tensors must be included. 

```
{
"graphs": [
{
"graph_name": ['...'],
"tensors": [
{
"tensor_name": "past_key_0_in",
"dataFormat": "QNN_TENSOR_DATA_FORMAT_HMX_WEIGHT_LAYOUT"
},
...
{
"tensor_name": "past_key_0_out",
"dataFormat": "QNN_TENSOR_DATA_FORMAT_HMX_WEIGHT_LAYOUT"
},
...
]
}
]
}

```

A sample of DataFormatFile.json to enable native KV input only. 

```
{
"graphs": [
{
"graph_name": [...],
"tensors": [
{
"tensor_name": "past_key_0_in",
"dataFormat": "QNN_TENSOR_DATA_FORMAT_HMX_WEIGHT_LAYOUT"
},
...
]
}
]
}

```

Note 
Currently QNN_TENSOR_DATA_FORMAT_HMX_WEIGHT_LAYOUT is only supported in native KVcache scenario. 
HMX_WEIGHT_LAYOUT explanation 
Assume the flat shape of a single head KV is [DIN, DOUT], where DIN represents the number of input channels and DOUT represents the number of output channels. The transformation from flat to HMX_WEIGHT layout is 
[DIN, DOUT] -> DOUT/KV_TILE_SIZE * [DIN, KV_TILE_SIZE] -> DOUT/KV_TILE_SIZE * [DIN/32, KV_TILE_SIZE/32, 8:DIN, 32:KV_TILE_SIZE, 4:DIN] -> [DOUT/KV_TILE_SIZE, DIN*KV_TILE_SIZE/1024, 1024] 
KV_TILE_SIZE is fixed in this SDK version. K_TILE_SIZE=256, V_TILE_SIZE=64 
Other Limitations 

- 
Cannot support v68 targets or lower 
- 
The maximum context length that can be supported is limited by the fixed VTCM size. For example, 2M VTCM won’t support 16K context length or above. 

## MaskedSoftmax

The MaskedSoftmax feature is designed to optimize the LLMs accuracy and performance executed on HTP backend. MaskedSoftmax is used to replace the Softmax(Add(In, Mask)) structure in attention block in LLMs. MaskedSoftmax can use the attention_mask tensor to mask out the invalid tokens in the softmax operation directly instead of using adding operation. 
Benefits 

- 
Improved LLMs accuracy 
- 
Improved TTFT and Token Rate, especially when valid tokens account for a small part of large context lengths 
Constraints 

- 
Applicability to only uint16 and uint8 quantization for now 
How to enable 
To enable MaskedSoftmax, model structure must be one of the following three patterns. 

Note 

- 
The difference of the above three kinds of structure is the datatype of attention_mask and how to treat the attention_mask as the condition of Where op. In pattern A and pattern B, the attention_mask is quantized to be uint16. In pattern C, the attention_mask is quantized to be uint8. In pattern A, entries with zero value in the attention_mask will be kept to perform softmax operation, and entries with non-zero value in the attention_mask will be masked out. In pattern B and pattern C, entries with zero value in the attention_mask will be masked out and entries with non-zero value in the attention_mask will be kept. 
- 
The Equal op in pattern A or NotEqual op in pattern B following attention_mask is used to convert the attention_mask to a mask tensor with 0 and 1, and the mask tensor is used as the condition of Where op. 
- 
The Where op is used to mask out the invalid entries in the input tensor, and the output of Where op is the input tensor of Softmax op. If the condition is false, the corresponding entry in the input tensor will be masked out and MaskedSoftmax will output 0 in the entry. If the condition is true, the corresponding entry in the input tensor will be kept and will be carried out softmax operation. 
- 
The Add(ReduceMin(Input), B) structure is used to avoid quantization/dequantization loss. To make sure that the output of softmax in the invalid entries is 0, the parameter B of Add op is currently constrained to be less than or equal to -20.0. 

## QnnContext_createFromBinaryWithCallback API

This API provides a callback-based registration mechanism for client-defined buffer allocators and data loading strategies, facilitating synchronous context creation. The client-defined callback is used to override the default buffer allocation and data loading behavior of QnnContext_createFromBinary, allowing for customized and flexible context binary loading. 
The Qnn_ContextBinaryCallback_t* callback parameter is extended to support external buffer allocation/deallocation and custom data loading workflows. It enables clients to define how context binary data is provided and released during context creation.This structure primarily includes the following client-defined callback functions: 

- 
Qnn_ContextBinaryDmaDataProviderFn_t / Qnn_ContextBinaryRawDataProviderFn_t 
- 
Qnn_ContextBinaryDmaDataReleaseFn_t / Qnn_ContextBinaryRawDataReleaseFn_t 
In each DataProviderFn_t callback, the client must ensure that the buffer for the requested data section has been properly allocated and the corresponding data has been fully loaded by the time the callback returns. Therefore, both buffer allocation and data loading schemes must be provided together to ensure correct and complete context creation behavior. For detailed definitions, refer to the Qnn_ContextBinaryCallback_t structure in QnnContext.h. 
Key Features: 

- 
Customized buffer management: Allows clients to manage external buffers and perform customized data loading strategies for context data (e.g., weights). 
- 
Initialization time optimization: When using Qnn_ContextBinaryDmaBufferCallback_t, clients can leverage direct I/O scheme to load context binary data directly into external DMA buffers. This avoids runtime copy from user-space to backend buffer and reduces memory and time overhead. Performance gain depends on the client’s callback implementation. 

Note In the Qnn_ContextBinaryCallback_t definition, two callback types are introduced:

- 
Qnn_ContextBinaryRawBufferCallback_t: For allocating standard raw buffers together with loading corresponding data. These buffers are typically user-space buffers not directly shared with the backend. 
- 
Qnn_ContextBinaryDmaBufferCallback_t: For allocating DMA buffers together with loading corresponding data. These buffers can be directly shared with the backend, enabling zero-copy context data loading. A typical use case is allocating and loading model weights buffers. 
Currently, only Qnn_ContextBinaryDmaBufferCallback_t is supported. The raw buffer callback type is reserved for future extensibility. The DMA buffer callback is currently only used for model weights allocation and data loading, referred to as the external weights-loaded buffer feature. 
Guidelines and limitations for using this API: 

- 
It is highly recommended that the callback implementation uses a direct I/O scheme in combination with Qnn_ContextBinaryDmaBufferCallback_t to achieve the zero-copy context binary data loading. 
- 
Proper data alignment support was introduced in the new context binary data format starting from qairt-2.37. Therefore, when using the direct I/O scheme, the context binary must be generated using qairt-2.37 or later. 
- 
This API is extended based on the QnnContext_createFromBinary API. 
- 
It is not supported together with the graph switch feature. 
- 
It is not supported together with the udma64 feature, which can be disabled during the graph preparation phase. 
- 
It is not supported together with the QNN_CONTEXT_CONFIG_OPTION_DEFER_GRAPH_INIT. 
- 
It is not supported together with the securePD model protection feature. 
- 
Currently it is only supported and validated on Android platform. Other platforms may require additional adaptation and validation. 
High-Level Usage Pipeline: 
Refer to the following diagram for a visual representation of the API usage. 

Steps to use the QnnContext_createFromBinaryWithCallback API: 
1. Define Callback Functions 
Implement the logic for buffer allocation together with context binary data loading. 
Users have to ensure that the following requirements are met for external buffers: 

- 
When using Qnn_ContextBinaryDmaBufferCallback_t, they have to be DMA buffers. 
- The buffer’s start address must be aligned to at least 4KB (page alignment). It is highly recommended that the valid data begins exactly at the start of the external buffer (i.e., dataStartOffset should ideally be 0). This recommendation is based on two key considerations.

- 
Starting from qairt-2.37, the context binary data format has been optimized to ensure that the main data section is 4KB-aligned relative to the file start, enabling efficient offset-based reads. 
- 
The backend currently has limitations in handling arbitrary dataStartOffset value during the mapping phase. 
- 
They must be at least the required size specified in the request parameter passed through the callback. 
- 
For external DMA buffers, the FDs returned by the dataProvider callback must always be distinct—each invocation must return a different FD. 
- 
After data loading, any modification to the external buffer should only be induced by QNN, otherwise behavior is undefined. 
- 
They must not be deallocated until QNN explicitly invokes the dataRelease callback. 
2. Create Context 
Use the QnnContext_createFromBinaryWithCallback API to create the context. 

- 
The dataProvider and dataRelease callbacks are registered with the context. 
- 
During context creation, QNN will invoke the registered dataProvider callback to allocate external buffers and load the required data.The callback might be invoked multiple times—once for each data section being loaded (e.g., shared weights and non-shared weights are handled separately). 
- 
Once the data is loaded(callback returns), QNN will handle the mapping of external buffers to ensure they are shared with the backend. 
- 
During context release, the dataRelease callback will be triggered to properly release the external buffers. 

### Linux/Android code example:

HTP external weights-loaded buffer example 

```
1// QnnInterface_t is defined in ${QNN_SDK_ROOT}/include/QNN/QnnInterface.h
2QnnInterface_t qnnInterface;
3// Init qnn interface ......
4// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code
5
6// Step 1. Define the DMA callback function
7Qnn_ErrorHandle_t dmaDataProviderFn(Qnn_ContextBinaryDataRequest_t req,
8 Qnn_ContextBinaryDmaDataResponse_t* dmaDataResponse,
9 void* notifyParam) {
10 // Implement buffer allocation and data loading processes
11 Qnn_ErrorHandle_t err = QNN_SUCCESS;
12
13 // notifyParam can be used to pass a custom instance for identifying which model to load.
14 std::pair<CustomClass*, uint32_t>* pair = reinterpret_cast<std::pair<CustomClass*, uint32_t>*>(notifyParam);
15 CustomClass* CustomClass = pair->first;
16 uint32_t contextId = pair->second;
17
18 if (req.size == 0) {
19 // handle error
20 return QNN_GRAPH_ERROR_INVALID_ARGUMENT;
21 }
22
23 //allocate dma buffer
24 int32_t memFd = -1;
25 const uint64_t alignOptimizedBufferSize = getAlignedSizeInBytes(PAGE_ALIGNED_SIZE, req.size);
26
27 BufferInfo bufferInfo;
28 err = CustomClass->derectIOScheme->allocateDmaBuffer(CustomClass->m_filePath[contextId], req.offset, alignOptimizedBufferSize, &bufferInfo);
29 if (bufferInfo.addr == nullptr) {
30 // handle error
31 return QNN_CONTEXT_ERROR_MEM_ALLOC;
32 }
33
34 dmaDataResponse->dmaBuffer.data = bufferInfo.addr;
35 dmaDataResponse->dmaBuffer.fd = bufferInfo.dma_fd;
36 dmaDataResponse->dataStartOffset = bufferInfo.paddingSize;
37 dmaDataResponse->alignedSize = bufferInfo.alignedSize;
38
39 //loading data to dma buffer
40 err = CustomClass->derectIOScheme->storeBufferData(bufferInfo);
41 if (err != QNN_SUCCESS) {
42 // handle error
43 return QNN_CONTEXT_ERROR_MEM_ALLOC;
44 }
45
46 return err;
47}
48
49Qnn_ErrorHandle_t dmaDataReleaseFn(Qnn_ContextBinaryDmaDataMem_t dmaDataMem,
50 void* notifyParam) {
51 // Implement buffer release process
52 Qnn_ErrorHandle_t err = QNN_SUCCESS;
53
54 // free dma buffer
55 err = CustomClass->derectIOScheme->deallocateDmaBuffer(dmaDataMem);
56 if (err != QNN_SUCCESS) {
57 // handle error
58 }
59
60 return err;
61}
62
63// Step2. Create the context with QnnContext_createFromBinaryWithCallback API
64std::pair<QnnApi*, uint32_t>* notifyParam =
65 new std::pair<QnnApi*, uint32_t>(this, static_cast<size_t>(contextIdx));
66
67Qnn_ContextBinaryCallback_t callback {
68 .type = QNN_CONTEXT_CALLBACK_DMA_BUFFER,
69 .dmaBufferCallback = Qnn_ContextBinaryDmaBufferCallback_t {
70 QNN_CONTEXT_CALLBACK_DMA_BUFFER_VERSION_1,
71 Qnn_ContextBinaryDmaBufferCallbackV1_t {dmaDataProviderFn,
72 dmaDataReleaseFn,
73 static_cast<void*>(notifyParam)}
74 }
75};
76
77Qnn_ContextHandle_t context;
78Qnn_ErrorHandle_t error = m_qnnFunctionPointers.qnnInterface.contextCreateFromBinaryWithCallback(
79 backend,
80 device,
81 config,
82 &callback,
83 binaryBuffer,
84 binaryBufferSize,
85 &context,
86 profile,
87 signal);
88
89if (error != QNN_SUCCESS) {
90// handle the error
91}
92
93// Execute graph
94// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code
95
96// Free context
97// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code for details
98if (QNN_CONTEXT_NO_ERROR != m_qnnFunctionPointers.qnnInterface.contextFree(context, profileBackendHandle)) {
99 // handle error
100}

```
Windows:
For Windows on Snapdragon (WoS), callbacks can use memory mapping of the context binary stored on disk to provide buffers back to QNN. This approach offers several benefits: 

- 
Lower system commit charge: Memory backed by a disk file reduces commit charge and alleviates pressure on the swap file. 
- 
Reduced RAM usage for shared models: When multiple instances of the same AI model run simultaneously, memory mapping minimizes RAM consumption compared to the QNN internal allocation. Memory-mapped pages share a single copy in RAM. 
- 
Efficient memory reclamation: If the OS needs to reclaim memory, it can do so without writing contents to the swap file as the memory mapped pages are backed by the file on the disk. Note:

- 
For the read-only memory mapping to work, there is dependency on underlying NPU driver and Windows OS. If the user tries to use the read-only memory mapping and the underlying platform doesn’t support then QNN returns `QNN_CONTEXT_ERROR_UNSUPPORTED_FEATURE` In this scenario, the caller can retry without the callbacks. 
- 
Only DMA data buffer callbacks are supported. 
- 
Because of memory alignment requirements, the callbacks are only supported from context binary version 3.3.3. If an older context binary is used, then QNN returns `QNN_CONTEXT_ERROR_INVALID_CONFIG`. 
- 
Memory mapping in callbacks is supported only on the WoS platform. 
Below is the sample code for implementing the callbacks using memory mapping on WoS. 

callbacks example implementation using memory mapping for WoS 

```
1// QnnInterface_t is defined in ${QNN_SDK_ROOT}/include/QNN/QnnInterface.h
2QnnInterface_t qnnInterface;
3// Init qnn interface ......
4// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code
5
6struct MapInfo_t {
7 HANDLE mappingHandle; // mapping handle
8 LPVOID basePtr;
9 std::uintmax_t fileSize;
10};
11// These constants are from the hexagon sdk header file remote_wos_ext.h
12// As we are not including the hexagon sdk include files, defining them here
13static constexpr int FASTRPC_ATTR_IMPORT_BUFFER = 256;
14static constexpr int FASTRPC_ATTR_READ_ONLY = 512;
15using RemoteRegAttr2Fn_t = void (*)(void* buff, size_t size, int fd, int attr);
16using RpcMemToFdFn_t = int (*)(void* buff);
17std::unordered_map<HANDLE, MapInfo_t> fileMapping; // key is file handle
18std::unordered_map<std::string, HANDLE> contextMap;
19HMODULE mLibcdsprpc = dlOpen("libcdsprpc.dll".c_str());
20RemoteRegAttr2Fn_t mRemoteRegAttr2Fn = dlSym(mLibcdsprpc, "remote_register_buf_attr2");
21RpcMemToFdFn_t mRpcMemToFdFn = dlSym(mLibcdsprpc, "rpcmem_to_fd");
22
23// Step1, define init, deinit and the callback functions
24// init need to be done for each context bin, before calling qnn api
25bool InitCallback(std::string contextBinaryPath) {
26 HANDLE fileHandle = CreateFileA(contextBinaryPath.c_str(),
27 GENERIC_READ,
28 FILE_SHARE_READ,
29 NULL,
30 OPEN_EXISTING,
31 FILE_ATTRIBUTE_NORMAL,
32 NULL);
33 if (fileHandle == INVALID_HANDLE_VALUE) {
34 // handle errors
35 }
36 // specify the length as file size
37 HANDLE mappingHandle = CreateFileMappingA(fileHandle, NULL, PAGE_READONLY, 0x00, 0x00, NULL);
38 if (mappingHandle == INVALID_HANDLE_VALUE) {
39 // handle errors
40 }
41 // Create the mapping
42 LPVOID basePtr = MapViewOfFile(mappingHandle, FILE_MAP_READ, 0, 0, 0);
43 std:uintmax_t fileSize = std::filesystem::file_size(contextBinaryPath.c_str());
44 if (fileSize == 0 || !basePtr) {
45 // handle error
46 }
47
48 fileMapping.insert({fileHandle, {mappingHandle, basePtr, fileSize}});
49 contextMap[contextBinaryPath] = fileHandle;
50 return true;
51}
52
53// deinit need to be done after calling qnn contextFree
54void deInitCallback(std::string contextBinaryPath) {
55 auto contextMapIter = contextMap.find(contextBinaryPath);
56 HANDLE fileHandle = contextMapIter->second;
57 auto iter = fileMapping.find(fileHandle);
58 Handle mappingHandle = iter->second.mappingHandle;
59
60 // now close the filemapping handle and the file handle
61 if (!CloseHandle(iter->second.mappingHandle)) {
62 // handle errors
63 }
64
65 if (!CloseHandle(fileHandle)) {
66 // handle errors
67 }
68
69 fileMapping.erase(iter);
70 contextMap.erase(contextMapIter);
71}
72
73Qnn_ErrorHandle_t dmaDataProviderFn(Qnn_ContextBinaryDataRequest_t req,
74 Qnn_ContextBinaryDmaDataResponse_t* dmaDataResponse,
75 void* notifyParam) {
76 HANDLE fileHandle = reinterpret_cast<HANDLE>(notifyParam);
77 const auto iter = fileMapping.find(fileHandle);
78 if (iter == fileMapping.end()) {
79 // handle error
80 }
81
82 if(!req.size || (req.size+req.offset) > iter->second.fileSize) {
83 // handle error
84 }
85
86 void* dataPtr = static_cast<char*>(iter->second.basePtr) + req.offset;
87 // Now register this memory with the rpc and get fd
88 mRemoteRegAttr2Fn(dataPtr,
89 req.size,
90 NULL,
91 FASTRPC_ATTR_IMPORT_BUFFER | FASTRPC_ATTR_READ_ONLY);
92 int fd = mRpcMemToFdFn(dataPtr);
93 if (fd == -1) {
94 // handle errors
95 // rpc is not able to handle this memory
96 return QNN_CONTEXT_ERROR_UNSUPPORTED_FEATURE;
97 }
98 dmaDataResponse->dmaBuffer.fd = fd;
99 dmaDataResponse->dmaBuffer.data = dataPtr;
100 dmaDataResponse->dataStartOffset = 0;
101 dmaDataResponse->alignedSize = req.size;
102 return QNN_SUCCESS;
103}
104
105Qnn_ErrorHandle_t dmaDataReleaseFn(Qnn_ContextBinaryDmaDataMem_t dmaDataMem,
106 void* notifyParam) {
107 // fd is set to -1 for de-registration
108 mRemoteRegAttr2Fn(dmaDataMem.dmaBuffer.data,
109 dmaDataMem.memSize,
110 -1,
111 FASTRPC_ATTR_IMPORT_BUFFER | FASTRPC_ATTR_READ_ONLY);
112
113 return QNN_SUCCESS;
114}
115
116// Step2. Create the context with QnnContext_createFromBinaryWithCallback API
117std::string ctxtBinPath = <Full file path to context binary>;
118InitCallback(ctxtBinPath);
119HANDLE notifyParam =contextMap[ctxtBinPath];
120
121Qnn_ContextBinaryCallback_t callback {
122 .type = QNN_CONTEXT_CALLBACK_DMA_BUFFER,
123 .dmaBufferCallback = Qnn_ContextBinaryDmaBufferCallback_t {
124 QNN_CONTEXT_CALLBACK_DMA_BUFFER_VERSION_1,
125 Qnn_ContextBinaryDmaBufferCallbackV1_t {dmaDataProviderFn,
126 dmaDataReleaseFn,
127 static_cast<void*>(notifyParam)}
128 }
129};
130
131
132Qnn_ContextHandle_t context;
133Qnn_ErrorHandle_t error = m_qnnFunctionPointers.qnnInterface.contextCreateFromBinaryWithCallback(
134 backend,
135 device,
136 config,
137 &callback,
138 binaryBuffer,
139 binaryBufferSize,
140 &context,
141 profile,
142 signal);
143
144if (error != QNN_SUCCESS) {
145 // handle the error, if file mapping is not available retry with file mapping disabled
146}
147
148// Execute graph
149// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code
150
151// Step3 Free context
152// See ${QNN_SDK_ROOT}/examples/QNN/SampleApp code for details
153if (QNN_CONTEXT_NO_ERROR != m_qnnFunctionPointers.qnnInterface.contextFree(context, profileBackendHandle)) {
154 // handle error
155}
156deInitCallback(ctxtBinPath);

```

## QNN HTP Monolithic LSTM

Monolithic LSTM is a feature that allows LSTM to be Monolithic rather than expanded during finalization. QNN HTP provides a configuration option for users to turn ON or OFF Monolithic LSTM through client usage like below: 

```
1 QnnHtpGraph_CustomConfig_t customConfig;
2 customConfig.option = QNN_HTP_GRAPH_CONFIG_OPTION_MONOLITHIC_LSTM;
3 customConfig.monolithicLstm = true;
4
5 QnnGraph_Config_t graphConfig;
6 graphConfig.option = QNN_GRAPH_CONFIG_OPTION_CUSTOM;
7 graphConfig.customConfig = &customConfig;
8
9 const QnnGraph_Config_t* pGraphConfig[] = {&graphConfig, NULL};

```

For offline preparation with Monolithic LSTM, the backend-specific configuration should specify the `monolithic_lstm` option along with any other options. 

- 
false – Disables Monolithic LSTM; default when option is not provided 
- 
true – Enables Monolithic LSTM 

```
{
"graphs": [
{
"vtcm_mb":...,
"graph_names":[...],
"monolithic_lstm": true // set to false to turn off, Monolithic LSTM
...
}
],
"devices": [
{
...
...
}
]
}

```

By default Monolithic LSTM will be in disabled state i.e. when configuration option is not provided. 

### Recommended Use Cases

It is recommended to enable Monolithic LSTM when graph contains multi-step LSTM with a large time step size (e.g., 2000) and a small depth size (e.g., <= 256). Conversely, for graph with small time steps or large depth size LSTM, it’s recommended to disable this option to expand the LSTM. 

Use case 
Description 
Suggestion 

single-step LSTM 
LSTM with only 1 time step. 
disable monolithic LSTM flag 

multi-step LSTM with small depth size 
LSTM with time step > 1 and depth size <= 256. In this case, LSTM performs well on prepare time, binary size and execution time. 
enable monolithic LSTM flag 

multi-step LSTM with big depth size 
LSTM with time step > 1 but depth size > 256. When depth size is too large, the memory becomes bottleneck of execution time. 
disable monolithic LSTM flag 
Benefits 

- 
Reduce prepare time 
- 
Reduce context binary size 
Limitations 

- 
May increase execute time when depth size is large or time step size is not sufficiently large. 
- 
Hexagon NPU Runtime Driver Path: Monolithic LSTM requires a compatible Hexagon NPU runtime driver. 

- 
During preparation: If the driver is outdated, the optimization will not be applied even if enabled. 
- 
During execution: If a context binary prepared with Monolithic LSTM is run with an outdated driver, execution will fail due to the unsupported feature. 

## Multi-SoC DLC with Reference Weight Sharing

This feature enables offline preparation of context binaries for multiple SoCs, embedded into a single DLC. Reference weight sharing reduces ROM size by storing one shared weight blob instead of duplicating weights across SoC-specific context binaries. 

- Multi-SoC DLC with Reference Weight Sharing 

Last Published: Jul 02, 2026

Previous 
DSP 
Next 
HTP API Usage Guidelines 

May contain U.S. and international export controlled information

Opt-Out Request Honored
This website processes personal data through our and third parties’ online tracking technologies, including analytics and advertising cookies. To learn more about how we and our affiliates within the Qualcomm Group may use your personal data and cookies, please review the Privacy Policy published at the bottom of this website and Qualcomm’s Cookie Policy. If you don’t want to share your website activities, including browsing behavior, with our third-party partners via these tracking technologies, click on “Cookie Settings" below to update your preferences. You can also update your cookie preferences at any time by clicking the Do Not Sell or Share My Personal Information link at the bottom of this website.

Opt-Out Request Honored
Do Not Sell or Share My Personal Data
As described in greater detail in the Privacy Policy at the bottom of this website and Qualcomm’s Cookie Policy, we use certain third party advertising and other cookies on this website, which may be considered a “sale” of personal information or “sharing” of personal information for targeted advertising under applicable data privacy laws. To opt out of the sale or sharing of your personal information, please click the “Share or Sale of Personal Information” toggle button below. When you have opted out, the button color will change from blue to grey. We will also honor your opt-out of sale or sharing requests communicated via opt-out preference signals, such as the Global Privacy Control. 

Manage Consent Preferences

Strictly Necessary Cookies
Always Active

These cookies are necessary for the website to function and cannot be switched off in our systems. They are usually only set in response to actions made by you which amount to a request for services, such as setting your privacy preferences, logging in or filling in forms. You can set your browser to block or alert you about these cookies, but some parts of the site will not then work.

Share Or Sale of Personal Information
Share Or Sale of Personal Information 

As described above, you may exercise your right to opt out of the sale or sharing of personal information by using this toggle button

- 
Analytics Cookies

Switch Label label
These cookies allow us to count visits and traffic sources so we can measure and improve the performance of our site. They help us to know which pages are the most and least popular and see how visitors move around the site. If you do not allow these cookies we will not know when you have visited our site, and will not be able to monitor its performance.

- 
Personalization Cookies

Switch Label label
These cookies enable the website to provide enhanced functionality and personalisation. They may be set by us or by third party providers whose services we have added to our pages. If you do not allow these cookies then some or all of these services may not function properly.

- 
Targeting Cookies

Switch Label label
These cookies may be set through our site by our advertising partners. They may be used by those companies to build a profile of your interests and show you relevant adverts on other sites. If you do not allow these cookies, you will experience less targeted advertising.

Cookie List

- 
checkbox label label

Consent Leg.Interest

checkbox label label
checkbox label label
checkbox label label

Your Privacy [`dialog closed`]
