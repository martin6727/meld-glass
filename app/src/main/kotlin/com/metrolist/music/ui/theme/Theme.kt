/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.metrolist.music.R
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun figtree(weight: FontWeight) =
    Font(R.font.figtree, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)

private val AppTypography = Typography().let { t ->
    fun TextStyle.f(weight: FontWeight? = null, tracking: Float? = null) = copy(
        fontFamily = Figtree,
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking?.sp ?: letterSpacing,
    )
    t.copy(
        displayLarge = t.displayLarge.f(FontWeight.ExtraBold, -1f),
        displayMedium = t.displayMedium.f(FontWeight.ExtraBold, -0.8f),
        displaySmall = t.displaySmall.f(FontWeight.Bold, -0.5f),
        headlineLarge = t.headlineLarge.f(FontWeight.Bold, -0.5f),
        headlineMedium = t.headlineMedium.f(FontWeight.Bold, -0.4f),
        headlineSmall = t.headlineSmall.f(FontWeight.Bold, -0.3f),
        titleLarge = t.titleLarge.f(FontWeight.Bold, -0.3f),
        titleMedium = t.titleMedium.f(FontWeight.SemiBold),
        titleSmall = t.titleSmall.f(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.f(),
        bodyMedium = t.bodyMedium.f(),
        bodySmall = t.bodySmall.f(),
        labelLarge = t.labelLarge.f(FontWeight.SemiBold),
        labelMedium = t.labelMedium.f(FontWeight.SemiBold),
        labelSmall = t.labelSmall.f(FontWeight.Medium),
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

// OLED: true black base with near-black layered surfaces (Netflix / YT Music style)
fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF26262A),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0C),
        surfaceContainer = Color(0xFF111114),
        surfaceContainerHigh = Color(0xFF18181C),
        surfaceContainerHighest = Color(0xFF202025),
        surfaceVariant = Color(0xFF1C1C21),
        onSurface = Color(0xFFF5F5F7),
        onBackground = Color(0xFFF5F5F7),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outlineVariant = Color(0xFF2A2A30),
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
