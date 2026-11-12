package com.yashraj.cadence.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.domain.SlashCommand
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButton
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.components.rememberGlassAdapt
import com.yashraj.cadence.ui.components.tappable
import com.yashraj.cadence.ui.theme.CIcon
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.IconDef
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

fun SlashCommand.icon(): IconDef = when (this) {
    SlashCommand.Text -> CadenceIcons.Type
    SlashCommand.Heading1 -> CadenceIcons.Heading1
    SlashCommand.Heading2 -> CadenceIcons.Heading2
    SlashCommand.Heading3 -> CadenceIcons.Heading3
    SlashCommand.Bullet -> CadenceIcons.BulletList
    SlashCommand.Numbered -> CadenceIcons.ListOrdered
    SlashCommand.Todo -> CadenceIcons.ListChecks
    SlashCommand.Quote -> CadenceIcons.TextQuote
    SlashCommand.Code -> CadenceIcons.SquareCode
    SlashCommand.Divider -> CadenceIcons.Minus
    SlashCommand.Bold -> CadenceIcons.Bold
    SlashCommand.Italic -> CadenceIcons.Italic
    SlashCommand.Strike -> CadenceIcons.Strike
    SlashCommand.InlineCode -> CadenceIcons.Code
    SlashCommand.Link -> CadenceIcons.Link
}

/**
 * The "/" menu: a raised glass list that sits above the format bar and narrows as you type. The first match is
 * highlighted; Enter picks it.
 */
@Composable
fun SlashMenu(commands: List<SlashCommand>, onPick: (SlashCommand) -> Unit, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(Radius.card)
    LazyColumn(
        modifier
            .fillMaxWidth()
            .heightIn(max = 264.dp)
            .glass(shape, raised = true, blurred = true, adapt = rememberGlassAdapt(canFlip = false, themeIsDark = Cadence.colors.isDark))
            .semantics { contentDescription = "Formatting commands" },
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp),
    ) {
        itemsIndexed(commands, key = { _, c -> c.name }) { index, command ->
            val rowShape = RoundedCornerShape(Radius.control)
            Row(
                Modifier
                    .fillMaxWidth()
                    .tappable({
                        haptics.fire(HapticKind.Tick)
                        onPick(command)
                    }, focusShape = rowShape, onClickLabel = command.title)
                    .then(if (index == 0) Modifier.background(colors.accentSoft, rowShape) else Modifier)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).background(colors.track, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { CIcon(command.icon(), size = 20.dp, tint = colors.ink) }
                Column(Modifier.weight(1f)) {
                    CText(command.title, CadenceType.bodyStrong, maxLines = 1)
                    CText(command.hint, CadenceType.caption, color = colors.inkMuted, maxLines = 1)
                }
            }
        }
    }
}

/** One button in the format bar. A null [onClick] makes a thin divider. */
class FormatAction(
    val icon: IconDef?,
    val label: String,
    val onClick: () -> Unit = {},
    val selected: Boolean = false,
)

/** The floating bar above the keyboard: formatting on the left, Done pinned to the right. */
@Composable
fun FormatBar(actions: List<FormatAction>, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .glass(RoundedCornerShape(Radius.pill), raised = true, blurred = true, adapt = rememberGlassAdapt(canFlip = false, themeIsDark = Cadence.colors.isDark))
            .padding(horizontal = Space.s2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            actions.forEach { a ->
                if (a.icon == null) {
                    Box(Modifier.padding(horizontal = 4.dp).size(width = 1.dp, height = 24.dp).background(colors.hairline))
                } else {
                    CIconButton(a.icon, a.label, a.onClick, glass = false, selected = a.selected)
                }
            }
        }
        CButton("Done", onDone, kind = ButtonKind.Quiet)
    }
}
