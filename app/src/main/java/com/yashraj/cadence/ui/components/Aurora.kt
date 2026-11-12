package com.yashraj.cadence.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.util.lerp
import kotlin.random.Random

/**
 * One soft light of the aurora. [x] and [y] place its centre as fractions of the screen (a little past 0..1 is
 * allowed, so a light can sit half off the edge), [radius] is a fraction of the screen width, [hue] walks the
 * palette's three glow colours (0 is glow 1, 1 is glow 2, 2 is glow 3) and [strength] scales its opacity.
 */
@Immutable
data class Light(val x: Float, val y: Float, val radius: Float, val hue: Float, val strength: Float)

/** Where the lights may go and how far they travel in one page change; kept small so the change stays calm. */
object Aurora {
    const val LightCount = 4
    const val MaxStepX = 0.35f
    const val MaxStepHue = 0.7f

    /** Each light stays in its own band of the screen, so the glow is spread top to bottom and never piles up. */
    private val Bands = listOf(-0.05f to 0.30f, 0.22f to 0.52f, 0.48f to 0.78f, 0.68f to 1.05f)

    private val XRange = -0.15f to 1.15f
    private val RadiusRange = 0.55f to 1.0f
    private val StrengthRange = 0.55f to 1.0f

    /**
     * The first arrangement ([previous] null) is free; every later one is a short random walk from [previous], so a
     * page change drifts the light instead of jumping it.
     */
    fun next(previous: List<Light>?, rng: Random): List<Light> = List(LightCount) { i ->
        val band = Bands[i]
        val before = previous?.getOrNull(i)
        val x = if (before == null) rng.range(XRange) else (before.x + rng.range(-MaxStepX, MaxStepX)).coerceIn(XRange.first, XRange.second)
        val hue = if (before == null) rng.range(0f, 2f) else (before.hue + rng.range(-MaxStepHue, MaxStepHue)).coerceIn(0f, 2f)
        Light(x = x, y = rng.range(band), radius = rng.range(RadiusRange), hue = hue, strength = rng.range(StrengthRange))
    }

    fun lerp(a: List<Light>, b: List<Light>, t: Float): List<Light> = a.zip(b) { p, q ->
        Light(lerp(p.x, q.x, t), lerp(p.y, q.y, t), lerp(p.radius, q.radius, t), lerp(p.hue, q.hue, t), lerp(p.strength, q.strength, t))
    }

    /** The colour at [hue] along glow 1, glow 2, glow 3. */
    fun colorAt(hue: Float, glow1: Color, glow2: Color, glow3: Color): Color =
        if (hue <= 1f) lerpColor(glow1, glow2, hue.coerceIn(0f, 1f)) else lerpColor(glow2, glow3, (hue - 1f).coerceIn(0f, 1f))

    private fun Random.range(lo: Float, hi: Float) = lo + nextFloat() * (hi - lo)
    private fun Random.range(r: Pair<Float, Float>) = range(r.first, r.second)
}

/**
 * The background light shared by every screen. It is random for each launch ([seed]) and moves to a new random
 * arrangement whenever [advance] is called, easing from wherever it currently is, so a page change that interrupts
 * another still moves smoothly. Screens read [lights] while drawing, so animating it never recomposes anything.
 */
@Stable
class AuroraState(seed: Long = System.nanoTime()) {
    private val rng = Random(seed)
    private var from by mutableStateOf(Aurora.next(null, rng))
    private var to by mutableStateOf(from)
    private val progress = Animatable(1f)

    /** The lights right now, between [from] and [to]. */
    fun lights(): List<Light> = Aurora.lerp(from, to, progress.value)

    /** Eases to a fresh arrangement over [spec], starting from the current one. */
    suspend fun advance(spec: AnimationSpec<Float>) {
        val now = lights()
        from = now
        to = Aurora.next(now, rng)
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
    }
}

/** The shared aurora; null where there is none (previews), and then no glow is drawn. */
val LocalAurora = staticCompositionLocalOf<AuroraState?> { null }
