package com.yashraj.cadence.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yashraj.cadence.ui.theme.Ease
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Where the light that catches the glass edges comes from. The light sits in the world, above and a little to the
 * left, so tilting the phone moves the highlights around the rims, as the glass in iOS does. [direction] is a unit
 * vector on screen (x right, y down) pointing toward the light.
 */
@Stable
class GlassLight {
    var direction by mutableStateOf(GlassLightMath.Rest)
        internal set

    /** Extra turn, in radians, while the light is travelling once around the glass; 0 at rest. */
    var sweepAngle by mutableFloatStateOf(0f)
        private set

    /**
     * The light travels once around every tile's silhouette and settles back where it was, as it does when an iPhone
     * is unlocked. Used when the app comes back on screen; never on a page change, which would be a distraction.
     */
    suspend fun sweep() {
        try {
            androidx.compose.animation.core.animate(0f, (2.0 * Math.PI).toFloat(), animationSpec = tween(1100, easing = Ease.easeInOut)) { value, _ ->
                sweepAngle = value
            }
        } finally {
            sweepAngle = 0f
        }
    }
}

val LocalGlassLight = staticCompositionLocalOf { GlassLight() }

/**
 * A touch on any interactive glass. The glow starts under the fingertip and spreads, and because the state is shared
 * every glass tile near the finger picks it up, so the light runs onto neighbouring tiles as it does in iOS.
 * [point] is in root pixels.
 */
@Stable
class GlassTouch(private val scope: CoroutineScope) {
    var point by mutableStateOf(Offset.Zero)
        private set
    val glow = Animatable(0f)
    val spread = Animatable(0f)

    /** Reduce Motion lights the whole reach at once instead of spreading. */
    fun press(at: Offset, instant: Boolean) {
        point = at
        scope.launch {
            spread.snapTo(if (instant) 1f else 0f)
            launch { glow.animateTo(1f, tween(90)) }
            if (!instant) spread.animateTo(1f, tween(520, easing = Ease.easeOut))
        }
    }

    /** A press that turned into a scroll lets go quickly; a tap lets the light fade. */
    fun release(quick: Boolean) {
        scope.launch { glow.animateTo(0f, tween(if (quick) 140 else 560, easing = Ease.easeOut)) }
    }
}

val LocalGlassTouch = staticCompositionLocalOf<GlassTouch?> { null }

object GlassLightMath {
    /** Upper left, the direction the original highlight used, and the pose when the phone lies flat. */
    val Rest = Offset(-0.5524f, -0.8336f)

    private const val Gravity = 9.81f

    /** Below this the phone is lying flat, where "up" says nothing about the screen. */
    private const val FlatThreshold = 2.0f

    /**
     * The light direction for an accelerometer reading ([ax] to the right of the device, [ay] toward its top, in
     * m/s2). The reading points at world-up in device axes, so on screen up is (ax, -ay); a pull toward the rest
     * direction keeps the highlight at the upper left instead of dead centre when the phone is upright.
     */
    fun fromAcceleration(ax: Float, ay: Float): Offset {
        if (hypot(ax, ay) < FlatThreshold) return Rest
        val up = Offset((ax / Gravity).coerceIn(-1f, 1f), (-ay / Gravity).coerceIn(-1f, 1f))
        return normalize(Offset(Rest.x * 0.5f + up.x * 0.9f, Rest.y * 0.5f + up.y * 0.9f))
    }

    /** One step of smoothing toward [target], so the highlight glides instead of tracking sensor noise. */
    fun smooth(current: Offset, target: Offset, amount: Float = 0.12f): Offset =
        normalize(Offset(current.x + (target.x - current.x) * amount, current.y + (target.y - current.y) * amount))

    /** True when [a] and [b] differ enough to be worth redrawing every glass tile for. */
    fun worthRedrawing(a: Offset, b: Offset): Boolean = abs(a.x - b.x) + abs(a.y - b.y) > 0.035f

    /** [v] turned by [radians] (positive is clockwise on screen). Used to send the light around a tile as it materialises. */
    fun rotate(v: Offset, radians: Float): Offset {
        val c = kotlin.math.cos(radians)
        val s = kotlin.math.sin(radians)
        return Offset(v.x * c - v.y * s, v.x * s + v.y * c)
    }

    /**
     * Larger glass is a thicker slab: more lensing, softer light. 1 for a small control, up to 1.6 for a sheet,
     * growing smoothly between 48dp and 208dp on the short side.
     */
    fun sizeBoost(shortSidePx: Float, density: Float): Float {
        val dp = shortSidePx / density
        return 1f + 0.6f * ((dp - 48f) / 160f).coerceIn(0f, 1f)
    }

    fun normalize(v: Offset): Offset {
        val length = hypot(v.x, v.y)
        return if (length < 1e-4f) Rest else Offset(v.x / length, v.y / length)
    }
}

/**
 * Follows the phone's tilt while the app is on screen and [enabled]. Off (Motion Off, or solid surfaces) the light
 * stays at its rest position. The sensor is only held while the app is resumed.
 */
@Composable
fun rememberGlassLight(enabled: Boolean): GlassLight {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val light = remember { GlassLight() }
    DisposableEffect(enabled, owner) {
        if (!enabled) {
            light.direction = GlassLightMath.Rest
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) return@DisposableEffect onDispose { }
        var smoothed = light.direction
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val target = GlassLightMath.fromAcceleration(event.values[0], event.values[1])
                smoothed = GlassLightMath.smooth(smoothed, target)
                if (GlassLightMath.worthRedrawing(light.direction, smoothed)) light.direction = smoothed
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        fun start() { manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) }
        fun stop() { manager.unregisterListener(listener) }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
        }
    }
    return light
}
