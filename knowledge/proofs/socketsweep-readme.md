# SocketSweep — Android Storage Scanner (README)

Source: "Copy of SOCKETSWEEP_README (Markor)" (Drive `1taes5xKB8GHgVwK5HBroANvJ2shywd8N`, appears identically in both the RESEARCH DOSSIER {CANNON} root and its PROOF/REVERSE ENGINEERING MATERIAL subfolder). Category: **case study / reference repo README**. Author: Vishnu Srivatsava. License: GPL 3.0.

## The Problem

Standard Android-to-PC USB file browsing uses MTP (Media Transfer Protocol), designed in 2008 for MP3 players. It transfers file metadata one item at a time with no caching, no parallel requests, and no fast recursive scan — unusable for scanning 100GB+ of modern phone storage. Result: "Calculating size..." hangs for 4+ minutes, painfully slow navigation, no practical way to find large files.

SocketSweep bypasses MTP entirely.

## Speed

Full `/sdcard` scan on a Samsung Galaxy S24 Ultra (256GB, ~47,000 files): SocketSweep completes in ~6–15 seconds with a full interactive treemap ready to explore (best case 6.9s with warm cache). Scan time varies with device load (background apps, media indexing, thermal state). <!-- UNVERIFIED: single-device benchmark figures from the project's own README, not independently reproduced; the README itself notes "proper side-by-side benchmarks against OpenMTP and other tools are coming soon." -->

## How It Works

1. Pushes a tiny C++ program (~1MB) to the phone via ADB.
2. That program scans the filesystem directly on the phone using native POSIX calls — this is the source of the speed (no MTP bottleneck).
3. Streams the results back to the PC over a TCP socket through the USB cable (ADB port forwarding).
4. Renders an interactive treemap in a React frontend.

Architecture explicitly inspired by **scrcpy** — "push a native binary via ADB, communicate over a local socket" pattern.

## Architecture (Three Layers)

- **Host Desktop**: React + Recharts interactive dashboard, Rust/Tauri backend as command orchestrator.
- **ADB Protocol**: ADB port forwarding, `TCP:5050 -> TCP:5050`.
- **Android Device**: C++17 daemon, headless socket server, POSIX filesystem access to `/sdcard`.

## Interaction Lifecycle

1. Rust (Tauri) invokes `init_daemon`.
2. `pkill daemon` (cleanup of any stale process).
3. `push daemon /data/local/tmp` (daemon binary pushed via ADB).
4. `appops set MANAGE_EXTERNAL_STORAGE allow`.
5. `nohup ./daemon &` starts the daemon headless.
6. `adb forward tcp:5050 tcp:5050`.
7. Ping-retry loop (150ms) until ACK connection received → "Connected!"
8. `invoke("run_scan", { path: "/sdcard" })` → TCP send `SCAN /sdcard\n` → daemon does recursive fast POSIX traversal → streams large JSON tree back → React parses and renders treemap.
9. `invoke("delete_item", { path })` → TCP send `DELETE /sdcard/...\n` → daemon runs `std::filesystem::remove_all` → returns `{"status":"ok"}` → UI updates/rescans.

## Installation (End User)

1. Download platform build: Windows installer (.exe) / Enterprise (.msi), macOS Apple Silicon (.dmg), Linux (AppImage / .deb). macOS note: since the build is ad-hoc signed, run `xattr -cr /Applications/SocketSweep.app` once after install.
2. Enable USB Debugging on the phone: Settings → About Phone → tap "Build Number" 7 times → Settings → Developer Options → enable "USB Debugging".
3. Plug in the phone via USB, open SocketSweep, click Connect (auto-pushes the daemon and sets everything up), click Scan, click any treemap block to drill down, delete directly from the app. No apps needed on the phone side, no Wi-Fi setup, no root required.

## Development Setup (Building from Source)

Prerequisites: Node.js v18+, Rust v1.70+ with Cargo, Android NDK v26d or newer, Android SDK/ADB on `$PATH`.

```bash
# 1. Compile the C++ daemon (cross-compile for aarch64-linux-android)
export NDK=/path/to/your/android-ndk-r26d
cd engine
bash ./build.sh
# generates the stripped daemon binary in engine/

# 2. Install frontend dependencies
cd ..
npm install

# 3. Run the app (phone must be plugged in, USB Debugging enabled)
npm run tauri dev
```

## Troubleshooting

- **"0 Files" or missing folders on Android 11+**: Scoped Storage restricts file access. SocketSweep tries to bypass automatically via `adb shell appops set com.android.shell MANAGE_EXTERNAL_STORAGE allow`. If still empty, check OEM-specific toggles (e.g. Xiaomi needs "USB Debugging (Security settings)" enabled separately).
- **Samsung Auto Blocker**: if USB Debugging is greyed out on a Samsung device, Auto Blocker (Settings → Security → Auto Blocker) is likely on — it disables USB Debugging entirely, blocking any ADB-based tool including SocketSweep. Off by default; only an issue if manually enabled.
- **Daemon fails to start / "Permission denied"**: ensure the daemon is pushed to `/data/local/tmp/`, not `/sdcard/` — modern Android blocks execution from `/sdcard/`. SocketSweep handles this automatically.

## Relevance to this project

Directly relevant as a working precedent for the "Local Shell Loop" / ADB-based device-control patterns described in the Omni Claw blueprint — specifically the scrcpy-style "push a native binary via ADB, talk over a local TCP socket" pattern that Omni Claw's tool-execution map also relies on (see `omni-claw-blueprint.md` §6). Also a concrete demonstration that a small C++ daemon plus ADB port-forwarding can outperform standard Android-PC bridging for filesystem-heavy tasks — a pattern potentially reusable for any future need to stream large local data (e.g. wiki/JSONL sync) off-device fast.
