package com.yashraj.cadence.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import com.yashraj.cadence.ui.theme.Ease
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.drawscope.scale
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.ui.theme.Blur
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.ShadowLayer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample

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
 * The lens. Apple's Liquid Glass defines itself by bending light at its edges, not by blurring, so this models a
 * slab of glass whose rim is a convex "squircle" bezel: height h(x) = (1 - (1 - x)^4)^(1/4), x running from 0 at the
 * outer edge to 1 where the flat top begins. A ray coming straight down meets the surface at an angle whose tangent is
 * the slope of that profile; Snell's law at n = 1.5 bends it toward the normal, and by the time it reaches the
 * backdrop beneath it has moved sideways by (height x tan(incidence - refraction)). That shift is exactly how far
 * inward the backdrop is sampled, so the rim magnifies it like the thick edge of a lens, strongest just inside the
 * edge and easing to nothing across the bezel; red and blue are sampled a little apart for a faint colour fringe.
 * The middle of the tile is untouched. Needs Android 13; earlier versions get the blur alone.
 */
private const val RefractionAgsl = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bezel;
uniform float thickness;
uniform float amount;
uniform float dispersion;
uniform float2 origin;
uniform float gridStep;
uniform half4 gridColor;
uniform float saturation;

half4 saturate(half4 c) {
    half l = dot(c.rgb, half3(0.2126, 0.7152, 0.0722));
    return half4(mix(half3(l), c.rgb, half(saturation)), c.a);
}

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - (halfSize - float2(r));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float depth = -sdRoundRect(p, halfSize, radius);
    if (depth >= bezel || amount <= 0.0) {
        return saturate(content.eval(coord));
    }
    float x = clamp(depth / bezel, 0.0, 1.0);
    float u = 1.0 - x;
    float u4 = u * u * u * u;
    float inner = max(1.0 - u4, 1.0e-4);
    float h = pow(inner, 0.25);
    float slope = (u * u * u * pow(inner, -0.75)) * thickness / bezel;
    float theta1 = atan(slope);
    float theta2 = asin(sin(theta1) / 1.5);
    float shift = h * thickness * tan(theta1 - theta2) * amount;
    float2 n = normalize(float2(
        sdRoundRect(p + float2(1.0, 0.0), halfSize, radius) - sdRoundRect(p - float2(1.0, 0.0), halfSize, radius),
        sdRoundRect(p + float2(0.0, 1.0), halfSize, radius) - sdRoundRect(p - float2(0.0, 1.0), halfSize, radius)));
    float2 uv = coord - n * shift;
    float2 fringe = n * shift * dispersion;
    half4 c = half4(content.eval(uv - fringe).r, content.eval(uv).g, content.eval(uv + fringe).b, 1.0);
    // The screen's grid, redrawn crisp at the displaced position: the blur has already softened the real one, and
    // lines are what make a lens visible. They bend where the glass bends and fade out toward the flat middle.
    float2 s = uv + origin;
    float2 g = abs(fract((s + 1.0) / gridStep - 0.5) - 0.5) * gridStep;
    float line = 1.0 - smoothstep(0.4, 1.4, min(g.x, g.y));
    c.rgb = mix(c.rgb, gridColor.rgb, half(line * gridColor.a * (1.0 - x)));
    return saturate(c);
}
"""

/**
 * The light on the glass: a thin rim of specular light along the edges that face the light, a fainter reflection on
 * the opposite edges, a soft glow inside the lit side, and a slim shadow edge where the glass turns away (which is
 * what separates a pale tile from a pale ground). All of it comes from the shape's edge normal, so it follows any
 * corner radius. A touch adds light from within, strongest under the fingertip and spreading outward.
 */
private const val SpecularAgsl = """
uniform float2 size;
uniform float radius;
uniform float2 light;
uniform float rim;
uniform float intensity;
uniform float shadeAmount;
uniform half3 tint;
uniform half3 touchTint;
uniform half3 shadeColor;
uniform float2 touch;
uniform float touchRadius;
uniform float touchAmount;

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - (halfSize - float2(r));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float depth = -sdRoundRect(p, halfSize, radius);
    if (depth < 0.0) {
        return half4(0.0);
    }
    float2 n = normalize(float2(
        sdRoundRect(p + float2(1.0, 0.0), halfSize, radius) - sdRoundRect(p - float2(1.0, 0.0), halfSize, radius),
        sdRoundRect(p + float2(0.0, 1.0), halfSize, radius) - sdRoundRect(p - float2(0.0, 1.0), halfSize, radius)));
    float facing = dot(n, light);
    float band = exp(-depth / rim);
    float key = band * pow(max(facing, 0.0), 1.4);
    float fill = band * pow(max(-facing, 0.0), 2.0) * 0.45;
    float glow = exp(-depth / (rim * 7.0)) * max(facing, 0.0) * 0.30;
    float touchGlow = 0.0;
    if (touchAmount > 0.0) {
        float d = distance(coord, touch) / max(touchRadius, 1.0);
        float spread = exp(-d * d);
        touchGlow = touchAmount * spread * (0.42 + 1.1 * band);
    }
    float rimLight = (key + fill + glow) * intensity;
    float hi = clamp(rimLight + touchGlow, 0.0, 1.0);
    float sh = band * pow(max(-facing, 0.0), 1.6) * shadeAmount;
    float a = clamp(hi + sh, 0.0, 1.0);
    if (a <= 0.0) {
        return half4(0.0);
    }
    half3 lightColor = mix(tint, touchTint, half(touchGlow / (rimLight + touchGlow + 1.0e-4)));
    half3 color = mix(shadeColor, lightColor, half(hi / (hi + sh + 1.0e-4)));
    return half4(color * half(a), half(a));
}
"""

private class EffectCache {
    private var key: List<Float>? = null
    private var effect: androidx.compose.ui.graphics.RenderEffect? = null

    /** One shader per tile; only its numbers change, so animating the lens never recompiles anything. */
    private var shader: Any? = null

    fun get(
        w: Float, h: Float, corner: Float, sigma: Float, thickness: Float, bezel: Float, amount: Float, saturation: Float,
        origin: Offset, gridStep: Float, grid: Color,
    ): androidx.compose.ui.graphics.RenderEffect {
        val k = listOf(w, h, corner, sigma, thickness, bezel, amount, saturation, origin.x, origin.y, gridStep, grid.red, grid.green, grid.blue, grid.alpha)
        val cached = effect
        if (cached != null && k == key) return cached
        val blur = if (sigma > 0.5f) RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.MIRROR) else null
        val built = if (Build.VERSION.SDK_INT >= 33) {
            val s = (shader as? RuntimeShader) ?: RuntimeShader(RefractionAgsl).also { shader = it }
            s.setFloatUniform("size", w, h)
            s.setFloatUniform("radius", corner)
            s.setFloatUniform("bezel", bezel)
            s.setFloatUniform("thickness", thickness)
            s.setFloatUniform("amount", amount)
            s.setFloatUniform("dispersion", 0.16f)
            s.setFloatUniform("origin", origin.x, origin.y)
            s.setFloatUniform("gridStep", gridStep)
            s.setFloatUniform("gridColor", grid.red, grid.green, grid.blue, grid.alpha)
            s.setFloatUniform("saturation", saturation)
            // One pass for the lens, the grid and the saturation; a blur only where the tile asks for one.
            val lens = RenderEffect.createRuntimeShaderEffect(s, "content")
            (if (blur != null) RenderEffect.createChainEffect(lens, blur) else lens).asComposeRenderEffect()
        } else {
            RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) }),
                blur ?: RenderEffect.createBlurEffect(0.01f, 0.01f, Shader.TileMode.MIRROR),
            ).asComposeRenderEffect()
        }
        key = k
        effect = built
        return built
    }
}

/** The light-on-glass shader of one tile, set up for the current frame. Null before Android 13. */
private class SpecularCache {
    private var shader: Any? = null

    fun brush(
        w: Float, h: Float, corner: Float, light: Offset, rim: Float, intensity: Float, shadeAmount: Float,
        tint: Color, touchTint: Color, shade: Color, touch: Offset, touchRadius: Float, touchAmount: Float,
    ): Brush? {
        if (Build.VERSION.SDK_INT < 33) return null
        val s = (shader as? RuntimeShader) ?: RuntimeShader(SpecularAgsl).also { shader = it }
        s.setFloatUniform("size", w, h)
        s.setFloatUniform("radius", corner)
        s.setFloatUniform("light", light.x, light.y)
        s.setFloatUniform("rim", rim)
        s.setFloatUniform("intensity", intensity)
        s.setFloatUniform("shadeAmount", shadeAmount)
        s.setFloatUniform("tint", tint.red, tint.green, tint.blue)
        s.setFloatUniform("touchTint", touchTint.red, touchTint.green, touchTint.blue)
        s.setFloatUniform("shadeColor", shade.red, shade.green, shade.blue)
        s.setFloatUniform("touch", touch.x, touch.y)
        s.setFloatUniform("touchRadius", touchRadius)
        s.setFloatUniform("touchAmount", touchAmount)
        return ShaderBrush(s)
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
 * Liquid glass, after the material Apple introduced with iOS 26. A tile is: a lens that bends the backdrop at its
 * rim (see [RefractionAgsl]), a translucent fill, light on its edges that comes from a source in the world and moves
 * with the phone's tilt ([LocalGlassLight]) and picks up the colour of the background light behind it, a three-layer
 * inset bevel, and a drop shadow. Touching an [interactive] tile lights it from within, from the fingertip outward.
 *
 * Two kinds, as in Apple's system: [blurred] is the regular, adaptive glass that floats over scrolling content (tab bar,
 * toast, sheets), which softens what passes under it; the default is the clearer glass of rows and cards, which keeps
 * the grid legible through it. Reduce transparency and high contrast swap all of it for a solid surface. Motion Off
 * fixes the light and drops the spreading touch glow; Subtle turns the light down.
 *
 * [materialize] runs 0 to 1 for glass that appears or disappears: instead of fading, the lens and the light ramp, which
 * is how Apple's glass comes and goes.
 */
@OptIn(FlowPreview::class)
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(Radius.card),
    raised: Boolean = false,
    blurred: Boolean = false,
    interactive: Boolean = false,
    materialize: () -> Float = { 1f },
    /** A colour poured into the glass, as Apple tints a primary action: it stays glass, it is not a solid fill. */
    tint: Color = Color.Unspecified,
    /** Awareness of the content behind a floating tile: set by tiles that float over scrolling content. */
    adapt: GlassAdapt? = null,
): Modifier {
    val colors = Cadence.colors
    val motion = Cadence.motion
    val density = LocalDensity.current
    // Floating tiles (tab bar, toast, sheet) refract the whole screen behind them; rows and cards refract only the
    // scenery of their own screen, never other rows.
    val floating = LocalBackdrop.current
    val scenery = LocalScenery.current
    val lightSource = LocalGlassLight.current
    val aurora = LocalAurora.current
    val window = LocalWindowInfo.current
    val inBackdropSource = LocalInBackdropSource.current
    val backdrop = if (colors.solidSurfaces) null else if (blurred && !inBackdropSource) floating else scenery
    val photoBackdrop = LocalBackground.current
    val tileLayer = rememberGraphicsLayer()
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var position by remember { mutableStateOf(Offset.Zero) }
    val cache = remember { EffectCache() }
    val specular = remember { SpecularCache() }
    val touch = LocalGlassTouch.current
    val fill = if (raised) colors.glassRaised else colors.glass
    // Rows and cards are clear glass: the middle should look through to the ground, with the rim and lens doing the work.
    val centreFill = if (!raised && !blurred) 0.4f else 1f
    val bevel = if (raised) colors.shadows.float else colors.shadows.glass
    val sheen = colors.glassSheen
    val edge = colors.glassEdge
    val solid = colors.solidSurfaces
    val glow1 = colors.glow1
    val glow2 = colors.glow2
    val glow3 = colors.glow3
    val dark = colors.isDark
    // The lens redraws the screen grid in a stronger ink so its bending can be seen.
    val gridInk = if (photoBackdrop != null && !colors.highContrast) Color.Transparent else colors.gridLine.let { it.copy(alpha = (it.alpha * 5f).coerceIn(0.12f, 0.5f)) }
    val reduceMotion = motion.off
    val lightScale = 0.7f + 0.3f * motion.m.coerceAtMost(1f)
    // Like Apple's glass when its window loses focus, it recedes: less light, less lensing.
    val focus by animateFloatAsState(if (window.isWindowFocused) 1f else 0f, tween(320), label = "glassFocus")
    val probeLayer = rememberGraphicsLayer()
    if (adapt != null && backdrop != null) {
        // A few times a second while the backdrop is changing, read a 1/12 scale copy of what is behind this tile.
        LaunchedEffect(backdrop, adapt) {
            snapshotFlow { backdrop.version }.sample(450).collect {
                val self = coordinates
                val source = backdrop.coordinates
                if (self != null && source != null && self.isAttached && source.isAttached && self.size.width > 0 && self.size.height > 0) {
                    val relative = self.positionInRoot() - source.positionInRoot()
                    val w = (self.size.width / 12).coerceAtLeast(4)
                    val h = (self.size.height / 12).coerceAtLeast(4)
                    probeLayer.record(density, LayoutDirection.Ltr, IntSize(w, h)) {
                        scale(1f / 12f, pivot = Offset.Zero) { translate(-relative.x, -relative.y) { drawLayer(backdrop.layer) } }
                    }
                    val bitmap = probeLayer.toImageBitmap()
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.readPixels(pixels, 0, 0, bitmap.width, bitmap.height)
                    adapt.update(pixels)
                }
            }
        }
    }

    return this
        .onGloballyPositioned {
            coordinates = it
            position = it.positionInRoot()
        }
        .then(
            if (interactive && !solid && touch != null) {
                // Watches every touch without taking it, so the tile's own tap handling is untouched. A press that
                // turns into a scroll lets go of the light quickly; a tap lets it fade.
                Modifier.pointerInput(reduceMotion, touch) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            touch.press((coordinates?.positionInRoot() ?: Offset.Zero) + down.position, reduceMotion)
                            var moved = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    break
                                }
                            }
                            touch.release(moved)
                        }
                    }
                }
            } else {
                Modifier
            },
        )
        .drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            val rr = outline.toRoundRect(size)
            val shapePath = outline.toPath(size)
            val appear = materialize().coerceIn(0f, 1f)
            val minSide = minOf(size.width, size.height)
            val corner = minOf(rr.topLeftCornerRadius.x, minSide / 2f)
            // Bigger glass is a thicker slab: more lensing and softer light.
            val boost = GlassLightMath.sizeBoost(minSide, density.density)

            // Light from the background light behind this tile: it tints the glass edge light and bleeds into the shadow.
            val spill = if (solid || appear <= 0f) null else aurora?.let {
                val screen = window.containerSize
                if (screen.width > 0) {
                    val centre = position + Offset(size.width / 2f, size.height / 2f)
                    Aurora.colorNear(it.lights(), centre.x / screen.width, centre.y / screen.height, screen.height.toFloat() / screen.width, glow1, glow2, glow3)
                } else {
                    null
                }
            }

            // The shadow deepens over text and eases over a plain ground, as Apple's does.
            val shadowK = if (adapt != null && adapt.known) GlassAdaptMath.shadowScale(adapt.busyness) else 1f
            drawIntoCanvas { canvas ->
                bevel.filter { !it.inset }.forEach {
                    val mixed = if (spill == null) it.color else lerp(it.color, spill, 0.16f)
                    val shadow = ShadowLayer(false, it.x, it.y, it.blur, it.spread, mixed.copy(alpha = (it.color.alpha * shadowK).coerceAtMost(1f)))
                    dropShadow(canvas, shadow, rr, shapePath)
                }
            }
            val fillNow = if (adapt != null && adapt.known) fill.copy(alpha = (fill.alpha * GlassAdaptMath.fillScale(adapt.busyness)).coerceAtMost(0.96f)) else fill
            // Tint tones follow the brightness underneath, like coloured glass: a touch darker over light, lighter over dark.
            val ambient = if (adapt != null && adapt.known) adapt.luminance else GlassAdaptMath.luminance(lerp(colors.backdrop, spill ?: colors.backdrop, 0.4f).toArgb())
            val tintNow = if (tint == Color.Unspecified) tint else lerp(tint, if (ambient > 0.5f) Color.Black else Color.White, 0.2f * abs(ambient - 0.5f)).copy(alpha = tint.alpha)

            clipPath(shapePath) {
                if (backdrop != null) {
                    // Read so a moved tile or a redrawn source refreshes the blurred copy.
                    backdrop.version
                    position
                    val source = backdrop.coordinates
                    val self = coordinates
                    if (source != null && source.isAttached && self != null && self.isAttached) {
                        val relative = self.positionInRoot() - source.positionInRoot()
                        val bezel = minOf((if (blurred) 28f else 24f).dp.toPx() * (0.8f + 0.2f * boost), minSide * 0.45f)
                        tileLayer.renderEffect = cache.get(
                            w = size.width,
                            h = size.height,
                            corner = corner,
                            // Clear glass keeps the grid legible through rows; floating bars soften what scrolls under them.
                            sigma = if (blurred) 8f.dp.toPx() else 0f,
                            // Proportionate to the tile, so a 23dp knob gets a lens it can hold.
                            thickness = minOf((if (blurred) 46f else 42f).dp.toPx() * boost, minSide * 0.8f),
                            bezel = bezel,
                            // Quantised so a materialising tile makes a handful of lenses, not one per frame.
                            amount = (appear * (0.55f + 0.45f * focus) * 24f).roundToInt() / 24f,
                            saturation = if (raised) 1.5f else 1.4f,
                            origin = Offset(relative.x.roundToInt().toFloat(), relative.y.roundToInt().toFloat()),
                            gridStep = 32.dp.toPx(),
                            grid = gridInk,
                        )
                        tileLayer.record(this, layoutDirection, IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                            translate(-relative.x, -relative.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(tileLayer)
                    }
                }
                drawRect(fillNow.copy(alpha = fillNow.alpha * centreFill))
                if (tintNow != Color.Unspecified) drawRect(tintNow)
                if (!solid) {
                    val angle = Math.toRadians(145.0)
                    val dir = Offset(Math.sin(angle).toFloat(), -Math.cos(angle).toFloat())
                    val length = abs(size.width * dir.x) + abs(size.height * dir.y)
                    val center = Offset(size.width / 2, size.height / 2)
                    drawRect(
                        Brush.linearGradient(
                            0f to sheen.copy(alpha = sheen.alpha * centreFill),
                            0.45f to sheen.copy(alpha = 0f),
                            1f to sheen.copy(alpha = 0f),
                            start = center - dir * (length / 2),
                            end = center + dir * (length / 2),
                        ),
                    )
                }
                drawIntoCanvas { canvas ->
                    bevel.filter { it.inset }.forEach {
                        // The soft white inner glow is what frosts the middle of a row; keep the crisp rim and thin the haze.
                        val layer = if (centreFill < 1f && it.blur > 4f && it.color.alpha > 0.2f) ShadowLayer(true, it.x, it.y, it.blur, it.spread, it.color.copy(alpha = it.color.alpha * centreFill)) else it
                        insetShadow(canvas, layer, rr)
                    }
                }
                if (!solid && appear > 0f) {
                    // The light: key and fill rims from the world light, tinted by the background light behind this tile.
                    val tint = lerp(Color.White, spill ?: Color.White, 0.38f)
                    val brush = specular.brush(
                        w = size.width,
                        h = size.height,
                        corner = corner,
                        // As glass materialises the light travels around its silhouette, settling where the world light is.
                        light = GlassLightMath.rotate(lightSource.direction, (1f - appear) * 2.6f + lightSource.sweepAngle),
                        rim = 1.5.dp.toPx() * boost,
                        intensity = (if (dark) 0.62f else 1.1f) * lightScale * appear * (0.35f + 0.65f * focus),
                        shadeAmount = (if (dark) 0.34f else 0.28f) * appear,
                        tint = tint,
                        // On a pale tile white light is invisible, so a touch there lights it in the colour of the light behind it.
                        touchTint = if (dark) Color.White else lerp(Color.White, spill ?: Color(0xFF9CC9A8), 0.85f),
                        shade = if (dark) Color.Black else Color(0xFF1F293D),
                        touch = if (touch == null) Offset.Zero else touch.point - position,
                        touchRadius = 240.dp.toPx() * (0.22f + 0.78f * (touch?.spread?.value ?: 0f)),
                        touchAmount = (touch?.glow?.value ?: 0f) * (if (dark) 0.42f else 0.55f) * lightScale,
                    )
                    if (brush != null) drawRect(brush)
                }
                this@drawWithContent.drawContent()
            }
            // The 1px rim sits inside the edge: stroke 2px centred on it and keep the inner half.
            clipPath(shapePath) { drawPath(shapePath, edge, style = Stroke(width = 2.dp.toPx())) }
        }
}

/**
 * True for everything drawn inside the content that regular (floating) glass copies. A glass tile in there must never
 * copy that content: it would be copying itself, and the renderer overflows its stack. Such tiles use their screen's
 * scenery instead.
 */
val LocalInBackdropSource = compositionLocalOf { false }

/** Radii for things nested inside rounded containers: inner corner = outer corner minus the inset, so the curves are concentric. */
object Concentric {
    fun radius(outer: androidx.compose.ui.unit.Dp, inset: androidx.compose.ui.unit.Dp, min: androidx.compose.ui.unit.Dp = 10.dp): androidx.compose.ui.unit.Dp =
        (outer - inset).coerceAtLeast(min)

    /** Buttons in a dialog, which is a sheet-radius card padded by 24dp. */
    val dialogButton: androidx.compose.ui.unit.Dp get() = radius(Radius.sheet, 24.dp)

    /** Buttons in the Next up card, a sheet-radius card padded by 20dp. */
    val nextUpButton: androidx.compose.ui.unit.Dp get() = radius(Radius.sheet, 20.dp)
}

/**
 * True inside a glass surface (card, sheet, dialog, menu). Apple's rule is "avoid glass on glass": what sits on top of
 * glass uses fills, transparency and vibrancy instead of becoming a second piece of the material.
 */
val LocalOnGlass = compositionLocalOf { false }

/**
 * A translucent tint with a soft sheen, for something that sits on glass: it reads as part of the glass beneath it
 * rather than as an opaque solid or a second layer of glass.
 */
fun Modifier.vibrantFill(color: Color, shape: Shape): Modifier = drawBehind {
    val path = shape.createOutline(size, layoutDirection, this).toPath(size)
    drawPath(path, color)
    drawPath(path, Brush.verticalGradient(0f to Color.White.copy(alpha = 0.24f), 0.5f to Color.Transparent))
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
