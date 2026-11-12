package com.yashraj.cadence.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButton
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.components.shadowBleed
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/** "A new version is out." Shown at start-up and from Settings; each button names its result. */
@Composable
fun UpdateDialog(version: String, current: String, onLater: () -> Unit, onDownload: () -> Unit) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        haptics.fire(HapticKind.Tick)
        appear.animateTo(1f, motion.fade(Dur.base, Ease.easeOut))
    }
    Dialog(onDismissRequest = onLater, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(vertical = shadowBleed(colors.shadows.float).dp)
                .padding(horizontal = Space.s6)
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    alpha = appear.value
                    val scale = 1f - 0.04f * motion.m * (1f - appear.value)
                    scaleX = scale
                    scaleY = scale
                }
                // A dialog is its own window, so nothing behind it is blurred: give the glass an opaque base so the screen below cannot show through the text.
                .background(colors.surfaceSolidRaised, RoundedCornerShape(Radius.sheet))
                .glass(RoundedCornerShape(Radius.sheet), raised = true, materialize = { appear.value })
                .padding(Space.s6),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            CText("Cadence $version is out", CadenceType.title)
            CText(
                "You have $current. Download opens the release page in your browser, where you can install the new version over this one. Your habits, tasks and notes stay as they are.",
                CadenceType.body,
                color = colors.inkMuted,
                maxLines = 6,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s2, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                CButton("Later", onLater, kind = ButtonKind.Glass)
                CButton("Download", onDownload)
            }
        }
    }
}
