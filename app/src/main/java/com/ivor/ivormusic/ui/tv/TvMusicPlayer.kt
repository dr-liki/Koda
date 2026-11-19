package com.ivor.ivormusic.ui.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ivor.ivormusic.data.LyricsResult
import com.ivor.ivormusic.ui.player.PlayerViewModel

/**
 * 10-foot Living Room Music Player.
 * Displays high-res album art, track details, D-pad controls, and karaoke auto-scrolling lyrics.
 */
@Composable
fun TvMusicPlayer(
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progressMs by playerViewModel.progress.collectAsState()
    val lyricsResult by playerViewModel.lyricsResult.collectAsState()

    val durationMs = currentSong?.duration ?: 0L
    val lyricsListState = rememberLazyListState()

    // Back button exits player to TV Home, allowing music to continue playing
    BackHandler {
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C14))
            .padding(
                horizontal = TvDimensions.OverscanHorizontal,
                vertical = TvDimensions.OverscanVertical
            )
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            playerViewModel.togglePlayPause()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            playerViewModel.skipToNext()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            playerViewModel.skipToPrevious()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // Left Column: Artwork, Song Info & Controls
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Artwork
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E2E))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentSong?.thumbnailUrl)
                            .crossfade(true)
                            .size(600, 600)
                            .build(),
                        contentDescription = currentSong?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Song Title & Artist
                Text(
                    text = currentSong?.title.orEmpty(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = currentSong?.artist.orEmpty(),
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar & Times
                val progress = if (durationMs > 0) {
                    (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatMs(progressMs), color = Color.Gray, fontSize = 12.sp)
                    Text(text = formatMs(durationMs), color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Media Controls Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvMusicControlButton(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = { playerViewModel.toggleShuffle() }
                    )

                    TvMusicControlButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        size = 46.dp,
                        iconSize = 24.dp,
                        onClick = { playerViewModel.skipToPrevious() }
                    )

                    TvMusicControlButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        size = 56.dp,
                        iconSize = 32.dp,
                        isPrimary = true,
                        onClick = { playerViewModel.togglePlayPause() }
                    )

                    TvMusicControlButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        size = 46.dp,
                        iconSize = 24.dp,
                        onClick = { playerViewModel.skipToNext() }
                    )

                    TvMusicControlButton(
                        icon = Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = { playerViewModel.toggleRepeat() }
                    )
                }
            }

            // Right Column: Synchronized Karaoke Lyrics
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                when (val result = lyricsResult) {
                    is LyricsResult.Success -> {
                        // Find current active lyric line
                        val currentLineIndex = remember(progressMs, result.lines) {
                            val idx = result.lines.indexOfLast { it.timeMs <= progressMs }
                            if (idx >= 0) idx else 0
                        }

                        // Auto-scroll lyrics to keep active line centered
                        LaunchedEffect(currentLineIndex) {
                            if (result.lines.isNotEmpty()) {
                                val target = (currentLineIndex - 2).coerceAtLeast(0)
                                lyricsListState.animateScrollToItem(target)
                            }
                        }

                        LazyColumn(
                            state = lyricsListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(result.lines) { index, line ->
                                val isActive = index == currentLineIndex
                                val textColor by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                    label = "lyric_color"
                                )

                                Text(
                                    text = line.text,
                                    fontSize = if (isActive) 24.sp else 18.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    is LyricsResult.Loading -> {
                        Text(
                            text = "Loading lyrics...",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {
                        Text(
                            text = "No synchronized lyrics available",
                            color = Color.DarkGray,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvMusicControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    isPrimary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background = when {
        isFocused -> MaterialTheme.colorScheme.primary
        isPrimary -> Color.White.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .tvFocusable(isFocused = isFocused, shape = CircleShape, focusedScale = 1.15f)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
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
