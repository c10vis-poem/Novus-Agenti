package com.horizons.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

    val stars = remember { generateStars(170) }
    var goatTaps by remember { mutableIntStateOf(0) }
    var showGoat by remember { mutableStateOf(false) }
    var goatReason by remember { mutableStateOf<String?>(null) }

    // Goat watchdog — if the runtime comes back broken, the goat delivers the news
    LaunchedEffect(backendStatus) {
        val bad = listOf("error", "fail", "crash", "dead", "unavailable")
        if (bad.any { backendStatus.contains(it, ignoreCase = true) }) {
            goatReason = backendStatus
            showGoat = true
            playGoatBleat()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Astral chart background ─────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAstralBackground(stars)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Banner — shell heredoc aesthetic ────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        goatTaps++
                        if (goatTaps >= 7) {
                            goatReason = null
                            showGoat = true
                            goatTaps = 0
                            playGoatBleat()
                        }
                    },
            ) {
                Text(
                    "cat    << 'EOF'",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                )
                Text(
                    "MØ[)u14R_  11(",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    letterSpacing = 1.sp,
                    color = HorizonsColors.PrimaryTeal,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "*Pioneer_Tech,",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    color = HorizonsColors.PrimaryTeal,
                )
                Text(
                    "  (Next-Gen Certified)",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp,
                    color = HorizonsColors.PrimaryTeal,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "EOF",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                    )
                    Text(
                        "v1.0",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                    )
                }
            }

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
                    slug = "/ about",
                    subtitle = "credits",
                    color = HorizonsColors.TileHorizons,
                    tileType = TileType.HORIZONS,
                    cmdHint = "\$_.home",
                    onClick = { onTileClick(Panel.Horizons) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "MONITOR",
                    slug = "/ cognito",
                    subtitle = "library",
                    color = HorizonsColors.TileMonitor,
                    tileType = TileType.MONITOR,
                    cmdHint = "\$_browser",
                    onClick = { onTileClick(Panel.Monitor) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "CHAT",
                    slug = "/ interface",
                    subtitle = "tools",
                    color = HorizonsColors.TileChat,
                    tileType = TileType.CHAT,
                    cmdHint = "\$_model",
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
                fontSize = 20.sp,
                color = Color.White,
            )
            Text(
                "\$_Statio",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = HorizonsColors.TileRouter.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(20.dp))

            // ── Bottom row: Settings (~4:30 SE) · Terminal (6:00 S) · Archives (~7:30 SW) ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TileCard(
                    name = "SETTINGS",
                    slug = "/ config",
                    subtitle = "vault",
                    color = HorizonsColors.TileSettings,
                    tileType = TileType.SETTINGS,
                    cmdHint = "\$_utils",
                    onClick = { onTileClick(Panel.Settings) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "TERMINAL",
                    slug = "/ shell",
                    subtitle = "commands",
                    color = HorizonsColors.TileTerminal,
                    tileType = TileType.TERMINAL,
                    cmdHint = "\$_bash",
                    onClick = { onTileClick(Panel.Terminal) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "ARCHIVES",
                    slug = "/ logs",
                    subtitle = "artifacts",
                    color = HorizonsColors.TileArtifacts,
                    tileType = TileType.ARTIFACTS,
                    cmdHint = "\$_files",
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

        // ── Goat Easter egg overlay ─────────────────────────────────────────
        if (showGoat) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.94f))
                    .clickable { showGoat = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("🐐", fontSize = 96.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (goatReason != null) "// GOAT_SAYS_NO" else "// GOAT_UNLOCKED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (goatReason != null) HorizonsColors.TileSettings
                        else HorizonsColors.PrimaryTeal,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (goatReason != null) {
                        Text(
                            "runtime came back wrong:",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = HorizonsColors.TileSettings.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            goatReason ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            "*Pioneer_Tech approved",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "(Next-Gen Certified)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "[ tap anywhere to dismiss ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.25f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ── Goat bleat — synthesized, no asset needed ───────────────────────────────

private fun playGoatBleat() {
    Thread {
        try {
            val sr = 22050
            val durSec = 0.7f
            val n = (sr * durSec).toInt()
            val buf = ShortArray(n)
            var phase = 0f
            for (i in 0 until n) {
                val t = i.toFloat() / sr
                // Vibrato around 260Hz gives the "meh-eh-eh" warble
                val freq = 260f + 45f * sin(2f * PI.toFloat() * 9f * t)
                phase += 2f * PI.toFloat() * freq / sr
                val saw = 2f * ((phase / (2f * PI.toFloat())) % 1f) - 1f
                val tremolo = 0.55f + 0.45f * sin(2f * PI.toFloat() * 11f * t)
                val envelope = (1f - t / durSec).coerceIn(0f, 1f) *
                    (t * 40f).coerceAtMost(1f)
                buf[i] = (saw * tremolo * envelope * 8500f).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(n * 2)
                .build()
            track.write(buf, 0, n)
            track.play()
            Thread.sleep((durSec * 1000).toLong() + 150)
            track.release()
        } catch (_: Exception) {
            // Sound is garnish — never let it crash the UI
        }
    }.start()
}

// ── Astral chart background ─────────────────────────────────────────────────

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun generateStars(count: Int): List<Star> {
    val rng = java.util.Random(42)
    return List(count) {
        Star(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 0.6f + rng.nextFloat() * 1.9f,
            alpha = 0.25f + rng.nextFloat() * 0.7f,
        )
    }
}

private fun DrawScope.drawAstralBackground(stars: List<Star>) {
    // Obsidian base — deep black with only a faint bluish tint; near-pure
    // black at the darkest aspects (top faint blue-lift, bottom near #010203)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF070B12), Color(0xFF04070C), Color(0xFF010203)),
        ),
    )

    // Obsidian facets — large angular glass shards, barely visible
    val facetRng = java.util.Random(137)
    for (i in 0 until 6) {
        val fx = facetRng.nextFloat() * size.width
        val fy = facetRng.nextFloat() * size.height
        val fw = (0.25f + facetRng.nextFloat() * 0.35f) * size.width
        val fh = (0.15f + facetRng.nextFloat() * 0.25f) * size.height
        val skew = (facetRng.nextFloat() - 0.5f) * fw * 0.6f
        val facet = Path().apply {
            moveTo(fx, fy)
            lineTo(fx + fw, fy + skew * 0.3f)
            lineTo(fx + fw * 0.75f + skew, fy + fh)
            lineTo(fx - fw * 0.1f + skew * 0.5f, fy + fh * 0.85f)
            close()
        }
        drawPath(
            facet,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2A3640).copy(alpha = 0.20f),
                    Color(0xFF0D1216).copy(alpha = 0.12f),
                ),
                start = Offset(fx, fy),
                end = Offset(fx + fw, fy + fh),
            ),
        )
        // Specular glint along the facet's top edge — light catching glass
        drawLine(
            color = Color(0xFF9FCAD6).copy(alpha = 0.06f + facetRng.nextFloat() * 0.05f),
            start = Offset(fx, fy),
            end = Offset(fx + fw, fy + skew * 0.3f),
            strokeWidth = 0.8f,
        )
    }

    val cx = size.width / 2f
    val cy = size.height * 0.42f

    stars.forEach { star ->
        val isTeal = star.alpha > 0.5f
        val bright = star.alpha > 0.62f
        val color = if (isTeal) Color(0xFF2DD4D9).copy(alpha = star.alpha * 0.88f)
        else Color.White.copy(alpha = star.alpha * 0.80f)
        val center = Offset(star.x * size.width, star.y * size.height)
        // Soft halo on the brighter stars so they read/pop against the black
        if (bright) {
            drawCircle(
                color = color.copy(alpha = star.alpha * 0.22f),
                radius = star.radius * 3.4f,
                center = center,
            )
        }
        drawCircle(
            color = color,
            radius = star.radius,
            center = center,
        )
    }

    // Orbital rings around center hub
    val ringColor = Color(0xFF2DD4D9).copy(alpha = 0.10f)
    for (i in 1..5) {
        val r = 60f + i * 55f
        drawCircle(
            color = ringColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.0f),
        )
    }

    // Telemetry / chart lines — faint radial spokes
    val spokeColor = Color(0xFF2DD4D9).copy(alpha = 0.06f)
    for (angle in 0 until 360 step 30) {
        val rad = angle * PI.toFloat() / 180f
        val len = 320f
        drawLine(
            color = spokeColor,
            start = Offset(cx, cy),
            end = Offset(cx + cos(rad) * len, cy + sin(rad) * len),
            strokeWidth = 0.8f,
        )
    }

    // Small chart circles at intersections
    val dotColor = Color(0xFF2DD4D9).copy(alpha = 0.14f)
    for (ring in 2..4) {
        val r = 60f + ring * 55f
        for (angle in listOf(0, 60, 120, 180, 240, 300)) {
            val rad = angle * PI.toFloat() / 180f
            drawCircle(
                color = dotColor,
                radius = 3f,
                center = Offset(cx + cos(rad) * r, cy + sin(rad) * r),
            )
        }
    }

    // ── Plasma tube conduits ────────────────────────────────────────────
    val topRowY = size.height * 0.225f
    val botRowY = size.height * 0.63f
    val leftX   = size.width  * 0.16f
    val midX    = cx
    val rightX  = size.width  * 0.84f
    val hub     = Offset(cx, size.height * 0.42f)

    data class Conduit(val from: Offset, val color: Color)
    val conduits = listOf(
        Conduit(Offset(leftX,  topRowY), Color(0xFF2DD4D9)),
        Conduit(Offset(midX,   topRowY), Color(0xFF2DD4D9)),
        Conduit(Offset(rightX, topRowY), Color(0xFF4FE7EC)),
        Conduit(Offset(leftX,  botRowY), Color(0xFFFF5577)),
        Conduit(Offset(midX,   botRowY), Color(0xFF00FF41)),
        Conduit(Offset(rightX, botRowY), Color(0xFFE8A838)),
    )

    conduits.forEach { (from, c) ->
        // Plasma tube — 4 glow layers
        drawLine(c.copy(alpha = 0.04f), from, hub, 22f, StrokeCap.Round)
        drawLine(c.copy(alpha = 0.08f), from, hub, 10f, StrokeCap.Round)
        drawLine(c.copy(alpha = 0.16f), from, hub,  4f, StrokeCap.Round)
        drawLine(c.copy(alpha = 0.32f), from, hub, 1.4f, StrokeCap.Round)

        // Plasma nodes along the tube
        val steps = 7
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            val nodeAlpha = 0.20f + sin(t * PI.toFloat()) * 0.30f
            val nodePos = Offset(from.x + (hub.x - from.x) * t, from.y + (hub.y - from.y) * t)
            drawCircle(c.copy(alpha = nodeAlpha * 0.7f), 3.5f, nodePos)
            drawCircle(c.copy(alpha = nodeAlpha * 0.25f), 6.5f, nodePos, style = Stroke(0.8f))
        }

        // Anchor dot at tile end
        drawCircle(c.copy(alpha = 0.45f), 3.5f, from)
        drawCircle(c.copy(alpha = 0.15f), 7f,   from, style = Stroke(0.8f))
    }
}

// ── 3D Hexagonal crystal ────────────────────────────────────────────────────

private fun DrawScope.drawCoreHubCrystal() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val W   = size.minDimension * 0.13f   // half-width — shrunk (was oversized "wizard hat")
    val H   = size.minDimension * 0.23f   // body half-height
    val SD  = size.minDimension * 0.075f  // side-face depth (45° perspective)
    val capH = W * 0.50f                   // 30° bevel — moderate, not pointy
    val ox  = cx - SD * 0.2f              // slight left offset for "off-center" look

    // Ambient glow
    for (layer in 3 downTo 0) {
        val glowR = W * (2.8f + layer * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFAA77FF).copy(alpha = 0.10f - layer * 0.02f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            center = Offset(cx, cy),
            radius = glowR,
        )
    }

    val bodyTop  = cy - H * 0.30f
    val bodyBot  = cy + H * 0.52f
    val peakY    = bodyTop - capH
    val botTipY  = bodyBot + capH * 0.55f

    // Front face of prism body
    val frontFace = Path().apply {
        moveTo(ox - W, bodyTop)
        lineTo(ox + W, bodyTop)
        lineTo(ox + W, bodyBot)
        lineTo(ox - W, bodyBot)
        close()
    }
    drawPath(frontFace, Color(0xFF8855CC).copy(alpha = 0.24f))
    drawPath(frontFace, Color(0xFFAA77FF).copy(alpha = 0.50f), style = Stroke(width = 1.5f))

    // Right side face (foreshortened at 45°)
    val sideFace = Path().apply {
        moveTo(ox + W,        bodyTop)
        lineTo(ox + W + SD,   bodyTop - SD * 0.45f)
        lineTo(ox + W + SD,   bodyBot - SD * 0.45f)
        lineTo(ox + W,        bodyBot)
        close()
    }
    drawPath(sideFace, Color(0xFFCC99FF).copy(alpha = 0.14f))
    drawPath(sideFace, Color(0xFFAA77FF).copy(alpha = 0.38f), style = Stroke(width = 1f))

    // Top cap — front facet (broad, 30°)
    val capFront = Path().apply {
        moveTo(ox - W,        bodyTop)
        lineTo(ox + W,        bodyTop)
        lineTo(ox + W * 0.25f, peakY)
        lineTo(ox - W * 0.25f, peakY)
        close()
    }
    drawPath(capFront, Color(0xFFBB99FF).copy(alpha = 0.32f))
    drawPath(capFront, Color(0xFFCC99FF).copy(alpha = 0.60f), style = Stroke(width = 1.2f))

    // Top cap — right side facet
    val capSide = Path().apply {
        moveTo(ox + W,                bodyTop)
        lineTo(ox + W + SD,           bodyTop - SD * 0.45f)
        lineTo(ox + W * 0.25f + SD * 0.75f, peakY - SD * 0.22f)
        lineTo(ox + W * 0.25f,        peakY)
        close()
    }
    drawPath(capSide, Color(0xFFDD99FF).copy(alpha = 0.20f))
    drawPath(capSide, Color(0xFFBB88FF).copy(alpha = 0.48f), style = Stroke(width = 1f))

    // Bottom taper (crystal termination)
    val botTaper = Path().apply {
        moveTo(ox - W, bodyBot)
        lineTo(ox + W, bodyBot)
        lineTo(ox + W + SD, bodyBot - SD * 0.45f)
        lineTo(ox + W * 0.1f + SD * 0.5f, botTipY - SD * 0.15f)
        lineTo(ox, botTipY)
        lineTo(ox - W * 0.1f, botTipY)
        lineTo(ox - W, bodyBot)
        close()
    }
    drawPath(botTaper, Color(0xFF7744AA).copy(alpha = 0.20f))
    drawPath(botTaper, Color(0xFFAA77FF).copy(alpha = 0.32f), style = Stroke(width = 1f))

    // Inner glow core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFEECCFF).copy(alpha = 0.58f),
                Color(0xFFAA77FF).copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = Offset(ox, cy - H * 0.05f),
            radius = W * 0.85f,
        ),
        center = Offset(ox, cy - H * 0.05f),
        radius = W * 0.85f,
    )

    // Specular highlight streak (upper-left of front face)
    drawLine(
        color = Color.White.copy(alpha = 0.38f),
        start = Offset(ox - W * 0.55f, bodyTop + 2f),
        end = Offset(ox - W * 0.20f, bodyTop - capH * 0.45f),
        strokeWidth = 1.3f,
        cap = StrokeCap.Round,
    )
}

// ── Canvas-drawn tile icons ─────────────────────────────────────────────────

private enum class TileType { HORIZONS, MONITOR, CHAT, ARTIFACTS, TERMINAL, SETTINGS }

private fun DrawScope.drawTileIcon(type: TileType, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f * 0.8f

    when (type) {
        TileType.HORIZONS -> {
            // Amber sun with rays + blue horizon line + pale pinkish-purple arch
            val lineY = cy + r * 0.22f
            val sunY = lineY - r * 0.38f
            val sunR = r * 0.17f
            // Pale pinkish-purple arch (sky dome)
            drawArc(
                color = Color(0xFFCC99CC),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.78f, lineY - r * 0.78f),
                size = Size(r * 1.56f, r * 1.0f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round),
            )
            // Blue horizon line
            drawLine(
                color = Color(0xFF40C4FF),
                start = Offset(cx - r, lineY),
                end = Offset(cx + r, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            // Amber sun disc
            drawCircle(color = Color(0xFFF5C518), radius = sunR, center = Offset(cx, sunY))
            // Sun rays (8)
            for (i in 0 until 8) {
                val ang = i * 45f * PI.toFloat() / 180f
                drawLine(
                    color = Color(0xFFF5C518).copy(alpha = 0.68f),
                    start = Offset(cx + cos(ang) * (sunR + 2.5f), sunY + sin(ang) * (sunR + 2.5f)),
                    end   = Offset(cx + cos(ang) * (sunR + r * 0.20f), sunY + sin(ang) * (sunR + r * 0.20f)),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round,
                )
            }
        }
        TileType.MONITOR -> {
            // Chat bubble with 2 horizontal lines inside + tail
            val bubbleW = r * 1.5f
            val bubbleH = r * 1.0f
            val bubbleTop = cy - bubbleH * 0.60f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - bubbleW / 2f, bubbleTop),
                size = Size(bubbleW, bubbleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.25f),
                style = Stroke(width = 2f),
            )
            val lInset = bubbleW * 0.18f
            val lY1 = bubbleTop + bubbleH * 0.34f
            val lY2 = bubbleTop + bubbleH * 0.65f
            drawLine(color.copy(alpha = 0.7f), Offset(cx - bubbleW / 2f + lInset, lY1), Offset(cx + bubbleW / 2f - lInset, lY1), 1.5f)
            drawLine(color.copy(alpha = 0.5f), Offset(cx - bubbleW / 2f + lInset, lY2), Offset(cx + bubbleW / 2f - lInset * 1.6f, lY2), 1.5f)
            val tail = Path().apply {
                moveTo(cx - r * 0.1f, bubbleTop + bubbleH)
                lineTo(cx - r * 0.40f, bubbleTop + bubbleH + r * 0.30f)
                lineTo(cx + r * 0.15f, bubbleTop + bubbleH)
            }
            drawPath(tail, color, style = Stroke(width = 2f, cap = StrokeCap.Round))
            // "PC" badge, top-right corner of the screen (was "AI")
            val badgeCx = cx + bubbleW / 2f - r * 0.18f
            val badgeCy = bubbleTop + r * 0.18f
            drawCircle(color.copy(alpha = 0.9f), r * 0.22f, Offset(badgeCx, badgeCy), style = Stroke(1.4f))
            drawContext.canvas.nativeCanvas.drawText(
                "PC",
                badgeCx,
                badgeCy + r * 0.09f,
                android.graphics.Paint().apply {
                    this.color = color.toArgb()
                    textSize = r * 0.24f
                    isAntiAlias = true
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                },
            )
        }
        TileType.CHAT -> {
            // Clean simple speech bubble — rounded body, tail bottom-left, 2 short lines
            val bw = r * 1.3f
            val bh = r * 0.95f
            val left = cx - bw / 2f
            val top = cy - bh * 0.55f
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.28f),
                style = Stroke(width = 2.5f),
            )
            // Tail, bottom-left
            val tail = Path().apply {
                moveTo(left + bw * 0.20f, top + bh)
                lineTo(left + bw * 0.08f, top + bh + r * 0.30f)
                lineTo(left + bw * 0.44f, top + bh)
            }
            drawPath(tail, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            // Two short lines inside (second shorter)
            val lx = left + bw * 0.22f
            drawLine(color, Offset(lx, top + bh * 0.40f), Offset(left + bw * 0.78f, top + bh * 0.40f), 2f, StrokeCap.Round)
            drawLine(color.copy(alpha = 0.8f), Offset(lx, top + bh * 0.62f), Offset(left + bw * 0.58f, top + bh * 0.62f), 2f, StrokeCap.Round)
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
private fun StatusDot(label: String, color: Color, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val dotC = if (active) color else color.copy(alpha = 0.15f)
            val dotR = size.minDimension / 2f * 0.46f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (active) {
                // Radial glow layers
                for (i in 3 downTo 0) {
                    drawCircle(
                        color = dotC.copy(alpha = 0.07f + i * 0.05f),
                        radius = dotR + i * 3.5f,
                        center = center,
                    )
                }
            }
            // Core
            drawCircle(dotC, dotR, center)
            if (active) {
                // Specular highlight
                drawCircle(
                    Color.White.copy(alpha = 0.42f),
                    dotR * 0.28f,
                    Offset(center.x - dotR * 0.26f, center.y - dotR * 0.26f),
                )
            }
        }
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
