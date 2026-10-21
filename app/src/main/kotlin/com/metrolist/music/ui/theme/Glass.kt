package com.metrolist.music.ui.theme

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/** The app content behind the floating glass bars, recorded once per frame. */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

object GlassDefaults {
    val Tint = Color(0xFF0B0B0F).copy(alpha = 0.42f)
    val BarTint = Color(0xFF000000).copy(alpha = 0.55f)
    val Blur = 14.dp
}

/**
 * Liquid glass surface: blurs and bends whatever is behind it, with a light rim and soft shadow.
 * [refraction] needs a rounded shape (RoundedCornerShape / CircleShape).
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color = GlassDefaults.Tint,
    blurRadius: Dp = GlassDefaults.Blur,
    refraction: Boolean = true,
    shadow: Boolean = true,
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    if (backdrop == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this
            .clip(shape)
            .background(tint.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (refraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = 14.dp.toPx(),
                    refractionAmount = 26.dp.toPx(),
                    depthEffect = true,
                )
            }
        },
        highlight = { Highlight.Default.copy(alpha = 0.55f) },
        shadow = if (shadow) {
            { Shadow(radius = 22.dp, color = Color.Black.copy(alpha = 0.45f)) }
        } else {
            null
        },
        onDrawSurface = { drawRect(tint) },
    ).clip(shape)
}

/** Springy shrink while pressed, used on glass buttons. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.9f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
