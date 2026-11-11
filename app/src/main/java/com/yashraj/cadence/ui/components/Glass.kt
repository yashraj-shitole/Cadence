package com.yashraj.cadence.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.ui.theme.Blur
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.ShadowLayer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The screen content behind floating glass (tab bar, toast, sheet). It is recorded into [layer] and each
 * floating tile draws a blurred copy of the part of it that sits behind the tile.
 */
@Stable
class BackdropState(val layer: GraphicsLayer) {
    /** Bumped each time the source redraws, so blurred tiles redraw one frame later. */
    var version by mutableIntStateOf(0)
        private set
    var coordinates: LayoutCoordinates? = null
    private var counter = 0

    fun bump() {
        version = ++counter
    }
}

@Composable
fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

val LocalBackdrop = compositionLocalOf<BackdropState?> { null }

/** Marks the content that floating glass blurs. Place floating tiles outside it, drawn after it. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.coordinates = it }
    .drawWithContent {
        state.layer.record { this@drawWithContent.drawContent() }
        drawLayer(state.layer)
        state.bump()
    }

/**
 * Edge refraction for a clear glass tile, as a shader over the tile's copy of the backdrop. Near the rim the
 * backdrop is sampled from further inside, so it is magnified like the thick edge of a lens; red and blue are
 * sampled slightly apart for a faint colour fringe; and light catches the bevel, brighter at the upper left and
 * darker at the lower right. The middle of the tile is untouched. Needs Android 13; earlier versions get the blur.
 */
private const val RefractionAgsl = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bezel;
uniform float strength;
uniform float dispersion;

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - (halfSize - float2(r));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float depth = -sdRoundRect(p, halfSize, radius);
    if (depth >= bezel) {
        return content.eval(coord);
    }
    float t = clamp(1.0 - depth / bezel, 0.0, 1.0);
    float k = t * t;
    float2 n = normalize(float2(
        sdRoundRect(p + float2(1.0, 0.0), halfSize, radius) - sdRoundRect(p - float2(1.0, 0.0), halfSize, radius),
        sdRoundRect(p + float2(0.0, 1.0), halfSize, radius) - sdRoundRect(p - float2(0.0, 1.0), halfSize, radius)));
    float2 uv = coord - n * strength * k;
    float2 fringe = n * dispersion * k;
    half4 c = half4(content.eval(uv - fringe).r, content.eval(uv).g, content.eval(uv + fringe).b, 1.0);
    float lit = dot(n, normalize(float2(-0.55, -0.83)));
    float glow = max(lit, 0.0) * k * k * 0.20;
    float shade = max(-lit, 0.0) * k * k * 0.10;
    c.rgb = c.rgb + half3(glow) - half3(shade);
    return c;
}
"""

private class EffectCache {
    private var key: List<Float>? = null
    private var effect: androidx.compose.ui.graphics.RenderEffect? = null

    fun get(w: Float, h: Float, corner: Float, sigma: Float, strength: Float, bezel: Float, saturation: Float): androidx.compose.ui.graphics.RenderEffect {
        val k = listOf(w, h, corner, sigma, strength, bezel, saturation)
        val cached = effect
        if (cached != null && k == key) return cached
        val blur = RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.MIRROR)
        val refracted = if (Build.VERSION.SDK_INT >= 33) {
            val shader = RuntimeShader(RefractionAgsl)
            shader.setFloatUniform("size", w, h)
            shader.setFloatUniform("radius", corner)
            shader.setFloatUniform("bezel", bezel)
            shader.setFloatUniform("strength", strength)
            shader.setFloatUniform("dispersion", strength * 0.14f)
            RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"), blur)
        } else {
            blur
        }
        val built = RenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) }),
            refracted,
        ).asComposeRenderEffect()
        key = k
        effect = built
        return built
    }
}

/** Grows (positive) or shrinks (negative) a rounded rect, moving the corner radii with the edges. */
private fun RoundRect.grow(delta: Float): RoundRect {
    fun CornerRadius.adjust() = CornerRadius((x + delta).coerceAtLeast(0f), (y + delta).coerceAtLeast(0f))
    return RoundRect(
        left - delta, top - delta, right + delta, bottom + delta,
        topLeftCornerRadius.adjust(), topRightCornerRadius.adjust(), bottomRightCornerRadius.adjust(), bottomLeftCornerRadius.adjust(),
    )
}

internal fun Outline.toPath(size: androidx.compose.ui.geometry.Size): Path = when (this) {
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Generic -> path
    else -> Path().apply { addRect(Rect(Offset.Zero, size)) }
}

internal fun Outline.toRoundRect(size: androidx.compose.ui.geometry.Size): RoundRect = when (this) {
    is Outline.Rounded -> roundRect
    is Outline.Rectangle -> RoundRect(rect)
    is Outline.Generic -> RoundRect(path.getBounds())
    else -> RoundRect(Rect(Offset.Zero, size))
}

/** A CSS box-shadow blur is 2 sigma; Skia converts a mask radius with sigma = 0.57735 * radius + 0.5. */
private fun maskRadius(cssBlurPx: Float): Float = ((cssBlurPx / 2f - 0.5f) / 0.57735f).coerceAtLeast(0.01f)

private fun shadowPaint(color: Color, blurPx: Float) = Paint().apply {
    this.color = color
    isAntiAlias = true
    if (blurPx > 0f) asFrameworkPaint().maskFilter = BlurMaskFilter(maskRadius(blurPx), BlurMaskFilter.Blur.NORMAL)
}

private fun DrawScope.dropShadow(canvas: Canvas, layer: ShadowLayer, rr: RoundRect, shapePath: Path) {
    val spread = layer.spread.dp.toPx()
    val shadow = rr.grow(spread).translate(Offset(layer.x.dp.toPx(), layer.y.dp.toPx()))
    val path = Path().apply { addRoundRect(shadow) }
    canvas.save()
    canvas.clipPath(shapePath, ClipOp.Difference)
    canvas.drawPath(path, shadowPaint(layer.color, layer.blur.dp.toPx()))
    canvas.restore()
}

/**
 * How far (dp) the drop shadows in [layers] reach past the tile in any direction. A tile in its own window, such as a
 * popup, must be padded by this much or the window clips the shadow to a square.
 */
fun shadowBleed(layers: List<ShadowLayer>): Float =
    layers.filter { !it.inset }.maxOfOrNull { it.blur + it.spread + maxOf(abs(it.x), abs(it.y)) }?.coerceAtLeast(0f) ?: 0f

private fun DrawScope.insetShadow(canvas: Canvas, layer: ShadowLayer, rr: RoundRect) {
    val spread = layer.spread.dp.toPx()
    val hole = rr.grow(-spread).translate(Offset(layer.x.dp.toPx(), layer.y.dp.toPx()))
    val pad = layer.blur.dp.toPx() * 2 + abs(layer.y.dp.toPx()) + abs(layer.x.dp.toPx()) + 64f
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(rr.left - pad, rr.top - pad, rr.right + pad, rr.bottom + pad))
        addRoundRect(hole)
    }
    canvas.drawPath(path, shadowPaint(layer.color, layer.blur.dp.toPx()))
}

/**
 * A frosted, softly moulded tile: translucent fill, 145 degree sheen, 1px rim, three-layer inset bevel, and
 * (for floating tiles) backdrop blur with saturate. Reduce transparency and high contrast swap the fill for a
 * solid surface and drop the blur and sheen.
 *
 * [blurred] only does something for tiles that float over scrolling content and when a [LocalBackdrop] exists.
 * Rows and cards sit on the glowing backdrop, where blur would change nothing you could see.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(Radius.card),
    raised: Boolean = false,
    blurred: Boolean = false,
): Modifier {
    val colors = Cadence.colors
    val density = LocalDensity.current
    // Floating tiles (tab bar, toast, sheet) refract the whole screen behind them; rows and cards refract only the
    // scenery of their own screen, never other rows.
    val floating = LocalBackdrop.current
    val scenery = LocalScenery.current
    val backdrop = if (colors.solidSurfaces) null else if (blurred) floating else scenery
    val tileLayer = rememberGraphicsLayer()
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var position by remember { mutableStateOf(Offset.Zero) }
    val cache = remember { EffectCache() }
    val fill = if (raised) colors.glassRaised else colors.glass
    val bevel = if (raised) colors.shadows.float else colors.shadows.glass
    val sheen = colors.glassSheen
    val edge = colors.glassEdge
    val solid = colors.solidSurfaces

    return this
        .onGloballyPositioned {
            coordinates = it
            position = it.positionInRoot()
        }
        .drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            val rr = outline.toRoundRect(size)
            val shapePath = outline.toPath(size)

            drawIntoCanvas { canvas -> bevel.filter { !it.inset }.forEach { dropShadow(canvas, it, rr, shapePath) } }

            clipPath(shapePath) {
                if (backdrop != null) {
                    // Read so a moved tile or a redrawn source refreshes the blurred copy.
                    backdrop.version
                    position
                    val source = backdrop.coordinates
                    val self = coordinates
                    if (source != null && source.isAttached && self != null && self.isAttached) {
                        val relative = self.positionInRoot() - source.positionInRoot()
                        val minSide = minOf(size.width, size.height)
                        val bezel = minOf((if (blurred) 20f else 16f).dp.toPx(), minSide * 0.45f)
                        tileLayer.renderEffect = cache.get(
                            w = size.width,
                            h = size.height,
                            corner = minOf(rr.topLeftCornerRadius.x, minSide / 2f),
                            // Clear glass keeps the grid legible through rows; floating bars soften what scrolls under them.
                            sigma = (if (blurred) 10f else 2.5f).dp.toPx(),
                            strength = (if (blurred) 13f else 10f).dp.toPx(),
                            bezel = bezel,
                            saturation = if (raised) 1.5f else 1.4f,
                        )
                        tileLayer.record(this, layoutDirection, IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                            translate(-relative.x, -relative.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(tileLayer)
                    }
                }
                drawRect(fill)
                if (!solid) {
                    val angle = Math.toRadians(145.0)
                    val dir = Offset(Math.sin(angle).toFloat(), -Math.cos(angle).toFloat())
                    val length = abs(size.width * dir.x) + abs(size.height * dir.y)
                    val center = Offset(size.width / 2, size.height / 2)
                    drawRect(
                        Brush.linearGradient(
                            0f to sheen,
                            0.45f to sheen.copy(alpha = 0f),
                            1f to sheen.copy(alpha = 0f),
                            start = center - dir * (length / 2),
                            end = center + dir * (length / 2),
                        ),
                    )
                }
                drawIntoCanvas { canvas -> bevel.filter { it.inset }.forEach { insetShadow(canvas, it, rr) } }
                this@drawWithContent.drawContent()
            }
            // The 1px rim sits inside the edge: stroke 2px centred on it and keep the inner half.
            clipPath(shapePath) { drawPath(shapePath, edge, style = Stroke(width = 2.dp.toPx())) }
        }
}

/** Shadow under the primary button and the centre add button: tinted by the palette. */
fun Modifier.accentGlow(layers: List<ShadowLayer>, shape: Shape): Modifier = drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, this)
    val rr = outline.toRoundRect(size)
    val path = outline.toPath(size)
    drawIntoCanvas { canvas -> layers.forEach { dropShadow(canvas, it, rr, path) } }
    drawContent()
}

/** Shadow for toggle knobs, slider thumbs and the segmented thumb. */
fun Modifier.thumbShadow(layers: List<ShadowLayer>, shape: Shape): Modifier = accentGlow(layers, shape)

/**
 * While [enabled], records everything inside into [state] as well as drawing it, so the walkthrough can show a
 * blurred copy with one control left sharp. When it is off this costs nothing.
 */
fun Modifier.windowCapture(state: BackdropState, enabled: () -> Boolean): Modifier = this.drawWithContent {
    if (enabled()) {
        state.layer.record { this@drawWithContent.drawContent() }
        drawLayer(state.layer)
        state.bump()
    } else {
        drawContent()
    }
}
