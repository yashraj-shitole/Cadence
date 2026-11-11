package com.yashraj.cadence.ui.components

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.feel.HapticEngine
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Scales
import com.yashraj.cadence.ui.theme.Sizes

/** Haptics bound to the window's view. Components fire these only from the user's own actions. */
@Stable
class Haptics(private val engine: HapticEngine?, private val view: View?) {
    fun fire(kind: HapticKind, levelOverride: Int? = null) {
        if (engine != null && view != null) engine.fire(view, kind, levelOverride)
    }

    companion object {
        val None = Haptics(null, null)
    }
}

val LocalHaptics = compositionLocalOf { Haptics.None }

/** Every touch target is at least 48dp in both directions, even when the visible mark is smaller. */
fun Modifier.minTouchTarget(): Modifier = defaultMinSize(Sizes.tapMin, Sizes.tapMin)

/** A 2dp focus ring with a 2dp offset, shown for keyboard and switch-control focus only. */
@Composable
fun Modifier.focusRing(shape: Shape): Modifier {
    val ring = Cadence.colors.focusRing
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.isFocused }
        .drawWithContent {
            drawContent()
            if (focused && keyboard) {
                val offset = 2.dp.toPx()
                inset(-offset) {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawPath(outline.toPath(size), ring, style = Stroke(2.dp.toPx()))
                }
            }
        }
}

/**
 * Tap with press feedback that starts on touch-down: scales to [pressScale] over 90ms, then settles back.
 * [onPressStart] runs on touch-down (used for the light haptic on checks and the primary button).
 */
@Composable
fun Modifier.tappable(
    onClick: () -> Unit,
    pressScale: Float = Scales.pressScale,
    onPressStart: (() -> Unit)? = null,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    focusShape: Shape? = null,
    /** Release with the one allowed overshoot (checks, toggles). */
    settle: Boolean = false,
): Modifier {
    val motion = Cadence.motion
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale(pressScale) else 1f,
        animationSpec = if (pressed) motion.move(Dur.press, Ease.easeOut) else if (settle) motion.move(Dur.base, motion.settle) else motion.move(Dur.quick, Ease.easeOut),
        label = "press",
    )
    val currentClick by rememberUpdatedState(onClick)
    val currentStart by rememberUpdatedState(onPressStart)
    var result = this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    if (focusShape != null) result = result.focusRing(focusShape)
    return result
        .focusable(enabled)
        .onKeyEvent {
            if (enabled && it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.DirectionCenter || it.key == Key.Spacebar)) {
                currentClick()
                true
            } else {
                false
            }
        }
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        currentStart?.invoke()
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { currentClick() },
                )
            }
        }
        .semantics(mergeDescendants = true) {
            if (role != null) this.role = role
            if (enabled) onClick(onClickLabel) {
                currentClick()
                true
            }
        }
}
