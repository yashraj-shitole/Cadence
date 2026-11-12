package com.yashraj.cadence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.theme.CIconSmall
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.IconDef
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Scales
import com.yashraj.cadence.ui.theme.Sizes
import com.yashraj.cadence.ui.theme.Space
import kotlin.math.roundToInt

enum class CheckState { Off, Done, Minimum }
enum class CheckShape { Circle, Square }

private val TickPath: Path by lazy { PathParser().parsePathString("M20 6 9 17l-5-5").toPath() }

/**
 * The check: a circle for a habit, a rounded square for a task. 28dp visible inside a 48dp target.
 * Touch-down gives a light haptic; completing gives a medium one as the tick lands; undoing gives a tick.
 */
@Composable
fun CCheck(
    state: CheckState,
    label: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CheckShape = CheckShape.Circle,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val visibleShape = if (shape == CheckShape.Circle) CircleShape else RoundedCornerShape(Radius.tag)
    val fillTarget = when (state) {
        CheckState.Off -> Color.Transparent
        CheckState.Done -> colors.accent
        CheckState.Minimum -> colors.accentSoft
    }
    val borderTarget = if (state == CheckState.Off) colors.controlEdge else colors.accent
    val fill by animateColorAsState(fillTarget, motion.fade(Dur.quick), label = "checkFill")
    val border by animateColorAsState(borderTarget, motion.fade(Dur.quick), label = "checkBorder")
    val tickColor = if (state == CheckState.Minimum) colors.accent else colors.onAccent
    val tick by animateFloatAsState(
        targetValue = if (state == CheckState.Off) 0f else 1f,
        animationSpec = if (motion.off) motion.fade(Dur.quick) else androidx.compose.animation.core.tween(motion.duration(Dur.base), delayMillis = 60, easing = Ease.easeOut),
        label = "checkTick",
    )
    val statusWord = when (state) {
        CheckState.Off -> "Not done"
        CheckState.Done -> "Done"
        CheckState.Minimum -> "Minimum done"
    }
    Box(
        modifier
            .size(Sizes.tapMin)
            .tappable(
                onClick = {
                    haptics.fire(if (state == CheckState.Off) HapticKind.Medium else HapticKind.Tick)
                    onToggle()
                },
                pressScale = Scales.pressScaleSmall,
                onPressStart = { haptics.fire(HapticKind.Light) },
                role = Role.Checkbox,
                focusShape = CircleShape,
                settle = true,
            )
            .semantics {
                contentDescription = label
                stateDescription = statusWord
                toggleableState = if (state == CheckState.Off) ToggleableState.Off else ToggleableState.On
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Sizes.check)
                .background(fill, visibleShape)
                .border(2.dp, border, visibleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(16.dp)) {
                if (tick > 0f) {
                    val measure = PathMeasure().apply { setPath(TickPath, false) }
                    val segment = Path()
                    measure.getSegment(0f, measure.length * tick, segment, true)
                    scale(size.width / 24f, pivot = Offset.Zero) {
                        drawPath(segment, tickColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

/** A filter or option chip: 40dp pill inside a 48dp target. Selected chips use `accent-soft` behind `ink` text. */
@Composable
fun CChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconDef? = null,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(Radius.pill)
    val onGlass = LocalOnGlass.current
    val fill by animateColorAsState(if (selected) colors.glassRaised else colors.glass, motion.fade(Dur.quick), label = "chipFill")
    val soft by animateColorAsState(if (selected) colors.accentSoft else Color.Transparent, motion.fade(Dur.quick), label = "chipSoft")
    val edge by animateColorAsState(if (selected) colors.accent else colors.glassEdge, motion.fade(Dur.quick), label = "chipEdge")
    Box(
        modifier
            .heightIn(min = Sizes.tapMin)
            .tappable(
                onClick = {
                    if (!selected) haptics.fire(HapticKind.Tick)
                    onClick()
                },
                role = Role.Button,
                focusShape = shape,
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(40.dp)
                .then(if (onGlass) Modifier.background(fill, shape) else Modifier.glass(shape, raised = selected, interactive = true))
                .background(soft, shape)
                .then(if (onGlass || selected) Modifier.border(if (selected) 2.dp else 1.dp, edge, shape) else Modifier)
                .padding(horizontal = Space.s4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) CIconSmall(icon)
            CText(text, CadenceType.callout.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(600)), maxLines = 1)
        }
    }
}

/** A small tag: radius-tag, `track` fill, caption text. High contrast adds a 1dp outline. */
@Composable
fun CTag(text: String, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val shape = RoundedCornerShape(Radius.tag)
    Box(
        modifier
            .height(24.dp)
            .background(colors.track, shape)
            .then(if (colors.highContrast) Modifier.border(1.dp, colors.controlEdge, shape) else Modifier)
            .padding(horizontal = Space.s2),
        contentAlignment = Alignment.Center,
    ) {
        CText(text, CadenceType.caption, color = colors.inkMuted, maxLines = 1)
    }
}

/** Segmented control: the thumb slides with `ease-in-out`, the label colours cross-fade. */
/**
 * Tracks whether the control is being touched without taking the touch, so its own gestures are unaffected.
 */
private fun Modifier.trackPress(onPressed: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onPressed(true)
            waitForUpOrCancellation(PointerEventPass.Initial)
            onPressed(false)
        }
    }
}

/**
 * The part of a control that moves (toggle knob, slider thumb, selector thumb) is a clear liquid-glass lens that rides
 * over its own track, which it bends at the rim exactly as the glass bends everything behind it. At rest it carries a
 * light translucent veil, so it stays quiet; while touched the veil clears and it swells a little, like a gel, so the
 * track beneath can be seen through the lens. The control passes its track as the backdrop with [LocalScenery].
 * Reduce transparency and high contrast fall back to a solid thumb.
 */
@Composable
private fun Modifier.glassThumb(shape: Shape, lift: Float, solidFallback: androidx.compose.ui.graphics.Color): Modifier {
    val colors = Cadence.colors
    if (colors.solidSurfaces) return this.thumbShadow(colors.shadows.thumb, shape).background(solidFallback, shape)
    val veil = androidx.compose.ui.graphics.Color.White.copy(alpha = (0.5f - 0.42f * lift) * (if (colors.isDark) 0.5f else 1f))
    return this
        .graphicsLayer {
            val grow = 1f + 0.08f * lift
            scaleX = grow
            scaleY = grow
        }
        .glass(shape, raised = true, interactive = true)
        .background(veil, shape)
}

@Composable
private fun rememberLift(pressed: Boolean): Float {
    val motion = Cadence.motion
    val lift by animateFloatAsState(if (pressed) 1f else 0f, motion.fade(Dur.base, Ease.easeOut), label = "lift")
    return lift
}

@Composable
fun CSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val trackShape = RoundedCornerShape(Radius.control)
    val thumbShape = RoundedCornerShape(12.dp)
    val position by animateFloatAsState(selected.toFloat(), motion.move(Dur.base, Ease.easeInOut), label = "segThumb")
    var segPressed by remember { mutableStateOf(false) }
    val segLift = rememberLift(segPressed)
    val segTrack = rememberBackdropState()
    Box(
        modifier
            .fillMaxWidth()
            .trackPress { segPressed = it }
            .background(colors.track, trackShape)
            .then(if (colors.highContrast) Modifier.border(1.dp, colors.controlEdge, trackShape) else Modifier)
            .padding(4.dp)
            .semantics { if (label != null) contentDescription = label },
    ) {
        // The track is what the thumb's lens bends.
        Box(Modifier.matchParentSize().backdropSource(segTrack).background(colors.track, trackShape))
        androidx.compose.runtime.CompositionLocalProvider(LocalScenery provides segTrack) {
            Box(
                Modifier
                    .fillMaxWidth(1f / options.size)
                    .height(40.dp)
                    .graphicsLayer { translationX = position * size.width }
                    .glassThumb(thumbShape, segLift, colors.surfaceSolidRaised)
                    .then(if (colors.highContrast) Modifier.border(2.dp, colors.accent, thumbShape) else Modifier),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val textColor by animateColorAsState(if (index == selected) colors.ink else colors.inkMuted, motion.fade(Dur.quick), label = "segText")
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(thumbShape)
                        .tappable(
                            onClick = {
                                if (index != selected) {
                                    haptics.fire(HapticKind.Tick)
                                    onSelect(index)
                                }
                            },
                            pressScale = 1f,
                            role = Role.Tab,
                            focusShape = thumbShape,
                        )
                        .semantics { this.selected = index == selected },
                    contentAlignment = Alignment.Center,
                ) {
                    CText(option, CadenceType.callout.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(600)), color = textColor, maxLines = 1)
                }
            }
        }
    }
}

/** On/off switch: 52 x 32 with a knob that slides with the one allowed overshoot. */
@Composable
fun CToggle(
    checked: Boolean,
    label: String,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val trackShape = RoundedCornerShape(Radius.pill)
    val trackFill by animateColorAsState(if (checked) colors.accent else colors.track, motion.fade(Dur.quick), label = "toggleTrack")
    val trackEdge by animateColorAsState(if (checked) colors.accent else colors.controlEdge, motion.fade(Dur.quick), label = "toggleEdge")
    val knobColor by animateColorAsState(if (checked) colors.onAccent else colors.controlEdge, motion.fade(Dur.quick), label = "toggleKnob")
    val knobP by animateFloatAsState(if (checked) 1f else 0f, motion.move(Dur.base, motion.settle), label = "toggleX")
    var togglePressed by remember { mutableStateOf(false) }
    val knobLift = rememberLift(togglePressed)
    val toggleTrack = rememberBackdropState()
    Box(
        modifier
            .trackPress { togglePressed = it }
            // The 60dp target is 4dp wider than the 52dp track; pull it out so the track sits flush with the row padding.
            .offset(x = 4.dp)
            .size(width = 60.dp, height = Sizes.tapMin)
            .tappable(
                onClick = {
                    haptics.fire(HapticKind.Tick)
                    onChange(!checked)
                },
                pressScale = 1f,
                role = Role.Switch,
                focusShape = trackShape,
            )
            .semantics {
                contentDescription = label
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 52.dp, height = 32.dp)) {
            // The track is what the knob's lens bends: a capsule of glass that widens while touched.
            Box(
                Modifier
                    .fillMaxSize()
                    .backdropSource(toggleTrack)
                    .background(trackFill, trackShape)
                    .border(1.5.dp, trackEdge, trackShape),
            )
            val knobWidth = 30f + 6f * knobLift
            androidx.compose.runtime.CompositionLocalProvider(LocalScenery provides toggleTrack) {
                Box(
                    Modifier
                        .offset(x = (4f + knobP * (44f - knobWidth)).dp, y = 4.dp)
                        .size(width = knobWidth.dp, height = 24.dp)
                        .glassThumb(CircleShape, knobLift, knobColor),
                )
            }
        }
    }
}

/**
 * Four-stop intensity slider (Off / Subtle / Standard / Expressive, or Off / Soft / Balanced / Firm).
 * The thumb snaps to a detent and a medium haptic is felt at the chosen level.
 */
@Composable
fun CLevelSlider(
    level: Int,
    labels: List<String>,
    label: String,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val density = LocalDensity.current
    val currentLevel by rememberUpdatedState(level)
    val currentChange by rememberUpdatedState(onLevelChange)
    val thumbPosition by animateFloatAsState(level.toFloat(), motion.move(Dur.base, motion.settle), label = "levelThumb")
    val trackShape = RoundedCornerShape(Radius.pill)
    var thumbPressed by remember { mutableStateOf(false) }
    val thumbLift = rememberLift(thumbPressed)
    val sliderTrack = rememberBackdropState()
    Column(modifier.alpha(if (enabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(Sizes.tapMin)
                .semantics {
                    contentDescription = label
                    stateDescription = labels[level]
                    progressBarRangeInfo = ProgressBarRangeInfo(level.toFloat(), 0f..3f, 2)
                    if (enabled) setProgress { value ->
                        val next = value.roundToInt().coerceIn(0, 3)
                        if (next != currentLevel) currentChange(next)
                        true
                    }
                }
                .trackPress { if (enabled) thumbPressed = it }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val thumb = with(density) { 28.dp.toPx() }
                    fun levelAt(x: Float): Int = (((x - thumb / 2) / (size.width - thumb)) * 3f).roundToInt().coerceIn(0, 3)
                    detectTapGestures { offset ->
                        val next = levelAt(offset.x)
                        if (next != currentLevel) currentChange(next)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val thumb = with(density) { 28.dp.toPx() }
                    fun levelAt(x: Float): Int = (((x - thumb / 2) / (size.width - thumb)) * 3f).roundToInt().coerceIn(0, 3)
                    detectDragGestures { change, _ ->
                        change.consume()
                        val next = levelAt(change.position.x)
                        if (next != currentLevel) currentChange(next)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val usable = maxWidth - 28.dp
            // The track, its fill and its detents are what the thumb's lens bends.
            Box(Modifier.fillMaxSize().backdropSource(sliderTrack), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(colors.track, trackShape)
                    .then(if (colors.highContrast) Modifier.border(1.dp, colors.controlEdge, trackShape) else Modifier),
            )
            Box(
                Modifier
                    .width(usable * (thumbPosition / 3f) + 14.dp)
                    .height(8.dp)
                    .background(colors.accent, trackShape),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { Box(Modifier.size(4.dp).background(colors.controlEdge, CircleShape)) }
            }
            }
            androidx.compose.runtime.CompositionLocalProvider(LocalScenery provides sliderTrack) {
                Box(
                    Modifier
                        .offset(x = usable * (thumbPosition / 3f))
                        .size(28.dp)
                        .glassThumb(CircleShape, thumbLift, colors.surfaceSolidRaised)
                        .then(if (colors.solidSurfaces) Modifier.border(2.dp, colors.accent, CircleShape) else Modifier),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, text ->
                CText(
                    text,
                    if (i == level) CadenceType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(600)) else CadenceType.caption,
                    color = if (i == level) colors.ink else colors.inkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}
