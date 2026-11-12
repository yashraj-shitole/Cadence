package com.yashraj.cadence.data

import org.json.JSONObject
import java.net.URLDecoder

/**
 * The backup file: habits, logs, tasks and notes plus the look-and-feel settings. [VERSION] 2 added settings and the
 * version number itself; files without a "format" key are version 1 and still import.
 */
object BackupFormat {
    const val VERSION = 2
    const val FILE_NAME = "cadence-backup.json"

    /** Throws [IllegalArgumentException] with a message fit for a toast when [root] cannot be imported. */
    fun requireImportable(root: JSONObject) {
        require(root.optString("app") == "Cadence") { "That file is not a Cadence export." }
        require(root.optInt("format", 1) <= VERSION) { "That backup came from a newer Cadence. Update the app, then try again." }
    }

    /** Settings that travel with a backup. The backup folder, last-backup time and walkthrough state stay on the device. */
    fun settingsToJson(s: Settings): JSONObject = JSONObject()
        .put("motion", s.motion).put("haptics", s.haptics).put("completionOnly", s.completionOnly)
        .put("palette", s.palette).put("theme", s.theme.name).put("backdrop", s.backdrop.name)
        .put("reduceTransparency", s.reduceTransparency).put("highContrast", s.highContrast)
        .put("reminderMinute", s.reminderMinute ?: JSONObject.NULL)
        .put("weekendRest", s.weekendRest).put("weekStart", s.weekStart.name)

    /**
     * Settings from a backup, or null when the file has none. Anything missing or out of range falls back to the
     * default, so a hand-edited or older file can never produce an invalid setting.
     */
    fun settingsFromJson(json: JSONObject?): Settings? {
        if (json == null) return null
        val d = Settings()
        return Settings(
            motion = if (json.has("motion")) json.optInt("motion", d.motion).coerceIn(0, 3) else d.motion,
            haptics = if (json.has("haptics")) json.optInt("haptics", d.haptics).coerceIn(0, 3) else d.haptics,
            completionOnly = json.optBoolean("completionOnly", d.completionOnly),
            palette = json.optString("palette", d.palette).ifBlank { d.palette },
            theme = enumOrNull<ThemeMode>(json.optString("theme")) ?: d.theme,
            backdrop = enumOrNull<BackdropStyle>(json.optString("backdrop")) ?: d.backdrop,
            reduceTransparency = json.optBoolean("reduceTransparency", d.reduceTransparency),
            highContrast = json.optBoolean("highContrast", d.highContrast),
            reminderMinute = if (json.isNull("reminderMinute")) null else json.optInt("reminderMinute", -1).takeIf { it in 0 until 24 * 60 },
            weekendRest = json.optBoolean("weekendRest", d.weekendRest),
            weekStart = enumOrNull<WeekStart>(json.optString("weekStart")) ?: d.weekStart,
            // A restored install has already been "set up", so the walkthrough stays out of the way.
            walkthroughSeen = true,
        )
    }

    /** "Documents/Cadence" from a folder picker URI such as .../tree/primary%3ADocuments%2FCadence. */
    fun folderLabel(treeUri: String): String {
        val id = treeUri.substringAfterLast("/tree/", treeUri).substringBefore("/document/")
        val decoded = try { URLDecoder.decode(id, "UTF-8") } catch (e: IllegalArgumentException) { id }
        // The Downloads provider hands back a raw path (raw:/storage/emulated/0/Download/...); show it like the others.
        val path = decoded.substringAfter(':', decoded).trim('/').removePrefix("storage/emulated/0/")
        return path.ifEmpty { "Internal storage" }
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
}
