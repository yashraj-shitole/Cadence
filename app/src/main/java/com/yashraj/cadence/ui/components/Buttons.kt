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
import androidx.compose.ui.unit.Dp
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
    /** Corner radius; buttons nested in a rounded card pass a concentric one. */
    cornerRadius: Dp = Radius.control,
) {
    val colors = Cadence.colors
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(cornerRadius)
    val onGlass = LocalOnGlass.current
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
        // Tinted glass, not a solid fill: still lensed and lit, with enough tint to keep the label at full contrast.
        // On glass it is a vibrant fill (never a second glass); on bare ground it is tinted glass.
        ButtonKind.Primary -> if (onGlass) {
            m.vibrantFill(colors.accent.copy(alpha = (if (colors.solidSurfaces) 1f else 0.92f) * (if (enabled) 1f else 0.4f)), shape)
        } else {
            m.glass(shape, interactive = true, tint = colors.accent.copy(alpha = (if (colors.solidSurfaces) 1f else 0.84f) * (if (enabled) 1f else 0.4f)))
        }
        // On bare ground a glass button is real glass (lens, light, touch glow); on glass it is a plain fill.
        ButtonKind.Glass -> if (onGlass) m.background(colors.glassRaised, shape).border(1.dp, colors.glassEdge, shape) else m.glass(shape, raised = true, interactive = true)
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
    // Small glass reads what is behind it and flips light/dark, glyph and all, when that content turns the other way.
    if (!glass) {
        IconButtonBody(icon, label, onClick, modifier, false, tint, selected, null)
        return
    }
    val adapt = rememberGlassAdapt(canFlip = true, themeIsDark = Cadence.colors.isDark)
    GlassFlip(adapt) { IconButtonBody(icon, label, onClick, modifier, true, tint, selected, adapt) }
}

@Composable
private fun IconButtonBody(
    icon: IconDef,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    glass: Boolean,
    tint: Color,
    selected: Boolean,
    adapt: GlassAdapt?,
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
            .then(if (glass) Modifier.glass(CircleShape, interactive = true, adapt = adapt) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CIcon(icon, tint = tintColor)
    }
}

/** Full-width variant used by sheets. */
@Composable
fun CButtonBlock(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: ButtonKind = ButtonKind.Primary, enabled: Boolean = true, cornerRadius: Dp = Radius.control) {
    CButton(text, onClick, modifier.fillMaxWidth(), kind, enabled = enabled, cornerRadius = cornerRadius)
}
