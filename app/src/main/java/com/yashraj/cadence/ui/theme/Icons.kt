package com.yashraj.cadence.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Line icons on a 24px box, drawn in the current colour with round caps and joins and no fills.
 * Geometry follows Lucide. The priority flag is the only filled icon.
 */
class IconDef(private val build: IconBuilder.() -> Unit, val filled: Boolean = false) {
    internal val paths: List<Path> by lazy { IconBuilder().apply(build).paths }
}

class IconBuilder {
    internal val paths = mutableListOf<Path>()
    fun path(d: String) { paths += PathParser().parsePathString(d).toPath() }
    fun circle(cx: Float, cy: Float, r: Float) { paths += Path().apply { addOval(Rect(Offset(cx, cy), r)) } }
}

object CadenceIcons {
    val Settings = IconDef({ path("M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4") })
    val Plus = IconDef({ path("M5 12h14M12 5v14") })
    val Back = IconDef({ path("m15 18-6-6 6-6") })
    val Forward = IconDef({ path("m9 18 6-6-6-6") })
    val More = IconDef({ path("M5 12h.01M12 12h.01M19 12h.01") })
    val Search = IconDef({ circle(11f, 11f, 8f); path("m21 21-4.3-4.3") })
    val Today = IconDef({
        circle(12f, 12f, 4f)
        path("M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41")
    })
    val Habits = IconDef({
        path("m17 2 4 4-4 4"); path("M3 11v-1a4 4 0 0 1 4-4h14"); path("m7 22-4-4 4-4"); path("M21 13v1a4 4 0 0 1-4 4H3")
    })
    val Tasks = IconDef({ path("m9 11 3 3L22 4"); path("M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11") })
    val Notes = IconDef({
        path("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"); path("M14 2v4a2 2 0 0 0 2 2h4M16 13H8M16 17H8")
    })
    val Check = IconDef({ path("M20 6 9 17l-5-5") })
    val Clock = IconDef({ circle(12f, 12f, 10f); path("M12 6v6l4 2") })
    val Timer = IconDef({ path("M10 2h4M12 14l3-3"); circle(12f, 14f, 8f) })
    val Flag = IconDef({ path("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"); path("M4 22v-7") }, filled = true)
    val Tag = IconDef({
        path("M12.6 2.6A2 2 0 0 0 11.2 2H4a2 2 0 0 0-2 2v7.2a2 2 0 0 0 .6 1.4l8.7 8.7a2.4 2.4 0 0 0 3.4 0l6.6-6.6a2.4 2.4 0 0 0 0-3.4z")
        circle(7.5f, 7.5f, 0.5f)
    })
    val Pin = IconDef({
        path("M12 17v5")
        path("M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z")
    })
    val Bell = IconDef({ path("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"); path("M10.3 21a1.94 1.94 0 0 0 3.4 0") })
    val Undo = IconDef({ path("M3 7v6h6"); path("M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13") })
    val ListChecks = IconDef({ path("m3 17 2 2 4-4M3 7l2 2 4-4M13 6h8M13 12h8M13 18h8") })
    val Trash = IconDef({
        path("M3 6h18"); path("M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"); path("M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2")
        path("M10 11v6"); path("M14 11v6")
    })
    val Close = IconDef({ path("M18 6 6 18M6 6l12 12") })
    val Type = IconDef({ path("M4 7V4h16v3M9 20h6M12 4v16") })
    val Heading1 = IconDef({ path("M4 12h8M4 18V6M12 18V6m5 6 3-2v8") })
    val Heading2 = IconDef({ path("M4 12h8M4 18V6M12 18V6M21 18h-4c0-4 4-3 4-6 0-1.5-2-2.5-4-1") })
    val Heading3 = IconDef({ path("M4 12h8M4 18V6M12 18V6M17.5 10.5c1.7-1 3.5 0 3.5 1.5a2 2 0 0 1-2 2M17 17.5c2 1.5 4 .3 4-1.5a2 2 0 0 0-2-2") })
    val BulletList = IconDef({ path("M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01") })
    val ListOrdered = IconDef({ path("M10 12h11M10 18h11M10 6h11M4 10h2M4 6h1v4M6 18H4c0-1 2-2 2-3s-1-1.5-2-1") })
    val TextQuote = IconDef({ path("M17 6H3M21 12H8M21 18H8M3 12v6") })
    val SquareCode = IconDef({
        path("m10 9-3 3 3 3"); path("m14 15 3-3-3-3")
        path("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
    })
    val Minus = IconDef({ path("M5 12h14") })
    val Bold = IconDef({ path("M6 12h9a4 4 0 0 1 0 8H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h7a4 4 0 0 1 0 8") })
    val Italic = IconDef({ path("M19 4h-9M14 20H5M15 4 9 20") })
    val Strike = IconDef({ path("M16 4H9a3 3 0 0 0-2.83 4M14 12a4 4 0 0 1 0 8H6M4 12h16") })
    val Code = IconDef({ path("m16 18 6-6-6-6M8 6l-6 6 6 6") })
    val Link = IconDef({
        path("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71")
        path("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71")
    })
    val Indent = IconDef({ path("M21 12H11M21 6H11M21 18H11"); path("m3 8 4 4-4 4") })
    val Outdent = IconDef({ path("M21 12H11M21 6H11M21 18H11"); path("m7 8-4 4 4 4") })
    val Slash = IconDef({ path("M7 21 17 3") })
    val Copy = IconDef({
        path("M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z")
        path("M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2")
    })
    val Download = IconDef({ path("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3") })
    val Edit = IconDef({
        path("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z")
        path("m15 5 4 4")
    })
}

/** One icon. [stroke] is 1.75 at 24px; icons beside text are 16px with a 2px stroke. */
@Composable
fun CIcon(
    icon: IconDef,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    stroke: Float = 1.75f,
) {
    val paths = remember(icon) { icon.paths }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { p ->
                if (icon.filled) drawPath(p, tint, style = Fill)
                drawPath(p, tint, style = style)
            }
        }
    }
}

/** The 16px variant used beside text in rows and chips. */
@Composable
fun CIconSmall(icon: IconDef, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    CIcon(icon, modifier, tint, size = 16.dp, stroke = 2f)
