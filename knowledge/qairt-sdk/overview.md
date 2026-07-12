# Qualcomm AI Runtime (QAIRT) Overview

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

- Overview

# Qualcomm AI Runtime (QAIRT) Overview

Updated: Jun 04, 2026 80-63442-10 Rev: AK 

On this page

- How to Use QAIRT
- QAIRT API (unified runtime API)
- Additional Runtimes (aka “Delegates”)

Qualcomm AI Runtime (QAIRT) Overview Qualcomm® AI Runtime (QAIRT) SDKs 

# Qualcomm AI Runtime (QAIRT) Overview

Welcome to Qualcomm’s AI RunTime (aka “QAIRT”) documentation. QAIRT is a suite of tools that help you develop, run, and optimize AI models for Qualcomm-supported hardware. 
There are several stages to go from having a trained AI model on your “host machine” to a runnable model on your “target device”. QAIRT helps prepare the proper files you will need on your target device. It also provides runtime interpretters for each backend and processor to turn model instructions into runnable code. 

## How to Use QAIRT

There are two primary SDKs which automate large portions of the AI build pipeline: 

- 
Qualcomm Snapdragon Neural Processing SDK (aka “SNPE”) is a simpler API and allows your model to execute using multiple processors. The tradeoff for that simplicity is that SNPE may have larger files and less granular control over how individual model operations are implemented. 
- 
Qualcomm AI Engine Direct (aka “QNN”) for granular control over how each operation in your model works. This SDK builds models to work with specific processors. 
- 
Generative AI Inference Extensions (GENIE) SDK. GENIE extends QNN specifically for generative AI use cases (Ex. LLMs). 
With both of these SDKs, you will need to: 

- 
Get an AI model (ex. downloading a TensorFlow model). 
- 
Use CLI tools provided in the SDKs to convert the model into a format the target device runtimes can interpret. 
- 
Write an app in C, C++, or Java using the chosen SDK’s API to execute your model. 
- 
Transfer the built model, app executable, and QAIRT runtimes to the target device. 
- 
Run your app to execute inferences on the target device (ex. passing images in to be classified by Inception V3). 
- 
Benchmark and optimize your performance. 
In order to use QAIRT, pick an SDK below and follow their tutorials to see the workflow in action: 

- 
SNPE - For a simpler API which can run your model on multiple processors. 
- 
QNN - For granular control over how your model is executed. 
- 
GENIE (extends QNN) - For LLM or Generative AI use cases. 

## QAIRT API (unified runtime API)

The QAIRT API is the low-level runtime API that sits beneath the SDKs. When you load a prepared model and run inference from an application, that application calls into QAIRT — directly if you are writing against the API, or indirectly through one of the SDKs. The SDK tools (converters, quantizers) produce artifacts that QAIRT consumes at runtime. 
Use QAIRT API directly when you want a single API surface that targets all Qualcomm accelerators (CPU, GPU, HTP) through a uniform interface without the SDK-specific conventions layered on top. There are two public entry points: 

- 
QAIRT C API — stable, ABI-compatible C interface. Preferred for systems integration, language bindings, and long-lived embedded deployments. 
- 
QAIRT C++ API — header-only RAII wrapper over the C API. Preferred for new application code. 
See the QAIRT API overview for how the C and C++ surfaces relate and guidance on which to pick. 

## Additional Runtimes (aka “Delegates”)

QAIRT SDKs provide runtimes that allow model operations to execute on target device processors (ex. CPU, GPU, HTP, etc.). For most situations, the runtimes provided by those SDKs will be the right ones for your use case. 
There are several additional runtimes which are either optimized for specific model frameworks (ex. TFLite Delegate) or made for specific hardware components. 
If these apply to your use case, you may need to follow the additional steps documented within to have your model execute in the proper environment: 

- 
TFLite Delegate - Specifically optimized for TFLite model files. 

Last Published: Jun 04, 2026

Next 
QAIRT API 

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
