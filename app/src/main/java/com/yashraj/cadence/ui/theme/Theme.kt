package com.yashraj.cadence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.yashraj.cadence.data.BackdropStyle
import com.yashraj.cadence.data.Settings
import com.yashraj.cadence.data.ThemeMode

/**
 * Every colour a screen may use, resolved for the active theme, palette and surface settings.
 * Components read from here and never hard-code a value.
 */
@Immutable
class CadenceColors(
    val themeId: ThemeId,
    private val base: BaseColors,
    private val palette: PaletteColors,
    val shadows: ShadowSet,
    /** Reduce transparency or high contrast: glass becomes solid surfaces and blur is removed. */
    val solidSurfaces: Boolean,
    val backdropStyle: BackdropStyle,
) {
    val isDark: Boolean = themeId == ThemeId.Dark || themeId == ThemeId.HcDark
    val highContrast: Boolean = themeId == ThemeId.HcLight || themeId == ThemeId.HcDark

    val backdrop: Color get() = base.backdrop
    val gridLine: Color get() = base.gridLine
    val scrim: Color get() = base.scrim
    val ink: Color get() = base.ink
    val inkMuted: Color get() = base.inkMuted
    val controlEdge: Color get() = base.controlEdge
    val hairline: Color get() = base.hairline
    val track: Color get() = base.track
    val danger: Color get() = base.danger
    val glassSheen: Color get() = base.glassSheen
    val surfaceSolid: Color get() = base.surfaceSolid
    val surfaceSolidRaised: Color get() = base.surfaceSolidRaised
    val focusRing: Color get() = base.ink

    val accent: Color get() = palette.accent
    val onAccent: Color get() = palette.onAccent
    val accentSoft: Color get() = palette.accentSoft
    val rest: Color get() = palette.rest
    val restSoft: Color get() = palette.restSoft

    val glass: Color get() = if (solidSurfaces) base.surfaceSolid else base.glass.clearer()
    val glassRaised: Color get() = if (solidSurfaces) base.surfaceSolidRaised else base.glassRaised.clearer()

    /** Rim of a glass tile: a 3:1 outline in high contrast, a hairline on solid surfaces, a bright edge on glass. */
    val glassEdge: Color get() = when {
        highContrast -> base.glassEdge
        solidSurfaces -> base.hairline
        else -> base.glassEdge
    }

    val accentShadow: List<ShadowLayer> get() = palette.accentShadow ?: shadows.accent

    val showGlows: Boolean get() = !highContrast && backdropStyle != BackdropStyle.Plain
    val glowAlpha: Float get() = when {
        backdropStyle == BackdropStyle.Soft -> 0.45f
        isDark -> 0.75f
        else -> 0.9f
    }
    val glow1: Color get() = palette.glow1 ?: base.glow1
    val glow2: Color get() = palette.glow2 ?: base.glow2
    val glow3: Color get() = palette.glow3 ?: base.glow3
}

/**
 * How much of the token glass fill is kept. The tokens are frosted; this makes the tiles clear glass, so the
 * backdrop shows through and the edge refraction (see `glass()`) is what separates a tile from the ground.
 * Solid surfaces (Reduce transparency, high contrast) ignore it.
 */
const val GlassFillScale = 0.5f

private fun Color.clearer() = copy(alpha = alpha * GlassFillScale)

fun resolveColors(settings: Settings, systemDark: Boolean): CadenceColors {
    val dark = when (settings.theme) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Auto -> systemDark
    }
    val id = when {
        settings.highContrast -> if (dark) ThemeId.HcDark else ThemeId.HcLight
        else -> if (dark) ThemeId.Dark else ThemeId.Light
    }
    val palette = (Palettes.firstOrNull { it.id == settings.palette } ?: Palettes.first { it.id == DefaultPaletteId }).colors.getValue(id)
    return CadenceColors(
        themeId = id,
        base = BaseThemes.getValue(id),
        palette = palette,
        shadows = ShadowThemes.getValue(id),
        solidSurfaces = settings.reduceTransparency || settings.highContrast,
        backdropStyle = settings.backdrop,
    )
}

val LocalCadence = compositionLocalOf<CadenceColors> { error("CadenceTheme is missing") }

/** The same palette and settings in the opposite theme, for small glass that flips to stay legible over contrasting content. */
val LocalCadenceFlipped = compositionLocalOf<CadenceColors> { error("CadenceTheme is missing") }
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/** Shorthand used by every component: `Cadence.colors.accent`. */
object Cadence {
    val colors: CadenceColors @Composable get() = LocalCadence.current
    val motion: Motion @Composable get() = LocalMotion.current
}

/**
 * [systemReduceMotion] is the OS setting; it always wins over the in-app Motion level.
 */
@Composable
fun CadenceTheme(settings: Settings, systemReduceMotion: Boolean, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val colors = remember(settings.theme, settings.palette, settings.highContrast, settings.reduceTransparency, settings.backdrop, systemDark) {
        resolveColors(settings, systemDark)
    }
    val flipped = remember(settings.theme, settings.palette, settings.highContrast, settings.reduceTransparency, settings.backdrop, systemDark) {
        resolveColors(settings.copy(theme = if (colors.isDark) ThemeMode.Light else ThemeMode.Dark), systemDark)
    }
    val motion = remember(settings.motion, systemReduceMotion) { Motion.forLevel(if (systemReduceMotion) 0 else settings.motion) }
    CompositionLocalProvider(
        LocalCadence provides colors,
        LocalCadenceFlipped provides flipped,
        LocalMotion provides motion,
        LocalContentColor provides colors.ink,
        content = content,
    )
}
