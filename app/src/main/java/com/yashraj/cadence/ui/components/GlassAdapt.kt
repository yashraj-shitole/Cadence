package com.yashraj.cadence.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yashraj.cadence.ui.theme.LocalCadence
import com.yashraj.cadence.ui.theme.LocalCadenceFlipped
import kotlin.math.sqrt

/**
 * What the glass can tell about the content behind it. Apple's glass is "aware of what's behind it": it deepens its
 * shadow over text and eases it over a plain light ground, widens or narrows its dynamic range so labels stay
 * legible, and a small element flips between light and dark when the content under it turns the other way.
 * The reading is taken a few times a second from a tiny copy of the backdrop, only while that backdrop is changing.
 *
 * [canFlip] is for small floating elements (tab bar, toast); larger ones, like sheets and menus, adapt but never flip.
 */
@Stable
class GlassAdapt(val canFlip: Boolean, private val themeIsDark: Boolean) {
    /** Mean brightness behind the tile, 0 black to 1 white; NaN until the first reading. */
    var luminance by mutableFloatStateOf(Float.NaN)
        private set

    /** How much detail (text, edges) sits behind the tile, 0 plain to 1 busy. */
    var busyness by mutableFloatStateOf(0f)
        private set

    /** True while a small element shows the opposite of the theme because the content behind it is the opposite. */
    var flipped by mutableStateOf(false)
        private set

    val known: Boolean get() = !luminance.isNaN()

    fun update(pixels: IntArray) {
        val reading = GlassAdaptMath.read(pixels) ?: return
        luminance = reading.luminance
        busyness = if (known) busyness + (reading.busyness - busyness) * 0.5f else reading.busyness
        if (canFlip) flipped = GlassAdaptMath.nextFlip(flipped, themeIsDark, reading.luminance)
    }
}

@Composable
fun rememberGlassAdapt(canFlip: Boolean, themeIsDark: Boolean): GlassAdapt = remember(canFlip, themeIsDark) { GlassAdapt(canFlip, themeIsDark) }

/**
 * Wraps a small glass element and its glyphs: while [adapt] says the content behind has turned the other way, everything
 * inside takes the opposite theme, so the glass and what is drawn on it stay legible. Menus, sheets and other big surfaces
 * do not use this: Apple's large glass adapts but never flips.
 */
@Composable
fun GlassFlip(adapt: GlassAdapt, content: @Composable () -> Unit) {
    val base = LocalCadence.current
    val colors = if (adapt.flipped && !base.solidSurfaces) LocalCadenceFlipped.current else base
    androidx.compose.runtime.CompositionLocalProvider(LocalCadence provides colors, content = content)
}

data class BackdropReading(val luminance: Float, val busyness: Float)

object GlassAdaptMath {
    /** Mean luminance and a busyness measure (spread of luminance) of ARGB [pixels]; null when empty. */
    fun read(pixels: IntArray): BackdropReading? {
        if (pixels.isEmpty()) return null
        var sum = 0.0
        var sumSquares = 0.0
        for (p in pixels) {
            val l = luminance(p)
            sum += l
            sumSquares += l * l
        }
        val n = pixels.size
        val mean = sum / n
        val variance = (sumSquares / n - mean * mean).coerceAtLeast(0.0)
        return BackdropReading(mean.toFloat(), (sqrt(variance) / 0.12).toFloat().coerceIn(0f, 1f))
    }

    /** Rec. 709 luminance of an ARGB int, 0 to 1. */
    fun luminance(argb: Int): Float {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * Whether a small element should show the opposite of the theme. The two thresholds are apart on purpose, so
     * content sitting near the boundary cannot make the element flicker between light and dark.
     */
    fun nextFlip(flipped: Boolean, themeIsDark: Boolean, luminance: Float): Boolean = if (themeIsDark) {
        if (flipped) luminance > 0.50f else luminance > 0.68f
    } else {
        if (flipped) luminance < 0.44f else luminance < 0.28f
    }

    /** Shadow opacity multiplier: stronger over detail such as text, gentler over a plain ground. */
    fun shadowScale(busyness: Float): Float = 0.5f + 1.1f * busyness.coerceIn(0f, 1f)

    /** Fill opacity multiplier: a little more body over busy content so labels stay legible, less over plain ground. */
    fun fillScale(busyness: Float): Float = 0.9f + 0.45f * busyness.coerceIn(0f, 1f)
}
