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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.Panel
import com.horizons.ui.theme.HorizonsColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeGrid(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()

    val npuReady = backendStatus.startsWith("Hexagon HTP") || backendStatus.startsWith("Adreno 830")

    val stars = remember { generateStars(120) }
    var goatTaps by remember { mutableIntStateOf(0) }
    var showGoat by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Astral chart background ─────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAstralBackground(stars)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Banner ──────────────────────────────────────────────────────
            Text(
                "MO)u14R_11(",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = HorizonsColors.PrimaryTeal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        goatTaps++
                        if (goatTaps >= 7) {
                            showGoat = true
                            goatTaps = 0
                        }
                    },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "*PIONEER_TECH  ·  (NEXT-GEN CERTIFIED)",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = HorizonsColors.PrimaryTeal.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Text(
                "HORIZONS // V4",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = HorizonsColors.PrimaryTeal.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                color = HorizonsColors.PrimaryTeal.copy(alpha = 0.15f),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))

            // ── Top row: Horizons · Monitor · Chat ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TileCard(
                    name = "HORIZONS",
                    slug = "/ home",
                    subtitle = "Home node · System\noverview",
                    color = HorizonsColors.TileHorizons,
                    tileType = TileType.HORIZONS,
                    cmdHint = "$ home --status",
                    onClick = { onTileClick(Panel.Horizons) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "MONITOR",
                    slug = "/ console",
                    subtitle = "Library · Browse ·\nCompatibility",
                    color = HorizonsColors.TileMonitor,
                    tileType = TileType.MONITOR,
                    cmdHint = "$ console",
                    onClick = { onTileClick(Panel.Monitor) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "CHAT",
                    slug = "/ interface",
                    subtitle = "Full AI interface ·\nArtifacts · History",
                    color = HorizonsColors.TileChat,
                    tileType = TileType.CHAT,
                    cmdHint = "$ chat --open",
                    onClick = { onTileClick(Panel.Chat) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Center: CORE_HUB / Router ───────────────────────────────────
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCoreHubCrystal()
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onTileClick(Panel.Router) },
                    contentAlignment = Alignment.Center,
                ) {}
            }
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

            // ── Bottom row: Settings (~4:30 SE) · Terminal (6:00 S) · Archives (~7:30 SW) ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TileCard(
                    name = "SETTINGS",
                    slug = "/ vault",
                    subtitle = "Deposits · Keys ·\nImports · Vault",
                    color = HorizonsColors.TileSettings,
                    tileType = TileType.SETTINGS,
                    cmdHint = "$ vault --open",
                    onClick = { onTileClick(Panel.Settings) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "TERMINAL",
                    slug = "/ garage",
                    subtitle = "Mod garage ·\nScripts · CLI",
                    color = HorizonsColors.TileTerminal,
                    tileType = TileType.TERMINAL,
                    cmdHint = "$ _",
                    onClick = { onTileClick(Panel.Terminal) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "ARCHIVES",
                    slug = "/ archive",
                    subtitle = "Artifacts · Logs ·\nSaved configs",
                    color = HorizonsColors.TileArtifacts,
                    tileType = TileType.ARTIFACTS,
                    cmdHint = "$ ls archive/",
                    onClick = { onTileClick(Panel.Artifacts) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))

            // ── System Status Bar ───────────────────────────────────────────
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

            // ── Input bar ───────────────────────────────────────────────────
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
}

// ── Astral chart background ─────────────────────────────────────────────────

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun generateStars(count: Int): List<Star> {
    val rng = java.util.Random(42)
    return List(count) {
        Star(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 0.5f + rng.nextFloat() * 1.5f,
            alpha = 0.15f + rng.nextFloat() * 0.7f,
        )
    }
}

private fun DrawScope.drawAstralBackground(stars: List<Star>) {
    drawRect(Color(0xFF222C34))

    val cx = size.width / 2f
    val cy = size.height * 0.42f

    stars.forEach { star ->
        val isTeal = star.alpha > 0.5f
        val color = if (isTeal) Color(0xFF2DD4D9).copy(alpha = star.alpha * 0.6f)
        else Color.White.copy(alpha = star.alpha * 0.5f)
        drawCircle(
            color = color,
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }

    // Orbital rings around center hub
    val ringColor = Color(0xFF2DD4D9).copy(alpha = 0.04f)
    for (i in 1..5) {
        val r = 60f + i * 55f
        drawCircle(
            color = ringColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 0.8f),
        )
    }

    // Telemetry / chart lines — faint radial spokes
    val spokeColor = Color(0xFF2DD4D9).copy(alpha = 0.025f)
    for (angle in 0 until 360 step 30) {
        val rad = angle * PI.toFloat() / 180f
        val len = 320f
        drawLine(
            color = spokeColor,
            start = Offset(cx, cy),
            end = Offset(cx + cos(rad) * len, cy + sin(rad) * len),
            strokeWidth = 0.6f,
        )
    }

    // Small chart circles at intersections
    val dotColor = Color(0xFF2DD4D9).copy(alpha = 0.06f)
    for (ring in 2..4) {
        val r = 60f + ring * 55f
        for (angle in listOf(0, 60, 120, 180, 240, 300)) {
            val rad = angle * PI.toFloat() / 180f
            drawCircle(
                color = dotColor,
                radius = 2.5f,
                center = Offset(cx + cos(rad) * r, cy + sin(rad) * r),
            )
        }
    }

    // ── Plasma tube conduits ────────────────────────────────────────────
    // Approximate tile center positions relative to screen
    val topRowY = size.height * 0.2f
    val botRowY = size.height * 0.62f
    val leftX = size.width * 0.17f
    val midX = cx
    val rightX = size.width * 0.83f

    data class Conduit(val from: Offset, val color: Color)
    val conduits = listOf(
        Conduit(Offset(leftX, topRowY), Color(0xFF2DD4D9)),     // Horizons
        Conduit(Offset(midX, topRowY), Color(0xFF2DD4D9)),      // Monitor
        Conduit(Offset(rightX, topRowY), Color(0xFF4FE7EC)),    // Chat
        Conduit(Offset(leftX, botRowY), Color(0xFFFF5577)),     // Settings
        Conduit(Offset(midX, botRowY), Color(0xFF00FF41)),      // Terminal
        Conduit(Offset(rightX, botRowY), Color(0xFFE8A838)),    // Archives
    )

    conduits.forEach { conduit ->
        val hub = Offset(cx, cy)
        // Outer glow (wide, faint)
        drawLine(
            color = conduit.color.copy(alpha = 0.06f),
            start = conduit.from,
            end = hub,
            strokeWidth = 12f,
            cap = StrokeCap.Round,
        )
        // Mid glow
        drawLine(
            color = conduit.color.copy(alpha = 0.10f),
            start = conduit.from,
            end = hub,
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
        // Core bright line
        drawLine(
            color = conduit.color.copy(alpha = 0.25f),
            start = conduit.from,
            end = hub,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
    }
}

// ── 3D Hexagonal crystal ────────────────────────────────────────────────────

private fun DrawScope.drawCoreHubCrystal() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseR = size.minDimension * 0.34f

    // Concentric elliptical ring base (3D platform)
    for (i in 4 downTo 0) {
        val ringR = baseR + i * 8f
        val ringAlpha = 0.06f + (4 - i) * 0.02f
        drawOval(
            color = Color(0xFFAA77FF).copy(alpha = ringAlpha),
            topLeft = Offset(cx - ringR, cy - ringR * 0.35f + 8f),
            size = Size(ringR * 2f, ringR * 0.7f),
            style = Stroke(width = 1.2f),
        )
    }

    // Radial glow layers
    for (layer in 3 downTo 0) {
        val glowR = baseR * (1.4f + layer * 0.3f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFAA77FF).copy(alpha = 0.12f - layer * 0.025f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            center = Offset(cx, cy),
            radius = glowR,
        )
    }

    // Hexagon base (bottom face, viewed at 45°)
    val hexBase = buildHexPath(cx, cy + 4f, baseR * 0.85f, 0.4f)
    drawPath(
        path = hexBase,
        color = Color(0xFFAA77FF).copy(alpha = 0.15f),
    )
    drawPath(
        path = hexBase,
        color = Color(0xFFAA77FF).copy(alpha = 0.35f),
        style = Stroke(width = 1.5f),
    )

    // Top facets — broad top face (30° pitch, not sharp point)
    val topY = cy - baseR * 0.35f
    val peakY = cy - baseR * 0.55f
    val hexR = baseR * 0.85f

    val topFacet = Path().apply {
        // Left front facet
        moveTo(cx, peakY)
        lineTo(cx - hexR * 0.6f, topY)
        lineTo(cx - hexR * 0.85f, cy - baseR * 0.05f)
        lineTo(cx, cy + 2f)
        close()
    }
    drawPath(topFacet, Color(0xFFAA77FF).copy(alpha = 0.18f))
    drawPath(topFacet, Color(0xFFAA77FF).copy(alpha = 0.3f), style = Stroke(width = 1f))

    val rightFacet = Path().apply {
        moveTo(cx, peakY)
        lineTo(cx + hexR * 0.6f, topY)
        lineTo(cx + hexR * 0.85f, cy - baseR * 0.05f)
        lineTo(cx, cy + 2f)
        close()
    }
    drawPath(rightFacet, Color(0xFFAA77FF).copy(alpha = 0.25f))
    drawPath(rightFacet, Color(0xFFAA77FF).copy(alpha = 0.3f), style = Stroke(width = 1f))

    // Top face polygon
    val topFace = Path().apply {
        moveTo(cx, peakY)
        lineTo(cx - hexR * 0.6f, topY)
        lineTo(cx - hexR * 0.3f, topY + 4f)
        lineTo(cx, topY - 2f)
        lineTo(cx + hexR * 0.3f, topY + 4f)
        lineTo(cx + hexR * 0.6f, topY)
        close()
    }
    drawPath(topFace, Color(0xFFAA77FF).copy(alpha = 0.22f))
    drawPath(topFace, Color(0xFFBB99FF).copy(alpha = 0.4f), style = Stroke(width = 1f))

    // Inner glow core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFCC99FF).copy(alpha = 0.5f),
                Color(0xFFAA77FF).copy(alpha = 0.15f),
                Color.Transparent,
            ),
            center = Offset(cx, cy - baseR * 0.1f),
            radius = baseR * 0.4f,
        ),
        center = Offset(cx, cy - baseR * 0.1f),
        radius = baseR * 0.4f,
    )

    // Faint circular border
    drawCircle(
        color = Color(0xFFAA77FF).copy(alpha = 0.08f),
        radius = baseR * 1.15f,
        center = Offset(cx, cy),
        style = Stroke(width = 0.8f),
    )
}

private fun buildHexPath(cx: Float, cy: Float, radius: Float, yScale: Float): Path {
    return Path().apply {
        for (i in 0 until 6) {
            val angle = (i * 60 - 30) * PI.toFloat() / 180f
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius * yScale
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

// ── Canvas-drawn tile icons ─────────────────────────────────────────────────

private enum class TileType { HORIZONS, MONITOR, CHAT, ARTIFACTS, TERMINAL, SETTINGS }

private fun DrawScope.drawTileIcon(type: TileType, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f * 0.8f

    when (type) {
        TileType.HORIZONS -> {
            // Horizon line (blue) + green arc (sunrise) + amber dot (sun)
            val lineY = cy + r * 0.2f
            drawLine(
                color = Color(0xFF40C4FF),
                start = Offset(cx - r, lineY),
                end = Offset(cx + r, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            drawArc(
                color = Color(0xFF00E676),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.6f, lineY - r * 0.6f),
                size = Size(r * 1.2f, r * 0.9f),
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFFE8A838),
                radius = r * 0.15f,
                center = Offset(cx, lineY - r * 0.38f),
            )
        }
        TileType.MONITOR -> {
            // Compass / target icon
            drawCircle(
                color = color,
                radius = r * 0.7f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f),
            )
            drawCircle(
                color = color,
                radius = r * 0.35f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f),
            )
            drawCircle(
                color = color,
                radius = r * 0.1f,
                center = Offset(cx, cy),
            )
            // Cross hairs
            val tickLen = r * 0.2f
            drawLine(color, Offset(cx, cy - r * 0.7f), Offset(cx, cy - r * 0.7f - tickLen), 1.5f)
            drawLine(color, Offset(cx, cy + r * 0.7f), Offset(cx, cy + r * 0.7f + tickLen), 1.5f)
            drawLine(color, Offset(cx - r * 0.7f, cy), Offset(cx - r * 0.7f - tickLen, cy), 1.5f)
            drawLine(color, Offset(cx + r * 0.7f, cy), Offset(cx + r * 0.7f + tickLen, cy), 1.5f)
        }
        TileType.CHAT -> {
            // Speech bubble with tail
            val bubbleW = r * 1.6f
            val bubbleH = r * 1.1f
            val bubbleTop = cy - bubbleH * 0.55f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - bubbleW / 2f, bubbleTop),
                size = Size(bubbleW, bubbleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.3f),
                style = Stroke(width = 2f),
            )
            val tail = Path().apply {
                moveTo(cx - r * 0.1f, bubbleTop + bubbleH)
                lineTo(cx - r * 0.35f, bubbleTop + bubbleH + r * 0.35f)
                lineTo(cx + r * 0.15f, bubbleTop + bubbleH)
            }
            drawPath(tail, color, style = Stroke(width = 2f, cap = StrokeCap.Round))
        }
        TileType.ARTIFACTS -> {
            // Stacked documents / clipboard
            val docW = r * 1.2f
            val docH = r * 1.4f
            // Back page
            drawRoundRect(
                color = color.copy(alpha = 0.3f),
                topLeft = Offset(cx - docW / 2f + 4f, cy - docH / 2f - 3f),
                size = Size(docW, docH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.1f),
                style = Stroke(width = 1.5f),
            )
            // Front page
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - docW / 2f - 2f, cy - docH / 2f + 3f),
                size = Size(docW, docH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.1f),
                style = Stroke(width = 2f),
            )
            // Lines on front page
            val lineStartX = cx - docW / 2f + 6f
            val lineEndX = cx + docW / 2f - 10f
            for (i in 0..2) {
                val ly = cy - docH / 2f + 16f + i * 8f
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(lineStartX, ly),
                    end = Offset(lineEndX, ly),
                    strokeWidth = 1.2f,
                )
            }
        }
        TileType.TERMINAL -> {
            // Terminal window with >_ prompt
            val winW = r * 1.6f
            val winH = r * 1.2f
            val winTop = cy - winH / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - winW / 2f, winTop),
                size = Size(winW, winH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.15f),
                style = Stroke(width = 2f),
            )
            // Title bar dots
            val dotY = winTop + 5f
            for (i in 0..2) {
                drawCircle(
                    color = color.copy(alpha = 0.5f),
                    radius = 1.8f,
                    center = Offset(cx - winW / 2f + 8f + i * 6f, dotY),
                )
            }
            // Divider under title bar
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(cx - winW / 2f, winTop + 10f),
                end = Offset(cx + winW / 2f, winTop + 10f),
                strokeWidth = 0.8f,
            )
            // >_ cursor
            val promptY = cy + 2f
            drawLine(
                color = color,
                start = Offset(cx - r * 0.3f, promptY - 4f),
                end = Offset(cx - r * 0.05f, promptY + 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(cx + r * 0.05f, promptY + 2f),
                end = Offset(cx + r * 0.3f, promptY + 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        TileType.SETTINGS -> {
            // Gear with lightning bolt
            val gearR = r * 0.6f
            val teeth = 8
            drawCircle(
                color = color,
                radius = gearR * 0.55f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f),
            )
            for (i in 0 until teeth) {
                val angle = (i * 360f / teeth) * PI.toFloat() / 180f
                val innerR = gearR * 0.7f
                val outerR = gearR * 1.0f
                drawLine(
                    color = color,
                    start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                    end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
            // Lightning bolt
            val bolt = Path().apply {
                moveTo(cx + 1f, cy - gearR * 0.35f)
                lineTo(cx - 3f, cy + 1f)
                lineTo(cx + 1f, cy + 1f)
                lineTo(cx - 1f, cy + gearR * 0.35f)
            }
            drawPath(bolt, color, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
    }
}

// ── Tile card ───────────────────────────────────────────────────────────────

@Composable
private fun TileCard(
    name: String,
    slug: String,
    subtitle: String,
    color: Color,
    tileType: TileType,
    cmdHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                // Edge glow
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.width * 0.8f,
                )
                drawRect(glowBrush)
            },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Canvas icon protruding above card
                Canvas(
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = (-6).dp),
                ) {
                    drawTileIcon(tileType, color)
                }
                Spacer(Modifier.height(2.dp))
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

// ── Status dot ──────────────────────────────────────────────────────────────

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
