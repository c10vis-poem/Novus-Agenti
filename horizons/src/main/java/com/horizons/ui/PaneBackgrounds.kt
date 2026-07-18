package com.horizons.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.ui.theme.HorizonsColors

private data class RainDrop(val x: Float, val y: Float, val radius: Float, val alpha: Float)
private data class Crack(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val alpha: Float)

private fun generateRainDrops(count: Int): List<RainDrop> {
    val rng = java.util.Random(31)
    return List(count) {
        RainDrop(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 1.5f + rng.nextFloat() * 3f,
            alpha = 0.04f + rng.nextFloat() * 0.08f,
        )
    }
}

private fun generateCracks(count: Int): List<Crack> {
    val rng = java.util.Random(17)
    return List(count) {
        val x = rng.nextFloat()
        val y = rng.nextFloat()
        val dx = (rng.nextFloat() - 0.5f) * 0.08f
        val dy = (rng.nextFloat() - 0.5f) * 0.08f
        Crack(x, y, x + dx, y + dy, 0.03f + rng.nextFloat() * 0.04f)
    }
}

@Composable
fun SlateStoneBackground(modifier: Modifier = Modifier) {
    val drops = remember { generateRainDrops(80) }
    val cracks = remember { generateCracks(25) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSlateTexture(drops, cracks)
    }
}

private fun DrawScope.drawSlateTexture(drops: List<RainDrop>, cracks: List<Crack>) {
    // Deep blue-gray stone base
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2830),
                Color(0xFF222C34),
                Color(0xFF1A2228),
            ),
        ),
    )

    // Stone grain — subtle horizontal streaks
    val grainColor = Color(0xFF3A4650).copy(alpha = 0.08f)
    val rng = java.util.Random(53)
    for (i in 0..40) {
        val y = rng.nextFloat() * size.height
        drawLine(
            color = grainColor,
            start = Offset(0f, y),
            end = Offset(size.width, y + (rng.nextFloat() - 0.5f) * 4f),
            strokeWidth = 0.5f + rng.nextFloat() * 1.5f,
        )
    }

    // Cracks / veins in stone
    cracks.forEach { crack ->
        drawLine(
            color = Color(0xFF4A5A68).copy(alpha = crack.alpha),
            start = Offset(crack.x1 * size.width, crack.y1 * size.height),
            end = Offset(crack.x2 * size.width, crack.y2 * size.height),
            strokeWidth = 0.6f,
        )
    }

    // Rain droplets beading on surface
    drops.forEach { drop ->
        val cx = drop.x * size.width
        val cy = drop.y * size.height
        // Droplet highlight (light reflection)
        drawCircle(
            color = Color.White.copy(alpha = drop.alpha * 0.6f),
            radius = drop.radius * 0.4f,
            center = Offset(cx - drop.radius * 0.2f, cy - drop.radius * 0.2f),
        )
        // Droplet body
        drawCircle(
            color = Color(0xFF5A7A90).copy(alpha = drop.alpha),
            radius = drop.radius,
            center = Offset(cx, cy),
        )
        // Droplet edge ring
        drawCircle(
            color = Color.White.copy(alpha = drop.alpha * 0.3f),
            radius = drop.radius,
            center = Offset(cx, cy),
            style = Stroke(width = 0.4f),
        )
    }
}

// ── Water droplet background (Chat pane) ───────────────────────────────────

private data class WaterDrop(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun generateWaterDrops(count: Int): List<WaterDrop> {
    val rng = java.util.Random(47)
    return List(count) {
        WaterDrop(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 2f + rng.nextFloat() * 5f,
            alpha = 0.05f + rng.nextFloat() * 0.12f,
        )
    }
}

@Composable
fun WaterDropletBackground(modifier: Modifier = Modifier) {
    val drops = remember { generateWaterDrops(100) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF122428),
                    Color(0xFF183034),
                    Color(0xFF0F1E22),
                ),
            ),
        )

        val rng = java.util.Random(61)
        for (i in 0..30) {
            val y = rng.nextFloat() * size.height
            drawLine(
                color = Color(0xFF2DD4D9).copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y + (rng.nextFloat() - 0.5f) * 6f),
                strokeWidth = 0.8f + rng.nextFloat() * 2f,
            )
        }

        drops.forEach { drop ->
            val cx = drop.x * size.width
            val cy = drop.y * size.height
            drawCircle(
                color = Color(0xFF2DD4D9).copy(alpha = drop.alpha),
                radius = drop.radius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color(0xFF4FE7EC).copy(alpha = drop.alpha * 0.5f),
                radius = drop.radius * 0.35f,
                center = Offset(cx - drop.radius * 0.25f, cy - drop.radius * 0.25f),
            )
            drawCircle(
                color = Color(0xFF2DD4D9).copy(alpha = drop.alpha * 0.3f),
                radius = drop.radius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.5f),
            )
        }
    }
}

// ── Astral space background (Horizons pane) ────────────────────────────────

private data class DeepStar(val x: Float, val y: Float, val size: Float, val alpha: Float, val isTeal: Boolean)

private fun generateDeepStars(count: Int): List<DeepStar> {
    val rng = java.util.Random(73)
    return List(count) {
        DeepStar(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            size = 0.5f + rng.nextFloat() * 2f,
            alpha = 0.1f + rng.nextFloat() * 0.6f,
            isTeal = rng.nextFloat() < 0.3f,
        )
    }
}

@Composable
fun AstralSpaceBackground(modifier: Modifier = Modifier) {
    val stars = remember { generateDeepStars(150) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A2830),
                    Color(0xFF111C22),
                    Color(0xFF0A1218),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.maxDimension * 0.8f,
            ),
        )

        stars.forEach { star ->
            val color = if (star.isTeal) Color(0xFF2DD4D9) else Color.White
            drawCircle(
                color = color.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }

        val nebulaColor = Color(0xFF2DD4D9).copy(alpha = 0.02f)
        drawCircle(
            color = nebulaColor,
            radius = size.minDimension * 0.4f,
            center = Offset(size.width * 0.3f, size.height * 0.2f),
        )
        drawCircle(
            color = nebulaColor,
            radius = size.minDimension * 0.3f,
            center = Offset(size.width * 0.7f, size.height * 0.6f),
        )

        val ringColor = Color(0xFF2DD4D9).copy(alpha = 0.04f)
        for (i in 1..3) {
            drawCircle(
                color = ringColor,
                radius = size.minDimension * 0.15f * i,
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                style = Stroke(width = 0.5f),
            )
        }
    }
}

// ── Circuit trace background (Router pane) ─────────────────────────────────

private data class TraceRun(val pts: List<Offset>, val gold: Boolean)

private fun generateTraces(count: Int): List<TraceRun> {
    val rng = java.util.Random(89)
    return List(count) {
        var x = rng.nextFloat()
        var y = rng.nextFloat()
        val pts = mutableListOf(Offset(x, y))
        // Circuit traces run in 45°/90° segments like a real PCB
        repeat(2 + rng.nextInt(3)) {
            val len = 0.05f + rng.nextFloat() * 0.14f
            when (rng.nextInt(4)) {
                0 -> x += len
                1 -> x -= len
                2 -> { x += len * 0.7f; y += len * 0.7f }
                else -> y += len
            }
            pts.add(Offset(x, y))
        }
        TraceRun(pts, rng.nextFloat() < 0.4f)
    }
}

@Composable
fun CircuitTraceBackground(modifier: Modifier = Modifier) {
    val traces = remember { generateTraces(26) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A222A), Color(0xFF222C34), Color(0xFF161E24)),
            ),
        )

        traces.forEach { trace ->
            val c = if (trace.gold) Color(0xFFF5C518) else Color(0xFF2DD4D9)
            for (i in 0 until trace.pts.size - 1) {
                drawLine(
                    color = c.copy(alpha = 0.07f),
                    start = Offset(trace.pts[i].x * size.width, trace.pts[i].y * size.height),
                    end = Offset(trace.pts[i + 1].x * size.width, trace.pts[i + 1].y * size.height),
                    strokeWidth = 1.2f,
                )
            }
            // Solder pads at trace endpoints
            val first = trace.pts.first()
            val last = trace.pts.last()
            drawCircle(c.copy(alpha = 0.12f), 3f, Offset(first.x * size.width, first.y * size.height))
            drawCircle(c.copy(alpha = 0.10f), 2.2f, Offset(last.x * size.width, last.y * size.height))
            drawCircle(c.copy(alpha = 0.05f), 5f, Offset(first.x * size.width, first.y * size.height), style = Stroke(0.8f))
        }
    }
}

// ── Oscilloscope background (Monitor pane) ─────────────────────────────────

@Composable
fun OscilloscopeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF141E20), Color(0xFF1A262A), Color(0xFF10181C)),
            ),
        )

        // Scope graticule grid
        val gridC = Color(0xFF2DD4D9).copy(alpha = 0.035f)
        val cell = size.minDimension / 10f
        var gx = 0f
        while (gx < size.width) {
            drawLine(gridC, Offset(gx, 0f), Offset(gx, size.height), 0.6f)
            gx += cell
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(gridC, Offset(0f, gy), Offset(size.width, gy), 0.6f)
            gy += cell
        }

        // Waveform traces — sine, square-ish, noise
        val waves = listOf(
            Triple(size.height * 0.25f, 40f, Color(0xFF2DD4D9).copy(alpha = 0.10f)),
            Triple(size.height * 0.55f, 26f, Color(0xFF00FF41).copy(alpha = 0.07f)),
            Triple(size.height * 0.80f, 34f, Color(0xFFF5C518).copy(alpha = 0.06f)),
        )
        waves.forEachIndexed { wi, (baseY, amp, c) ->
            var px = 0f
            var prevY = baseY
            while (px < size.width) {
                val t = px / size.width
                val y = baseY + kotlin.math.sin((t * 6f + wi) * 2f * Math.PI).toFloat() * amp *
                    (0.6f + 0.4f * kotlin.math.sin(t * 23f + wi * 7f))
                drawLine(c, Offset(px - 6f, prevY), Offset(px, y), 1.4f)
                prevY = y
                px += 6f
            }
        }
    }
}

// ── Vault door background (Settings pane) ──────────────────────────────────

@Composable
fun VaultDoorBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Dark brushed steel
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF20262C), Color(0xFF262E36), Color(0xFF1A2026)),
            ),
        )
        val rng = java.util.Random(97)
        for (i in 0..50) {
            val y = rng.nextFloat() * size.height
            drawLine(
                Color(0xFF3A444E).copy(alpha = 0.05f),
                Offset(0f, y), Offset(size.width, y),
                0.5f + rng.nextFloat(),
            )
        }

        // Concentric vault-door rings, off-center upper right
        val hubX = size.width * 0.78f
        val hubY = size.height * 0.22f
        for (i in 1..6) {
            drawCircle(
                Color(0xFFFF5577).copy(alpha = 0.05f - i * 0.004f),
                radius = i * size.minDimension * 0.11f,
                center = Offset(hubX, hubY),
                style = Stroke(width = if (i % 2 == 0) 2.5f else 1f),
            )
        }
        // Bolt circles around inner ring
        for (a in 0 until 360 step 30) {
            val rad = a * Math.PI.toFloat() / 180f
            val r = size.minDimension * 0.22f
            drawCircle(
                Color(0xFF5A6670).copy(alpha = 0.14f),
                radius = 3f,
                center = Offset(
                    hubX + kotlin.math.cos(rad) * r,
                    hubY + kotlin.math.sin(rad) * r,
                ),
            )
        }
        // Spoke handle
        for (a in intArrayOf(0, 120, 240)) {
            val rad = a * Math.PI.toFloat() / 180f
            drawLine(
                Color(0xFF5A6670).copy(alpha = 0.10f),
                Offset(hubX, hubY),
                Offset(
                    hubX + kotlin.math.cos(rad) * size.minDimension * 0.14f,
                    hubY + kotlin.math.sin(rad) * size.minDimension * 0.14f,
                ),
                3f,
            )
        }
    }
}

// ── Film grain background (Archives pane) ──────────────────────────────────

private data class GrainFleck(val x: Float, val y: Float, val alpha: Float)

private fun generateGrain(count: Int): List<GrainFleck> {
    val rng = java.util.Random(101)
    return List(count) {
        GrainFleck(rng.nextFloat(), rng.nextFloat(), 0.02f + rng.nextFloat() * 0.06f)
    }
}

@Composable
fun FilmGrainBackground(modifier: Modifier = Modifier) {
    val flecks = remember { generateGrain(240) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF201E1A), Color(0xFF262420), Color(0xFF1A1816)),
            ),
        )

        // Static noise flecks — warm sepia
        flecks.forEach { f ->
            drawCircle(
                Color(0xFFE8A838).copy(alpha = f.alpha),
                radius = 0.9f,
                center = Offset(f.x * size.width, f.y * size.height),
            )
        }

        // Horizontal scanlines
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
            y += 5f
        }

        // Vertical film scratches
        val rng = java.util.Random(113)
        for (i in 0..3) {
            val x = rng.nextFloat() * size.width
            drawLine(
                Color(0xFFE8A838).copy(alpha = 0.04f),
                Offset(x, 0f), Offset(x + (rng.nextFloat() - 0.5f) * 8f, size.height),
                0.7f,
            )
        }

        // Sprocket holes down left edge
        var sy = 14f
        while (sy < size.height) {
            drawRoundRect(
                Color(0xFFE8A838).copy(alpha = 0.05f),
                topLeft = Offset(6f, sy),
                size = androidx.compose.ui.geometry.Size(10f, 14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                style = Stroke(0.8f),
            )
            sy += 34f
        }
    }
}

// ── Goat Easter egg popup ───────────────────────────────────────────────────

@Composable
fun GoatPopup(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        // Dim overlay
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.7f))
        }

        Surface(
            modifier = Modifier.size(260.dp),
            shape = RoundedCornerShape(16.dp),
            color = HorizonsColors.Surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "404",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = HorizonsColors.TileSettings,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))

                // ASCII goat
                Text(
                    buildString {
                        appendLine("  /\\_/\\  ")
                        appendLine(" ( o.o ) ")
                        appendLine("  > ^ <  ")
                        appendLine(" /|   |\\ ")
                        appendLine("(_|   |_)")
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = HorizonsColors.PrimaryTeal,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "GOAT_NOT_FOUND",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = HorizonsColors.TileSettings.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "tap to dismiss",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                )
            }
        }
    }
}
