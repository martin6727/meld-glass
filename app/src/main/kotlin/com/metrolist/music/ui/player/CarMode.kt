package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.playback.PlayerConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import androidx.compose.foundation.border
import com.metrolist.music.ui.theme.pressScale
import com.metrolist.music.utils.makeTimeString
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Landscape "car mode": huge art, huge text, huge buttons. Shown inside the expanded player
 * whenever a phone is turned sideways.
 */
@Composable
fun CarModePlayer(onClose: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptics = LocalHapticFeedback.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val liked = currentSong?.song?.liked == true

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekFraction by remember { mutableStateOf<Float?>(null) }
    var clock by remember { mutableStateOf(LocalTime.now()) }

    // Listen Together guests follow the host, so they can't change songs
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    // Swipe-to-skip state for the cover
    val scope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    var artWidth by remember { mutableFloatStateOf(1f) }
    var instantArtSwap by remember { mutableStateOf(false) }
    var swipeBusy by remember { mutableStateOf(false) }
    val currentCanSkipNext by rememberUpdatedState(canSkipNext)
    val currentCanSkipPrevious by rememberUpdatedState(canSkipPrevious)
    val currentIsGuest by rememberUpdatedState(isListenTogetherGuest)
    fun canSwipe(toNext: Boolean): Boolean {
        if (currentIsGuest) return false
        val casting = runCatching {
            playerConnection.service.castConnectionHandler?.isCasting?.value == true
        }.getOrDefault(false)
        return when {
            toNext -> currentCanSkipNext
            casting -> currentCanSkipPrevious
            // "Previous" on the button can mean "restart"; a swipe should only go to a real earlier song
            else -> playerConnection.player.hasPreviousMediaItem()
        }
    }

    LaunchedEffect(playerConnection) {
        while (true) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration.coerceAtLeast(0L)
            clock = LocalTime.now()
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.65f)),
                ),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            // Album art: swipe left for the next song, right for the previous one
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .onSizeChanged { artWidth = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        val tracker = VelocityTracker()
                        var pastThreshold = false
                        detectHorizontalDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                pastThreshold = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (swipeBusy) return@detectHorizontalDragGestures
                                change.consume()
                                tracker.addPointerInputChange(change)
                                val target = swipeOffset.value + dragAmount
                                val allowed = canSwipe(toNext = target < 0)
                                // Rubber-band when there's no song in that direction
                                val next = if (allowed) target else swipeOffset.value + dragAmount * 0.25f
                                val crossed = allowed && abs(next) > size.width * SwipeThreshold
                                if (crossed && !pastThreshold) {
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                }
                                pastThreshold = crossed
                                scope.launch { swipeOffset.snapTo(next) }
                            },
                            onDragCancel = {
                                if (!swipeBusy) scope.launch { swipeOffset.animateTo(0f, SettleSpring) }
                            },
                            onDragEnd = {
                                if (swipeBusy) return@detectHorizontalDragGestures
                                val width = size.width.toFloat()
                                val offset = swipeOffset.value
                                val velocity = tracker.calculateVelocity().x
                                val flingVelocity = 700.dp.toPx()
                                val goNext = offset < 0 && canSwipe(toNext = true) &&
                                    (offset < -width * SwipeThreshold || velocity < -flingVelocity)
                                val goPrevious = offset > 0 && canSwipe(toNext = false) &&
                                    (offset > width * SwipeThreshold || velocity > flingVelocity)
                                if (!goNext && !goPrevious) {
                                    scope.launch { swipeOffset.animateTo(0f, SettleSpring, initialVelocity = velocity) }
                                    return@detectHorizontalDragGestures
                                }
                                swipeBusy = true
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                scope.launch {
                                    try {
                                        val dir = if (goNext) -1f else 1f
                                        swipeOffset.animateTo(dir * width * SwipeFlyDistance, tween(160, easing = FastOutLinearInEasing))
                                        val oldId = playerConnection.mediaMetadata.value?.id
                                        instantArtSwap = true
                                        if (goNext) playerConnection.seekToNext() else playerConnection.skipToPreviousTrack()
                                        withTimeoutOrNull(800) { playerConnection.mediaMetadata.first { it?.id != oldId } }
                                        // The new cover comes in from the side the old one left
                                        swipeOffset.snapTo(-dir * width * 0.35f)
                                        swipeOffset.animateTo(0f, EnterSpring)
                                    } finally {
                                        instantArtSwap = false
                                        swipeBusy = false
                                    }
                                }
                            },
                        )
                    },
            ) {
                // Skip hints, revealed behind the cover as it moves
                SwipeHint(R.drawable.skip_previous, Alignment.CenterStart) {
                    (swipeOffset.value / (artWidth * SwipeThreshold)).coerceIn(0f, 1f)
                }
                SwipeHint(R.drawable.skip_next, Alignment.CenterEnd) {
                    (-swipeOffset.value / (artWidth * SwipeThreshold)).coerceIn(0f, 1f)
                }

                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        if (instantArtSwap) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            (fadeIn(tween(400)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), initialScale = 0.92f))
                                .togetherWith(fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 1.04f))
                        }
                    },
                    label = "carArt",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val travel = (abs(swipeOffset.value) / (size.width * SwipeFlyDistance)).coerceIn(0f, 1f)
                            translationX = swipeOffset.value
                            alpha = 1f - travel * travel
                            scaleX = 1f - travel * 0.1f
                            scaleY = scaleX
                        },
                ) { url ->
                    val artScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1f else 0.94f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                        label = "carArtScale",
                    )
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                            }
                            .shadow(32.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF1A1A1E)),
                    )
                }
            }

            Spacer(Modifier.width(36.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top row: clock + close
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painter = painterResource(R.drawable.directions_car),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = clock.format(DateTimeFormatter.ofPattern("HH:mm")),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    CarButton(
                        icon = R.drawable.expand_more,
                        size = 56.dp,
                        iconSize = 30.dp,
                        onClick = onClose,
                    )
                }

                // Title + artist
                Column {
                    AnimatedContent(
                        targetState = mediaMetadata?.title.orEmpty(),
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "carTitle",
                    ) { title ->
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 38.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Progress
                Column {
                    val fraction = seekFraction ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    BoxWithConstraints(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(duration) {
                                detectTapGestures { offset ->
                                    if (duration > 0) {
                                        playerConnection.seekTo((offset.x / size.width * duration).toLong())
                                    }
                                }
                            }
                            .pointerInput(duration) {
                                detectHorizontalDragGestures(
                                    onDragStart = { seekFraction = (it.x / size.width).coerceIn(0f, 1f) },
                                    onHorizontalDrag = { change, _ ->
                                        seekFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        seekFraction?.let { if (duration > 0) playerConnection.seekTo((it * duration).toLong()) }
                                        seekFraction = null
                                    },
                                    onDragCancel = { seekFraction = null },
                                )
                            },
                    ) {
                        val barHeight = if (seekFraction != null) 12.dp else 8.dp
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                        )
                        Box(
                            Modifier
                                .width(maxWidth * fraction)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = makeTimeString(seekFraction?.let { (it * duration).toLong() } ?: position),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = makeTimeString(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Controls: full size when there's room, scaled down evenly on narrower screens
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val s = (maxWidth / CarControlsFullWidth).coerceIn(0.55f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CarButton(
                        icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                        size = 64.dp * s,
                        iconSize = 30.dp * s,
                        tint = if (liked) Color(0xFFFF4D6D) else Color.White,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            playerConnection.toggleLike()
                        },
                    )
                    CarButton(
                        icon = R.drawable.skip_previous,
                        size = 84.dp * s,
                        iconSize = 42.dp * s,
                        enabled = canSkipPrevious && !isListenTogetherGuest,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playerConnection.seekToPrevious()
                        },
                    )
                    CarButton(
                        icon = when {
                            playbackState == Player.STATE_ENDED -> R.drawable.replay
                            isPlaying -> R.drawable.pause
                            else -> R.drawable.play
                        },
                        size = 112.dp * s,
                        iconSize = 56.dp * s,
                        filled = true,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.togglePlayPause()
                            }
                        },
                    )
                    CarButton(
                        icon = R.drawable.skip_next,
                        size = 84.dp * s,
                        iconSize = 42.dp * s,
                        enabled = canSkipNext && !isListenTogetherGuest,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playerConnection.seekToNext()
                        },
                    )
                    CarButton(
                        icon = if (shuffle) R.drawable.shuffle_on else R.drawable.shuffle,
                        size = 64.dp * s,
                        iconSize = 28.dp * s,
                        tint = Color.White.copy(alpha = if (shuffle) 1f else 0.7f),
                        onClick = { playerConnection.player.shuffleModeEnabled = !shuffle },
                    )
                }
                }
            }
        }
    }
}

// 64 + 84 + 112 + 84 + 64 dp of buttons plus breathing room between them
private val CarControlsFullWidth = 456.dp

// Fraction of the cover width a swipe must travel to change the song
private const val SwipeThreshold = 0.28f

// How far (in cover widths) the old cover slides while fading out
private const val SwipeFlyDistance = 0.6f
private val SettleSpring = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)
private val EnterSpring = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/** Round skip icon behind the cover; [progress] is read while drawing so dragging doesn't recompose. */
@Composable
private fun BoxScope.SwipeHint(icon: Int, alignment: Alignment, progress: () -> Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(alignment)
            .padding(horizontal = 20.dp)
            .size(72.dp)
            .graphicsLayer {
                val p = progress()
                alpha = p
                scaleX = 0.6f + 0.4f * p
                scaleY = scaleX
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp),
        )
    }
}

/** Always moves to the previous song (the previous button restarts the current song after 3 seconds). */
private fun PlayerConnection.skipToPreviousTrack() {
    val castHandler = runCatching { service.castConnectionHandler }.getOrNull()
    if (castHandler?.isCasting?.value == true) {
        castHandler.skipToPrevious()
        return
    }
    if (!player.hasPreviousMediaItem()) {
        seekToPrevious()
        return
    }
    player.seekToPreviousMediaItem()
    if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
        player.prepare()
    }
    player.playWhenReady = true
    onSkipPrevious?.invoke()
}

@Composable
private fun CarButton(
    icon: Int,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    filled: Boolean = false,
    tint: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .pressScale(interactionSource, 0.88f)
            .then(
                if (filled) {
                    Modifier
                        .shadow(24.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(Color.White)
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f)),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                },
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), initialScale = 0.6f) + fadeIn())
                    .togetherWith(scaleOut(targetScale = 0.6f) + fadeOut())
            },
            label = "carIcon",
        ) { res ->
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = if (filled) Color.Black else tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
