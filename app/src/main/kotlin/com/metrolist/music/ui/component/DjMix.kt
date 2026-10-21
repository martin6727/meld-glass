package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.theme.pressScale
import kotlin.math.PI
import kotlin.math.sin

private val DjGradient = listOf(Color(0xFF8A5CFF), Color(0xFFFF4D9D), Color(0xFFFFB84D), Color(0xFF8A5CFF))

/** "DJ Mix" pill for playlist and album headers: starts the list with beat-matched transitions. */
@Composable
fun DjMixButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val motion = rememberInfiniteTransition(label = "djButton")
    val sweep by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing), RepeatMode.Restart),
        label = "djSweep",
    )
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "djSpin",
    )
    val contentColor = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(44.dp)
            .pressScale(interaction, 0.94f)
            .clip(CircleShape)
            .drawBehind {
                // A slow colour sweep along the pill and its rim
                val travel = size.width * 2f
                val start = Offset(-size.width + travel * sweep, 0f)
                val brush = Brush.linearGradient(DjGradient, start, start + Offset(size.width, size.height), TileMode.Mirror)
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(brush, cornerRadius = radius, alpha = 0.22f)
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    brush,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = radius,
                    style = Stroke(stroke),
                )
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.dj_mix),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = spin },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.dj_mix),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

/** "DJ Mix" toggle for the player: glowing when on, dim when off. Tap to switch. */
@Composable
fun DjMixStatusChip(
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val enabled by playerConnection.djMixEnabled.collectAsStateWithLifecycle()
    val mixing by playerConnection.djMixing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onText = stringResource(R.string.dj_mix_on)
    val offText = stringResource(R.string.dj_mix_off)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .drawBehind {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                if (enabled) {
                    drawRoundRect(Brush.horizontalGradient(DjGradient), cornerRadius = radius, alpha = if (mixing) 0.45f else 0.28f)
                } else {
                    drawRoundRect(
                        contentColor.copy(alpha = 0.35f),
                        topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                        size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                        cornerRadius = radius,
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
            .clickable {
                val turnOn = !enabled
                playerConnection.setDjMixEnabled(turnOn)
                Toast.makeText(context, if (turnOn) onText else offText, Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 12.dp),
    ) {
        if (enabled) {
            EqualizerBars(color = contentColor, lively = mixing, modifier = Modifier.size(14.dp))
        } else {
            Icon(
                painter = painterResource(R.drawable.dj_mix),
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(if (mixing) R.string.dj_mix_mixing else R.string.dj_mix),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.7f),
        )
    }
}

/** Three bouncing bars; they move faster while a mix is happening. */
@Composable
private fun EqualizerBars(
    color: Color,
    lively: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = rememberInfiniteTransition(label = "djBars")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (lively) 700 else 1_400, easing = LinearEasing), RepeatMode.Restart),
        label = "djBarsPhase",
    )
    Canvas(modifier) {
        val barWidth = size.width / 5
        for (i in 0 until 3) {
            val level = 0.35f + 0.65f * (0.5f + 0.5f * sin(phase + i * 2.1f))
            val height = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * barWidth * 2, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}
