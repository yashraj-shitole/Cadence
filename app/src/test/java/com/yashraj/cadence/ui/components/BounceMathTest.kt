package com.yashraj.cadence.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BounceMathTest {
    @Test
    fun draggingBackPaysBackThePullBeforeAnythingScrolls() {
        val r = BounceMath.pullBack(offset = 40f, delta = -15f)
        assertEquals(25f, r.offset, 1e-4f)
        assertEquals(-15f, r.consumed, 1e-4f)
    }

    @Test
    fun draggingBackPastZeroStopsAtZeroAndLeavesTheRestForTheList() {
        val r = BounceMath.pullBack(offset = 10f, delta = -25f)
        assertEquals(0f, r.offset, 1e-4f)
        assertEquals(-10f, r.consumed, 1e-4f)
    }

    @Test
    fun draggingFurtherTheSameWayIsNotPaidBack() {
        val r = BounceMath.pullBack(offset = 10f, delta = 25f)
        assertEquals(10f, r.offset, 1e-4f)
        assertEquals(0f, r.consumed, 1e-4f)
    }

    @Test
    fun nothingToPayBackAtRest() {
        val r = BounceMath.pullBack(0f, -30f)
        assertEquals(0f, r.offset, 1e-4f)
        assertEquals(0f, r.consumed, 1e-4f)
    }

    @Test
    fun pullingGetsHarderTheFurtherYouGoAndNeverPastTheLimit() {
        val max = 300f
        val first = BounceMath.resist(0f, 100f, max)
        val later = BounceMath.resist(200f, 100f, max) - 200f
        assertTrue(first > later)
        var o = 0f
        repeat(200) { o = BounceMath.resist(o, 500f, max) }
        assertEquals(max, o, 1e-3f)
        var n = 0f
        repeat(200) { n = BounceMath.resist(n, -500f, max) }
        assertEquals(-max, n, 1e-3f)
    }

    @Test
    fun theResistedPullIsAlwaysLessThanTheFingerMoved() {
        assertTrue(BounceMath.resist(0f, 100f, 300f) < 100f)
    }
}
