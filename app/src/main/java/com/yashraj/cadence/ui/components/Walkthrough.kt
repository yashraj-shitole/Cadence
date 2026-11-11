package com.yashraj.cadence.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Space
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the walkthrough can point. Each is registered by the control itself with [walkTarget]. */
enum class WalkTarget { TabToday, TabHabits, TabTasks, TabNotes, Add, Settings }

/** Screen rectangles of the controls the walkthrough points at. */
class WalkTargets {
    internal val rects = mutableStateMapOf<WalkTarget, Rect>()
    fun rect(target: WalkTarget): Rect? = rects[target]
}

val LocalWalkTargets = compositionLocalOf { WalkTargets() }

/** Marks a control as something the walkthrough can point at. */
@Composable
fun Modifier.walkTarget(target: WalkTarget): Modifier {
    val targets = LocalWalkTargets.current
    return this.onGloballyPositioned { targets.rects[target] = it.boundsInRoot() }
}

/** One stop on the walkthrough. */
class WalkStep(val target: WalkTarget, val title: String, val body: String, val tab: TabId)

val WalkSteps = listOf(
    WalkStep(WalkTarget.TabToday, "Today", "One number, one next step. Everything due lands here.", TabId.Today),
    WalkStep(WalkTarget.TabHabits, "Habits", "Give each one a cue and a minimum. Rest days and missed days stay quiet.", TabId.Habits),
    WalkStep(WalkTarget.TabTasks, "Tasks", "Add a time, a length and a flag. Tick them off right in the list.", TabId.Tasks),
    WalkStep(WalkTarget.TabNotes, "Notes", "Write in blocks. Press / for headings, lists and more.", TabId.Notes),
    WalkStep(WalkTarget.Add, "Add anything", "A title alone is a complete entry.", TabId.Today),
    WalkStep(WalkTarget.Settings, "Make it yours", "Palette, motion and haptics live here.", TabId.Today),
)

private val Chalk = Color(0xFFF2EFE9)
private val Dim = Color(0xFF0E1116)

/** Where the caption and arrow go for one target, all in pixels. */
private class WalkLayout(
    val spot: Rect,
    val captionWidth: Float,
    val captionLeft: Float,
    /** The caption edge facing the target: its bottom when the target is below it, its top when above. */
    val captionEdgeY: Float,
    val captionAbove: Boolean,
    val arrowStart: Offset,
    val arrowEnd: Offset,
)

private fun layoutFor(spot: Rect, screen: Size, density: Float): WalkLayout {
    val margin = 24f * density
    val width = minOf(screen.width - margin * 2, 300f * density)
    val cx = spot.center.x.coerceIn(margin + width / 2, screen.width - margin - width / 2)
    val left = cx - width / 2
    val gap = 96f * density
    val belowTarget = spot.center.y < screen.height / 2
    return if (belowTarget) {
        // Target near the top: caption sits below it and the arrow points up.
        val edge = spot.bottom + gap
        WalkLayout(spot, width, left, edge, false, Offset(cx, edge - 8f * density), Offset(spot.center.x, spot.bottom + 8f * density))
    } else {
        // Target near the bottom: caption sits above it and the arrow points down.
        val edge = spot.top - gap
        WalkLayout(spot, width, left, edge, true, Offset(cx, edge + 8f * density), Offset(spot.center.x, spot.top - 8f * density))
    }
}

/** A hand-drawn curve from [from] to [to], with a little wobble so it does not look ruled. */
private fun arrowPath(from: Offset, to: Offset, density: Float): Path {
    val path = Path()
    val dx = to.x - from.x
    val dy = to.y - from.y
    val side = if (dx >= 0) 1f else -1f
    val bend = (40f + kotlin.math.abs(dx) * 0.25f) * density * side
    val c1 = Offset(from.x + bend, from.y + dy * 0.35f)
    val c2 = Offset(to.x - bend * 0.5f, to.y - dy * 0.3f)
    val steps = 36
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val u = 1 - t
        var x = u * u * u * from.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * to.x
        var y = u * u * u * from.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * to.y
        val wobble = sin(t * 17f) * 1.6f * density * sin(t * PI.toFloat())
        x += wobble
        y += cos(t * 13f) * 1.2f * density * sin(t * PI.toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

private fun DrawScope.drawArrow(path: Path, progress: Float, color: Color, density: Float) {
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    if (length <= 0f || progress <= 0f) return
    val shown = Path()
    measure.getSegment(0f, length * progress, shown, true)
    drawPath(shown, color, style = Stroke(width = 3.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (progress >= 0.98f) {
        // Arrowhead: two strokes of slightly different length, like a pen flick.
        val end = measure.getPosition(length)
        val before = measure.getPosition(length * 0.94f)
        val angle = atan2(end.y - before.y, end.x - before.x)
        fun stroke(offset: Float, len: Float) {
            val a = angle + PI.toFloat() + offset
            drawLine(
                color,
                end,
                Offset(end.x + cos(a) * len * density, end.y + sin(a) * len * density),
                strokeWidth = 3.2f * density,
                cap = StrokeCap.Round,
            )
        }
        stroke(0.5f, 17f)
        stroke(-0.45f, 13f)
    }
}

/**
 * The first-run walkthrough. Everything behind is blurred and dimmed, the control being explained stays sharp inside
 * a spotlight, and a hand-drawn arrow leads from a handwritten note to it. [windowLayer] holds the app drawn
 * underneath. Reduce transparency swaps the blur for a heavier dim.
 */
@Composable
fun WalkthroughOverlay(
    stepIndex: Int,
    windowLayer: BackdropState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val density = LocalDensity.current
    val targets = LocalWalkTargets.current
    val step = WalkSteps[stepIndex]
    val last = stepIndex == WalkSteps.lastIndex

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, motion.fade(Dur.slow, Ease.easeOut)) }

    // The spotlight glides from one control to the next. It is padded so the control has room to breathe.
    val target = targets.rect(step.target)
    var lastTarget by remember { mutableStateOf(target) }
    if (target != null) lastTarget = target
    val goal = lastTarget ?: Rect.Zero
    val pad = with(density) { 8.dp.toPx() }
    val spec = motion.move<Float>(Dur.slow, Ease.easeInOut)
    val left by animateFloatAsState(goal.left - pad, spec, label = "spotLeft")
    val top by animateFloatAsState(goal.top - pad, spec, label = "spotTop")
    val right by animateFloatAsState(goal.right + pad, spec, label = "spotRight")
    val bottom by animateFloatAsState(goal.bottom + pad, spec, label = "spotBottom")

    val arrow = remember(stepIndex) { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        arrow.snapTo(0f)
        if (motion.off) arrow.snapTo(1f) else arrow.animateTo(1f, tween(motion.duration(520), delayMillis = 260, easing = Ease.easeOut))
    }
    val caption = remember(stepIndex) { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        caption.snapTo(0f)
        caption.animateTo(1f, motion.fade(Dur.slow, Ease.easeOut))
    }

    val blurLayer = rememberGraphicsLayer()
    val sigma = with(density) { 14.dp.toPx() }
    val blurEffect = remember(sigma) { RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.CLAMP).asComposeRenderEffect() }
    val solid = colors.solidSurfaces

    BoxWithScreen(modifier) { screen ->
        val spot = Rect(left, top, right, bottom)
        val geometry = if (lastTarget != null) layoutFor(spot, screen, density.density) else null
        val labelText = "Walkthrough, step ${stepIndex + 1} of ${WalkSteps.size}. ${step.title}. ${step.body}"

        Box(
            Modifier
                .fillMaxSize()
                // Nothing underneath is reachable while the walkthrough is up.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
                .semantics {
                    contentDescription = labelText
                    liveRegion = LiveRegionMode.Polite
                }
                .drawWithContent {
                    val alpha = appear.value
                    // Redraw whenever the app underneath redraws.
                    windowLayer.version
                    if (!solid) {
                        blurLayer.renderEffect = blurEffect
                        blurLayer.record(this, layoutDirection, IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                            drawLayer(windowLayer.layer)
                        }
                        drawLayer(blurLayer)
                    }
                    drawRect(Dim.copy(alpha = (if (solid) 0.84f else 0.58f) * alpha))
                    if (geometry != null) {
                        val radius = minOf(spot.height, spot.width) / 2f
                        val corner = CornerRadius(radius)
                        val hole = Path().apply { addRoundRect(RoundRect(spot, corner)) }
                        // The control itself, sharp and undimmed.
                        clipPath(hole) { drawLayer(windowLayer.layer) }
                        drawRoundRect(
                            Chalk.copy(alpha = 0.9f * alpha),
                            topLeft = spot.topLeft,
                            size = spot.size,
                            cornerRadius = corner,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        drawArrow(arrowPath(geometry.arrowStart, geometry.arrowEnd, density.density), arrow.value, Chalk, density.density)
                    }
                    // The note and the buttons go on top of the dimmed screen.
                    drawContent()
                },
        ) {
            if (geometry != null) {
                // The handwritten note: its edge nearest the target is fixed, so the arrow always leaves from it.
                val widthDp = with(density) { geometry.captionWidth.toDp() }
                Column(
                    Modifier
                        .width(widthDp)
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            val x = geometry.captionLeft.roundToInt()
                            val y = if (geometry.captionAbove) geometry.captionEdgeY - p.height else geometry.captionEdgeY
                            layout(constraints.maxWidth, constraints.maxHeight) { p.place(x, y.roundToInt()) }
                        }
                        .graphicsLayer {
                            alpha = caption.value
                            translationY = (1f - caption.value) * motion.travel(12f) * density.density * (if (geometry.captionAbove) 1f else -1f)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CText(step.title, CadenceType.handTitle, color = Chalk, textAlign = TextAlign.Center, maxLines = 2)
                    CText(step.body, CadenceType.handBody, color = Chalk.copy(alpha = 0.92f), textAlign = TextAlign.Center, maxLines = 5)
                }
            }

            // Controls sit at the opposite side of the screen from the control being explained.
            val controlsAtTop = geometry != null && spot.center.y > screen.height / 2
            Row(
                Modifier
                    .align(if (controlsAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .then(if (controlsAtTop) Modifier.statusBarsPadding().padding(top = Space.s6) else Modifier.navigationBarsPadding().padding(bottom = Space.s8))
                    .padding(horizontal = Space.s5)
                    .graphicsLayer { alpha = appear.value },
                horizontalArrangement = Arrangement.spacedBy(Space.s5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepDots(stepIndex)
                if (!last) WalkButton("Skip", onSkip, filled = false)
                WalkButton(if (last) "Done" else "Next", onNext, filled = true)
            }
        }
    }
}

/** Gives its content the size of the screen in pixels. */
@Composable
private fun BoxWithScreen(modifier: Modifier, content: @Composable (Size) -> Unit) {
    var size by remember { mutableStateOf(Size.Zero) }
    Box(modifier.fillMaxSize().onGloballyPositioned { size = Size(it.size.width.toFloat(), it.size.height.toFloat()) }) {
        if (size != Size.Zero) content(size)
    }
}

@Composable
private fun StepDots(current: Int) {
    val motion = Cadence.motion
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        WalkSteps.indices.forEach { i ->
            val width by animateFloatAsState(if (i == current) 20f else 8f, motion.move(Dur.base, Ease.easeOut), label = "dot")
            Box(
                Modifier
                    .size(width = width.dp, height = 8.dp)
                    .background(if (i == current) Chalk else Chalk.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
            )
        }
    }
}

/** Buttons on the dimmed screen: chalk-coloured so they read on any theme. */
@Composable
private fun WalkButton(text: String, onClick: () -> Unit, filled: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .tappable(onClick, focusShape = shape, role = Role.Button)
            .size(width = if (filled) 112.dp else 80.dp, height = 48.dp)
            .then(if (filled) Modifier.background(Chalk, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CText(text, CadenceType.bodyStrong, color = if (filled) Dim else Chalk, maxLines = 1)
    }
}
