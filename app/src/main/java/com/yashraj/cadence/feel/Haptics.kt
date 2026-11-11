package com.yashraj.cadence.feel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import com.yashraj.cadence.ui.theme.HapticTokens

/** Haptic events from motion-and-haptics.md. Each fires in the same frame as the visual change it belongs to. */
enum class HapticKind(val amplitude: Float) {
    /** Chip, segmented control, slider detent, tab change. */
    Tick(HapticTokens.tick),

    /** Touch-down on a check or the add button. */
    Light(HapticTokens.light),

    /** A beat completes. */
    Medium(HapticTokens.medium),

    /** All beats done: two pulses 90ms apart. */
    Success(HapticTokens.success),

    /** A destructive confirm appears. */
    Warning(HapticTokens.warning),
}

/** What the user chose in Settings > Feel. */
data class HapticSettings(val level: Int = 2, val completionOnly: Boolean = false)

/** System settings that always win over the in-app ones. */
object SystemFeel {
    /** True when the system's animation scale is 0 (Remove animations / Reduce motion). */
    fun reduceMotion(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    /** True when touch feedback is turned off in system settings. */
    fun hapticsOff(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0
}

/**
 * Plays haptics scaled by the Haptics setting. Balanced uses the platform's action constants; Soft and Firm use
 * amplitude-scaled primitives where the device supports them and fall back to a plain pulse where it does not.
 */
class HapticEngine(private val context: Context) {
    private val vibrator: Vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    var settings = HapticSettings()

    /** [levelOverride] previews a level before it has been saved (the Haptics slider). */
    fun fire(view: View, kind: HapticKind, levelOverride: Int? = null) {
        val level = levelOverride ?: settings.level
        if (level <= 0 || SystemFeel.hapticsOff(context)) return
        // Soft keeps only completions; "Completion only" silences everything but finishing a beat or the day.
        if (kind == HapticKind.Tick && level == 1) return
        if (settings.completionOnly && kind != HapticKind.Medium && kind != HapticKind.Success) return
        if (level == 2) fireConstants(view, kind) else firePrimitives(kind, if (level == 1) 0.5f else 1f)
    }

    private fun fireConstants(view: View, kind: HapticKind) {
        when (kind) {
            HapticKind.Tick -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            HapticKind.Light -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            HapticKind.Medium -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            HapticKind.Warning -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            HapticKind.Success -> {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                handler.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }, 90)
            }
        }
    }

    private fun firePrimitives(kind: HapticKind, levelScale: Float) {
        val scale = (kind.amplitude * levelScale).coerceIn(0f, 1f)
        val primitive = when (kind) {
            HapticKind.Tick -> VibrationEffect.Composition.PRIMITIVE_TICK
            HapticKind.Warning -> VibrationEffect.Composition.PRIMITIVE_THUD
            else -> VibrationEffect.Composition.PRIMITIVE_CLICK
        }
        val effect = if (vibrator.areAllPrimitivesSupported(primitive)) {
            val composition = VibrationEffect.startComposition().addPrimitive(primitive, scale)
            if (kind == HapticKind.Success) composition.addPrimitive(primitive, scale, 90)
            composition.compose()
        } else {
            val amplitude = if (vibrator.hasAmplitudeControl()) (scale * 255).toInt().coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
            VibrationEffect.createOneShot(if (kind == HapticKind.Tick) 8 else 18, amplitude)
        }
        vibrator.vibrate(effect)
    }
}
