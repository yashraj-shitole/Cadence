package com.yashraj.cadence.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yashraj.cadence.R

/** Geist for text, Bricolage Grotesque for headings and numbers. Both are bundled variable fonts. */
private val GeistFamily = FontFamily(
    listOf(400, 450, 500, 600, 650, 700).map { w ->
        Font(R.font.geist, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun bricolage(weight: Int, size: Float) = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.opticalSizing(size.sp)),
    ),
)

private fun TypeSpec.style(tabular: Boolean = false) = TextStyle(
    fontFamily = if (display) bricolage(weight, size) else GeistFamily,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = letterSpacing.em,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** Handwriting, used only for the walkthrough's notes in the margin. */
private val HandFamily = FontFamily(
    listOf(500, 600, 700).map { w ->
        Font(R.font.caveat, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** The type scale from tokens.json. Counts and times use tabular numerals. */
object CadenceType {
    val numeral = TypeSpecs.numeral.style(tabular = true)
    val display = TypeSpecs.display.style()
    val title = TypeSpecs.title.style()
    val heading = TypeSpecs.heading.style()
    val body = TypeSpecs.body.style()
    val bodyStrong = TypeSpecs.bodyStrong.style()
    val callout = TypeSpecs.callout.style()
    val caption = TypeSpecs.caption.style(tabular = true)
    val label = TypeSpecs.label.style(tabular = true)

    /** Walkthrough headline, handwritten. */
    val handTitle = TextStyle(fontFamily = HandFamily, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight(700))

    /** Walkthrough note, handwritten. */
    val handBody = TextStyle(fontFamily = HandFamily, fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight(600))
}

/** Plain text in the Cadence scale. Colour defaults to the surrounding content colour. */
@Composable
fun CText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) style.copy(color = color, textAlign = textAlign) else style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}
