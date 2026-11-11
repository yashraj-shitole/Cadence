package com.yashraj.cadence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.theme.CIconSmall
import com.yashraj.cadence.ui.theme.CIcon
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.IconDef
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Scales
import com.yashraj.cadence.ui.theme.Sizes
import com.yashraj.cadence.ui.theme.Space

enum class ButtonKind { Primary, Glass, Quiet, Danger }

/**
 * Buttons are verbs that name the result: "Mark done", "Add task", "Later". One accent-filled [ButtonKind.Primary]
 * per screen. The primary button gives a light haptic on touch-down.
 */
@Composable
fun CButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: IconDef? = null,
    enabled: Boolean = true,
) {
    val colors = Cadence.colors
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(Radius.control)
    val contentColor = when (kind) {
        ButtonKind.Primary -> colors.onAccent
        ButtonKind.Glass -> colors.ink
        ButtonKind.Quiet -> colors.accent
        ButtonKind.Danger -> colors.danger
    }
    val horizontal = if (kind == ButtonKind.Quiet || kind == ButtonKind.Danger) Space.s3 else Space.s5
    var m = modifier.tappable(
        onClick = onClick,
        onPressStart = if (kind == ButtonKind.Primary) ({ haptics.fire(HapticKind.Light) }) else null,
        enabled = enabled,
        focusShape = shape,
    )
    if (kind == ButtonKind.Primary) m = m.accentGlow(colors.accentShadow, shape)
    m = when (kind) {
        ButtonKind.Primary -> m.background(if (enabled) colors.accent else colors.accent.copy(alpha = 0.4f), shape)
        ButtonKind.Glass -> m.background(colors.glassRaised, shape).border(1.dp, colors.glassEdge, shape)
        else -> m
    }
    Row(
        m
            .heightIn(min = Sizes.tapMin)
            .padding(horizontal = horizontal),
        horizontalArrangement = Arrangement.spacedBy(Space.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CIconSmall(icon, tint = contentColor)
        CText(text, CadenceType.bodyStrong, color = contentColor, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** A 48dp round icon button. Allowed only in the top bar and floating bars, and always labelled. */
@Composable
fun CIconButton(
    icon: IconDef,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: Boolean = true,
    tint: Color = Color.Unspecified,
    selected: Boolean = false,
) {
    val colors = Cadence.colors
    val haptics = LocalHaptics.current
    val tintColor = if (tint == Color.Unspecified) (if (selected) colors.accent else colors.ink) else tint
    Box(
        modifier
            .tappable(
                onClick = onClick,
                pressScale = Scales.pressScaleSmall,
                onPressStart = { haptics.fire(HapticKind.Light) },
                focusShape = CircleShape,
            )
            .size(Sizes.tapMin)
            .then(if (glass) Modifier.glass(CircleShape) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CIcon(icon, tint = tintColor)
    }
}

/** Full-width variant used by sheets. */
@Composable
fun CButtonBlock(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: ButtonKind = ButtonKind.Primary, enabled: Boolean = true) {
    CButton(text, onClick, modifier.fillMaxWidth(), kind, enabled = enabled)
}
