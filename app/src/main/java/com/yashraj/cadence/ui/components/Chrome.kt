package com.yashraj.cadence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.yashraj.cadence.ui.theme.LocalCadenceFlipped
import com.yashraj.cadence.ui.theme.LocalCadence
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.theme.CIcon
import com.yashraj.cadence.ui.theme.CIconSmall
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.IconDef
import com.yashraj.cadence.ui.theme.Palettes
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Scales
import com.yashraj.cadence.ui.theme.Sizes
import com.yashraj.cadence.ui.theme.Space
import com.yashraj.cadence.ui.theme.ThemeId
import kotlin.math.roundToInt

enum class TabId(val label: String, val icon: IconDef) {
    Today("Today", CadenceIcons.Today),
    Habits("Habits", CadenceIcons.Habits),
    Tasks("Tasks", CadenceIcons.Tasks),
    Notes("Notes", CadenceIcons.Notes),
}

/** The floating tab bar: Today, Habits, [Quick add], Tasks, Notes. Always the lowest layer of chrome. */
@Composable
fun CTabBar(
    selected: TabId?,
    onSelect: (TabId) -> Unit,
    addOpen: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Small floating glass reads what is behind it and flips light/dark when that content turns the other way.
    val base = Cadence.colors
    val adapt = rememberGlassAdapt(canFlip = true, themeIsDark = base.isDark)
    val colors = if (adapt.flipped && !base.solidSurfaces) LocalCadenceFlipped.current else base
    androidx.compose.runtime.CompositionLocalProvider(LocalCadence provides colors) {
        TabBarBody(selected, onSelect, addOpen, onAdd, adapt, modifier)
    }
}

@Composable
private fun TabBarBody(
    selected: TabId?,
    onSelect: (TabId) -> Unit,
    addOpen: Boolean,
    onAdd: () -> Unit,
    adapt: GlassAdapt,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val rotation by animateFloatAsState(
        targetValue = if (addOpen) 45f * minOf(motion.m, 1f) else 0f,
        animationSpec = motion.move(Dur.quick, motion.settle),
        label = "addRotation",
    )
    // The selection is a glass capsule that slides between tabs and stretches as it goes: its two edges spring at
    // different speeds, so it shape-shifts from one tab to the next instead of jumping.
    val bounds = remember { androidx.compose.runtime.mutableStateMapOf<TabId, androidx.compose.ui.geometry.Rect>() }
    val target = selected?.let { bounds[it] }
    val left = remember { androidx.compose.animation.core.Animatable(0f) }
    val right = remember { androidx.compose.animation.core.Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(target, motion.off) {
        if (target == null) return@LaunchedEffect
        if (!placed || motion.off) {
            left.snapTo(target.left)
            right.snapTo(target.right)
            placed = true
        } else {
            launch { left.animateTo(target.left, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 420f)) }
            launch { right.animateTo(target.right, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 210f)) }
        }
    }
    val tabDensity = androidx.compose.ui.platform.LocalDensity.current
    Box(
        modifier
            .fillMaxWidth()
            .height(Sizes.tabbarHeight)
            .glass(RoundedCornerShape(Radius.pill), raised = true, blurred = true, interactive = true, adapt = adapt)
            .padding(horizontal = Space.s2)
            .semantics { contentDescription = "Main" },
    ) {
        if (selected != null && placed) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(minOf(left.value, right.value).roundToInt(), 0) }
                    .size(
                        width = with(tabDensity) { kotlin.math.abs(right.value - left.value).toDp() },
                        height = Sizes.tapMin,
                    )
                    .glass(RoundedCornerShape(Radius.pill), raised = false),
            )
        }
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        TabItem(TabId.Today, selected, onSelect) { bounds[TabId.Today] = it }
        TabItem(TabId.Habits, selected, onSelect) { bounds[TabId.Habits] = it }
        Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .tappable(
                        onClick = onAdd,
                        pressScale = Scales.pressScaleSmall,
                        onPressStart = { haptics.fire(HapticKind.Light) },
                        focusShape = CircleShape,
                        settle = true,
                    )
                    .size(56.dp)
                    .walkTarget(WalkTarget.Add)
                    .accentGlow(colors.accentShadow, CircleShape)
                    .background(colors.accent, CircleShape)
                    .semantics { contentDescription = "Quick add" },
                contentAlignment = Alignment.Center,
            ) {
                CIcon(CadenceIcons.Plus, Modifier.graphicsLayer { rotationZ = rotation }, tint = colors.onAccent)
            }
        }
        TabItem(TabId.Tasks, selected, onSelect) { bounds[TabId.Tasks] = it }
        TabItem(TabId.Notes, selected, onSelect) { bounds[TabId.Notes] = it }
    }
    }
}

@Composable
private fun RowScope.TabItem(tab: TabId, selected: TabId?, onSelect: (TabId) -> Unit, onBounds: (androidx.compose.ui.geometry.Rect) -> Unit) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val isSelected = tab == selected
    val textColor by animateColorAsState(if (isSelected) colors.ink else colors.inkMuted, motion.fade(Dur.quick), label = "tabText")
    val iconColor by animateColorAsState(if (isSelected) colors.accent else colors.inkMuted, motion.fade(Dur.quick), label = "tabIcon")
    Column(
        Modifier
            .weight(1f)
            .height(Sizes.tapMin)
            .onGloballyPositioned { onBounds(it.boundsInParent()) }
            .walkTarget(
                when (tab) {
                    TabId.Today -> WalkTarget.TabToday
                    TabId.Habits -> WalkTarget.TabHabits
                    TabId.Tasks -> WalkTarget.TabTasks
                    TabId.Notes -> WalkTarget.TabNotes
                },
            )
            .tappable(
                onClick = {
                    if (!isSelected) haptics.fire(HapticKind.Tick)
                    onSelect(tab)
                },
                pressScale = Scales.pressScaleSmall,
                role = Role.Tab,
                focusShape = RoundedCornerShape(Radius.control),
            )
            .semantics { this.selected = isSelected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        CIcon(tab.icon, tint = iconColor, stroke = if (isSelected) 2.25f else 1.75f)
        CText(tab.label, CadenceType.caption.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight(600)), color = textColor, maxLines = 1)
    }
}

/**
 * The Undo toast: rises 16dp and fades in, drains over five seconds, and carries the one Undo action.
 * [token] changes for each new toast so the rise and the drain restart.
 */
@Composable
fun CToast(message: String, token: Long, durationMs: Int, onUndo: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val density = androidx.compose.ui.platform.LocalDensity.current
    val appear = remember(token) { Animatable(0f) }
    val drain = remember(token) { Animatable(1f) }
    LaunchedEffect(token) { appear.animateTo(1f, motion.fade(Dur.base, Ease.easeOut)) }
    LaunchedEffect(token) { drain.animateTo(0f, tween(durationMs, easing = LinearEasing)) }
    val shape = RoundedCornerShape(Radius.pill)
    Row(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(0, with(density) { (motion.travel(16f) * (1f - appear.value)).dp.roundToPx() }) }
            .graphicsLayer {
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                // The lens does most of the arriving; the fade only finishes it off.
                alpha = (appear.value * 2.2f).coerceAtMost(1f)
            }
            .heightIn(min = 52.dp)
            .glass(shape, raised = true, blurred = true, materialize = { appear.value }, adapt = rememberGlassAdapt(canFlip = false, themeIsDark = colors.isDark))
            .drawBehind {
                val inset = 22.dp.toPx()
                drawRect(
                    colors.accent,
                    topLeft = Offset(inset, size.height - 2.dp.toPx()),
                    size = Size((size.width - inset * 2) * drain.value, 2.dp.toPx()),
                )
            }
            .padding(start = Space.s5, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CText(message, CadenceType.callout.copy(fontWeight = FontWeight(500)), modifier = Modifier.weight(1f), maxLines = 1)
        if (onUndo != null) CButton("Undo", onUndo, kind = ButtonKind.Quiet, icon = CadenceIcons.Undo)
    }
}

/** Group heading on the bare backdrop: heading style in `ink`, with an optional count at the right. */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = Cadence.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Space.s1, end = Space.s1, top = Space.s6, bottom = Space.s2)
            .semantics { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        CText(title, CadenceType.heading, color = colors.ink)
        if (trailing != null) CText(trailing, CadenceType.caption, color = colors.ink)
    }
}

/** A settings group: one glass card with hairline-divided rows. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(Radius.card))
            .clip(RoundedCornerShape(Radius.card)),
        content = content,
    )
}

@Composable
fun GroupDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Cadence.colors.hairline))
}

/**
 * A settings row, 60dp minimum. [onClick] makes the whole row a button; [value] and a chevron sit at the right;
 * [trailing] holds a toggle or other control.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: IconDef? = null,
    value: String? = null,
    chevron: Boolean = false,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Cadence.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.tappable(onClick, focusShape = RoundedCornerShape(Radius.card)) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CIcon(icon, tint = colors.ink)
        Column(Modifier.weight(1f)) {
            CText(title, CadenceType.bodyStrong, color = if (danger) colors.danger else colors.ink)
            if (sub != null) CText(sub, CadenceType.callout, color = colors.inkMuted)
        }
        if (value != null) CText(value, CadenceType.callout, color = colors.inkMuted, maxLines = 1)
        if (chevron) CIconSmall(CadenceIcons.Forward, tint = colors.inkMuted)
        trailing?.invoke()
    }
}

/** A settings row whose control sits below its title (sliders, segmented controls, palettes). */
@Composable
fun SettingStack(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Cadence.colors
    Column(
        modifier.fillMaxWidth().padding(start = Space.s4, end = Space.s4, top = Space.s3, bottom = Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            CText(title, CadenceType.bodyStrong, color = colors.ink)
            if (note != null) CText(note, CadenceType.callout, color = colors.inkMuted, maxLines = 1)
        }
        content()
    }
}

/** A pill search field on glass. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = Cadence.colors
    val shape = RoundedCornerShape(Radius.pill)
    Row(
        modifier
            .fillMaxWidth()
            .height(Sizes.tapMin)
            .glass(shape)
            .padding(horizontal = Space.s4),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CIcon(CadenceIcons.Search, tint = colors.inkMuted)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) CText(placeholder, CadenceType.body, color = colors.inkMuted, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = CadenceType.body.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
    }
}

/** Six palette tiles: rounded squares, never circles. */
@Composable
fun PalettePicker(selectedId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Colour palette" }, verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        Palettes.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                row.forEach { palette ->
                    Swatch(palette.id, palette.displayName, palette.id == selectedId, { onSelect(palette.id) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Swatch(id: String, name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val palette = Palettes.first { it.id == id }
    val lookup = if (colors.isDark) ThemeId.Dark else ThemeId.Light
    val p = palette.colors.getValue(lookup)
    val shape = RoundedCornerShape(Radius.control)
    val tick by animateFloatAsState(if (selected) 1f else 0f, motion.fade(Dur.quick), label = "swatchTick")
    Column(
        modifier
            .heightIn(min = Sizes.tapMin)
            .tappable(
                onClick = {
                    if (!selected) haptics.fire(HapticKind.Tick)
                    onClick()
                },
                role = Role.RadioButton,
                focusShape = shape,
            )
            .semantics {
                this.selected = selected
                contentDescription = name
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(if (selected) Modifier.border(2.dp, colors.ink, RoundedCornerShape(Radius.control + 2.dp)).padding(2.dp) else Modifier)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        0f to (p.glow1 ?: Color.Gray),
                        0.62f to (p.glow2 ?: Color.Gray),
                        1f to (p.glow3 ?: Color.Gray),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .drawBehind {
                    drawRoundRect(
                        p.accent,
                        topLeft = Offset(8.dp.toPx(), size.height - 8.dp.toPx() - 10.dp.toPx()),
                        size = Size(34.dp.toPx(), 10.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                    )
                    drawRect(Brush.linearGradient(0f to colors.glassSheen, 0.45f to colors.glassSheen.copy(alpha = 0f), 1f to colors.glassSheen.copy(alpha = 0f), start = Offset.Zero, end = Offset(size.width, size.height)))
                }
                .border(if (colors.highContrast) 2.dp else 1.dp, if (colors.highContrast) colors.controlEdge else colors.glassEdge, shape),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .graphicsLayer {
                        alpha = tick
                        val s = 0.6f + 0.4f * tick
                        scaleX = s
                        scaleY = s
                    }
                    .background(colors.ink, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CIcon(CadenceIcons.Check, tint = colors.backdrop, size = 12.dp, stroke = 3f)
            }
        }
        CText(
            name,
            if (selected) CadenceType.caption.copy(fontWeight = FontWeight(650)) else CadenceType.caption.copy(fontWeight = FontWeight(500)),
            color = colors.ink,
            maxLines = 1,
        )
    }
}
