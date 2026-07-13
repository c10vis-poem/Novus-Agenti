# QairtGraph

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
- QairtGraph

# QairtGraph

Updated: Jul 02, 2026 80-63442-10 Rev: AL 

QairtGraph Qualcomm® AI Runtime (QAIRT) SDKs 

# QairtGraph

Note 
Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality. 
Include: `#include "QairtCppApi/QairtGraph.hpp"` 
C++ wrapper for the QAIRT graph API. namespace qairt

Enums enum class Priority : std::underlying_type_t<Qairt_Priority_t>

Execution priority levels for graph scheduling. 

Enumerator 
Description 

`Low` 
Lowest scheduling priority. 

`NormalLow` 
Below-normal scheduling priority. 

`Normal` 
Normal scheduling priority. 

`Default` 
Default scheduling priority, equivalent to Normal. 

`NormalHigh` 
Above-normal scheduling priority. 

`High` 
High scheduling priority. 

`HighPlus` 
Higher than High scheduling priority. 

`Critical` 
Critical scheduling priority. 

`CriticalPlus` 
Highest scheduling priority. 

`Undefined` 
Priority is not set or unrecognized. 
Values: enumerator Low = QAIRT_PRIORITY_LOW
enumerator NormalLow = QAIRT_PRIORITY_NORMAL_LOW
enumerator Normal = QAIRT_PRIORITY_NORMAL
enumerator Default = QAIRT_PRIORITY_DEFAULT
enumerator NormalHigh = QAIRT_PRIORITY_NORMAL_HIGH
enumerator High = QAIRT_PRIORITY_HIGH
enumerator HighPlus = QAIRT_PRIORITY_HIGH_PLUS
enumerator Critical = QAIRT_PRIORITY_CRITICAL
enumerator CriticalPlus = QAIRT_PRIORITY_CRITICAL_PLUS
enumerator Undefined = QAIRT_PRIORITY_UNDEFINED
enum class GraphError : std::underlying_type_t<QairtGraph_Error_t>

Error codes returned by QAIRT graph operations. 

Enumerator 
Description 

`MinError` 
Sentinel for the minimum error value. 

`NoError` 
Operation succeeded. 

`UnsupportedFeature` 
An optional API feature is not yet supported. 

`MemAlloc` 
Memory allocation failure in graph processing. 

`General` 
Unclassified graph error; any graph API may return this. 

`InvalidArguemnt` 
An argument to the graph API is invalid. 

`InvalidHandle` 
The provided graph handle is not valid. 

`GraphDoesNotExist` 
No graph with the specified name is registered in the backend. 

`InvalidName` 
Graph name is NULL, empty, or duplicates an existing name. 

`InvalidTensor` 
A tensor handle is NULL or invalid. 

`InvalidOpConfig` 
One or more elements of the op configuration are invalid. 

`SetProfile` 
Failed to bind the profile handle to the graph. 

`UnconnectedNode` 
A node was added before one or more of its input-producing nodes. 

`CreateFailed` 
Graph creation failed. 

`OtimizationFailed` 
Graph optimization failed with the specified ops or configuration. 

`FinalizeFailed` 
Graph finalization failed. 

`GraphNotFinalized` 
Attempted to execute a graph that has not been finalized. 

`GraphFinalized` 
Attempted to modify a graph after finalization. 

`ExecutionAsyncFifoFull` 
Async execution queue is full; no new requests can be registered. 

`SignalInUse` 
The supplied signal object is already in use by another call. 

`Aborted` 
Call aborted early due to a signal trigger. 

`ProfileInUse` 
The profile handle is already bound to another graph. 

`TimedOut` 
Call aborted early due to a signal timeout. 

`Subgraph` 
Operation is not permitted on a subgraph. 

`Disabled` 
The graph was disabled during context deserialization. 

`DynamicTensorShape` 
Dynamic tensor shape exceeded configured limits. 

`TensorSparsity` 
Tensor sparsity constraint violation. 

`EarlyTermination` 
Graph execution terminated early due to op-defined behavior. 

`InvalidContext` 
The context associated with this graph has already been freed. 

`MaxError` 
Sentinel for the maximum error value. 

`Undefined` 
Unused; present to ensure a 32-bit enum size. 
Values: enumerator MinError = QAIRT_GRAPH_MIN_ERROR
enumerator NoError = QAIRT_GRAPH_NO_ERROR
enumerator UnsupportedFeature = QAIRT_GRAPH_ERROR_UNSUPPORTED_FEATURE
enumerator MemAlloc = QAIRT_GRAPH_ERROR_MEM_ALLOC
enumerator General = QAIRT_GRAPH_ERROR_GENERAL
enumerator InvalidArguemnt = QAIRT_GRAPH_ERROR_INVALID_ARGUMENT
enumerator InvalidHandle = QAIRT_GRAPH_ERROR_INVALID_HANDLE
enumerator GraphDoesNotExist = QAIRT_GRAPH_ERROR_GRAPH_DOES_NOT_EXIST
enumerator InvalidName = QAIRT_GRAPH_ERROR_INVALID_NAME
enumerator InvalidTensor = QAIRT_GRAPH_ERROR_INVALID_TENSOR
enumerator InvalidOpConfig = QAIRT_GRAPH_ERROR_INVALID_OP_CONFIG
enumerator SetProfile = QAIRT_GRAPH_ERROR_SET_PROFILE
enumerator UnconnectedNode = QAIRT_GRAPH_ERROR_UNCONNECTED_NODE
enumerator CreateFailed = QAIRT_GRAPH_ERROR_CREATE_FAILED
enumerator OtimizationFailed = QAIRT_GRAPH_ERROR_OPTIMIZATION_FAILED
enumerator FinalizeFailed = QAIRT_GRAPH_ERROR_FINALIZE_FAILED
enumerator GraphNotFinalized = QAIRT_GRAPH_ERROR_GRAPH_NOT_FINALIZED
enumerator GraphFinalized = QAIRT_GRAPH_ERROR_GRAPH_FINALIZED
enumerator ExecutionAsyncFifoFull = QAIRT_GRAPH_ERROR_EXECUTION_ASYNC_FIFO_FULL
enumerator SignalInUse = QAIRT_GRAPH_ERROR_SIGNAL_IN_USE
enumerator Aborted = QAIRT_GRAPH_ERROR_ABORTED
enumerator ProfileInUse = QAIRT_GRAPH_ERROR_PROFILE_IN_USE
enumerator TimedOut = QAIRT_GRAPH_ERROR_TIMED_OUT
enumerator Subgraph = QAIRT_GRAPH_ERROR_SUBGRAPH
enumerator Disabled = QAIRT_GRAPH_ERROR_DISABLED
enumerator DynamicTensorShape = QAIRT_GRAPH_ERROR_DYNAMIC_TENSOR_SHAPE
enumerator TensorSparsity = QAIRT_GRAPH_ERROR_TENSOR_SPARSITY
enumerator EarlyTermination = QAIRT_GRAPH_ERROR_EARLY_TERMINATION
enumerator InvalidContext = QAIRT_GRAPH_ERROR_INVALID_CONTEXT
enumerator MaxError = QAIRT_GRAPH_MAX_ERROR
enumerator Undefined = QAIRT_GRAPH_ERROR_UNDEFINED
enum class GraphProfilingState : std::underlying_type_t<QairtGraph_ProfilingState_t>

Profiling enabled/disabled state for a graph. 

Enumerator 
Description 

`Enabled` 
Profiling is active for this graph. 

`Disabled` 
Profiling is not active for this graph. 

`Undefined` 
Unused; present to ensure a 32-bit enum size. 
Values: enumerator Enabled = QAIRT_GRAPH_PROFILING_STATE_ENABLED
enumerator Disabled = QAIRT_GRAPH_PROFILING_STATE_DISABLED
enumerator Undefined = QAIRT_GRAPH_PROFILING_STATE_UNDEFINED
enum class TensorSetMemType

Values: class Graph : public qairt::ApiType<Graph, QairtGraph_V1_t>

#include <QairtGraph.hpp> 
Wrapper for a QAIRT graph handle. 

```
Obtained via Context::createGraph() or Context::retrieveGraph().

```

Public Functions ~Graph() = default
Graph(const Graph&) = delete
Graph(Graph&&) noexcept = default
Graph &operator=(const Graph&) = delete
Graph &operator=(Graph&&) noexcept = default
inline void createGraphTensor(Tensor &tensor)

Create a tensor registered with this graph. 

See also 
QairtGraph_createGraphTensor Parameters 
tensor – [inout] Pre-configured tensor to register. The backend assigns a tensor ID directly to this handle as part of this call. Throws 
qairt::Exception – on:

- 
invalid graph or tensor handle 
- 
invalid or unsupported tensor parameters 
- 
memory allocation failure inline void updateGraphTensors(const std::vector<Tensor*> &tensors)

Update previously created graph tensors with new data or quantization parameters. 
Valid fields to update depend on tensor type:

- 
UPDATEABLE_STATIC tensors: data and quantization parameters. 
- 
UPDATEABLE_NATIVE, UPDATEABLE_APP_READ, UPDATEABLE_APP_WRITE, UPDATEABLE_APP_READWRITE tensors: quantization parameters only. 

See also 
QairtGraph_updateGraphTensors Parameters 
tensors – [in] Array of pointers to tensors to update. Each tensor must carry the ID assigned during creation. Must not be empty. Throws 
qairt::Exception – on:

- 
invalid graph or tensor handle 
- 
incompatible tensor update 
- 
graph not finalized inline void addNode(const OpConfig &opConfig)

Add an operation node to this graph. 
Nodes must be added in dependency order: all native input tensors to the node must be outputs of a previously added node. 

See also 
QairtGraph_addNode Parameters 
opConfig – [in] Operation configuration describing the node to add. All tensors referenced must have been created via createGraphTensor(). Throws 
qairt::Exception – on:

- 
invalid graph handle 
- 
invalid op configuration or tensor reference 
- 
graph already finalized 
- 
node added out of dependency order inline Graph createSubgraph(const std::string &graphName)

Create a named subgraph as a child of this graph. 
A subgraph cannot be finalized or executed directly. Only a top-level graph with no parent can be finalized and executed. Nodes and tensors may be added to a subgraph before or after it is referenced in an op configuration. 

See also 
QairtGraph_createSubgraph Parameters 
graphName – [in] Unique name for the subgraph within the parent context. Must not be NULL or duplicate an existing graph name. Throws 
qairt::Exception – on:

- 
invalid or duplicate graph name 
- 
invalid parent graph handle 
- 
memory allocation failure Returns 
A new Graph object representing the created subgraph. inline void setConfig(const GraphConfiguration &config)

Apply a configuration to this graph. 
Modifies configuration options on an already-created graph. Must be called before finalize(). If the backend cannot support all provided configuration options, this call will fail. 

See also 
QairtGraph_setConfig Parameters 
config – [in] Configuration object specifying priority, profiling, and custom options to apply. Throws 
qairt::Exception – on:

- 
invalid graph or configuration handle 
- 
unsupported configuration option 
- 
graph already finalized 
- 
profile handle already in use by another graph inline void finalize()

Finalize this graph for execution without a profiling handle. 
Validates all operations, checks connectivity, and prepares the graph for execution. Some backends also require finalization of graphs retrieved from a context binary before execution. 

See also 
QairtGraph_finalize Throws 
qairt::Exception – on:

- 
invalid graph handle 
- 
op or kernel creation failure 
- 
graph optimization failure 
- 
subgraph finalization attempt 
- 
graph has zero nodes inline void finalize(Profile &profile)

See also 
Graph::finalize() inline void execute(IOTensorSet &ioTensors)
inline void execute(const std::vector<Tensor> &inputs, std::vector<Tensor> &outputs, std::shared_ptr<Profile> profile = nullptr, std::shared_ptr<Signal> signal = nullptr)

Execute this finalized graph synchronously with the given input and output tensors. 
Blocks until execution completes. If other executions are already enqueued, this call waits in the same queue with equal priority to asynchronous calls. 

See also 
QairtGraph_execute Parameters 

- 
inputs – [in] Input tensors. Each must carry the ID assigned during createGraphTensor(). May be empty only if the graph has no application-writable tensors. 
- 
outputs – [out] Output tensors to be populated by the backend. Each must carry the ID assigned during createGraphTensor(). 
- 
profile – [in] Optional profile object for collecting execution metrics. Must be null if continuous profiling is configured via GraphConfiguration::setProfile(). 
- 
signal – [in] Optional signal for aborting or timing out execution. Throws 
qairt::Exception – on:

- 
invalid graph handle 
- 
graph not finalized 
- 
subgraph execution attempted 
- 
invalid or null tensors 
- 
invalid or in-use signal 
- 
set profile failed 
- 
graph disabled during context deserialization 
- 
dynamic tensor shape limit exceeded 
- 
tensor sparsity constraint violated 
- 
execution terminated early 
- 
execution aborted or timed out 
- 
context freed prior to execution inline void execute(const std::vector<std::shared_ptr<Tensor>> &inputs, std::vector<std::shared_ptr<Tensor>> &outputs)

See also 
Graph::execute(const std::vector<Tensor>&, std::vector<Tensor>&, std::shared_ptr<Profile>, std::shared_ptr<Signal>) inline void executeAsync(const std::vector<std::shared_ptr<Tensor>> &inputs, std::vector<std::shared_ptr<Tensor>> &outputs, ApiTypeRef<const Profile&> profile, ApiTypeRef<const Signal&> signal, std::function<void(void*, NotifyStatus)> fn, void *notifyParam)
inline void executeAsync(const std::vector<std::shared_ptr<Tensor>> &inputs, std::vector<std::shared_ptr<Tensor>> &outputs, std::function<void(void*, NotifyStatus)> fn, void *notifyParam)
inline void executeAsync(const std::vector<std::shared_ptr<Tensor>> &inputs, std::vector<std::shared_ptr<Tensor>> &outputs, std::function<void(NotifyStatus)> fn)
inline void executeAsync(const std::vector<std::shared_ptr<Tensor>> &inputs, std::vector<std::shared_ptr<Tensor>> &outputs)

Private Functions inline void customFree(handle_type handle)
inline Graph(const std::shared_ptr<ApiTable> &apiTable, QairtContext_Handle_t contextHandle, const char *name, ApiTypeRef<const GraphConfiguration&> graphConfig)
inline Graph(const std::shared_ptr<ApiTable> &apiTable, QairtContext_Handle_t contextHandle, const char *name)
inline Graph(const std::shared_ptr<ApiTable> &apiTable, QairtGraph_Handle_t parentHandle, const char *subgraphName)

Private Members friend Context bool m_isRetreived = false

True if this graph was retrieved from an existing context rather than created. 

Private Static Functions static inline void asyncCallbackTrampoline(void *trampolineObject, Qairt_Status_t status)

Friends friend class Api friend class ::qairt::ApiType struct GraphRetrieveContext

Public Members QairtContext_Handle_t m_contextHandle
class IOTensorSet

Public Functions inline IOTensorSet(std::vector<Tensor> inputs, std::vector<Tensor> outputs)
inline std::vector<Tensor> &getInputs()
inline const std::vector<Tensor> &getInputs() const
inline std::vector<Tensor> &getOutputs()
inline const std::vector<Tensor> &getOutputs() const

Private Members friend Graph struct ParentGraphHandle

Public Members QairtGraph_Handle_t m_parentHandle
class GraphConfiguration : public qairt::ApiType<GraphConfiguration, QairtGraph_ConfigV1_t>

#include <QairtGraph.hpp> 
Configuration object for graph creation and execution behavior. 

```
Construct directly — `GraphConfiguration()` — and call setter methods to
configure priority, profiling, and custom options before passing to
Context::createGraph().

```

Public Functions GraphConfiguration() noexcept = default
GraphConfiguration(GraphConfiguration&&) noexcept = default
GraphConfiguration &operator=(GraphConfiguration&&) noexcept = default
inline GraphConfiguration &setCustomConfig(const GraphCustomConfig &config)

Set a single backend-specific custom configuration entry on this graph configuration. 

See also 
QairtGraph_Config_setCustomConfigs Parameters 
config – [in] Single custom configuration entry. Throws 
qairt::Exception – on invalid handle or invalid argument. Returns 
Reference to this configuration object, enabling method chaining. inline void setCustomConfigs(const GraphCustomConfiguration &config)

Set multiple backend-specific custom configuration entries on this graph configuration. 

See also 
QairtGraph_Config_setCustomConfigs Parameters 
config – [in] Collection of custom configuration entries. Throws 
qairt::Exception – on invalid handle or invalid argument. inline Priority getPriority() const

Get the scheduling priority for this graph configuration. 

See also 
QairtGraph_Config_getPriority Throws 
qairt::Exception – on invalid handle. Returns 
The current priority level. inline void setPriority(Priority priority)

Set the scheduling priority for this graph configuration. 

See also 
QairtGraph_Config_setPriority Parameters 
priority – [in] Desired scheduling priority level. Throws 
qairt::Exception – on invalid handle or invalid argument. inline Profile &getProfile()

Get the profile handle bound to this graph configuration. 

See also 
QairtGraph_Config_getProfileHandle Throws 
qairt::Exception – on invalid handle. Returns 
Reference to the bound Profile object. inline const Profile &getProfile() const

Get the profile handle bound to this graph configuration. 

See also 
QairtGraph_Config_getProfileHandle Throws 
qairt::Exception – on invalid handle. Returns 
Const reference to the bound Profile object. inline void setProfile(const Profile &profile)

Set the profile handle on this graph configuration. 

See also 
QairtGraph_Config_setProfileHandle Parameters 
profile – [in] Profile object to bind. Throws 
qairt::Exception – on invalid handle or if the profile is already in use. inline GraphProfilingState getGraphProfilingState() const

Get the profiling state for this graph configuration. 

See also 
QairtGraph_Config_getProfilingState Throws 
qairt::Exception – on invalid handle. Returns 
The current profiling state. inline void setGraphProfilingState(GraphProfilingState graphProfilingState)

Set the profiling state for this graph configuration. 

See also 
QairtGraph_Config_setProfilingState Parameters 
graphProfilingState – [in] Desired profiling state. Throws 
qairt::Exception – on invalid handle or invalid argument. inline uint32_t getNumProfilingExecutions() const

Get the number of profiling executions configured for this graph. 

See also 
QairtGraph_Config_getNumProfilingExecutions Throws 
qairt::Exception – on invalid handle. Returns 
Number of executions to profile. inline void setNumProfilingExecutions(uint32_t numProfilingExecutions)

Set the number of executions to profile for this graph configuration. 

See also 
QairtGraph_Config_setNumProfilingExecutions Parameters 
numProfilingExecutions – [in] Number of executions to profile. Throws 
qairt::Exception – on invalid handle or invalid argument. 

Private Functions inline GraphConfiguration(const std::shared_ptr<ApiTable> &apiTable, QairtGraph_ConfigHandle_t handle)
inline void prepareToCross() const
inline void updateAfterCross() const
inline explicit GraphConfiguration(const std::shared_ptr<ApiTable> &apiTable)

Private Members detail::crossable<detail::non_owning<Profile>, &interface_type::getProfileHandle, &interface_type::setProfileHandle> m_profile

Profile handle bound to this graph configuration for continuous profiling. 

Friends friend class Api class GraphCustomConfig : public qairt::CustomConfigType

#include <QairtGraph.hpp> 
Abstract base class for a single backend-specific graph custom configuration entry. 

Public Functions virtual ~GraphCustomConfig() = default
virtual QairtGraph_CustomConfigHandle_t getCustomConfigHandle() const = 0

Protected Functions GraphCustomConfig() = default
GraphCustomConfig(const GraphCustomConfig&) = default
GraphCustomConfig(GraphCustomConfig&&) noexcept = default
GraphCustomConfig &operator=(const GraphCustomConfig&) = default
GraphCustomConfig &operator=(GraphCustomConfig&&) noexcept = default
class GraphCustomConfiguration

#include <QairtGraph.hpp> 
Abstract base class for a collection of backend-specific graph custom configuration entries. 

Public Functions virtual ~GraphCustomConfiguration() = default
virtual std::vector<QairtGraph_CustomConfigHandle_t> getCustomConfigs() const = 0

Protected Functions GraphCustomConfiguration() = default
GraphCustomConfiguration(const GraphCustomConfiguration&) = default
GraphCustomConfiguration(GraphCustomConfiguration&&) noexcept = default
GraphCustomConfiguration &operator=(const GraphCustomConfiguration&) = default
GraphCustomConfiguration &operator=(GraphCustomConfiguration&&) noexcept = default
struct NotifyStatus

Public Members QairtGraph_Error_t error
class TensorSet

Public Functions TensorSet() = default
TensorSet(TensorSet&&) noexcept = default
TensorSet(const TensorSet&) = delete
TensorSet &operator=(TensorSet&&) noexcept = default
TensorSet &operator=(const TensorSet&) = delete
inline std::vector<std::shared_ptr<Tensor>> &getInputs()
inline const std::vector<std::shared_ptr<Tensor>> &getInputs() const
inline void setInputs(std::vector<std::shared_ptr<Tensor>> inputs)
inline TensorSetMemType getMemType()
inline TensorSetMemType getMemType() const
inline void setMemType(TensorSetMemType memType)
inline std::vector<std::shared_ptr<Tensor>> &getOutputs()
inline const std::vector<std::shared_ptr<Tensor>> &getOutputs() const
inline void setOutputs(std::vector<std::shared_ptr<Tensor>> outputs)

Last Published: Jul 02, 2026

Previous 
QairtContext 
Next 
QairtTensor 

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
