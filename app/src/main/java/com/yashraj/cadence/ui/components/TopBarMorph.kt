package com.yashraj.cadence.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

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
    return with(shared) {
        base.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = scope,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(120)),
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
