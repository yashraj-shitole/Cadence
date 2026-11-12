package com.yashraj.cadence.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** The arithmetic of the bounce, apart from the gestures so it can be tested. */
object BounceMath {
    /** Result of letting a drag that moves back toward the edge pay back an existing pull: the new offset and how much of [delta] it took. */
    data class PullBack(val offset: Float, val consumed: Float)

    fun pullBack(offset: Float, delta: Float): PullBack {
        if (offset == 0f || delta == 0f || sign(delta) == sign(offset)) return PullBack(offset, 0f)
        val next = offset + delta
        return if (sign(next) != sign(offset)) PullBack(0f, -offset) else PullBack(next, delta)
    }

    /** Pulling further past the end gets harder the further it goes, and never past [max]. */
    fun resist(offset: Float, delta: Float, max: Float): Float {
        val ease = (1f - abs(offset) / max).coerceIn(0.08f, 1f)
        return (offset + delta * 0.55f * ease).coerceIn(-max, max)
    }
}

/**
 * Pulling past the end of a list moves the whole list, the way iOS does, and lets it spring back. Android's own
 * overscroll stretches the pixels of the list while the ground behind stays put, which tears every glass tile away from
 * the background it is bending. This moves the list through layout instead, so each tile is really somewhere else and
 * bends the part of the background that is actually behind it.
 */
class BounceOverscrollEffect(private val maxPx: Float) : OverscrollEffect {
    private var offset by mutableFloatStateOf(0f)

    override val isInProgress: Boolean get() = offset != 0f

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        val pre = BounceMath.pullBack(offset, delta.y)
        offset = pre.offset
        val forScroll = Offset(delta.x, delta.y - pre.consumed)
        val consumedByScroll = performScroll(forScroll)
        val left = forScroll - consumedByScroll
        if (source == NestedScrollSource.UserInput && abs(left.y) > 0.5f) offset = BounceMath.resist(offset, left.y, maxPx)
        return Offset(consumedByScroll.x, consumedByScroll.y + pre.consumed)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        val consumed = performFling(velocity)
        val remaining = velocity - consumed
        if (offset == 0f && remaining.y == 0f) return
        animate(offset, 0f, initialVelocity = remaining.y * 0.2f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 170f)) { value, _ ->
            offset = value
        }
        offset = 0f
    }

    private inner class OffsetNode : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) { placeable.placeRelative(0, offset.roundToInt()) }
        }
    }

    override val node: DelegatableNode = OffsetNode()
}

class BounceOverscrollFactory(private val maxPx: Float) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = BounceOverscrollEffect(maxPx)
    override fun hashCode(): Int = maxPx.hashCode()
    override fun equals(other: Any?): Boolean = other is BounceOverscrollFactory && other.maxPx == maxPx
}
