package com.yashraj.cadence.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFormatTest {
    private val custom = Settings(
        motion = 3, haptics = 1, completionOnly = true, palette = "dusk", theme = ThemeMode.Dark,
        backdrop = BackdropStyle.Soft, reduceTransparency = true, highContrast = true,
        reminderMinute = 7 * 60 + 30, weekendRest = false, weekStart = WeekStart.Sunday,
    )

    @Test
    fun settingsSurviveARoundTripThroughJsonText() {
        val text = BackupFormat.settingsToJson(custom).toString()
        val back = BackupFormat.settingsFromJson(JSONObject(text))!!
        assertEquals(custom.copy(walkthroughSeen = true), back)
    }

    @Test
    fun deviceOnlyValuesNeverTravelWithABackup() {
        val withDeviceState = custom.copy(backupFolder = "content://tree/primary%3ADocuments", lastBackupAt = 123, walkthroughSeen = false)
        val json = BackupFormat.settingsToJson(withDeviceState)
        assertFalse(json.has("backupFolder"))
        assertFalse(json.has("lastBackupAt"))
        assertFalse(json.has("walkthroughSeen"))
        val back = BackupFormat.settingsFromJson(json)!!
        assertNull(back.backupFolder)
        assertEquals(0L, back.lastBackupAt)
    }

    @Test
    fun aRestoredInstallSkipsTheWalkthrough() {
        assertTrue(BackupFormat.settingsFromJson(JSONObject("{}"))!!.walkthroughSeen)
    }

    @Test
    fun noSettingsInTheFileMeansNullSoNothingIsOverwritten() {
        assertNull(BackupFormat.settingsFromJson(null))
    }

    @Test
    fun missingBadOrOutOfRangeValuesFallBackToDefaults() {
        val d = Settings()
        val back = BackupFormat.settingsFromJson(
            JSONObject("""{"motion": 99, "haptics": -4, "theme": "Neon", "backdrop": 5, "weekStart": "", "palette": "", "reminderMinute": 5000}"""),
        )!!
        assertEquals(3, back.motion)
        assertEquals(0, back.haptics)
        assertEquals(d.theme, back.theme)
        assertEquals(d.backdrop, back.backdrop)
        assertEquals(d.weekStart, back.weekStart)
        assertEquals(d.palette, back.palette)
        assertNull(back.reminderMinute)
    }

    @Test
    fun reminderOffStaysOff() {
        val json = BackupFormat.settingsToJson(custom.copy(reminderMinute = null))
        assertTrue(json.isNull("reminderMinute"))
        assertNull(BackupFormat.settingsFromJson(json)!!.reminderMinute)
    }

    @Test
    fun versionOneFilesAndCurrentFilesAreImportable() {
        BackupFormat.requireImportable(JSONObject("""{"app":"Cadence"}"""))
        BackupFormat.requireImportable(JSONObject("""{"app":"Cadence","format":${BackupFormat.VERSION}}"""))
    }

    @Test
    fun otherFilesAndNewerBackupsAreRefusedWithAReadableMessage() {
        val notCadence = runCatching { BackupFormat.requireImportable(JSONObject("""{"app":"Other"}""")) }.exceptionOrNull()
        assertTrue(notCadence is IllegalArgumentException)
        assertEquals("That file is not a Cadence export.", notCadence?.message)

        val newer = runCatching { BackupFormat.requireImportable(JSONObject("""{"app":"Cadence","format":${BackupFormat.VERSION + 1}}""")) }.exceptionOrNull()
        if (newer !is IllegalArgumentException) fail("A newer format must be refused")
        assertTrue(newer?.message.orEmpty().contains("newer Cadence"))
    }

    @Test
    fun folderLabelShowsTheReadablePath() {
        assertEquals("Documents/Cadence", BackupFormat.folderLabel("content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FCadence"))
        assertEquals("Documents", BackupFormat.folderLabel("content://com.android.externalstorage.documents/tree/primary%3ADocuments"))
        assertEquals("Download/Cadence", BackupFormat.folderLabel("content://com.android.providers.downloads.documents/tree/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FCadence"))
        assertEquals("Internal storage", BackupFormat.folderLabel("content://com.android.externalstorage.documents/tree/primary%3A"))
    }
}
