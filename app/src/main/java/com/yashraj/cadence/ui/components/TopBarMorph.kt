package com.yashraj.cadence.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * The actions in a screen's top corner shape-shift into the next screen's instead of being swapped: their bounds
 * glide from one arrangement to the other (a single button to a pair, and back) while the icons cross-fade, so the
 * glass in the corner reads as one continuous object. Where there is no transition around it, this does nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.topBarMorph(key: String = "top-actions"): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val scope = LocalAnimatedScope.current ?: return this
    val base = this
    // The morphing bounds are clipped to their own rectangle while they animate, which would cut a button's shadow square.
    // So the bounds are made 40dp larger on every side than the buttons, and the layout is told they are not: the extra
    // room only holds the shadow.
    val bleed = 40.dp
    return with(shared) {
        base
            .layout { measurable, constraints ->
                val px = bleed.roundToPx()
                val placeable = measurable.measure(constraints)
                layout((placeable.width - 2 * px).coerceAtLeast(0), (placeable.height - 2 * px).coerceAtLeast(0)) { placeable.place(-px, -px) }
            }
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = scope,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(120)),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            )
            .padding(bleed)
    }
}
