package com.yashraj.cadence.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.yashraj.cadence.ui.theme.Blur
import com.yashraj.cadence.ui.theme.Cadence

/**
 * One backdrop glow, placed the way the mockups place it (CSS px read as dp). [x] is the distance from the left
 * edge, or from the right edge when [fromRight]. [y] is the distance from the top, from the bottom when
 * [fromBottom], or the top offset on the 844dp-tall artboard scaled to this screen when [onArtboard].
 */
class GlowSpec(
    val which: Int,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val fromRight: Boolean = false,
    val fromBottom: Boolean = false,
    val onArtboard: Boolean = false,
)

/** Glow layouts per screen: the upper third leans glow-1, the middle glow-3 and the lower third glow-2 on Today. */
object Glows {
    val Today = listOf(
        GlowSpec(1, 300, 280, x = -90, y = -60, fromRight = true),
        GlowSpec(3, 240, 240, x = -90, y = 300, onArtboard = true),
        GlowSpec(2, 340, 300, x = 60, y = -80, fromBottom = true),
    )
    val Habits = listOf(
        GlowSpec(2, 300, 280, x = -90, y = -40, fromRight = true),
        GlowSpec(1, 260, 260, x = -100, y = 330, onArtboard = true),
        GlowSpec(3, 340, 280, x = -60, y = -60, fromRight = true, fromBottom = true),
    )
    val Detail = listOf(
        GlowSpec(1, 320, 300, x = -60, y = 120, onArtboard = true),
        GlowSpec(3, 240, 220, x = -90, y = -50, fromRight = true),
        GlowSpec(2, 300, 260, x = -80, y = 40, fromRight = true, fromBottom = true),
    )
    val Tasks = listOf(
        GlowSpec(3, 300, 260, x = -80, y = -50, fromRight = true),
        GlowSpec(2, 260, 260, x = -100, y = 280, onArtboard = true),
        GlowSpec(1, 340, 300, x = -70, y = -40, fromRight = true, fromBottom = true),
    )
    val Notes = listOf(
        GlowSpec(1, 280, 260, x = -100, y = -40),
        GlowSpec(2, 280, 260, x = -100, y = 260, fromRight = true, onArtboard = true),
        GlowSpec(3, 340, 300, x = -40, y = -60, fromBottom = true),
    )
    val Editor = listOf(
        GlowSpec(3, 300, 260, x = -80, y = -60),
        GlowSpec(1, 280, 280, x = -110, y = 240, fromRight = true, onArtboard = true),
        GlowSpec(2, 320, 280, x = -60, y = -40, fromBottom = true),
    )
    val Settings = listOf(
        GlowSpec(2, 300, 260, x = -90, y = -50),
        GlowSpec(1, 280, 300, x = -110, y = 250, fromRight = true, onArtboard = true),
        GlowSpec(3, 340, 280, x = -40, y = -70, fromBottom = true),
    )
}

/**
 * The ground behind every screen: near-white (or near-black) with a faint 32dp grid, lit from behind by three
 * blurred glows. The glows are scenery with no edges; nothing interactive is ever a glow colour.
 */
@Composable
fun Scenery(glows: List<GlowSpec>, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    Box(modifier.fillMaxSize().drawBehind { drawRect(colors.backdrop) }) {
        if (colors.showGlows) {
            BoxWithConstraints(Modifier.fillMaxSize().alpha(colors.glowAlpha)) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                glows.forEach { g ->
                    val color = when (g.which) {
                        1 -> colors.glow1
                        2 -> colors.glow2
                        else -> colors.glow3
                    }
                    val left: Dp = if (g.fromRight) screenWidth - g.width.dp - g.x.dp else g.x.dp
                    val top: Dp = when {
                        g.fromBottom -> screenHeight - g.height.dp - g.y.dp
                        g.onArtboard -> screenHeight * (g.y / 844f)
                        else -> g.y.dp
                    }
                    Box(
                        Modifier
                            .offset(left, top)
                            .size(g.width.dp, g.height.dp)
                            .blur(Blur.glow, BlurredEdgeTreatment.Unbounded)
                            .drawBehind { drawRoundRect(color, cornerRadius = CornerRadius(size.width * 0.3f, size.height * 0.3f)) },
                    )
                }
            }
        }
        val grid = colors.gridLine
        Canvas(Modifier.fillMaxSize()) {
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
fun SceneryHost(glows: List<GlowSpec>, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberBackdropState()
    CompositionLocalProvider(LocalScenery provides state) {
        Box(modifier.fillMaxSize()) {
            Scenery(glows, Modifier.backdropSource(state))
            content()
        }
    }
}
