package com.yashraj.cadence.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassAdaptTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun luminanceOfBlackAndWhite() {
        assertEquals(0f, GlassAdaptMath.luminance(black), 1e-4f)
        assertEquals(1f, GlassAdaptMath.luminance(white), 1e-4f)
        assertTrue(GlassAdaptMath.luminance(0xFF00FF00.toInt()) > GlassAdaptMath.luminance(0xFF0000FF.toInt()))
    }

    @Test
    fun plainGroundIsBrightAndQuiet() {
        val r = GlassAdaptMath.read(IntArray(100) { white })
        assertNotNull(r)
        assertEquals(1f, r!!.luminance, 1e-3f)
        assertEquals(0f, r.busyness, 1e-3f)
    }

    @Test
    fun textLikeContentIsBusy() {
        val pixels = IntArray(100) { if (it % 4 == 0) black else white }
        val r = GlassAdaptMath.read(pixels)!!
        assertEquals(0.75f, r.luminance, 1e-3f)
        assertTrue(r.busyness > 0.9f)
    }

    @Test
    fun emptyInputGivesNoReading() {
        assertNull(GlassAdaptMath.read(IntArray(0)))
    }

    @Test
    fun shadowIsStrongerOverDetailAndFillGainsBody() {
        assertTrue(GlassAdaptMath.shadowScale(1f) > GlassAdaptMath.shadowScale(0f))
        assertTrue(GlassAdaptMath.shadowScale(0f) < 1f && GlassAdaptMath.shadowScale(1f) > 1f)
        assertTrue(GlassAdaptMath.fillScale(1f) > GlassAdaptMath.fillScale(0f))
    }

    @Test
    fun smallGlassFlipsToTheOppositeOfTheThemeWhenContentTurnsOver() {
        // Light theme: dark content behind flips it to the dark look.
        assertTrue(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = false, luminance = 0.1f))
        assertFalse(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = false, luminance = 0.9f))
        // Dark theme: bright content flips it to the light look.
        assertTrue(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = true, luminance = 0.9f))
        assertFalse(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = true, luminance = 0.1f))
    }

    @Test
    fun theFlipHasHysteresisSoBorderlineContentCannotFlicker() {
        // Light theme, flipped at 0.3 would not trigger (needs < 0.28), but once flipped it stays until > 0.44.
        assertFalse(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = false, luminance = 0.35f))
        assertTrue(GlassAdaptMath.nextFlip(flipped = true, themeIsDark = false, luminance = 0.35f))
        assertFalse(GlassAdaptMath.nextFlip(flipped = true, themeIsDark = false, luminance = 0.5f))
        assertFalse(GlassAdaptMath.nextFlip(flipped = false, themeIsDark = true, luminance = 0.6f))
        assertTrue(GlassAdaptMath.nextFlip(flipped = true, themeIsDark = true, luminance = 0.6f))
    }
}
