# QairtBackend

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
- QairtBackend

# QairtBackend

Updated: Jul 02, 2026 80-63442-10 Rev: AL 

QairtBackend Qualcomm® AI Runtime (QAIRT) SDKs 

# QairtBackend

Note 
Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality. 
Include: `#include "QairtCppApi/QairtBackend.hpp"` 
C++ wrapper for the QAIRT Backend API. namespace qairt

Enums enum class BackendError : std::underlying_type_t<QairtBackend_Error_t>

Error codes returned by QAIRT backend operations. 

Enumerator 
Description 

`NoError` 
Operation succeeded. 

`MemAlloc` 
Memory allocation failure. 

`UnsupportedPlatform` 
Backend creation attempted on an unsupported platform. 

`CannotInitialize` 
Backend failed to initialize. 

`TerminateFailed` 
Failed to free allocated resources during termination. 

`NotSupported` 
Requested functionality is not supported by this backend. 

`InvalidArgument` 
An argument to the operation was invalid. 

`OpPackageNotFound` 
The specified op package library could not be found. 

`OpPackageIfProviderNotFound` 
The interface provider symbol was not found in the op package. 

`OpPackageRegistrationFailed` 
Op package registration failed. 

`OpPackageUnsupportedVersion` 
The op package interface version is not supported. 

`OpPackageDuplicate` 
An op with the same package and op name is already registered. 

`InconsistentConfig` 
Backend configuration is inconsistent across create calls. 

`InvalidHandle` 
The provided backend handle is not valid. 

`InvalidConfig` 
One or more configuration values are invalid. 

`Undefined` 
An undefined or unknown error occurred. 
Values: enumerator NoError = QAIRT_BACKEND_NO_ERROR
enumerator MemAlloc = QAIRT_BACKEND_ERROR_MEM_ALLOC
enumerator UnsupportedPlatform = QAIRT_BACKEND_ERROR_UNSUPPORTED_PLATFORM
enumerator CannotInitialize = QAIRT_BACKEND_ERROR_CANNOT_INITIALIZE
enumerator TerminateFailed = QAIRT_BACKEND_ERROR_TERMINATE_FAILED
enumerator NotSupported = QAIRT_BACKEND_ERROR_NOT_SUPPORTED
enumerator InvalidArgument = QAIRT_BACKEND_ERROR_INVALID_ARGUMENT
enumerator OpPackageNotFound = QAIRT_BACKEND_ERROR_OP_PACKAGE_NOT_FOUND
enumerator OpPackageIfProviderNotFound = QAIRT_BACKEND_ERROR_OP_PACKAGE_IF_PROVIDER_NOT_FOUND
enumerator OpPackageRegistrationFailed = QAIRT_BACKEND_ERROR_OP_PACKAGE_REGISTRATION_FAILED
enumerator OpPackageUnsupportedVersion = QAIRT_BACKEND_ERROR_OP_PACKAGE_UNSUPPORTED_VERSION
enumerator OpPackageDuplicate = QAIRT_BACKEND_ERROR_OP_PACKAGE_DUPLICATE
enumerator InconsistentConfig = QAIRT_BACKEND_ERROR_INCONSISTENT_CONFIG
enumerator InvalidHandle = QAIRT_BACKEND_ERROR_INVALID_HANDLE
enumerator InvalidConfig = QAIRT_BACKEND_ERROR_INVALID_CONFIG
enumerator Undefined = QAIRT_BACKEND_ERROR_UNDEFINED
enum class ErrorReportingConfigLevel : std::underlying_type_t<QairtErrorReporting_Config_Level_t>

Verbosity levels for the error reporting configuration. 

Enumerator 
Description 

`Brief` 
Collect basic summary information about each error. 

`Detailed` 
Collect detailed, memory-resident error information. 

`Undefined` 
Level is unset or unknown. 
Values: enumerator Brief = QAIRT_ERROR_REPORTING_LEVEL_BRIEF
enumerator Detailed = QAIRT_ERROR_REPORTING_LEVEL_DETAILED
enumerator Undefined = QAIRT_ERROR_REPORTING_LEVEL_UNDEFINED
class Backend : public qairt::ApiType<Backend, QairtBackend_V1_t>

#include <QairtBackend.hpp> 
Wrapper for a QAIRT Backend handle. 
Obtained via Api::createBackend(). 

Public Functions Backend() noexcept = default
Backend(const Backend&) = delete
Backend(Backend&&) noexcept = default
Backend &operator=(const Backend&) = delete
Backend &operator=(Backend&&) noexcept = default
inline Backend(const std::shared_ptr<ApiTable> &apiTable, QairtBackend_Handle_t handle)
inline void setConfig(const BackendConfiguration &config)

Set configuration options on this backend after creation. 

See also 
QairtBackend_setConfig Parameters 
config – [in] The backend configuration to apply. Throws 
qairt::Exception – on invalid handle, invalid config, or unsupported feature. inline void registerOpPackage(const char *packagePath, const char *interfaceProvider, const char *target)

Register an op package library with this backend. 
Loads the shared library at packagePath and registers its operations using the interface provider function interfaceProvider. An optional target platform string restricts registration to a specific processing unit. 

See also 
QairtBackend_registerOpPackage Parameters 

- 
packagePath – [in] Path on disk to the op package shared library. Must not be NULL. 
- 
interfaceProvider – [in] Name of the interface provider function exported by the op package library. Must not be NULL. 
- 
target – [in] Optional target platform string. NULL applies no target restriction. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
NULL packagePath or interfaceProvider 
- 
library not found 
- 
interface provider symbol not found 
- 
registration failure 
- 
unsupported op package interface version 
- 
duplicate op registration inline void registerOpPackage(const std::string &packagePath, const std::string &interfaceProvider, const std::string &target)

Wrapper which allows for `std::string` path arguments instead of `const char*`. 

See also 
Backend::registerOpPackage(const char*, const char*, const char*) inline std::vector<BackendOperationName> getSupportedOperations() const

Get all operations supported by this backend, including built-in ops. 

See also 
QairtBackend_getSupportedOperations Throws 
qairt::Exception – on invalid handle. Returns 
Vector of BackendOperationName descriptors, one per supported operation. inline void validateOpConfig(const OpConfig &opConfig)

Validate an op configuration against the appropriate registered op package. 
The backend selects the op package for validation based on attributes of opConfig. 

See also 
QairtBackend_validateOpConfig Parameters 
opConfig – [in] The op configuration to validate. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
validation failure 
- 
validation not supported by this backend 
- 
no matching op package found inline void validateContextBinary(ApiTypeRef<const Device&> device, ApiTypeRef<const ContextBinaryBuffer&> contextBinary, ApiTypeRef<const ContextConfiguration&> contextConfig)

Validate a context binary against a device and context configuration. 
Checks that the binary is compatible with device and the options in contextConfig before it is loaded via createContextFromBinary(). Parameters 

- 
device – [in] The device on which the binary would be loaded. 
- 
contextBinary – [in] The context binary buffer to validate. 
- 
contextConfig – [in] The context configuration to validate against. Throws 
qairt::Exception – on invalid handle or validation failure. inline Context createContext(ApiTypeRef<const ContextConfiguration&> contextConfig = {})

Create a context using this backend. 
Creates a context with no device (uses backend default) and an optional context configuration. Parameters 
contextConfig – [in] Context configuration. Optional. Throws 
qairt::Exception – on invalid handle or configuration error. Returns 
A new Context. inline Context createContext(ApiTypeRef<const Device&> device, ApiTypeRef<const ContextConfiguration&> contextConfig)

Create a context for a specific device using this backend. Parameters 

- 
device – [in] The device on which to create the context. 
- 
contextConfig – [in] Context configuration. Optional. Throws 
qairt::Exception – on invalid handle or configuration error. Returns 
A new Context. inline Context createContextFromBinary(ApiTypeRef<const Device&> device, ApiTypeRef<const ContextConfiguration&> contextConfig, ApiTypeRef<const ContextBinaryBuffer&> contextBinaryBuffer, ApiTypeRef<const Signal&> signal = {}, ApiTypeRef<const Profile&> profile = {})

Create a context from a serialized context binary. 
Pass a Signal to enable aborting or timing out the load operation. Parameters 

- 
device – [in] The device on which to load the context binary. 
- 
contextConfig – [in] Context configuration. Optional. 
- 
contextBinaryBuffer – [in] The serialized context binary to load. 
- 
signal – [in] Optional signal to control the load operation. 
- 
profile – [in] Optional profile handle to collect load-time events. Throws 
qairt::Exception – on invalid handle, binary incompatibility, or configuration error. Returns 
A new Context. inline Profile createProfile(uint32_t level)

Create a profiling handle at the specified granularity. 

See also 
QairtBackend_createProfile Parameters 
level – [in] Granularity level at which events should be collected. Throws 
qairt::Exception – on invalid handle, unsupported profiling level, or memory error. Returns 
A new Profile. inline std::shared_ptr<Profile> createSharedProfile(uint32_t level)

Create a shared profiling handle at the specified granularity. 

See also 
QairtBackend_createProfile Parameters 
level – [in] Granularity level at which events should be collected. Throws 
qairt::Exception – on invalid handle, unsupported profiling level, or memory error. Returns 
A `std::shared_ptr` to a new Profile. inline Signal createSignal(ApiTypeRef<const SignalConfiguration&> signalConfig = {})

Create a new signal object associated with this backend. 
Signals are used to control the execution of API calls that accept them (e.g., Graph::execute, Context::createFromBinary). The created signal is idle and immediately available for use. 
The signal is backend-scoped: the backend allocates any required synchronization primitives and validates signal support. A signal may only be used with the backend that created it. Parameters 
signalConfig – [in] Configuration for the signal. Optional. Throws 
qairt::Exception – on:

- 
invalid handle 
- 
signals not supported by this backend 
- 
invalid signal configuration 
- 
memory allocation failure Returns 
A new Signal object. 

Private Functions inline Backend(const std::shared_ptr<ApiTable> &apiTable, ApiTypeRef<const Log&> log, ApiTypeRef<const BackendConfiguration&> config = {})

Private Members detail::crossable<detail::set_only<BackendConfiguration>, nullptr, &interface_type::setConfig> m_memory

Staging storage for a BackendConfiguration being crossed to the C layer. 

Friends friend class Api class BackendConfiguration : public qairt::ApiType<BackendConfiguration, QairtBackend_ConfigV1_t>

#include <QairtBackend.hpp> 
Configuration object for backend creation and reconfiguration. 
Construct directly — `BackendConfiguration()` — and call setter methods to populate options before passing to Api::createBackend() or Backend::setConfig(). All setter methods return `*this` to support method chaining. 

Public Functions BackendConfiguration() noexcept = default
BackendConfiguration(const BackendConfiguration&) = delete
BackendConfiguration(BackendConfiguration&&) noexcept = default
BackendConfiguration &operator=(const BackendConfiguration&) = delete
BackendConfiguration &operator=(BackendConfiguration&&) noexcept = default
inline BackendConfiguration &setCustomConfig(const BackendCustomConfig &backendCustomConfig)

Set a single backend-specific custom configuration item on this configuration. 

See also 
QairtBackend_Config_setCustomConfigs Parameters 
backendCustomConfig – [in] The custom configuration item whose handle will be applied. Throws 
qairt::Exception – on invalid handle. Returns 
Reference to this object for method chaining. inline BackendConfiguration &setCustomConfigs(const BackendCustomConfiguration &config)

Set a collection of backend-specific custom configuration items on this configuration. 

See also 
QairtBackend_Config_setCustomConfigs Parameters 
config – [in] The custom configuration collection whose handles will be applied. Throws 
qairt::Exception – on invalid handle. Returns 
Reference to this object for method chaining. inline uint32_t getNumPlatformOptions() const

Get the number of platform options set on this configuration. 

See also 
QairtBackend_Config_getNumPlatformOptions Throws 
qairt::Exception – on invalid handle. Returns 
Number of platform option strings currently set. inline std::string_view getPlatformOptionAt(uint32_t idx) const

Get the platform option string at the specified index. 

See also 
QairtBackend_Config_getPlatformOptionAt Parameters 
idx – [in] Zero-based index into the platform options list. Must be less than getNumPlatformOptions(). Throws 
qairt::Exception – on invalid handle or out-of-range index. Returns 
The null-terminated platform option key-value pair string at idx, or an empty view if the stored pointer is null. inline std::vector<std::string_view> getPlatformOptions() const

Get all platform option strings set on this configuration. 

See also 
QairtBackend_Config_getNumPlatformOptions Throws 
qairt::Exception – on invalid handle. Returns 
Vector of platform option key-value pair strings. inline BackendConfiguration &setPlatformOptions(const std::vector<const char*> &platformOptions)

Set the platform options on this configuration from an array of C strings. 

See also 
QairtBackend_Config_setPlatformOptions Parameters 
platformOptions – [in] Array of null-terminated platform option strings. Throws 
qairt::Exception – on invalid handle or invalid options. Returns 
Reference to this object for method chaining. inline BackendConfiguration &setPlatformOptions(const std::vector<std::string> &platformOptions)

Wrapper which allows for `std::string` platform option values instead of `const char*`. 

See also 
BackendConfiguration::setPlatformOptions(const std::vector<const char*>&) inline BackendConfiguration &setPlatformOptions(const std::vector<std::string_view> &platformOptions)

Wrapper which allows for `std::string_view` platform option values instead of `const char*`. 

See also 
BackendConfiguration::setPlatformOptions(const std::vector<const char*>&) inline BackendConfiguration &resetPlatformOptions()

Clear all platform options from this configuration. 

See also 
QairtBackend_Config_setPlatformOptions Throws 
qairt::Exception – on invalid handle. Returns 
Reference to this object for method chaining. inline std::optional<std::reference_wrapper<ErrorReportingConfig>> getErrorReportingConfig()

Get the error reporting configuration attached to this backend configuration. 

See also 
QairtBackend_Config_getErrorReportingConfig Throws 
qairt::Exception – on invalid handle. Returns 
A reference wrapper to the attached ErrorReportingConfig, or an empty optional if none has been set. inline std::optional<std::reference_wrapper<ErrorReportingConfig>> getErrorReportingConfig() const

Get the error reporting configuration attached to this backend configuration. 

See also 
QairtBackend_Config_getErrorReportingConfig Throws 
qairt::Exception – on invalid handle. Returns 
A const reference wrapper to the attached ErrorReportingConfig, or an empty optional if none has been set. inline void setErrorReportingConfig(const ErrorReportingConfig &errorReportingConfig)

Attach an error reporting configuration to this backend configuration. 

See also 
QairtBackend_Config_setErrorReportingConfig Parameters 
errorReportingConfig – [in] The error reporting configuration to attach. Throws 
qairt::Exception – on invalid handle. 

Private Functions inline void prepareToCross() const
inline void updateAfterCross() const
inline explicit BackendConfiguration(const std::shared_ptr<ApiTable> &apiTable)

Private Members std::optional<detail::crossable<detail::non_owning<ErrorReportingConfig>, &interface_type::getErrorReportingConfig, &interface_type::setErrorReportingConfig>> m_errorReportingConfig

Optional error reporting configuration cross-linked to the C handle. 

Friends friend class Api class BackendCustomConfig : public qairt::CustomConfigType

#include <QairtBackend.hpp> 
Abstract base class for a single backend-specific custom configuration item. 
Subclass this to provide a backend-specific configuration handle to BackendConfiguration::setCustomConfig(). Refer to the backend documentation for the concrete subclass and valid handle values. 

Public Functions virtual ~BackendCustomConfig() = default
virtual QairtBackend_CustomConfigHandle_t getCustomConfigHandle() const = 0

Get the underlying C handle for this custom configuration item. Returns 
The backend-specific custom configuration handle. 

Protected Functions BackendCustomConfig() = default
BackendCustomConfig(const BackendCustomConfig&) = default
BackendCustomConfig(BackendCustomConfig&&) noexcept = default
BackendCustomConfig &operator=(const BackendCustomConfig&) = default
BackendCustomConfig &operator=(BackendCustomConfig&&) noexcept = default
class BackendCustomConfiguration

#include <QairtBackend.hpp> 
Abstract base class for a collection of backend-specific custom configuration items. 
Subclass this to provide multiple backend-specific configuration handles to BackendConfiguration::setCustomConfigs(). Refer to the backend documentation for the concrete subclass and valid handle values. 

Public Functions virtual ~BackendCustomConfiguration() = default
virtual std::vector<QairtBackend_CustomConfigHandle_t> getCustomConfigs() const = 0

Get the list of underlying C handles for this custom configuration collection. Returns 
Vector of backend-specific custom configuration handles. 

Protected Functions BackendCustomConfiguration() = default
BackendCustomConfiguration(const BackendCustomConfiguration&) = default
BackendCustomConfiguration(BackendCustomConfiguration&&) noexcept = default
BackendCustomConfiguration &operator=(const BackendCustomConfiguration&) = default
BackendCustomConfiguration &operator=(BackendCustomConfiguration&&) noexcept = default
class BackendOperationName

#include <QairtBackend.hpp> 
Name descriptor for a single operation supported by a backend. 
Obtained from Backend::getSupportedOperations(). All string views are non-owning references to backend-managed memory. 

Public Functions constexpr BackendOperationName() noexcept = default
constexpr BackendOperationName(const BackendOperationName&) noexcept = default
constexpr BackendOperationName(BackendOperationName&&) noexcept = default
constexpr BackendOperationName &operator=(const BackendOperationName&) noexcept = default
constexpr BackendOperationName &operator=(BackendOperationName&&) noexcept = default
inline constexpr BackendOperationName(std::string_view packageName, std::string_view name, std::string_view target) noexcept

Construct a BackendOperationName from its three name components. Parameters 

- 
packageName – [in] Name of the op package that provides this operation. 
- 
name – [in] Name of the operation within the op package. 
- 
target – [in] Target platform for this operation entry. May be empty if the backend does not distinguish targets. inline const std::string_view &getPackageName() const noexcept

Get the op package name for this operation. 

See also 
QairtBackend_OperationName_getPackageName Returns 
Name of the op package that provides this operation. inline const std::string_view &getName() const noexcept

Get the operation name within its op package. 

See also 
QairtBackend_OperationName_getName Returns 
Name of the operation within its package. inline const std::string_view getTarget() const noexcept

Get the target platform for this operation entry. 

See also 
QairtBackend_OperationName_getTarget Returns 
Target platform string, or an empty view if unused by this backend. 

Private Members std::string_view m_packageName

The op package that provides this operation. std::string_view m_name

The name of the operation within its package. std::string_view m_target

The target platform for which this operation entry is registered. class ErrorReportingConfig : public qairt::ApiType<ErrorReportingConfig, QairtErrorReporting_Config_V1_t>

#include <QairtBackend.hpp> 
Configuration object for backend error reporting behavior. 
Controls how much detail is captured when errors occur and how much memory is reserved for error data. Obtained via Api::createBackend() or set on a BackendConfiguration before backend creation. 

Public Functions ErrorReportingConfig() = default
inline ErrorReportingConfigLevel getReportingLevel() const

Get the reporting verbosity level for this error reporting configuration. 

See also 
QairtErrorReporting_Config_getReportingLevel Throws 
qairt::Exception – on invalid handle. Returns 
The current reporting level. inline void setReportingLevel(ErrorReportingConfigLevel level)

Set the reporting verbosity level for this error reporting configuration. 

See also 
QairtErrorReporting_Config_setReportingLevel Parameters 
level – [in] Desired reporting verbosity. Must be a valid enumerator. Throws 
qairt::Exception – on invalid handle or invalid level. inline uint32_t getStorageLimit() const

Get the memory storage limit for this error reporting configuration. 

See also 
QairtErrorReporting_Config_getStorageLimit Throws 
qairt::Exception – on invalid handle. Returns 
Storage limit in kilobytes. inline void setStorageLimit(uint32_t limit)

Set the memory storage limit for this error reporting configuration. 

See also 
QairtErrorReporting_Config_setStorageLimit Parameters 
limit – [in] Maximum memory reserved for error information, in kilobytes. Throws 
qairt::Exception – on invalid handle or invalid limit. 

Private Functions inline ErrorReportingConfig(const std::shared_ptr<ApiTable> &apiTable, ErrorReportingConfigLevel level, uint32_t storageLimit)
inline explicit ErrorReportingConfig(const std::shared_ptr<ApiTable> &apiTable)

Friends friend class Api friend class ::qairt::ApiType 

Last Published: Jul 02, 2026

Previous 
QairtTensor 
Next 
QairtDevice 

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
