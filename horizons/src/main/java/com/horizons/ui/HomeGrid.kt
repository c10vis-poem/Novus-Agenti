package com.horizons.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
        Spacer(Modifier.height(24.dp))

        // ── Banner ───────────────────────────────────────────────────────────
        Text(
            "MO)u14R_11(",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = HorizonsColors.PrimaryTeal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "*PIONEER_TECH  ·  (NEXT-GEN CERTIFIED)  ·  HORIZONS // V4",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ── Top row: Horizons · Monitor · Chat ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TileCard(
                name = "HORIZONS",
                slug = "/ home",
                color = HorizonsColors.TileHorizons,
                icon = "⌂",
                onClick = { onTileClick(Panel.Horizons) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "MONITOR",
                slug = "/ cognito",
                color = HorizonsColors.TileMonitor,
                icon = "◎",
                onClick = { onTileClick(Panel.Monitor) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "CHAT",
                slug = "/ interface",
                color = HorizonsColors.TileChat,
                icon = "◬",
                onClick = { onTileClick(Panel.Chat) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Center: CORE_HUB / Router ───────────────────────────────────────
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Glow background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HorizonsColors.TileRouter.copy(alpha = 0.4f),
                            HorizonsColors.TileRouter.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension / 2,
                    ),
                )
            }
            // Hub hexagon (simplified as circle for now)
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onTileClick(Panel.Router) },
                color = HorizonsColors.TileRouter.copy(alpha = 0.15f),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "⬡",
                        fontSize = 28.sp,
                        color = HorizonsColors.TileRouter,
                    )
                }
            }
            // Label below
            Text(
                "ROUTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = HorizonsColors.TileRouter,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp),
            )
            Text(
                "/ route",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = HorizonsColors.TileRouter.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 24.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Bottom row: Artifacts · Terminal · Settings ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TileCard(
                name = "ARTIFACTS",
                slug = "/ logs_skills",
                color = HorizonsColors.TileArtifacts,
                icon = "▤",
                onClick = { onTileClick(Panel.Artifacts) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "TERMINAL",
                slug = "/ shell",
                color = HorizonsColors.TileTerminal,
                icon = ">_",
                onClick = { onTileClick(Panel.Terminal) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TileCard(
                name = "SETTINGS",
                slug = "/ config",
                color = HorizonsColors.TileSettings,
                icon = "⚙",
                onClick = { onTileClick(Panel.Settings) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))

        // ── System Status Bar ────────────────────────────────────────────────
        Text(
            "// SYSTEM_STATUS",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatusDot("ASR", HorizonsColors.StatusAsr, active = true)
            StatusDot("LLM", HorizonsColors.StatusLlm, active = npuReady)
            StatusDot("TTS", HorizonsColors.StatusTts, active = true)
            StatusDot("MLLM", HorizonsColors.StatusMllm, active = false)
            StatusDot("VAG", HorizonsColors.StatusVag, active = false)
        }

        Spacer(Modifier.height(8.dp))

        // ── Input bar ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onTileClick(Panel.Chat) },
            shape = RoundedCornerShape(24.dp),
            color = HorizonsColors.Surface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "↑",
                    fontSize = 18.sp,
                    color = HorizonsColors.PrimaryTeal,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TileCard(
    name: String,
    slug: String,
    color: Color,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                icon,
                fontSize = 22.sp,
                color = color,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = color,
                textAlign = TextAlign.Center,
            )
            Text(
                slug,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = color.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
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
                .size(14.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.2f)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = if (active) color else color.copy(alpha = 0.3f),
        )
    }
}
