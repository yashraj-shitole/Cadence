package com.yashraj.cadence.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class GlassLightTest {
    private fun length(o: Offset) = hypot(o.x, o.y)

    @Test
    fun lyingFlatKeepsTheRestLight() {
        assertEquals(GlassLightMath.Rest, GlassLightMath.fromAcceleration(0.1f, -0.2f))
    }

    @Test
    fun uprightPhoneLightsFromAboveWithALeftLean() {
        val l = GlassLightMath.fromAcceleration(0f, 9.81f)
        assertTrue("points up the screen", l.y < -0.9f)
        assertTrue("leans left", l.x < 0f)
        assertEquals(1f, length(l), 1e-4f)
    }

    @Test
    fun tiltingTheTopOfThePhoneToTheRightMovesTheLightTowardTheRight() {
        val upright = GlassLightMath.fromAcceleration(0f, 9.81f)
        // World-up now has a positive x component in device axes.
        val tilted = GlassLightMath.fromAcceleration(4.9f, 8.5f)
        assertTrue(tilted.x > upright.x)
    }

    @Test
    fun upsideDownPhoneLightsFromTheBottomOfTheScreen() {
        val l = GlassLightMath.fromAcceleration(0f, -9.81f)
        assertTrue(l.y > 0f)
    }

    @Test
    fun everyResultIsAUnitVector() {
        for (ax in -10..10) for (ay in -10..10) {
            assertEquals(1f, length(GlassLightMath.fromAcceleration(ax.toFloat(), ay.toFloat())), 1e-4f)
        }
    }

    @Test
    fun smoothingMovesPartWayAndStaysUnitLength() {
        val from = Offset(0f, -1f)
        val to = Offset(1f, 0f)
        val step = GlassLightMath.smooth(from, to, 0.5f)
        assertEquals(1f, length(step), 1e-4f)
        assertTrue(step.x > 0f && step.y < 0f)
    }

    @Test
    fun tinyChangesAreNotWorthRedrawingEveryTile() {
        assertFalse(GlassLightMath.worthRedrawing(Offset(0f, -1f), Offset(0.01f, -0.999f)))
        assertTrue(GlassLightMath.worthRedrawing(Offset(0f, -1f), Offset(0.1f, -0.995f)))
    }

    @Test
    fun rotatingByAQuarterTurnMovesTheLightAcrossTheTile() {
        val r = GlassLightMath.rotate(Offset(0f, -1f), (Math.PI / 2).toFloat())
        assertEquals(1f, r.x, 1e-4f)
        assertEquals(0f, r.y, 1e-4f)
        assertEquals(1f, length(GlassLightMath.rotate(GlassLightMath.Rest, 1.3f)), 1e-4f)
        assertEquals(GlassLightMath.Rest.x, GlassLightMath.rotate(GlassLightMath.Rest, 0f).x, 1e-6f)
    }

    @Test
    fun biggerGlassIsThickerUpToALimit() {
        val d = 2.75f
        assertEquals(1f, GlassLightMath.sizeBoost(40f * d, d), 1e-4f)
        assertTrue(GlassLightMath.sizeBoost(120f * d, d) > GlassLightMath.sizeBoost(60f * d, d))
        assertEquals(1.6f, GlassLightMath.sizeBoost(400f * d, d), 1e-4f)
    }
}
