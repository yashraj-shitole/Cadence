package com.yashraj.cadence.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yashraj.cadence.feel.HapticKind
import com.yashraj.cadence.ui.components.ButtonKind
import com.yashraj.cadence.ui.components.CButtonBlock
import com.yashraj.cadence.ui.components.Concentric
import com.yashraj.cadence.ui.components.LocalHaptics
import com.yashraj.cadence.ui.components.LocalOnGlass
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.components.shadowBleed
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Dur
import com.yashraj.cadence.ui.theme.Ease
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space

/** The shared look of Cadence dialogs: fades in at 96% scale on a raised glass card. */
@Composable
private fun CadenceDialogCard(title: String, body: String, onDismiss: () -> Unit, buttons: @Composable ColumnScope.() -> Unit) {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val haptics = LocalHaptics.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        haptics.fire(HapticKind.Tick)
        appear.animateTo(1f, motion.fade(Dur.base, Ease.easeOut))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalOnGlass provides true) {
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
            CText(title, CadenceType.title)
            CText(body, CadenceType.body, color = colors.inkMuted, maxLines = 8)
            Column(verticalArrangement = Arrangement.spacedBy(Space.s2), content = buttons)
        }
        }
    }
}

/** After picking a backup file in Settings: merge it in, or replace everything with it. */
@Composable
fun ImportChoiceDialog(onAdd: () -> Unit, onReplace: () -> Unit, onCancel: () -> Unit) {
    CadenceDialogCard(
        title = "Import this backup?",
        body = "Add keeps what you have and adds anything new. Replace erases everything on this phone first, then restores the backup, including your settings.",
        onDismiss = onCancel,
    ) {
        CButtonBlock("Add to my data", onAdd, kind = ButtonKind.Glass, cornerRadius = Concentric.dialogButton)
        CButtonBlock("Replace everything", onReplace, kind = ButtonKind.Danger, cornerRadius = Concentric.dialogButton)
        CButtonBlock("Cancel", onCancel, kind = ButtonKind.Quiet, cornerRadius = Concentric.dialogButton)
    }
}

/** First run on an empty install: offer to bring a previous backup back before the walkthrough. */
@Composable
fun RestoreOfferDialog(onChoose: () -> Unit, onStartFresh: () -> Unit) {
    CadenceDialogCard(
        title = "Restore from a backup?",
        body = "If you used Cadence before, choose your backup file (cadence-backup.json) to bring back your habits, tasks, notes and settings.",
        onDismiss = onStartFresh,
    ) {
        CButtonBlock("Choose backup file", onChoose, cornerRadius = Concentric.dialogButton)
        CButtonBlock("Start fresh", onStartFresh, kind = ButtonKind.Glass, cornerRadius = Concentric.dialogButton)
    }
}
