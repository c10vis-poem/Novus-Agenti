package com.horizons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.Panel
import com.horizons.ui.theme.HorizonsColors

@Composable
fun HomeGrid(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()

    val npuReady = backendStatus.startsWith("Hexagon HTP") || backendStatus.startsWith("Adreno 830")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HorizonsColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // ── Banner ───────────────────────────────────────────────────────────
        Text(
            "MO)u14R_11(",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = HorizonsColors.PrimaryTeal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "*PIONEER_TECH  ·  (NEXT-GEN CERTIFIED)  ·  HORIZONS // V4",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(20.dp))

        // ── Top row: Horizons · Monitor · Chat ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TileCard(
                name = "HORIZONS",
                slug = "/ home",
                subtitle = "Home node · System\noverview",
                color = HorizonsColors.TileHorizons,
                icon = "⌂",
                cmdHint = "$ home --status",
                onClick = { onTileClick(Panel.Horizons) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "MONITOR",
                slug = "/ cognito",
                subtitle = "Model · API ·\nCLI",
                color = HorizonsColors.TileMonitor,
                icon = "◎",
                cmdHint = "$ cognito",
                onClick = { onTileClick(Panel.Monitor) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "CHAT",
                slug = "/ interface",
                subtitle = "Full AI interface ·\nArtifacts · History",
                color = HorizonsColors.TileChat,
                icon = "◬",
                cmdHint = "$ chat --open",
                onClick = { onTileClick(Panel.Chat) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Center: CORE_HUB / Router ───────────────────────────────────────
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HorizonsColors.TileRouter.copy(alpha = 0.5f),
                            HorizonsColors.TileRouter.copy(alpha = 0.15f),
                            HorizonsColors.TileRouter.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension * 0.6f,
                    ),
                )
            }
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onTileClick(Panel.Router) },
                color = HorizonsColors.TileRouter.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.dp, HorizonsColors.TileRouter.copy(alpha = 0.4f)),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "⬡",
                        fontSize = 32.sp,
                        color = HorizonsColors.TileRouter,
                    )
                }
            }
        }
        // Labels outside the Box so they don't get clipped
        Text(
            "// CORE_HUB",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = HorizonsColors.TileRouter.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "ROUTER",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = HorizonsColors.TileRouter,
        )
        Text(
            "/ route",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = HorizonsColors.TileRouter.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(20.dp))

        // ── Bottom row: Artifacts · Terminal · Settings ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TileCard(
                name = "ARTIFACTS",
                slug = "/ logs_skills",
                subtitle = "Logs · Prompts ·\nWarm models",
                color = HorizonsColors.TileArtifacts,
                icon = "▤",
                cmdHint = "$ ls artifacts/",
                onClick = { onTileClick(Panel.Artifacts) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "TERMINAL",
                slug = "/ shell",
                subtitle = "Matrix shell ·\nGreen on black",
                color = HorizonsColors.TileTerminal,
                icon = ">_",
                cmdHint = "$ _",
                onClick = { onTileClick(Panel.Terminal) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "SETTINGS",
                slug = "/ config",
                subtitle = "Config · Matrix\ntheme overlay",
                color = HorizonsColors.TileSettings,
                icon = "⚙",
                cmdHint = "$ config --open",
                onClick = { onTileClick(Panel.Settings) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))

        // ── System Status Bar ────────────────────────────────────────────────
        Surface(
            color = HorizonsColors.Surface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp),
            ) {
                Text(
                    "// SYSTEM_STATUS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatusDot("ASR", HorizonsColors.StatusAsr, active = true)
                    StatusDot("LLM", HorizonsColors.StatusLlm, active = npuReady)
                    StatusDot("TTS", HorizonsColors.StatusTts, active = true)
                    StatusDot("MLLM", HorizonsColors.StatusMllm, active = false)
                    StatusDot("VAG", HorizonsColors.StatusVag, active = false)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Input bar ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onTileClick(Panel.Chat) },
            shape = RoundedCornerShape(24.dp),
            color = HorizonsColors.Surface,
            border = BorderStroke(1.dp, HorizonsColors.PrimaryTeal.copy(alpha = 0.2f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "⊕",
                    fontSize = 16.sp,
                    color = HorizonsColors.PrimaryTeal,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "tap_or_hold  ask //",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "↑",
                    fontSize = 18.sp,
                    color = HorizonsColors.PrimaryTeal,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TileCard(
    name: String,
    slug: String,
    subtitle: String,
    color: Color,
    icon: String,
    cmdHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Icon + name at top
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(icon, fontSize = 20.sp, color = color)
                Spacer(Modifier.height(4.dp))
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                Text(
                    subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = color.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                )
            }

            // Command hint + gear at bottom
            HorizontalDivider(color = color.copy(alpha = 0.12f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cmdHint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = color.copy(alpha = 0.5f),
                )
                Text(
                    "⚙",
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun StatusDot(
    label: String,
    color: Color,
    active: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.15f)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = if (active) color else color.copy(alpha = 0.25f),
        )
    }
}
