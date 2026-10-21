package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

private val YouTubeMusicRed = Color(0xFFFF2D55)
private val SpotifyGreen = Color(0xFF1ED760)

/** Two-way pill: search YouTube Music (left) or Spotify (right). */
@Composable
fun SearchSourceSwitch(
    useSpotify: Boolean,
    onSelect: (spotify: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val pillShape = CircleShape
    val tint by animateColorAsState(if (useSpotify) SpotifyGreen else YouTubeMusicRed, label = "sourceTint")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(pillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), pillShape),
    ) {
        val half = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (useSpotify) half else 0.dp,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "sourceIndicator",
        )
        // Sliding highlight under the chosen service
        Box(
            Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(half)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(pillShape)
                .background(tint.copy(alpha = 0.22f))
                .border(1.dp, tint.copy(alpha = 0.45f), pillShape),
        )
        Row(Modifier.fillMaxSize()) {
            listOf(false to stringResource(R.string.youtube_music), true to stringResource(R.string.spotify)).forEach { (spotify, label) ->
                val selected = spotify == useSpotify
                val textColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "sourceText",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(spotify)
                            }
                        },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                    )
                }
            }
        }
    }
}
