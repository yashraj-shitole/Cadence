package com.yashraj.cadence.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AuroraTest {
    @Test
    fun theSameSeedGivesTheSameBackgroundAndADifferentSeedDoesNot() {
        assertEquals(Aurora.next(null, Random(7)), Aurora.next(null, Random(7)))
        assertNotEquals(Aurora.next(null, Random(7)), Aurora.next(null, Random(8)))
    }

    @Test
    fun everyArrangementHasFourLightsInsideTheAllowedRanges() {
        val rng = Random(1)
        var lights = Aurora.next(null, rng)
        repeat(500) {
            assertEquals(Aurora.LightCount, lights.size)
            lights.forEach {
                assertTrue(it.x in -0.15f..1.15f)
                assertTrue(it.y in -0.05f..1.05f)
                assertTrue(it.radius in 0.55f..1.0f)
                assertTrue(it.hue in 0f..2f)
                assertTrue(it.strength in 0.55f..1.0f)
            }
            lights = Aurora.next(lights, rng)
        }
    }

    @Test
    fun aPageChangeDriftsTheLightInsteadOfJumpingIt() {
        val rng = Random(3)
        var lights = Aurora.next(null, rng)
        repeat(500) {
            val after = Aurora.next(lights, rng)
            lights.zip(after).forEach { (a, b) ->
                assertTrue(kotlin.math.abs(a.x - b.x) <= Aurora.MaxStepX + 1e-4f)
                assertTrue(kotlin.math.abs(a.hue - b.hue) <= Aurora.MaxStepHue + 1e-4f)
            }
            lights = after
        }
    }

    @Test
    fun eachLightStaysInItsOwnBandSoTheGlowIsSpreadTopToBottom() {
        val lights = Aurora.next(null, Random(11))
        assertTrue(lights[0].y < lights[3].y)
        assertTrue(lights[0].y <= 0.30f && lights[3].y >= 0.68f)
    }

    @Test
    fun interpolationHitsBothEndsAndTheMiddle() {
        val a = Aurora.next(null, Random(1))
        val b = Aurora.next(a, Random(2))
        assertEquals(a, Aurora.lerp(a, b, 0f))
        assertEquals(b, Aurora.lerp(a, b, 1f))
        val mid = Aurora.lerp(a, b, 0.5f)
        assertEquals((a[0].x + b[0].x) / 2f, mid[0].x, 1e-5f)
        assertEquals((a[2].hue + b[2].hue) / 2f, mid[2].hue, 1e-5f)
    }

    @Test
    fun hueWalksTheThreeGlowColoursInOrder() {
        val g1 = Color.Red
        val g2 = Color.Green
        val g3 = Color.Blue
        assertEquals(g1, Aurora.colorAt(0f, g1, g2, g3))
        assertEquals(g2, Aurora.colorAt(1f, g1, g2, g3))
        assertEquals(g3, Aurora.colorAt(2f, g1, g2, g3))
        val between = Aurora.colorAt(0.5f, g1, g2, g3)
        assertTrue(between.red > 0f && between.green > 0f && between.blue == 0f)
    }
}
