package com.yashraj.cadence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/**
 * A text field on glass with a 1.5dp `control-edge` outline that turns `accent` with a soft ring on focus.
 * [prefix] is a muted word at the left ("Cue", "Minimum").
 */
@Composable
fun CField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    minHeight: Dp = 56.dp,
    textStyle: TextStyle = CadenceType.bodyStrong.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(Radius.control)
    val edge by animateColorAsState(if (focused) colors.accent else colors.controlEdge, motion.fade(Dur.quick), label = "fieldEdge")
    val ring by animateColorAsState(if (focused) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent, motion.fade(Dur.quick), label = "fieldRing")
    Row(
        modifier
            .fillMaxWidth()
            .background(ring, RoundedCornerShape(Radius.control + 3.dp))
            .padding(3.dp)
            .background(colors.glass, shape)
            .border(1.5.dp, edge, shape)
            .heightIn(min = minHeight)
            .padding(horizontal = Space.s4, vertical = if (singleLine) 0.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = verticalAlignment,
    ) {
        if (prefix != null) CText(prefix, CadenceType.callout.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(500)), color = colors.inkMuted, maxLines = 1)
        Box(Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
            if (value.isEmpty()) CText(placeholder, textStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400)), color = colors.inkMuted, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = textStyle.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) imeAction else ImeAction.Default),
                keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }, onNext = { onImeAction?.invoke() }, onGo = { onImeAction?.invoke() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .semantics { contentDescription = label },
            )
        }
    }
}
