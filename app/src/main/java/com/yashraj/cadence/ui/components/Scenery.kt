package com.yashraj.cadence.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.yashraj.cadence.ui.theme.Cadence

/**
 * The ground behind every screen: near-white (or near-black) with a faint 32dp grid, lit from behind by the shared
 * [LocalAurora]. The light is scenery with no edges; nothing interactive is ever a glow colour.
 *
 * Every screen draws the same aurora state, so while one screen fades into another the light they share does not
 * cross-fade: it simply keeps moving.
 */
@Composable
fun Scenery(modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val aurora = LocalAurora.current
    // High contrast never puts a photo under the text.
    val photo = if (colors.highContrast) null else LocalBackground.current
    val scrim = colors.backdrop.copy(alpha = BackgroundMath.scrimAlpha(photo?.luminance ?: 0.5f, colors.isDark))
    Box(
        modifier.fillMaxSize().drawBehind {
            drawRect(colors.backdrop)
            if (photo != null) {
                val crop = BackgroundMath.centreCrop(photo.image.width, photo.image.height, size.width, size.height)
                drawImage(
                    photo.image,
                    srcOffset = IntOffset(crop.x, crop.y),
                    srcSize = IntSize(crop.width, crop.height),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                )
                // The ground colour over the photo keeps text legible whatever picture is chosen.
                drawRect(scrim)
            }
        },
    ) {
        if (photo == null && colors.showGlows && aurora != null) {
            val glow1 = colors.glow1
            val glow2 = colors.glow2
            val glow3 = colors.glow3
            val alpha = colors.glowAlpha
            Canvas(Modifier.fillMaxSize()) {
                // Read here, in the draw phase, so an animating aurora redraws this canvas and recomposes nothing.
                for (light in aurora.lights()) {
                    val base = Aurora.colorAt(light.hue, glow1, glow2, glow3)
                    val peak = base.alpha * alpha * light.strength
                    val radius = light.radius * size.width
                    val center = Offset(light.x * size.width, light.y * size.height)
                    // Stops follow a soft bell, not a straight line, so there is no visible rim at any size.
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to base.copy(alpha = peak),
                            0.3f to base.copy(alpha = peak * 0.62f),
                            0.6f to base.copy(alpha = peak * 0.2f),
                            1f to base.copy(alpha = 0f),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
        }
        val grid = colors.gridLine
        if (photo == null) Canvas(Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            var x = -1f
            while (x < size.width) {
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = -1f
            while (y < size.height) {
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
    }
}

/** The scenery of the screen being drawn: rows and cards on it refract this, and nothing else. */
val LocalScenery = compositionLocalOf<BackdropState?> { null }

/** A screen's ground: scenery recorded into its own layer so glass tiles on the screen can refract it. */
@Composable
fun SceneryHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberBackdropState()
    CompositionLocalProvider(LocalScenery provides state) {
        Box(modifier.fillMaxSize()) {
            Scenery(Modifier.backdropSource(state))
            content()
        }
    }
}
