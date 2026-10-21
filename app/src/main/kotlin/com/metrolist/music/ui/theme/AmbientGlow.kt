package com.metrolist.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.metrolist.music.constants.BackgroundGlow

object GlowPalettes {
    val Aurora = listOf(Color(0xFF8A5CFF), Color(0xFFFF4D9D), Color(0xFFFFB84D))
    val Ocean = listOf(Color(0xFF2D7DFF), Color(0xFF00D1FF), Color(0xFF3DFFC5))
    val Sunset = listOf(Color(0xFFFF5E3A), Color(0xFFFF2D87), Color(0xFFFFC14D))
    val Forest = listOf(Color(0xFF1ED760), Color(0xFF00B3A6), Color(0xFFB6FF5C))

    fun forStyle(style: BackgroundGlow): List<Color>? =
        when (style) {
            BackgroundGlow.AURORA -> Aurora
            BackgroundGlow.OCEAN -> Ocean
            BackgroundGlow.SUNSET -> Sunset
            BackgroundGlow.FOREST -> Forest
            BackgroundGlow.ALBUM -> Aurora
            BackgroundGlow.OFF -> null
        }
}

/** Three bright glow colours taken from an album cover. Grey covers give a soft silver glow. */
fun Palette.glowColors(): List<Color>? {
    val swatches =
        listOfNotNull(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch, dominantSwatch, mutedSwatch, lightMutedSwatch)
            .distinctBy { it.rgb }
    if (swatches.isEmpty()) return null
    val colors = swatches.take(3).map { brighten(Color(it.rgb)) }
    return List(3) { colors[it % colors.size] }
}

private fun brighten(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    if (hsv[1] >= 0.15f) hsv[1] = hsv[1].coerceAtLeast(0.6f)
    hsv[2] = if (hsv[1] >= 0.15f) 1f else 0.85f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Soft diagonal light streaks on the black background: strongest along the top, dim through the
 * middle so text stays easy to read. Static apart from a slow colour fade when [colors] change.
 */
@Composable
fun AmbientGlow(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val first by animateColorAsState(colors.getOrElse(0) { Color.Transparent }, tween(1_500), label = "glow1")
    val second by animateColorAsState(colors.getOrElse(1) { colors.firstOrNull() ?: Color.Transparent }, tween(1_500), label = "glow2")
    val third by animateColorAsState(colors.getOrElse(2) { colors.firstOrNull() ?: Color.Transparent }, tween(1_500), label = "glow3")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        streak(first, 0.6f, Offset(w * 0.12f, h * 0.05f), length = w * 1.7f, thickness = w * 0.42f, angle = -16f)
        streak(second, 0.48f, Offset(w * 0.92f, h * 0.17f), length = w * 1.4f, thickness = w * 0.34f, angle = -16f)
        streak(third, 0.2f, Offset(w * 0.55f, h * 0.55f), length = w * 1.6f, thickness = w * 0.3f, angle = 20f)
        streak(first, 0.28f, Offset(w * 0.15f, h * 0.98f), length = w * 1.5f, thickness = w * 0.36f, angle = -10f)
    }
}

// A long soft-edged band of light: a radial glow stretched along [angle]
private fun DrawScope.streak(
    color: Color,
    alpha: Float,
    center: Offset,
    length: Float,
    thickness: Float,
    angle: Float,
) {
    if (color.alpha == 0f) return
    val radius = thickness / 2f
    withTransform({
        rotate(angle, pivot = center)
        scale(scaleX = length / thickness, scaleY = 1f, pivot = center)
    }) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    0f to color.copy(alpha = alpha),
                    0.45f to color.copy(alpha = alpha * 0.45f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
    }
}
