package com.yashraj.cadence.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yashraj.cadence.data.BackdropStyle
import com.yashraj.cadence.data.Settings
import com.yashraj.cadence.data.ThemeMode
import com.yashraj.cadence.data.WeekStart
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.feel.SystemFeel
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.UpdateStatus
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButton
import com.yashraj.cadence.ui.components.CCheck
import com.yashraj.cadence.ui.components.CChip
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.CLevelSlider
import com.yashraj.cadence.ui.components.CSegmented
import com.yashraj.cadence.ui.components.CToggle
import com.yashraj.cadence.ui.components.CheckState
import com.yashraj.cadence.ui.components.GroupDivider
import com.yashraj.cadence.ui.components.Glows
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.PalettePicker
import com.yashraj.cadence.ui.components.SceneryHost
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.components.SettingRow
import com.yashraj.cadence.ui.components.SettingStack
import com.yashraj.cadence.ui.components.SettingsGroup
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.components.shadowBleed
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PaletteNotes = mapOf(
    "meadow" to "Fresh green, yellow wash",
    "ultramarine" to "Cool blue",
    "dusk" to "Soft violet, rose glow",
    "rose" to "Raspberry over peach",
    "lagoon" to "Clear teal",
    "graphite" to "Neutral and quiet",
)

private val MotionNotes = listOf("Cross-fades only", "Shorter travel, no overshoot", "Default", "More travel, 15% longer")
private val HapticNotes = listOf("Silent", "Half strength", "Default", "Full strength")
private val ReminderTimes = listOf(7 * 60, 8 * 60, 9 * 60, 12 * 60, 18 * 60, 21 * 60)

/** Settings: Feel, Appearance, Rhythm, Data. Every control applies at once; there is no Save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val colors = Cadence.colors
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return

    val systemMotionOff = remember { SystemFeel.reduceMotion(context) }
    val systemHapticsOff = remember { SystemFeel.hapticsOff(context) }

    var tried by remember { mutableStateOf(false) }
    var reminderOpen by remember { mutableStateOf(false) }
    var erasing by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val json = vm.exportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            vm.showToast("Data exported")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = try {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }
            } catch (e: java.io.IOException) {
                null
            }
            if (text == null) vm.showToast("Couldn't read that file") else vm.importJson(text)
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun setReminder(minute: Int?) {
        vm.setReminder(minute)
        if (minute != null && Build.VERSION.SDK_INT >= 33 && !com.yashraj.cadence.reminder.ReminderScheduler.canNotify(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SceneryHost(Glows.Settings) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = Space.s4, start = Space.s5, end = Space.s5),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = Space.s4)) { CIconButton(CadenceIcons.Back, "Back to Today", onBack) }
            CText("Settings", CadenceType.display, Modifier.padding(bottom = 4.dp), color = colors.ink)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp),
            ) {
                // ---- Feel ----
                SectionHeading("Feel")
                SettingsGroup {
                    val motionLevel = if (systemMotionOff) 0 else s.motion
                    SettingStack("Motion", note = if (systemMotionOff) "Following your system setting" else MotionNotes[motionLevel]) {
                        CLevelSlider(
                            level = motionLevel,
                            labels = listOf("Off", "Subtle", "Standard", "Expressive"),
                            label = "Motion intensity",
                            onLevelChange = { vm.setMotion(it) },
                            enabled = !systemMotionOff,
                        )
                    }
                    GroupDivider()
                    val hapticLevel = if (systemHapticsOff) 0 else s.haptics
                    SettingStack("Haptics", note = if (systemHapticsOff) "Following your system setting" else HapticNotes[hapticLevel]) {
                        CLevelSlider(
                            level = hapticLevel,
                            labels = listOf("Off", "Soft", "Balanced", "Firm"),
                            label = "Haptic intensity",
                            onLevelChange = {
                                vm.setHaptics(it)
                                haptics.fire(HapticKind.Medium, levelOverride = it)
                            },
                            enabled = !systemHapticsOff,
                        )
                    }
                    GroupDivider()
                    SettingRow(
                        title = "Completion only",
                        sub = "Feel a beat finish, nothing else",
                        trailing = { CToggle(s.completionOnly, "Completion-only haptics", { vm.setCompletionOnly(it) }) },
                    )
                    GroupDivider()
                    SettingRow(
                        title = "Try it",
                        sub = "Tap to feel the current settings",
                        trailing = { CCheck(if (tried) CheckState.Done else CheckState.Off, "Try a completion", { tried = !tried }, Modifier.offset(x = 10.dp)) },
                    )
                }

                // ---- Appearance ----
                SectionHeading("Appearance")
                SettingsGroup {
                    SettingStack("Theme") {
                        CSegmented(ThemeMode.entries.map { it.label }, s.theme.ordinal, { vm.setTheme(ThemeMode.entries[it]) }, label = "Theme")
                    }
                    GroupDivider()
                    SettingStack("Colour palette", note = PaletteNotes[s.palette]) {
                        PalettePicker(s.palette, { vm.setPalette(it) })
                    }
                    GroupDivider()
                    SettingStack("Backdrop") {
                        CSegmented(BackdropStyle.entries.map { it.label }, s.backdrop.ordinal, { vm.setBackdrop(BackdropStyle.entries[it]) }, label = "Backdrop")
                    }
                    GroupDivider()
                    SettingRow(
                        title = "Reduce transparency",
                        sub = "Solid surfaces instead of glass",
                        trailing = { CToggle(s.reduceTransparency, "Reduce transparency", { vm.setReduceTransparency(it) }) },
                    )
                    GroupDivider()
                    SettingRow(
                        title = "High contrast",
                        sub = "Stronger text and outlined surfaces",
                        trailing = { CToggle(s.highContrast, "High contrast", { vm.setHighContrast(it) }) },
                    )
                }

                // ---- Rhythm ----
                SectionHeading("Rhythm")
                SettingsGroup {
                    SettingRow(
                        title = "Daily reminder",
                        sub = "One a day, never more",
                        icon = CadenceIcons.Bell,
                        value = s.reminderMinute?.let { Dates.time(it) } ?: "Off",
                        chevron = true,
                        onClick = { reminderOpen = !reminderOpen },
                    )
                    if (reminderOpen) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(start = Space.s4, end = Space.s4, bottom = Space.s3),
                            horizontalArrangement = Arrangement.spacedBy(Space.s2),
                        ) {
                            CChip("Off", s.reminderMinute == null, { setReminder(null) })
                            ReminderTimes.forEach { minute -> CChip(Dates.time(minute), s.reminderMinute == minute, { setReminder(minute) }) }
                            CChip("Other time", s.reminderMinute != null && s.reminderMinute !in ReminderTimes, {
                                val start = s.reminderMinute ?: (8 * 60)
                                TimePickerDialog(context, { _, h, m -> setReminder(h * 60 + m) }, start / 60, start % 60, false).show()
                            })
                        }
                    }
                    GroupDivider()
                    SettingRow(
                        title = "Weekend rest days",
                        sub = "Sat and Sun keep every streak",
                        trailing = { CToggle(s.weekendRest, "Weekend rest days", { vm.setWeekendRest(it) }) },
                    )
                    GroupDivider()
                    SettingStack("Week starts on") {
                        CSegmented(WeekStart.entries.map { it.label }, s.weekStart.ordinal, { vm.setWeekStart(WeekStart.entries[it]) }, label = "Week starts on")
                    }
                }

                // ---- Data ----
                SectionHeading("Data")
                SettingsGroup {
                    SettingRow(
                        title = "Show the walkthrough",
                        sub = "A quick tour of the buttons",
                        chevron = true,
                        onClick = {
                            vm.setWalkthroughSeen(false)
                            onBack()
                        },
                    )
                    GroupDivider()
                    SettingRow(
                        title = "Export data",
                        sub = "Habits, tasks and notes as one file",
                        chevron = true,
                        onClick = { exportLauncher.launch("cadence-export.json") },
                    )
                    GroupDivider()
                    SettingRow(
                        title = "Import data",
                        sub = "Add habits, tasks and notes from an export",
                        chevron = true,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    )
                    GroupDivider()
                    SettingRow(
                        title = "Erase all data",
                        sub = "Asks you to confirm first",
                        danger = true,
                        onClick = { erasing = true },
                    )
                }

                // ---- About ----
                SectionHeading("About")
                SettingsGroup {
                    val update by vm.updateStatus.collectAsState()
                    SettingRow(
                        title = "Check for updates",
                        sub = when (val u = update) {
                            UpdateStatus.Checking -> "Checking…"
                            UpdateStatus.UpToDate -> "You have the latest version"
                            UpdateStatus.Failed -> "Couldn't reach GitHub. Try again"
                            is UpdateStatus.Available -> "Version ${u.release.version} is available"
                            UpdateStatus.Idle -> "Looks for a newer release on GitHub"
                        },
                        value = "v${vm.appVersion}",
                        chevron = true,
                        onClick = {
                            val u = update
                            if (u is UpdateStatus.Available) vm.showUpdatePrompt(u.release) else vm.checkForUpdates(manual = true)
                        },
                    )
                }
            }
        }
    }

    if (erasing) EraseDialog(onCancel = { erasing = false }, onErase = {
        erasing = false
        vm.eraseAll()
        vm.showToast("All data erased")
    })
}

/** The only dialog in the app. It names the result and fades in with a 96% scale. */
@Composable
private fun EraseDialog(onCancel: () -> Unit, onErase: () -> Unit) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        haptics.fire(HapticKind.Warning)
        appear.animateTo(1f, motion.fade(Dur.base, Ease.easeOut))
    }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                // The dialog window wraps its content, so leave room for the drop shadow or it is clipped square.
                .padding(vertical = shadowBleed(colors.shadows.float).dp)
                .padding(horizontal = Space.s6)
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    alpha = appear.value
                    val scale = 1f - 0.04f * motion.m * (1f - appear.value)
                    scaleX = scale
                    scaleY = scale
                }
                .glass(RoundedCornerShape(Radius.sheet), raised = true)
                .padding(Space.s6),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            CText("Erase all data?", CadenceType.title)
            CText(
                "This removes every habit, task and note on this phone. It can't be undone.",
                CadenceType.body,
                color = colors.inkMuted,
                maxLines = 5,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s2, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                CButton("Keep my data", onCancel, kind = ButtonKind.Glass)
                CButton("Erase everything", onErase, kind = ButtonKind.Danger)
            }
        }
    }
}
