package com.ivor.ivormusic.ui.tv

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 10-foot Android TV Fullscreen Video Player with remote-controlled OSD overlay.
 */
@OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayer(
    videoPlayerViewModel: VideoPlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsState()
    val isPlaying by videoPlayerViewModel.isPlaying.collectAsState()
    val isBuffering by videoPlayerViewModel.isBuffering.collectAsState()

    var showOsd by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    val playPauseFocusRequester = remember { FocusRequester() }

    // Poll current playback position and duration
    LaunchedEffect(videoPlayerViewModel.exoPlayer, isPlaying) {
        val player = videoPlayerViewModel.exoPlayer
        while (isActive) {
            if (player != null) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide OSD after 4 seconds of inactivity
    LaunchedEffect(showOsd, isPlaying) {
        if (showOsd && isPlaying) {
            delay(4000)
            showOsd = false
        }
    }

    // Back button: Dismiss OSD first; exit player if OSD already hidden
    BackHandler {
        if (showOsd) {
            showOsd = false
        } else {
            videoPlayerViewModel.closePlayer()
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            if (!showOsd) {
                                showOsd = true
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showOsd) {
                                showOsd = true
                            }
                            val target = (currentPositionMs - 10_000L).coerceAtLeast(0L)
                            videoPlayerViewModel.seekTo(target)
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showOsd) {
                                showOsd = true
                            }
                            val target = (currentPositionMs + 10_000L).coerceAtMost(durationMs)
                            videoPlayerViewModel.seekTo(target)
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showOsd = true
                            return@onPreviewKeyEvent false
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            videoPlayerViewModel.playFromExternal()
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            videoPlayerViewModel.pause()
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            videoPlayerViewModel.seekTo((currentPositionMs + 15_000L).coerceAtMost(durationMs))
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            videoPlayerViewModel.seekTo((currentPositionMs - 15_000L).coerceAtLeast(0L))
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            videoPlayerViewModel.playNextOrRelated()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            videoPlayerViewModel.playPreviousOrRestart()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        // Fullscreen ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = videoPlayerViewModel.exoPlayer
                }
            },
            update = { playerView ->
                if (playerView.player != videoPlayerViewModel.exoPlayer) {
                    playerView.player = videoPlayerViewModel.exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // On-Screen Display (OSD) Overlay
        AnimatedVisibility(
            visible = showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .padding(
                        horizontal = TvDimensions.OverscanHorizontal,
                        vertical = TvDimensions.OverscanVertical
                    )
            ) {
                // Top Bar: Video Title & Channel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = currentVideo?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentVideo?.channelName.orEmpty(),
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvPlayerIconButton(
                        icon = Icons.Rounded.FastRewind,
                        contentDescription = "Rewind 10s",
                        onClick = {
                            videoPlayerViewModel.seekTo((currentPositionMs - 10_000L).coerceAtLeast(0L))
                        }
                    )

                    TvPlayerIconButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier
                            .size(64.dp)
                            .focusRequester(playPauseFocusRequester),
                        iconSize = 36.dp,
                        onClick = {
                            if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                        }
                    )

                    TvPlayerIconButton(
                        icon = Icons.Rounded.FastForward,
                        contentDescription = "Forward 10s",
                        onClick = {
                            videoPlayerViewModel.seekTo((currentPositionMs + 10_000L).coerceAtMost(durationMs))
                        }
                    )

                    TvPlayerIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next Video",
                        onClick = { videoPlayerViewModel.playNextOrRelated() }
                    )
                }

                // Bottom Bar: Scrubber & Timestamps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(currentPositionMs),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = formatMs(durationMs),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    // Progress Bar
                    val progress = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    iconSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.2f))
            .tvFocusable(isFocused = isFocused, shape = CircleShape, focusedBorderColor = Color.White, focusedScale = 1.15f)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
