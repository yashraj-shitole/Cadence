package com.yashraj.cadence.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.domain.DayState
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceColors
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/**
 * One segment per beat today. A filled segment grows from the left with the one allowed overshoot, 80ms after the
 * tick lands. When every beat is done, [sweepKey] changing plays one light sweep across the bar.
 */
@Composable
fun BeatBar(total: Int, done: Int, modifier: Modifier = Modifier, sweepKey: Int = 0) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val pill = RoundedCornerShape(Radius.pill)
    val sweep = remember { Animatable(-1f) }
    LaunchedEffect(sweepKey) {
        if (sweepKey > 0 && !motion.off) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(motion.duration(Dur.celebrate), easing = Ease.easeInOut))
            sweep.snapTo(-1f)
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$done of $total beats done"
                progressBarRangeInfo = ProgressBarRangeInfo(done.toFloat(), 0f..total.toFloat().coerceAtLeast(1f), (total - 1).coerceAtLeast(0))
            }
            .drawWithContent {
                drawContent()
                val s = sweep.value
                if (s >= 0f) {
                    val width = size.width * 0.35f
                    val x = -width + (size.width + width) * s
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            startX = x,
                            endX = x + width,
                        ),
                        topLeft = Offset(x, 0f),
                        size = Size(width, size.height),
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        repeat(total) { i ->
            val fill by animateFloatAsState(
                targetValue = if (i < done) 1f else 0f,
                animationSpec = if (motion.off) motion.fade(Dur.quick) else tween(motion.duration(Dur.base), delayMillis = 80, easing = motion.settle),
                label = "beat",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(pill)
                    .background(colors.track)
                    .then(if (colors.highContrast) Modifier.border(1.dp, colors.controlEdge, pill) else Modifier),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = fill.coerceAtLeast(0f)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                        .background(colors.accent, pill),
                )
            }
        }
    }
}

/** Draws one day mark (the shape for a day state) inside a [size] box. Cells use rounded squares, dots use circles. */
private fun DrawScope.drawDayMark(state: DayState, colors: CadenceColors, cell: Boolean) {
    val w = size.minDimension
    val ring = 2.dp.toPx()
    val radius = if (cell) CornerRadius(Radius.tag.toPx()) else CornerRadius(w / 2)
    fun ringed(fill: Color?, edge: Color, dashed: Boolean = false, width: Float = ring) {
        if (fill != null) drawRoundRect(fill, cornerRadius = radius)
        val inset = width / 2
        drawRoundRect(
            edge,
            topLeft = Offset(inset, inset),
            size = Size(size.width - width, size.height - width),
            cornerRadius = CornerRadius((radius.x - inset).coerceAtLeast(0f)),
            style = Stroke(width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(width * 1.6f, width * 1.4f)) else null),
        )
    }
    when (state) {
        DayState.Done -> drawRoundRect(colors.accent, cornerRadius = radius)
        DayState.Minimum -> ringed(colors.accentSoft, colors.accent)
        DayState.Rest -> ringed(if (cell) colors.restSoft else null, colors.rest)
        DayState.Missed -> drawCircle(colors.controlEdge, radius = (if (cell) 3.dp else 2.dp).toPx())
        DayState.Today -> ringed(null, colors.controlEdge, dashed = true)
        DayState.Future, DayState.NotStarted -> if (cell) {
            ringed(null, if (colors.highContrast) colors.controlEdge else colors.hairline, dashed = true, width = 1.dp.toPx())
        }
    }
}

/** One 10dp week dot. */
@Composable
fun DayDot(state: DayState, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    Canvas(modifier.size(10.dp)) { drawDayMark(state, colors, cell = false) }
}

/** The last seven days as dots: filled = done, tinted ring = minimum, clay ring = rest, small dot = missed, dashed = today. */
@Composable
fun WeekDots(days: List<DayState>, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEach { DayDot(it) }
    }
}

/** Five weeks of habit days, with weekday initials above. The current week is the last row. */
@Composable
fun ConsistencyGrid(days: List<DayState>, sundayFirst: Boolean, description: String, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val initials = if (sundayFirst) listOf("S", "M", "T", "W", "T", "F", "S") else Dates.weekdayInitials
    Column(
        modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            initials.forEach { letter ->
                Box(Modifier.weight(1f).padding(bottom = 2.dp), contentAlignment = Alignment.Center) {
                    CText(letter, CadenceType.label, color = colors.inkMuted)
                }
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { state ->
                    Canvas(Modifier.weight(1f).aspectRatio(1f)) { drawDayMark(state, colors, cell = true) }
                }
            }
        }
    }
}

/** Done, Minimum, Rest, Missed: the key for week dots and the grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayLegend(modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        listOf(DayState.Done to "Done", DayState.Minimum to "Minimum", DayState.Rest to "Rest", DayState.Missed to "Missed").forEach { (state, word) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DayDot(state)
                CText(word, CadenceType.caption, color = colors.inkMuted)
            }
        }
    }
}
