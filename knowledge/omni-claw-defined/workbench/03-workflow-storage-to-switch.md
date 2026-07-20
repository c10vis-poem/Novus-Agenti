The Horizons Workflow: From Storage to Switch
Welcome to the architect’s guide to Horizons. As you enter this environment, imagine yourself
as a lead technician stepping up to a high-performance, industrial workbench. In traditional
software, applications often fail by trying to "guess" your intent—silently loading heavy model
weights in the background and crashing before you can even see the interface. Horizons rejects
this. It is a stable, empty-at-boot workspace where you are the loader.

1. Foundations: The App as a Stable Workbench
The core philosophy of Horizons is stability through manual agency. We treat the Kotlin UI as a
visual "Operating System" or workbench, while the "heavy hitters"—the Chromium browser, the
Terminal, and the inference engines—act as independent guest ports running underneath it.
By keeping the workbench empty at launch, we ensure the system remains responsive and
lightweight, regardless of the heavy assets you intend to deploy.| Feature | Legacy "Auto-Boot"
(Unstable) | Horizons Manual Loading (Stable) || ------ | ------ | ------ || Startup State | Attempts to
auto-detect and load models secretly. | Boots to a clean, empty "workbench" state. || Memory
Usage | High and unpredictable; prone to "OOM" kills. | Low and stable; resources used only
upon activation. || Crash Risk | High risk of silent background failures. | Virtually zero; manual
"fuses" prevent structural crashes. || User Agency | System-driven; "black box" logic. |
Architect-driven ; manual plating of guest ports. |
This stable workbench is organized into a specific spatial geometry where each "room" serves a
distinct stage of mechanical assembly.

2. The Seven-Tile Geometry: Mapping Your Command Center
Horizons is mapped across a seven-tile clock-face layout. To master the workflow, you must
understand where your tools are stored and where the work is performed:
   ●​ Horizons Panel (Top-Left): The deck for system information, credits, and legal
       blueprints.
   ●​ Monitor / Checkpoint (12:00): The dashboard for "window-shopping" your library and
       running "green-light" safety checks.
   ●​ Chat (Right): The primary engagement interface; the agentic engine’s front-end.
   ●​ Router / Fuse Box (Center): The final staging area where configurations are "plated"
       and activated.
   ●​ Settings / Platform Armory (Southeast/4:30): The vault for raw ingredients—API
       keys, tokens, SDKs, and model weights.
   ●​ Terminal / Mod Garage (Southwest/6:00): The space for scripting, bash commands,
       and defining custom runtimes.
   ●​ Archives (7:30): The storage bay for exported harnesses, saved environments, and
       historical artifacts.Understanding this layout is the first step toward moving an inert file
       from storage into a live, agentic engine.
3. Step 1: Landing (The Platform Armory)
The workflow begins in the Platform Armory . Unlike other apps, Horizons treats imported files
as "inert" ingredients. They do not spin up the moment they land.
 1.​ Initial Import: Use the "Open with → Horizons" command on your device for any model
        weight (.gguf), library (.so), or plugin.
 2.​ Storage in the Vault: The file is registered in the Armory. It is now part of your "pantry"
        but remains disconnected from the system's power.
 3.​ Persistence: Because these files land in an inert state, they cannot cause memory
        pressure or startup crashes. They sit safely in the vault until you are ready to "bake"
        them into a configuration.Once a file has landed, it requires a "green light" from the
        Checkpoint before it can be plated for use.

4. Step 2: Verification (The Monitor Checkpoint)
Before any configuration is powered on, it must pass through the Monitor . This is where you
verify the "10-amp fuse" of your setup. The Monitor runs a greenLight check to ensure the guest
port is safe to open.
    ●​ Binary Presence: Is the engine file (e.g., llama.cpp) readable in the vault?
    ●​ Execution Permissions: Does the binary have the "exec bit" set? (System runs
         chmod +x to ensure it can actually run).
    ●​ Asset Availability: Are all required .so plugins and libraries accounted for?
    ●​ Model Path Acknowledgment: Has the user explicitly "plugged in" the model
         weight?The Monitor provides immediate visual feedback: "All Green" signifies a valid
         recipe, while "N Red" identifies a blown fuse—such as a missing library or a
         permission error. Once the light is green, you hand the verified configuration over to the
         Router for the final phase.

5. Step 3: Activation (The Router Fuse Box)
The Router is the central information highway where configurations are "plated." This is where
the UI interacts with CliffordService , a dedicated background daemon manager. We built
CliffordService as a separate process because Android tends to "silently kill" background
threads when a UI gets heavy; by keeping the daemon separate, your inference remains alive
even if the interface is under load.When you are in the Router, you have three primary
architectural controls:
     ●​ The Re-Check on Flip: When you flip the switch to "ON," the Router re-runs the
        green-light checks instantly. If you unplugged a model in the Armory, the switch won't
        close.
     ●​ The "RUNNING" State: This is the live handshake where the daemon is engaged and
        the guest port is open.
     ●​ Sleep/Archive Options: You can "Sleep" a config (unload it from memory but keep the
        "on the deck" settings) or "Archive" it to the 7:30 bay for long-term storage.The "Goat"
        Failure State: If a handshake fails or a fuse blows during activation, the system triggers
        the // GOAT_SAYS_NO state. Rather than a silent crash, you are greeted with a
        structural failure banner and a synthesized, warbling "meh-eh-eh" bleat, alerting you that
       the machine needs a manual adjustment in the Mod Garage.This manual control creates
       a powerful, agentic workflow where you are the master of the machine’s resources.

Conclusion: The Obsidian Vision
A successful workflow is a sensory experience. You move from an "empty cart" to a
high-performance engine, watching the UI transform into a landscape of volcanic glass facets
with specular glints . As you flip the switch, layered glow nodes pulse along the plasma
conduits, and the amber sun of the Horizons icon illuminates the horizon.By following this
journey— Landing, Verification, and Activation —you ensure your system remains a stable,
powerful workbench. You are no longer a spectator to background processes; you are the lead
architect of your own agentic engine.
