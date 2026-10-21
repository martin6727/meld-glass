package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.pressScale

/**
 * Previous / play-pause / next row for the full player: a round play button in the middle
 * with plain skip icons either side.
 */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    isEnded: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playColor: Color,
    playIconColor: Color,
    sideColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    playModifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        SkipButton(
            icon = R.drawable.skip_previous,
            color = sideColor,
            enabled = canSkipPrevious,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious()
            },
        )

        val playInteraction = remember { MutableInteractionSource() }
        val icon = when {
            isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
            isEnded -> R.drawable.replay
            isPlaying -> R.drawable.pause
            else -> R.drawable.play
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = playModifier
                .size(80.dp)
                .pressScale(playInteraction, 0.9f)
                .shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(playColor)
                .clickable(interactionSource = playInteraction, indication = null) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onPlayPause()
                },
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), initialScale = 0.4f) + fadeIn())
                        .togetherWith(scaleOut(targetScale = 0.4f) + fadeOut())
                },
                label = "playPauseIcon",
            ) { res ->
                Icon(
                    painter = painterResource(res),
                    contentDescription = null,
                    tint = playIconColor,
                    modifier = Modifier.size(38.dp),
                )
            }
        }

        SkipButton(
            icon = R.drawable.skip_next,
            color = sideColor,
            enabled = canSkipNext,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext()
            },
        )
    }
}

@Composable
private fun SkipButton(
    icon: Int,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A soft round highlight blooms behind the icon while it's held
    val highlight by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "skipHighlight",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(68.dp)
            .pressScale(interaction, 0.85f)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = highlight
                    scaleX = 0.6f + 0.4f * highlight
                    scaleY = scaleX
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
        )
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(40.dp),
        )
    }
}
