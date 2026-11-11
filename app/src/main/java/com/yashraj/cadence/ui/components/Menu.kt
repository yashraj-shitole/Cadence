package com.yashraj.cadence.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.yashraj.cadence.ui.theme.CIconSmall
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.IconDef
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Sizes
import com.yashraj.cadence.ui.theme.Space

class MenuItem(val label: String, val icon: IconDef, val danger: Boolean = false, val onClick: () -> Unit)

/** The "More options" menu: a small raised glass list under the top-right button. Destructive items carry a word. */
@Composable
fun ActionMenu(expanded: Boolean, onDismiss: () -> Unit, items: List<MenuItem>) {
    if (!expanded) return
    val colors = Cadence.colors
    val shape = RoundedCornerShape(Radius.card)
    // The popup is its own window, sized to its content, so it clips anything drawn outside the menu: the drop
    // shadow came out as a square. Pad the window by the shadow's reach and position the menu inside the padding.
    val bleed = shadowBleed(colors.shadows.float).dp
    val bleedPx = with(LocalDensity.current) { bleed.roundToPx() }
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
                IntOffset(
                    anchorBounds.right - popupContentSize.width + bleedPx,
                    anchorBounds.bottom + 8 - bleedPx,
                )
        },
        onDismissRequest = onDismiss,
        // The transparent padding may reach past the screen edge; the shadow there is off screen anyway.
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        Box(
            // Taps on the transparent padding dismiss like taps outside the window would.
            Modifier
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .padding(bleed),
        ) {
        Column(
            Modifier
                .width(200.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .glass(shape, raised = true)
                .padding(vertical = Space.s2),
        ) {
            items.forEach { item ->
                Row(
                    Modifier
                        .tappable({
                            onDismiss()
                            item.onClick()
                        }, focusShape = shape)
                        .heightIn(min = Sizes.tapMin)
                        .padding(horizontal = Space.s4),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CIconSmall(item.icon, tint = if (item.danger) colors.danger else colors.ink)
                    CText(item.label, CadenceType.bodyStrong, color = if (item.danger) colors.danger else colors.ink, maxLines = 1)
                }
            }
        }
        }
    }
}
