package com.yashraj.cadence.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashraj.cadence.ui.components.GlowSpec
import com.yashraj.cadence.ui.components.SceneryHost
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Sizes
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.delay

/** Bottom padding that lets the last row clear the floating tab bar: tab bar height + space-8, plus its own offset. */
@Composable
fun tabBarClearance(): Dp =
    Sizes.tabbarHeight + Space.s8 + TabBarBottomGap + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

val TabBarBottomGap = 24.dp

/** The date-style eyebrow above a screen title: 13sp semibold in `ink` (it sits on bare backdrop). */
val EyebrowStyle: TextStyle get() = CadenceType.caption.copy(fontWeight = FontWeight(600), letterSpacing = 0.02.sp)

/** Every screen: scenery, then one display title top left, with one or two icon buttons at the right. */
@Composable
fun ScreenFrame(glows: List<GlowSpec>, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    SceneryHost(glows, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = Space.s4, start = Space.s5, end = Space.s5),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Cadence.colors
    Row(
        modifier.fillMaxWidth().padding(bottom = Space.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) CText(eyebrow, EyebrowStyle, color = colors.ink, maxLines = 1)
            CText(title, CadenceType.display, Modifier.semantics { heading() }, color = colors.ink, maxLines = 2)
        }
        if (trailing != null) Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) { trailing() }
    }
}

/** List padding: screen margin is applied by the frame; this clears the tab bar at the end. */
@Composable
fun listPadding(): PaddingValues = PaddingValues(bottom = tabBarClearance())

/**
 * First load: rows rise 8px, 30ms apart, at most six staggered. Subtle and Off do not stagger, and Off does not move.
 * [play] is false once the screen has settled so scrolling never replays it.
 */
@Composable
fun Modifier.riseIn(index: Int, play: Boolean): Modifier {
    val motion = Cadence.motion
    val density = LocalDensity.current
    if (!play || motion.off) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (motion.staggers) delay(minOf(index, 5) * 30L)
        progress.animateTo(1f, motion.fade(Dur.base, Ease.easeOut))
    }
    return this.graphicsLayer {
        // Not the default offscreen layer: that is clipped to the row's bounds and cuts the drop shadow to a
        // square until the fade ends.
        compositingStrategy = CompositingStrategy.ModulateAlpha
        alpha = progress.value
        translationY = (1f - progress.value) * motion.travel(8f) * density.density
    }
}

/** Lets a scrolling row run edge to edge inside a screen that has side margins: widen by [by] each side, then pad back. */
fun Modifier.bleedHorizontal(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val width = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}
