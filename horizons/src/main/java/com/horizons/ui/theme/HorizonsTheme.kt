package com.horizons.ui.theme

import androidx.compose.ui.graphics.Color

object HorizonsColors {
    val Background    = Color(0xFF222C34)
    val Surface       = Color(0xFF35414A)
    val PrimaryTeal   = Color(0xFF2DD4D9)
    val HighlightTeal = Color(0xFF4FE7EC)
    val IconBackplate = Color(0xFF050709)
    val ActionYellow  = Color(0xFFF5C518)

    val TileHorizons  = Color(0xFF40C4FF)  // blue (spec §2: 10:00 HORIZONS is blue; amber sun)
    val TileMonitor   = Color(0xFF2DD4D9)  // teal/cyan (spec §2: 12:00 MONITOR)
    val TileChat      = Color(0xFF4FE9A6)  // soft green — distinct from Terminal matrix-green (spec §2 note)
    val TileRouter    = Color(0xFFAA77FF)  // violet (crystal icon; ROUTER *label* is white — spec §3)
    val TileArtifacts = Color(0xFFE8A838)  // amber (spec §2: 8:00 ARCHIVES)
    val TileTerminal  = Color(0xFF00FF41)  // matrix green (spec §2: 6:00 TERMINAL, brighter)
    val TileSettings  = Color(0xFFFF5577)  // pink/crimson (spec §2: 4:00 SETTINGS)

    // Nebula purple — header/bold/outline accent (spec §6: the nebula lock-screen 09:57 clock hue)
    val NebulaPurple  = Color(0xFFC77DFF)

    // Terminal card gets a deeper near-black bg vs other tiles (spec §2 color note)
    val TerminalCardBg = Color(0xFF060A07)

    val StatusAsr     = Color(0xFF00E676)  // green
    val StatusLlm     = Color(0xFF40C4FF)  // blue
    val StatusTts     = Color(0xFFFFB74D)  // orange
    val StatusMllm    = Color(0xFFCE93D8)  // purple
    val StatusVag     = Color(0xFFFF4081)  // pink
}
