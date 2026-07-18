package com.horizons.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

// ── Wet slate-stone background (Chat pane) ─────────────────────────────────
// Rain-splashed blue-grey slate: cracked stone plates + highlighted droplets

private data class WaterDrop(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun generateWaterDrops(count: Int): List<WaterDrop> {
    val rng = java.util.Random(47)
    return List(count) {
        WaterDrop(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 2f + rng.nextFloat() * 5f,
            alpha = 0.06f + rng.nextFloat() * 0.14f,
        )
    }
}

private data class Crack(val pts: List<Offset>)

private fun generateCracks(count: Int): List<Crack> {
    val rng = java.util.Random(53)
    return List(count) {
        var x = rng.nextFloat()
        var y = rng.nextFloat()
        val pts = mutableListOf(Offset(x, y))
        // Jagged fissure wandering across the stone
        repeat(3 + rng.nextInt(4)) {
            x += (rng.nextFloat() - 0.5f) * 0.22f
            y += (rng.nextFloat() - 0.5f) * 0.22f
            pts.add(Offset(x, y))
        }
        Crack(pts)
    }
}

@Composable
fun WaterDropletBackground(modifier: Modifier = Modifier) {
    val drops = remember { generateWaterDrops(90) }
    val cracks = remember { generateCracks(16) }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Wet blue-grey slate — deep, cool, faintly reflective
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF161C22),
                    Color(0xFF212B33),
                    Color(0xFF0D1217),
                ),
            ),
        )

        // Broad specular wet-sheen across the upper slate
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3A4A56).copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.35f, size.height * 0.25f),
                radius = size.maxDimension * 0.6f,
            ),
        )

        // Cracked stone — dark fissures with a wet highlight edge catching light
        cracks.forEach { crack ->
            for (i in 0 until crack.pts.size - 1) {
                val a = Offset(crack.pts[i].x * size.width, crack.pts[i].y * size.height)
                val b = Offset(crack.pts[i + 1].x * size.width, crack.pts[i + 1].y * size.height)
                drawLine(Color(0xFF070A0D).copy(alpha = 0.55f), a, b, 1.6f)
                drawLine(
                    Color(0xFF8AA0AE).copy(alpha = 0.10f),
                    Offset(a.x + 1f, a.y - 1f), Offset(b.x + 1f, b.y - 1f), 0.7f,
                )
            }
        }

        // Rain-splash streaks running down the wet stone
        val rng = java.util.Random(61)
        for (i in 0..24) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            drawLine(
                color = Color(0xFFB8C8D2).copy(alpha = 0.05f),
                start = Offset(x, y),
                end = Offset(x + (rng.nextFloat() - 0.5f) * 4f, y + 10f + rng.nextFloat() * 20f),
                strokeWidth = 0.8f,
            )
        }

        // Highlighted water droplets — pale, specular, wet (not teal)
        drops.forEach { drop ->
            val cx = drop.x * size.width
            val cy = drop.y * size.height
            drawCircle(
                color = Color(0xFF9FB4C0).copy(alpha = drop.alpha),
                radius = drop.radius,
                center = Offset(cx, cy),
            )
            // Bright specular glint (top-left) — the highlight the spec calls for
            drawCircle(
                color = Color.White.copy(alpha = drop.alpha * 0.9f),
                radius = drop.radius * 0.3f,
                center = Offset(cx - drop.radius * 0.3f, cy - drop.radius * 0.3f),
            )
            // Meniscus rim
            drawCircle(
                color = Color(0xFF6E8492).copy(alpha = drop.alpha * 0.5f),
                radius = drop.radius,
                center = Offset(cx, cy),
                style = Stroke(width = 0.6f),
            )
        }
    }
}

// ── Deep-space butterfly nebula (Horizons pane) ────────────────────────────
// Bipolar (butterfly) nebula: mirrored purple/blue wings, gold-white core,
// a dark equatorial dust lane, and a purple/blue/gold star field.

private data class DeepStar(val x: Float, val y: Float, val size: Float, val alpha: Float, val tint: Int)

private fun generateDeepStars(count: Int): List<DeepStar> {
    val rng = java.util.Random(73)
    return List(count) {
        DeepStar(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            size = 0.5f + rng.nextFloat() * 2f,
            alpha = 0.15f + rng.nextFloat() * 0.7f,
            tint = rng.nextInt(4), // 0=white 1=blue 2=purple 3=gold
        )
    }
}

@Composable
fun AstralSpaceBackground(modifier: Modifier = Modifier) {
    val stars = remember { generateDeepStars(160) }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep space base — near-black with a violet core
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0E0A18),
                    Color(0xFF080611),
                    Color(0xFF020103),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = size.maxDimension * 0.8f,
            ),
        )

        val cx = size.width * 0.5f
        val cy = size.height * 0.42f

        // Butterfly wings — two mirrored lobes along a diagonal outflow axis.
        // Each wing = stacked radial gradients fading purple -> blue -> clear.
        fun wing(dirX: Float, dirY: Float) {
            for (k in 0 until 4) {
                val dist = 0.10f + k * 0.07f
                val wx = cx + dirX * size.minDimension * dist
                val wy = cy + dirY * size.minDimension * dist
                val r = size.minDimension * (0.26f - k * 0.03f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7A3FD0).copy(alpha = 0.10f),
                            Color(0xFF3E5CC8).copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = Offset(wx, wy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(wx, wy),
                )
            }
        }
        wing(-0.8f, -0.7f) // upper-left lobe
        wing(0.8f, 0.7f)   // lower-right lobe

        // Magenta bloom hugging the core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB84FD0).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx, cy),
                radius = size.minDimension * 0.34f,
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(cx, cy),
        )

        // Hot gold-white core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE9B0).copy(alpha = 0.55f),
                    Color(0xFFF5C518).copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = size.minDimension * 0.10f,
            ),
            radius = size.minDimension * 0.10f,
            center = Offset(cx, cy),
        )

        // Dark equatorial dust lane across the waist (perpendicular to wings)
        rotate(degrees = -49f, pivot = Offset(cx, cy)) {
            drawRect(
                color = Color.Black.copy(alpha = 0.38f),
                topLeft = Offset(cx - size.width * 0.7f, cy - size.minDimension * 0.028f),
                size = androidx.compose.ui.geometry.Size(size.width * 1.4f, size.minDimension * 0.056f),
            )
        }

        // Star field — white / blue / purple / gold, brighter ones haloed
        stars.forEach { star ->
            val color = when (star.tint) {
                1 -> Color(0xFF7FA8FF)
                2 -> Color(0xFFB98BFF)
                3 -> Color(0xFFFFD98A)
                else -> Color.White
            }
            val center = Offset(star.x * size.width, star.y * size.height)
            if (star.alpha > 0.6f) {
                drawCircle(color.copy(alpha = star.alpha * 0.22f), star.size * 3f, center)
            }
            drawCircle(color.copy(alpha = star.alpha), star.size, center)
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
                colors = listOf(Color(0xFF0A1015), Color(0xFF0C141A), Color(0xFF06090C)),
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
                colors = listOf(Color(0xFF08120F), Color(0xFF0A1518), Color(0xFF05090B)),
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

