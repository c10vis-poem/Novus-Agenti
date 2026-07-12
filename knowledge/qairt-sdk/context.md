# QairtContext

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

- QAIRT API
- C++ API
- QairtContext

# QairtContext

Updated: Jul 02, 2026 80-63442-10 Rev: AL 

QairtContext Qualcomm® AI Runtime (QAIRT) SDKs 

# QairtContext

Note 
Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality. 
Include: `#include "QairtCppApi/QairtContext.hpp"` 
C++ wrapper for the QAIRT context API. 

```
A Backend must be created before constructing a Context object.

```
namespace qairt

Enums enum class ContextBinaryCompatibilityType : std::underlying_type_t<QairtContext_BinaryCompatibilityType_t>

Binary compatibility policy used when loading a cached context binary. 

Enumerator 
Description 

`Permissive` 
Binary is accepted if it can run on the device. Default policy. 

`Strict` 
Binary is accepted only if it fully utilizes hardware capability. 

`Undefined` 
Sentinel value; not a valid policy selection. 
Values: enumerator Permissive = QAIRT_CONTEXT_BINARY_COMPATIBILITY_PERMISSIVE
enumerator Strict = QAIRT_CONTEXT_BINARY_COMPATIBILITY_STRICT
enumerator Undefined = QAIRT_CONTEXT_BINARY_COMPATIBILITY_TYPE_UNDEFINED
enum class ContextError : std::underlying_type_t<QairtContext_Error_t>

Error codes returned by QAIRT context operations. 

Enumerator 
Description 

`NoError` 
Operation succeeded. 

`UnsupportedFeature` 
An optional API feature is not supported by the backend. 

`MemAlloc` 
Memory allocation or deallocation failure. 

`InvalidArgument` 
An argument to the operation was invalid. 

`CtxDoesNotExist` 
The context has not yet been created in the backend. 

`InvalidHandle` 
The provided handle is not valid. 

`NotFinalized` 
Operation attempted before all graphs in the context were finalized. 

`BinaryVersion` 
The context binary has an incompatible version. 

`CreateFromBinary` 
Failed to create a context from a binary. 

`GetBinarySizeFailed` 
Failed to retrieve the size of the serialized context. 

`GetBinaryFailed` 
Failed to generate the serialized context. 

`BinaryConfiguration` 
The context binary configuration is invalid. 

`SetProfile` 
Failed to set profiling information. 

`InvalidConfig` 
One or more configuration values are invalid. 

`BinarySuboptimal` 
A suboptimal binary was used with strict compatibility mode. 

`Aborted` 
Call was aborted early due to a signal trigger. 

`TimedOut` 
Call was aborted early due to a signal timeout. 

`IncrementInvalidBuffer` 
The incremental binary buffer was not allocated by the backend. 

`Undefined` 
An undefined or unknown error occurred. 
Values: enumerator NoError = QAIRT_CONTEXT_NO_ERROR
enumerator UnsupportedFeature = QAIRT_CONTEXT_ERROR_UNSUPPORTED_FEATURE
enumerator MemAlloc = QAIRT_CONTEXT_ERROR_MEM_ALLOC
enumerator InvalidArgument = QAIRT_CONTEXT_ERROR_INVALID_ARGUMENT
enumerator CtxDoesNotExist = QAIRT_CONTEXT_ERROR_CTX_DOES_NOT_EXIST
enumerator InvalidHandle = QAIRT_CONTEXT_ERROR_INVALID_HANDLE
enumerator NotFinalized = QAIRT_CONTEXT_ERROR_NOT_FINALIZED
enumerator BinaryVersion = QAIRT_CONTEXT_ERROR_BINARY_VERSION
enumerator CreateFromBinary = QAIRT_CONTEXT_ERROR_CREATE_FROM_BINARY
enumerator GetBinarySizeFailed = QAIRT_CONTEXT_ERROR_GET_BINARY_SIZE_FAILED
enumerator GetBinaryFailed = QAIRT_CONTEXT_ERROR_GET_BINARY_FAILED
enumerator BinaryConfiguration = QAIRT_CONTEXT_ERROR_BINARY_CONFIGURATION
enumerator SetProfile = QAIRT_CONTEXT_ERROR_SET_PROFILE
enumerator InvalidConfig = QAIRT_CONTEXT_ERROR_INVALID_CONFIG
enumerator BinarySuboptimal = QAIRT_CONTEXT_ERROR_BINARY_SUBOPTIMAL
enumerator Aborted = QAIRT_CONTEXT_ERROR_ABORTED
enumerator TimedOut = QAIRT_CONTEXT_ERROR_TIMED_OUT
enumerator IncrementInvalidBuffer = QAIRT_CONTEXT_ERROR_INCREMENT_INVALID_BUFFER
enumerator Undefined = QAIRT_CONTEXT_ERROR_UNDEFINED
enum class ContextBinaryType : std::underlying_type_t<QairtContext_BinaryType_t>

Storage format of a context binary. 

Enumerator 
Description 

`Raw` 
Binary stored as a raw memory buffer. 

`MemHandle` 
Binary referenced via a memory handle. 

`Undefined` 
Sentinel value; not a valid binary type. 
Values: enumerator Raw = QAIRT_CONTEXT_BINARY_TYPE_RAW
enumerator MemHandle = QAIRT_CONTEXT_BINARY_TYPE_MEM_HANDLE
enumerator Undefined = QAIRT_CONTEXT_BINARY_TYPE_UNDEFINED
enum class ContextSectionType : std::underlying_type_t<QairtContext_SectionType_t>

Portion of the context binary targeted by section operations. 

Enumerator 
Description 

`Updatable` 
Section containing all recent updates applied via tensor update APIs. 

`UpdatableWeights` 
Section containing recent static weight updates only. 

`UpdatableQuantParams` 
Section containing recent quantization parameter updates only. 

`Undefined` 
Sentinel value; not a valid section type. 
Values: enumerator Updatable = QAIRT_CONTEXT_SECTION_UPDATABLE
enumerator UpdatableWeights = QAIRT_CONTEXT_SECTION_UPDATABLE_WEIGHTS
enumerator UpdatableQuantParams = QAIRT_CONTEXT_SECTION_UPDATABLE_QUANT_PARAMS
enumerator Undefined = QAIRT_CONTEXT_SECTION_UNDEFINED
class Context : public qairt::ApiType<Context, QairtContext_V1_t>

Public Functions Context() = default
Context(const Context&) = delete
Context(Context&&) noexcept = default
Context &operator=(const Context&) = delete
Context &operator=(Context&&) noexcept = default
inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtContext_Handle_t handle)
inline void setConfig(const ContextConfiguration &config)

Set or modify configuration options on this context. 
Backends are not required to support reconfiguration after context creation. If the backend does not support the provided configuration, this call will fail. 

See also 
QairtContext_setConfig Parameters 
config – [in] Context configuration object containing the options to apply. Throws 
qairt::Exception – on invalid handle, unsupported feature, or invalid configuration. template<typename T>
inline std::enable_if_t<std::is_base_of_v<ContextCustomConfiguration, T>> setConfiguration(const T &customConfigs)

Apply a collection of backend-specific custom configuration entries to this context. Parameters 
customConfigs – [in] Backend-specific custom configuration collection whose handles are applied individually to this context. Throws 
qairt::Exception – on invalid handle. inline uint64_t getBinarySize() const

Get the size in bytes of the serialized binary representation of this context. 
All graphs in the context must be finalized before calling this method. Call getBinary() or getBinary(void*, uint64_t) after allocating a buffer of at least this size. 

See also 
QairtContext_getBinarySize Throws 
qairt::Exception – on:

- 
invalid handle 
- 
unsupported feature 
- 
unfinalized graphs in the context 
- 
other retrieval failure Returns 
Size in bytes required to hold the serialized context binary. inline uint64_t getBinary(ContextBinaryBuffer &buffer)

Serialize this context into the provided binary buffer. 
All graphs in the context must be finalized before calling this method. Call getBinarySize() first to determine the required buffer size. The buffer’s data pointer and size must be set before calling this method. 

See also 
QairtContext_getBinary Parameters 
buffer – [inout] Pre-allocated binary buffer to receive the serialized context. The buffer’s size field must be at least getBinarySize() bytes. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
unsupported feature 
- 
unfinalized graphs in the context 
- 
other serialization failure Returns 
Number of bytes written into the buffer. inline uint64_t getBinary(void *buffer, uint64_t bufferSize)

Wrapper which allows for serializing directly into a caller-managed raw memory buffer. 

See also 
Context::getBinary(ContextBinaryBuffer&) inline void updateContextTensors(const std::vector<Tensor*> &tensors)

Update the data and quantization parameters of previously created context tensors. 
Valid fields to update depend on tensor type:

- 
UPDATEABLE_STATIC: data and quantization parameters. 
- 
UPDATEABLE_NATIVE, UPDATEABLE_APP_READ, UPDATEABLE_APP_WRITE, UPDATEABLE_APP_READWRITE: quantization parameters only. 
Updates take effect only after QairtGraph_finalize() is called for one or more of the graphs to which the context tensors are associated. 

See also 
QairtContext_updateContextTensors Parameters 
tensors – [in] Pointers to tensors to update. Each tensor must carry the ID assigned during tensor creation. Throws 
qairt::Exception – on:

- 
invalid context or tensor handle 
- 
NULL tensor handle array 
- 
incompatible tensor type 
- 
unsupported feature inline uint64_t getBinarySectionSize(const Graph &graph, ContextSectionType section) const

Get the size in bytes of a binary section for a specific graph. 
All graphs in the context must be finalized before calling this method. Use this to determine the buffer size needed before calling getBinarySection(). 

See also 
QairtContext_getBinarySectionSize Parameters 

- 
graph – [in] Graph whose binary section size is queried. 
- 
section – [in] Portion of the context binary to query. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
unsupported feature 
- 
unfinalized graphs in the context 
- 
other retrieval failure Returns 
Size in bytes needed to hold the requested binary section. inline uint64_t getBinarySection(const Graph &graph, ContextSectionType section, ContextBinaryBuffer &buffer, ApiTypeRef<const Profile&> profile, ApiTypeRef<const Signal&> signal)

Retrieve a section of the context binary for a specific graph. 
All graphs in the context must be finalized before calling this method. Call getBinarySectionSize() first to determine the required buffer size. The signal, if non-null, is considered in-use for the duration of this call. 

See also 
QairtContext_getBinarySection Parameters 

- 
graph – [in] Graph whose binary section is retrieved. 
- 
section – [in] Portion of the context binary to retrieve. 
- 
buffer – [inout] Pre-allocated binary buffer to receive the section. Must be sized to at least getBinarySectionSize() bytes. 
- 
profile – [in] Optional profile handle to collect metrics. 
- 
signal – [in] Optional signal handle for controlling the operation. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
unsupported feature 
- 
unfinalized graphs in the context 
- 
other serialization failure Returns 
Number of bytes written into the buffer. inline void applyBinarySection(const Graph &graph, ContextSectionType section, ContextBinaryBuffer &buffer, ApiTypeRef<const Profile&> profile, ApiTypeRef<const Signal&> signal)

Apply a previously retrieved binary section to this context. 

See also 
QairtContext_applyBinarySection Parameters 

- 
graph – [in] Graph to which the binary section applies. 
- 
section – [in] Portion of the context binary being applied. 
- 
buffer – [in] Binary buffer containing the section to apply. When persistent binary mode is enabled, this buffer must remain valid through context teardown. 
- 
profile – [in] Optional profile handle to collect metrics. 
- 
signal – [in] Optional signal handle for controlling the operation. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
unsupported feature 
- 
memory allocation failure 
- 
profiling error inline Graph createGraph(const char *graphName, ApiTypeRef<const qairt::GraphConfiguration&> graphConfiguration)

Create a new graph within this context. 

See also 
QairtContext_createGraph Parameters 

- 
graphName – [in] Unique null-terminated identifier for the graph within this context. 
- 
graphConfiguration – [in] Configuration options for the graph. Optional. Throws 
qairt::Exception – on:

- 
invalid context handle 
- 
NULL or duplicate graph name 
- 
memory or resource allocation failure 
- 
unsupported configuration options Returns 
The newly created Graph object. inline std::shared_ptr<Graph> retrieveGraph(const char *graphName)

See also 
Context::retrieveGraph(const std::string&) inline std::shared_ptr<Graph> retrieveGraph(const std::string &graphName)

Retrieve an existing graph from this context by name. 

See also 
QairtContext_retrieveGraph Parameters 
graphName – [in] Name of the graph to retrieve. Throws 
qairt::Exception – on:

- 
invalid context handle 
- 
NULL or invalid graph name 
- 
no graph with the specified name exists in this context 
- 
memory allocation failure Returns 
Shared pointer to the retrieved Graph object. inline void setFreeProfile(Profile &profile)

Set the profile handle used to collect metrics during context teardown. 

See also 
QairtContext_free Parameters 
profile – [in] Profile object to populate during context teardown. template<typename T, typename U, typename V>
inline ApiType(const ApiType<T, U, V> &parent, detail::non_owning_handle<handle_type> noh)
inline explicit ApiType(const std::shared_ptr<T_Table> &apiTable)
inline ApiType(copy_table_tag_t, const ApiType &other)
ApiType() noexcept = default
ApiType(const ApiType&) = delete
ApiType(ApiType&&) noexcept = default
ApiType &operator=(const ApiType&) = delete
ApiType &operator=(ApiType&&) noexcept = default

Private Functions inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtBackend_Handle_t backendHandle, QairtDevice_Handle_t deviceHandle, QairtContext_ConfigHandle_t contextConfigHandle)
inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtBackend_Handle_t backendHandle, QairtDevice_Handle_t deviceHandle, QairtContext_ConfigHandle_t contextConfigHandle, QairtContext_BinaryBufferHandle_t binaryBufferHandle, QairtSignal_Handle_t signalHandle = nullptr, QairtProfile_Handle_t profileHandle = nullptr)
inline void customFree(QairtContext_Handle_t handle) const

Private Members friend Api QairtProfile_Handle_t m_freeProfileHandle = nullptr

Profile handle used to collect metrics during context teardown. 

Friends friend class ::qairt::ApiType class ContextAsyncExecutionQueueDepth : public qairt::ApiType<ContextAsyncExecutionQueueDepth, QairtContext_AsyncExecutionDepthV1_t>

#include <QairtContext.hpp> 
Queue depth configuration for asynchronous context execution. 

Public Functions ContextAsyncExecutionQueueDepth() noexcept = default
ContextAsyncExecutionQueueDepth(ContextAsyncExecutionQueueDepth&&) noexcept = default
ContextAsyncExecutionQueueDepth &operator=(ContextAsyncExecutionQueueDepth&&) noexcept = default
inline uint32_t getDepth() const

Get the current queue depth for asynchronous execution. 

See also 
QairtContext_AsyncExecutionGetDepth Throws 
qairt::Exception – on invalid handle. Returns 
Maximum number of outstanding asynchronous execution requests. inline void setDepth(uint32_t depth)

Set the queue depth for asynchronous execution. 

See also 
QairtContext_AsyncExecutionSetDepth Parameters 
depth – [in] Maximum number of outstanding asynchronous execution requests. Throws 
qairt::Exception – on invalid handle or invalid argument. 

Private Functions inline explicit ContextAsyncExecutionQueueDepth(const std::shared_ptr<ApiTable> &apiTable)

Friends friend class Api class ContextBinary : public qairt::ApiType<ContextBinary, QairtContext_BinaryV1_t>

#include <QairtContext.hpp> 
Descriptor pairing a binary type with its associated buffer for context serialization. 

Public Functions ContextBinary() noexcept = default
ContextBinary(ContextBinary&&) noexcept = default
ContextBinary &operator=(ContextBinary&&) noexcept = default
inline ContextBinaryType getType() const

Get the storage format type of this context binary. 

See also 
QairtContext_binaryGetType Throws 
qairt::Exception – on invalid handle. Returns 
Storage format of this binary (e.g., Raw or MemHandle). inline ContextBinaryBuffer &getBuffer()

Get the binary buffer associated with this context binary. 

See also 
QairtContext_binaryGetBuffer Throws 
qairt::Exception – on invalid handle. Returns 
Reference to the associated ContextBinaryBuffer. inline const ContextBinaryBuffer &getBuffer() const

Get the binary buffer associated with this context binary. 

See also 
QairtContext_binaryGetBuffer Throws 
qairt::Exception – on invalid handle. Returns 
Const reference to the associated ContextBinaryBuffer. inline void setBuffer(ContextBinaryBuffer &&buffer)

Set the binary buffer for this context binary. 

See also 
QairtContext_binarySetBuffer Parameters 
buffer – [in] Binary buffer to associate with this context binary object. Throws 
qairt::Exception – on invalid handle. 

Private Members friend Api detail::crossable<detail::non_owning<ContextBinaryBuffer>, &interface_type::getBuffer, &interface_type::setBuffer> m_buffer

Binary buffer associated with this context binary object. class ContextBinaryBuffer : public qairt::ApiType<ContextBinaryBuffer, QairtContext_BinaryBufferV1_t>

#include <QairtContext.hpp> 
Buffer descriptor for a serialized context binary. 

```
Obtained via `Api::make<ContextBinaryBuffer>()`.

```

Public Functions ContextBinaryBuffer() noexcept = default
ContextBinaryBuffer(ContextBinaryBuffer&&) noexcept = default
ContextBinaryBuffer &operator=(ContextBinaryBuffer&&) noexcept = default
inline void *getData()

Get the raw data pointer stored in this buffer. 

See also 
QairtContext_BinaryBuffer_getData Throws 
qairt::Exception – on invalid handle. Returns 
Pointer to the underlying buffer memory, or nullptr if none has been set. inline const void *getData() const

Get the raw data pointer stored in this buffer. 

See also 
QairtContext_BinaryBuffer_getData Throws 
qairt::Exception – on invalid handle. Returns 
Pointer to the underlying buffer memory, or nullptr if none has been set. inline void setData(void *data)

Set the raw data pointer for this buffer. 

See also 
QairtContext_BinaryBuffer_setData Parameters 
data – [in] Pointer to the memory region to associate with this buffer. Throws 
qairt::Exception – on invalid handle. inline uint64_t getSize() const

Get the size of this buffer in bytes. 

See also 
QairtContext_BinaryBuffer_getSize Throws 
qairt::Exception – on invalid handle. Returns 
Buffer size in bytes, or 0 if no size has been set. inline void setSize(uint64_t size) const

Set the size of this buffer in bytes. 

See also 
QairtContext_BinaryBuffer_setSize Parameters 
size – [in] Size in bytes of the memory region referenced by this buffer. Throws 
qairt::Exception – on invalid handle. 

Private Functions inline explicit ContextBinaryBuffer(const std::shared_ptr<ApiTable> &apiTable)

Private Members friend Api 

Friends friend class ::qairt::ApiType class ContextConfiguration : public qairt::ApiType<ContextConfiguration, QairtContext_ConfigV1_t>

#include <QairtContext.hpp> 
Configuration object for context creation and reconfiguration. 

Public Functions ContextConfiguration() noexcept = default
ContextConfiguration(ContextConfiguration&&) noexcept = default
ContextConfiguration &operator=(ContextConfiguration&&) noexcept = default
inline void setPriority(Priority p)

Set the scheduling priority for this context configuration. 

See also 
QairtContext_Config_setPriority Parameters 
p – [in] Desired execution priority. Throws 
qairt::Exception – on invalid handle or invalid argument. inline Priority getPriority() const

Get the scheduling priority for this context configuration. 

See also 
QairtContext_Config_getPriority Throws 
qairt::Exception – on invalid handle. Returns 
Current execution priority. inline std::string &getOemKey()

Get the Original Equipment Manufacturer (OEM) key string for this context configuration. 

See also 
QairtContext_Config_getOemKey Throws 
qairt::Exception – on invalid handle. Returns 
Reference to the OEM authentication key string. Empty if not set. inline const std::string &getOemKey() const

Get the Original Equipment Manufacturer (OEM) key string for this context configuration. 

See also 
QairtContext_Config_getOemKey Throws 
qairt::Exception – on invalid handle. Returns 
Const reference to the OEM authentication key string. Empty if not set. inline void setOemKey(std::string &&oemKey)

Set the Original Equipment Manufacturer (OEM) key string for this context configuration. 

See also 
QairtContext_Config_setOemKey Parameters 
oemKey – [in] OEM authentication key string to set. Throws 
qairt::Exception – on invalid handle. inline void getOemKey(const std::string &oemKey)
inline void setAsyncExecutionQueueDepth(const ContextAsyncExecutionQueueDepth &aed)

Set the asynchronous execution queue depth for this context configuration. 

See also 
QairtContext_Config_setAsyncQueueDepth Parameters 
aed – [in] Async execution queue depth configuration object. Throws 
qairt::Exception – on invalid handle. inline ContextAsyncExecutionQueueDepth &getAsyncExecutionQueueDepth()

Get the asynchronous execution queue depth configuration for this context. 

See also 
QairtContext_Config_getAsyncQueueDepth Throws 
qairt::Exception – on invalid handle. Returns 
Reference to the async execution queue depth object. inline const ContextAsyncExecutionQueueDepth &getAsyncExecutionQueueDepth() const

Get the asynchronous execution queue depth configuration for this context. 

See also 
QairtContext_Config_getAsyncQueueDepth Throws 
qairt::Exception – on invalid handle. Returns 
Const reference to the async execution queue depth object. inline ContextConfiguration &setCustomConfig(const ContextCustomConfig &config)

Set a single backend-specific custom configuration entry on this context configuration. 

See also 
QairtContext_Config_setCustomConfigs Parameters 
config – [in] Backend-specific custom configuration entry to apply. Throws 
qairt::Exception – on invalid handle. Returns 
Reference to this configuration object, allowing method chaining. inline ContextConfiguration &setCustomConfigs(const ContextCustomConfiguration &config)

Set a collection of backend-specific custom configuration entries on this context configuration. 

See also 
QairtContext_Config_setCustomConfigs Parameters 
config – [in] Collection of backend-specific custom configuration entries to apply. Throws 
qairt::Exception – on invalid handle. Returns 
Reference to this configuration object, allowing method chaining. inline std::vector<std::string> &getEnableGraphs()

Get the list of graph names selectively enabled for this context configuration. 

See also 
QairtContext_Config_getNumEnabledGraphs Throws 
qairt::Exception – on invalid handle. Returns 
Reference to the vector of enabled graph name strings. inline const std::vector<std::string> &getEnableGraphs() const

Get the list of graph names selectively enabled for this context configuration. 

See also 
QairtContext_Config_getNumEnabledGraphs Throws 
qairt::Exception – on invalid handle. Returns 
Const reference to the vector of enabled graph name strings. inline void setEnableGraphs(std::vector<std::string> enabledGraphs)

Set the list of graph names selectively enabled for this context configuration. 

See also 
QairtContext_Config_setEnableGraph Parameters 
enabledGraphs – [in] Names of graphs to enable. Throws 
qairt::Exception – on invalid handle. inline void setMemoryLimitHint(uint64_t limit)

Set a hint on the maximum memory the backend should use for this context. 
This is advisory only; the backend may exceed the limit if required. 

See also 
QairtContext_Config_setMemoryLimitHint Parameters 
limit – [in] Memory limit hint in bytes. Throws 
qairt::Exception – on invalid handle or invalid argument. inline uint64_t getMemoryLimitHint() const

Get the memory limit hint for this context configuration. 

See also 
QairtContext_Config_getMemoryLimitHint Throws 
qairt::Exception – on invalid handle. Returns 
Memory limit hint in bytes, or 0 if no limit has been set. inline void setIsPersistentBinary(bool isPersistentBinary)

Set whether the context binary should be treated as persistent. 

See also 
QairtContext_Config_setIsPersistentBinary Parameters 
isPersistentBinary – [in] True to enable persistent binary mode; false to disable. Throws 
qairt::Exception – on invalid handle or invalid argument. inline bool getIsPersistentBinary() const

Get whether the context binary is configured as persistent. 

See also 
QairtContext_Config_getIsPersistentBinary Throws 
qairt::Exception – on invalid handle. Returns 
True if persistent binary mode is enabled; false otherwise. inline void setBinaryCompatibilityType(ContextBinaryCompatibilityType bct)

Set the binary compatibility policy for loading cached context binaries. 

See also 
QairtContext_Config_setBinaryCompatibilityType Parameters 
bct – [in] Binary compatibility policy to enforce. Throws 
qairt::Exception – on invalid handle or invalid argument. inline ContextBinaryCompatibilityType getBinaryCompatibilityType() const

Get the binary compatibility policy for loading cached context binaries. 

See also 
QairtContext_Config_getBinaryCompatibilityType Throws 
qairt::Exception – on invalid handle. Returns 
Current binary compatibility policy. 

Private Functions inline void prepareToCross() const
inline void updateAfterCross() const
inline explicit ContextConfiguration(const std::shared_ptr<ApiTable> &apiTable)

Private Members friend Api detail::crossable<std::string, &interface_type::getOemKey, &interface_type::setOemKey> m_oemKey

Original Equipment Manufacturer (OEM) key string for backend authentication. detail::crossable<detail::non_owning<ContextAsyncExecutionQueueDepth>, &interface_type::getAsyncQueueDepth, &interface_type::setAsyncQueueDepth> m_depth

Maximum number of outstanding asynchronous execution requests. mutable std::vector<std::string> m_enabledGraphs

Names of graphs selectively enabled for this context configuration. class ContextCustomConfig : public qairt::CustomConfigType

#include <QairtContext.hpp> 
Abstract base class for a single backend-specific context custom configuration entry. 

Public Functions virtual ~ContextCustomConfig() = default
virtual QairtContext_CustomConfigHandle_t getCustomConfigHandle() const = 0

Protected Functions ContextCustomConfig() = default
ContextCustomConfig(const ContextCustomConfig&) = default
ContextCustomConfig(ContextCustomConfig&&) noexcept = default
ContextCustomConfig &operator=(const ContextCustomConfig&) = default
ContextCustomConfig &operator=(ContextCustomConfig&&) noexcept = default
class ContextCustomConfiguration

#include <QairtContext.hpp> 
Abstract base class for a collection of backend-specific context custom configuration entries. 

Public Functions virtual ~ContextCustomConfiguration() = default
virtual std::vector<QairtContext_CustomConfigHandle_t> getCustomConfigs() const = 0

Protected Functions ContextCustomConfiguration() = default
ContextCustomConfiguration(const ContextCustomConfiguration&) = default
ContextCustomConfiguration(ContextCustomConfiguration&&) noexcept = default
ContextCustomConfiguration &operator=(const ContextCustomConfiguration&) = default
ContextCustomConfiguration &operator=(ContextCustomConfiguration&&) noexcept = default

Last Published: Jul 02, 2026

Previous 
QairtApi 
Next 
QairtGraph 

May contain U.S. and international export controlled information

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
