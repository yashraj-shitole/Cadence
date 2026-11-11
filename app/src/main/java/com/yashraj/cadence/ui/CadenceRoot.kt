package com.yashraj.cadence.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yashraj.cadence.CadenceApp
import com.yashraj.cadence.data.HabitEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.feel.SystemFeel
import com.yashraj.cadence.ui.components.CTabBar
import com.yashraj.cadence.ui.components.CToast
import com.yashraj.cadence.ui.components.Haptics
import com.yashraj.cadence.ui.components.LocalBackdrop
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.TabId
import com.yashraj.cadence.ui.components.LocalWalkTargets
import com.yashraj.cadence.ui.components.WalkSteps
import com.yashraj.cadence.ui.components.WalkTargets
import com.yashraj.cadence.ui.components.WalkthroughOverlay
import com.yashraj.cadence.ui.components.windowCapture
import com.yashraj.cadence.ui.components.backdropSource
import com.yashraj.cadence.ui.components.rememberBackdropState
import com.yashraj.cadence.ui.screens.HabitDetailScreen
import com.yashraj.cadence.ui.screens.HabitsScreen
import com.yashraj.cadence.ui.screens.NoteEditorScreen
import com.yashraj.cadence.ui.screens.NotesScreen
import com.yashraj.cadence.ui.screens.SettingsScreen
import com.yashraj.cadence.ui.screens.TasksScreen
import com.yashraj.cadence.ui.screens.TodayScreen
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceTheme
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease

sealed interface Route {
    val depth: Int

    data class Tab(val id: TabId) : Route { override val depth = 0 }
    data object Settings : Route { override val depth = 1 }
    data class HabitDetail(val id: Long) : Route { override val depth = 1 }
    data class NoteEditor(val id: Long?) : Route { override val depth = 1 }
}

/** A back stack with the four tabs at its base: switching tab replaces the stack, opening a detail pushes onto it. */
class NavState {
    var stack by mutableStateOf<List<Route>>(listOf(Route.Tab(TabId.Today)))
        private set
    val current: Route get() = stack.last()
    val tab: TabId? get() = (current as? Route.Tab)?.id

    fun push(route: Route) { stack = stack + route }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun selectTab(tab: TabId) { stack = listOf(Route.Tab(tab)) }
}

@Composable
fun CadenceRoot(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val s = settings
    val context = LocalContext.current
    var systemReduceMotion by remember { mutableStateOf(SystemFeel.reduceMotion(context)) }
    LifecycleResumeEffect(Unit) {
        systemReduceMotion = SystemFeel.reduceMotion(context)
        vm.onResume()
        onPauseOrDispose { }
    }
    // Until Settings has loaded there is nothing to draw but the window ground; this avoids a wrong-theme flash.
    if (s == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF4F4F1)))
        return
    }
    CadenceTheme(s, systemReduceMotion) { CadenceContent(vm) }
}

@Composable
private fun CadenceContent(vm: AppViewModel) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val view = LocalView.current
    val context = LocalContext.current
    val engine = (context.applicationContext as CadenceApp).container.haptics
    val haptics = remember(view) { Haptics(engine, view) }
    val backdrop = rememberBackdropState()
    val nav = remember { NavState() }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    val today by vm.today.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val toast by vm.toast.collectAsState()
    val settings by vm.settings.collectAsState()
    val walkTargets = remember { WalkTargets() }
    val windowCapture = rememberBackdropState()
    var walkStep by remember { mutableStateOf<Int?>(null) }

    // First run, and "Show the walkthrough" in Settings (which marks it unseen again): start from Today.
    val walkthroughSeen = settings?.walkthroughSeen ?: true
    androidx.compose.runtime.LaunchedEffect(walkthroughSeen) {
        if (!walkthroughSeen && walkStep == null) {
            nav.selectTab(TabId.Today)
            sheet = null
            kotlinx.coroutines.delay(600)
            walkStep = 0
        }
    }
    // Each step shows the screen it talks about behind the blur.
    androidx.compose.runtime.LaunchedEffect(walkStep) {
        walkStep?.let { nav.selectTab(WalkSteps[it].tab) }
    }
    fun finishWalkthrough() {
        walkStep = null
        vm.setWalkthroughSeen(true)
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !colors.isDark
        controller.isAppearanceLightNavigationBars = !colors.isDark
        window.setBackgroundDrawable(ColorDrawable(colors.backdrop.toArgb()))
    }

    // Success haptic for the last beat of the day, from whichever screen it was completed on.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.events.collect { if (it is UiEvent.DayComplete) haptics.fire(HapticKind.Success) }
    }

    BackHandler(enabled = sheet != null || nav.stack.size > 1 || nav.tab != TabId.Today) {
        when {
            sheet != null -> sheet = null
            nav.stack.size > 1 -> nav.pop()
            else -> nav.selectTab(TabId.Today)
        }
    }

    val actions = remember(vm) {
        SheetActions(
            addTask = vm::addTask,
            updateTask = vm::updateTask,
            deleteTask = vm::deleteTask,
            addHabit = vm::addHabit,
            updateHabit = vm::updateHabit,
            deleteHabit = { habit -> vm.habitViews.value?.firstOrNull { it.habit.id == habit.id }?.let(vm::deleteHabit) },
            addNote = vm::addNote,
        )
    }
    val openSheet: (SheetTarget) -> Unit = { sheet = it }
    val tags = (tasks?.mapNotNull { it.tag } ?: emptyList()).distinct().sorted()

    CompositionLocalProvider(LocalHaptics provides haptics, LocalBackdrop provides backdrop, LocalWalkTargets provides walkTargets) {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(colors.backdrop).windowCapture(windowCapture) { walkStep != null }) {
            Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
                AnimatedContent(
                    targetState = nav.current,
                    transitionSpec = {
                        val push = targetState.depth > initialState.depth
                        val pop = targetState.depth < initialState.depth
                        when {
                            // Row to detail: the new screen grows into place while the old one gives way.
                            push -> ContentTransform(
                                fadeIn(motion.fade(Dur.slow, Ease.easeOut)) + scaleIn(motion.move(Dur.slow, Ease.easeOut), initialScale = 1f - 0.04f * motion.m),
                                fadeOut(motion.fade(Dur.quick, Ease.easeIn)),
                                targetContentZIndex = 1f,
                            )
                            pop -> ContentTransform(
                                fadeIn(motion.fade(Dur.quick, Ease.easeOut)),
                                fadeOut(motion.fade(Dur.base, Ease.easeIn)) + scaleOut(motion.move(Dur.base, Ease.easeIn), targetScale = 1f - 0.03f * motion.m),
                                targetContentZIndex = -1f,
                            )
                            else -> fadeIn(motion.fade(Dur.base, Ease.easeOut)) togetherWith fadeOut(motion.fade(Dur.base, Ease.easeIn))
                        }
                    },
                    label = "screens",
                ) { route ->
                    when (route) {
                        is Route.Tab -> when (route.id) {
                            TabId.Today -> TodayScreen(
                                vm,
                                onOpenSettings = { nav.push(Route.Settings) },
                                onOpenHabit = { nav.push(Route.HabitDetail(it)) },
                                onEditTask = { openSheet(SheetTarget.EditTask(it)) },
                                onAdd = { openSheet(SheetTarget.New(it)) },
                            )
                            TabId.Habits -> HabitsScreen(
                                vm,
                                onOpenHabit = { nav.push(Route.HabitDetail(it)) },
                                onNew = { openSheet(SheetTarget.New(it)) },
                            )
                            TabId.Tasks -> TasksScreen(
                                vm,
                                onEdit = { openSheet(SheetTarget.EditTask(it)) },
                                onNew = { openSheet(SheetTarget.New(it)) },
                            )
                            TabId.Notes -> NotesScreen(vm, onOpen = { nav.push(Route.NoteEditor(it)) })
                        }
                        Route.Settings -> SettingsScreen(vm, onBack = nav::pop)
                        is Route.HabitDetail -> HabitDetailScreen(
                            vm,
                            habitId = route.id,
                            onBack = nav::pop,
                            onEdit = { habit: HabitEntity -> openSheet(SheetTarget.EditHabit(habit)) },
                        )
                        is Route.NoteEditor -> NoteEditorScreen(vm, route.id, onBack = nav::pop)
                    }
                }
            }

            // Floating layers: the tab bar, the Undo toast, then the Quick add sheet above them.
            AnimatedVisibility(
                visible = nav.tab != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(motion.fade(Dur.base)),
                exit = fadeOut(motion.fade(Dur.quick)),
            ) {
                CTabBar(
                    selected = nav.tab,
                    onSelect = { nav.selectTab(it) },
                    addOpen = sheet != null,
                    onAdd = {
                        sheet = if (sheet != null) null else SheetTarget.New(
                            when (nav.tab) {
                                TabId.Habits -> AddMode.Habit
                                TabId.Notes -> AddMode.Note
                                else -> AddMode.Task
                            },
                        )
                    },
                    modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                )
            }

            var lastToast by remember { mutableStateOf<ToastState?>(null) }
            if (toast != null) lastToast = toast
            AnimatedVisibility(
                visible = toast != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(motion.fade(Dur.base)),
                exit = fadeOut(motion.fade(Dur.quick)),
            ) {
                lastToast?.let { t ->
                    CToast(
                        message = t.message,
                        token = t.token,
                        durationMs = ToastMillis,
                        onUndo = t.undo?.let { undo ->
                            {
                                haptics.fire(HapticKind.Tick)
                                vm.dismissToast()
                                undo()
                            }
                        },
                        modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    )
                }
            }

            QuickAddOverlay(
                target = sheet,
                today = today,
                tags = tags,
                actions = actions,
                onDismiss = { sheet = null },
            )
        }

        walkStep?.let { step ->
            BackHandler { finishWalkthrough() }
            WalkthroughOverlay(
                stepIndex = step,
                windowLayer = windowCapture,
                onNext = { if (step >= WalkSteps.lastIndex) finishWalkthrough() else walkStep = step + 1 },
                onSkip = { finishWalkthrough() },
            )
        }
      }
    }
}
