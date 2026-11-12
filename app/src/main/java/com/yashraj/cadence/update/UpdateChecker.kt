package com.yashraj.cadence.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A published GitHub release newer than the running app. */
data class ReleaseInfo(val version: String, val url: String)

sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Available(val release: ReleaseInfo) : CheckResult
    data object Failed : CheckResult
}

/**
 * Asks GitHub for the latest published release and compares it with the running version. This is the only
 * network request the app makes. It sends nothing about the user or the device, and it only happens at
 * start-up or when the user taps "Check for updates".
 */
object UpdateChecker {
    private const val LATEST_API = "https://api.github.com/repos/yashraj-shitole/Cadence/releases/latest"
    const val RELEASE_PAGE = "https://github.com/yashraj-shitole/Cadence/releases/latest"

    /** Every release carries the installer under this fixed name, so a link to it always starts the download. */
    private const val ApkAssetName = "Cadence.apk"

    suspend fun check(currentVersion: String): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Cadence-Android")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext CheckResult.Failed
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.optString("tag_name")
                if (tag.isBlank()) return@withContext CheckResult.Failed
                val page = json.optString("html_url").ifBlank { RELEASE_PAGE }
                // Prefer the installer itself, so Download starts the download instead of opening a web page.
                val assets = json.optJSONArray("assets")
                val apk = (0 until (assets?.length() ?: 0))
                    .mapNotNull { assets?.optJSONObject(it) }
                    .firstOrNull { it.optString("name") == ApkAssetName }
                    ?.optString("browser_download_url")
                    ?.takeIf { it.isNotBlank() }
                if (isNewer(tag, currentVersion)) CheckResult.Available(ReleaseInfo(tag.removePrefix("v"), apk ?: page)) else CheckResult.UpToDate
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            CheckResult.Failed
        }
    }

    /** True when [latest] is a higher dotted version than [current]. A leading "v" and any "-suffix" are ignored. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = parts(latest)
        val b = parts(current)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String): List<Int> {
        val core = v.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        val nums = core.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() }
        return if (nums.any { it == null }) emptyList() else nums.filterNotNull()
    }
}
