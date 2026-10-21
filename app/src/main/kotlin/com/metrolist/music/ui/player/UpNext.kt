package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.ui.theme.pressScale

/** "Up next" preview under the player controls; tapping it opens the queue. */
@Composable
fun UpNextRow(
    onClick: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val windowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val windows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val next =
        remember(windowIndex, windows, shuffle) {
            runCatching {
                val player = playerConnection.player
                val index = player.nextMediaItemIndex
                if (index == C.INDEX_UNSET) null else player.getMediaItemAt(index).metadata
            }.getOrNull()
        }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(26.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressScale(interaction, 0.97f)
            .clip(shape)
            .background(textColor.copy(alpha = 0.08f))
            .border(1.dp, textColor.copy(alpha = 0.10f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        AnimatedContent(
            targetState = next,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it?.id },
            label = "upNext",
            modifier = Modifier.weight(1f),
        ) { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item?.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.up_next),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                    )
                    Text(
                        text = item?.let { listOf(it.title, it.artists.joinToString { a -> a.name }).filter(String::isNotBlank).joinToString(" • ") }
                            ?: stringResource(R.string.queue),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Icon(
            painter = painterResource(R.drawable.queue_music),
            contentDescription = stringResource(R.string.queue),
            tint = textColor.copy(alpha = 0.75f),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(22.dp),
        )
    }
}
