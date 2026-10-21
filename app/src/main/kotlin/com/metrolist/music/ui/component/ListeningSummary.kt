package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.SongWithStats
import com.metrolist.music.viewmodels.ListeningSummary
import java.text.NumberFormat

private val SummaryGradient = listOf(Color(0xFF8A5CFF), Color(0xFFFF4D9D), Color(0xFFFFB84D))

/** Headline listening numbers for the selected period, shown at the top of the stats screen. */
@Composable
fun ListeningSummaryHeader(
    summary: ListeningSummary?,
    topSong: SongWithStats?,
    topArtist: Artist?,
    modifier: Modifier = Modifier,
) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val minutes by animateIntAsState(
        targetValue = (summary?.minutes ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(900),
        label = "summaryMinutes",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Minutes listened, with a day-by-day chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .background(Brush.linearGradient(SummaryGradient.map { it.copy(alpha = 0.16f) }))
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.minutes_listened),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = numbers.format(minutes),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.minutes_short),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            val total = summary?.minutes ?: 0L
            if (total >= 60) {
                Text(
                    text = stringResource(R.string.hours_and_minutes, total / 60, total % 60),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val days = summary?.dailyMinutes.orEmpty()
            if (days.size >= 2) {
                Spacer(Modifier.height(16.dp))
                DailyBars(days = days, modifier = Modifier.fillMaxWidth().height(64.dp))
            }
        }

        // Plays, songs, artists
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(stringResource(R.string.plays_label), numbers.format(summary?.plays ?: 0), Modifier.weight(1f))
            StatTile(stringResource(R.string.songs), numbers.format(summary?.songs ?: 0), Modifier.weight(1f))
            StatTile(stringResource(R.string.artists), numbers.format(summary?.artists ?: 0), Modifier.weight(1f))
        }

        if (topSong != null) {
            TopRow(
                label = stringResource(R.string.top_song),
                title = topSong.title,
                subtitle = listOfNotNull(
                    topSong.artistName,
                    stringResource(
                        R.string.plays_and_minutes,
                        topSong.songCountListened,
                        ((topSong.timeListened ?: 0L) / 60_000L).toInt(),
                    ),
                ).joinToString(" • "),
                imageUrl = topSong.thumbnailUrl,
                round = false,
            )
        }
        if (topArtist != null) {
            TopRow(
                label = stringResource(R.string.top_artist),
                title = topArtist.artist.name,
                subtitle = stringResource(
                    R.string.plays_and_minutes,
                    topArtist.songCount,
                    ((topArtist.timeListened ?: 0) / 60_000),
                ),
                imageUrl = topArtist.artist.thumbnailUrl,
                round = true,
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopRow(
    label: String,
    title: String,
    subtitle: String,
    imageUrl: String?,
    round: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(if (round) CircleShape else RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Minutes per day as rounded bars; days without listening show as gaps. Last 30 days at most. */
@Composable
private fun DailyBars(
    days: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
) {
    val filled = remember(days) {
        val byDay = days.toMap()
        val last = days.maxOf { it.first }
        val first = maxOf(days.minOf { it.first }, last - 29)
        (first..last).map { byDay[it] ?: 0f }
    }
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(filled) { grown = true }
    val growth by animateFloatAsState(if (grown) 1f else 0f, tween(700), label = "barsGrow")
    val max = (filled.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val peakIndex = filled.indexOf(filled.maxOrNull())
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val peakColors = SummaryGradient

    Canvas(modifier) {
        val slot = size.width / filled.size
        val barWidth = (slot * 0.62f).coerceAtMost(14.dp.toPx())
        filled.forEachIndexed { i, value ->
            val height = (size.height * (value / max) * growth).coerceAtLeast(if (value > 0f) 3.dp.toPx() else 0f)
            if (height <= 0f) return@forEachIndexed
            val left = i * slot + (slot - barWidth) / 2
            val top = size.height - height
            if (i == peakIndex) {
                drawRoundRect(
                    brush = Brush.verticalGradient(peakColors, startY = top, endY = size.height),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            } else {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
    }
}
