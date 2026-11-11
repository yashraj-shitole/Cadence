package com.yashraj.cadence.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * The Motion setting (and the OS Reduce Motion setting, which wins). [m] multiplies travel and overshoot,
 * [durationFactor] stretches durations at Expressive.
 *
 * Off: no movement or scale, changes cross-fade in 120ms, progress still updates.
 * Subtle: distances x 0.6, no overshoot, no stagger.
 * Expressive: distances x 1.25, durations +15%.
 */
@Immutable
class Motion(val level: Int, val m: Float, val durationFactor: Float, val overshoot: Boolean) {
    val off: Boolean get() = m == 0f
    val staggers: Boolean get() = level >= 2

    /** Easing for a check mark, a filling beat, a toggle knob: the one allowed overshoot. */
    val settle: Easing get() = if (overshoot) Ease.easeSettle else Ease.easeOut

    /** Scales a travel distance (in dp or px) by the Motion setting. */
    fun travel(distance: Float): Float = distance * m

    /** Press scale: 1 - (1 - scale) * m. */
    fun pressScale(scale: Float): Float = 1f - (1f - scale) * m

    fun duration(ms: Int): Int = (ms * durationFactor).toInt()

    /** For anything that moves or scales: snaps when Motion is Off. */
    fun <T> move(ms: Int, easing: Easing = Ease.easeOut): FiniteAnimationSpec<T> =
        if (off) snap() else tween(duration(ms), easing = easing)

    /** For colours and opacity: still cross-fades in 120ms when Motion is Off. */
    fun <T> fade(ms: Int, easing: Easing = Ease.easeOut): FiniteAnimationSpec<T> =
        tween(if (off) 120 else duration(ms), easing = easing)

    companion object {
        fun forLevel(level: Int): Motion = when (level) {
            0 -> Motion(0, Scales.motionOff, 1f, false)
            1 -> Motion(1, Scales.motionSubtle, 1f, false)
            3 -> Motion(3, Scales.motionExpressive, 1.15f, true)
            else -> Motion(2, Scales.motionStandard, 1f, true)
        }
    }
}

val LocalMotion = compositionLocalOf { Motion.forLevel(2) }
