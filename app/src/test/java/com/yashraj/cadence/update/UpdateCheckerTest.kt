package com.yashraj.cadence.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun higherVersionIsNewer() {
        assertTrue(UpdateChecker.isNewer("v1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.9"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.0"))
    }

    @Test
    fun sameOrLowerVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("v1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("0.9.0", "1.0.0"))
    }

    @Test
    fun missingPartsCountAsZero() {
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0"))
        assertFalse(UpdateChecker.isNewer("1", "1.0.1"))
    }

    @Test
    fun suffixesAreIgnoredAndJunkNeverPrompts() {
        assertFalse(UpdateChecker.isNewer("1.0.0-beta", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.2.0-rc1", "1.1.0"))
        assertFalse(UpdateChecker.isNewer("latest", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", ""))
    }
}
