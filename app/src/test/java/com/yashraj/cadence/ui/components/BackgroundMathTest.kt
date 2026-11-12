package com.yashraj.cadence.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundMathTest {
    @Test
    fun aWidePhotoOnATallScreenIsCroppedFromTheMiddleSideways() {
        val c = BackgroundMath.centreCrop(imgW = 1600, imgH = 900, dstW = 1000f, dstH = 2000f)
        assertEquals(900, c.height)
        assertEquals(450, c.width)
        assertEquals(0, c.y)
        assertEquals((1600 - 450) / 2, c.x)
    }

    @Test
    fun aTallPhotoOnAWideScreenIsCroppedFromTheMiddleVertically() {
        val c = BackgroundMath.centreCrop(imgW = 900, imgH = 1600, dstW = 2000f, dstH = 1000f)
        assertEquals(900, c.width)
        assertEquals(450, c.height)
        assertEquals(0, c.x)
        assertEquals((1600 - 450) / 2, c.y)
    }

    @Test
    fun aPhotoWithTheScreensShapeIsUsedWhole() {
        val c = BackgroundMath.centreCrop(1000, 2000, 500f, 1000f)
        assertEquals(BackgroundMath.Crop(0, 0, 1000, 2000), c)
    }

    @Test
    fun theCropAlwaysKeepsTheScreensAspectAndStaysInsideThePhoto() {
        for ((iw, ih) in listOf(4000 to 3000, 1000 to 1000, 600 to 1600)) {
            val c = BackgroundMath.centreCrop(iw, ih, 1080f, 2400f)
            assertTrue(c.x >= 0 && c.y >= 0 && c.x + c.width <= iw && c.y + c.height <= ih)
            assertEquals(1080f / 2400f, c.width.toFloat() / c.height, 0.01f)
        }
    }

    @Test
    fun degenerateSizesDoNotCrash() {
        BackgroundMath.centreCrop(0, 0, 100f, 100f)
        BackgroundMath.centreCrop(100, 100, 0f, 0f)
    }

    @Test
    fun brightPhotosNeedMoreScrimInTheDarkThemeAndDarkOnesMoreInTheLightTheme() {
        assertTrue(BackgroundMath.scrimAlpha(1f, themeIsDark = true) > BackgroundMath.scrimAlpha(0f, themeIsDark = true))
        assertTrue(BackgroundMath.scrimAlpha(0f, themeIsDark = false) > BackgroundMath.scrimAlpha(1f, themeIsDark = false))
    }

    @Test
    fun theScrimIsNeverTooThinOrTooThick() {
        for (l in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) for (dark in listOf(true, false)) {
            val a = BackgroundMath.scrimAlpha(l, dark)
            assertTrue(a in 0.30f..0.80f)
        }
    }
}
