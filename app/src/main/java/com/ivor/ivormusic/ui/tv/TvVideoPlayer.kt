package com.ivor.ivormusic.ui.tv

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ivor.ivormusic.data.CaptionTrack
import com.ivor.ivormusic.data.PlayerTrackOption
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.data.VideoQuality
import com.ivor.ivormusic.ui.theme.ThemeViewModel
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class TvPlayerDialog {
    NONE,
    QUALITY,
    SPEED,
    CAPTIONS,
    AUDIO,
    QUEUE
}

/**
 * 10-foot Living Room Video Player modeled after OpenStream.
 * Features:
 * - Dedicated focusable interactive SeekBar with D-pad scrub, live remaining time, and expanding thumb
 * - Full remote toolbar with phone feature parity: Play/Pause, Rewind, Forward, Previous, Next,
 *   Resolution/Quality, Captions, Audio Dubs, Speed, Aspect Ratio, Repeat, SponsorBlock, Up Next
 * - TV-optimized modal dialogs for Quality, Speed, Captions, Audio, and Up Next Queue
 * - High-contrast focus highlights and auto-hiding OSD overlay (5s timer)
 */
@OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayer(
    videoPlayerViewModel: VideoPlayerViewModel,
    themeViewModel: ThemeViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsState()
    val isPlaying by videoPlayerViewModel.isPlaying.collectAsState()
    val isBuffering by videoPlayerViewModel.isBuffering.collectAsState()
    val isLooping by videoPlayerViewModel.isLooping.collectAsState()
    val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsState()
    val currentQuality by videoPlayerViewModel.currentQuality.collectAsState()
    val availableQualities by videoPlayerViewModel.availableQualities.collectAsState()
    val captionTracks by videoPlayerViewModel.captionTracks.collectAsState()
    val selectedCaption by videoPlayerViewModel.selectedCaption.collectAsState()
    val audioTracks by videoPlayerViewModel.audioTracks.collectAsState()
    val relatedVideos by videoPlayerViewModel.relatedVideos.collectAsState()
    val sponsorBlockEnabled by themeViewModel.sponsorBlockEnabled.collectAsState()

    var showOsd by remember { mutableStateOf(true) }
    var activeDialog by remember { mutableStateOf(TvPlayerDialog.NONE) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var resizeModeIndex by remember { mutableIntStateOf(0) } // 0 = FIT, 1 = ZOOM, 2 = FILL

    val playPauseFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit (16:9)",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop (Fill)",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
    )

    // Position & duration polling
    LaunchedEffect(videoPlayerViewModel.exoPlayer, isPlaying) {
        val player = videoPlayerViewModel.exoPlayer
        while (isActive) {
            if (player != null) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }

    // Auto-hide OSD after 5 seconds of inactivity (only when playing and no dialog is open)
    LaunchedEffect(showOsd, isPlaying, activeDialog) {
        if (showOsd && isPlaying && activeDialog == TvPlayerDialog.NONE) {
            delay(5000)
            showOsd = false
        }
    }

    // Request initial focus on Play/Pause button
    LaunchedEffect(showOsd) {
        if (showOsd && activeDialog == TvPlayerDialog.NONE) {
            delay(100)
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    fun onInteraction() {
        showOsd = true
    }

    // Back button handling: dismiss dialog first, then OSD, then exit player
    BackHandler {
        when {
            activeDialog != TvPlayerDialog.NONE -> activeDialog = TvPlayerDialog.NONE
            showOsd -> showOsd = false
            else -> {
                videoPlayerViewModel.closePlayer()
                onClose()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                onInteraction()
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        if (!showOsd) {
                            showOsd = true
                            if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!showOsd) {
                            showOsd = true
                            videoPlayerViewModel.seekBy(-10_000L)
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!showOsd) {
                            showOsd = true
                            videoPlayerViewModel.seekBy(10_000L)
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!showOsd) {
                            showOsd = true
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (showOsd && activeDialog == TvPlayerDialog.NONE) {
                            showOsd = false
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                        return@onPreviewKeyEvent true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        videoPlayerViewModel.playFromExternal()
                        return@onPreviewKeyEvent true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        videoPlayerViewModel.pause()
                        return@onPreviewKeyEvent true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        videoPlayerViewModel.seekBy(15_000L)
                        return@onPreviewKeyEvent true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        videoPlayerViewModel.seekBy(-15_000L)
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
                false
            }
    ) {
        // Fullscreen ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    resizeMode = resizeModes[resizeModeIndex].first
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
                playerView.resizeMode = resizeModes[resizeModeIndex].first
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Spinner
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 3.dp
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
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(
                        horizontal = TvDimensions.OverscanHorizontal,
                        vertical = 24.dp
                    )
            ) {
                // Top Bar: Video Title, Channel, & Close Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = currentVideo?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 22.sp,
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

                // Lower Third: Interactive SeekBar + Remote Action Toolbar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Interactive TV SeekBar
                    TvPlayerSeekBar(
                        currentTimeMs = currentPositionMs,
                        durationMs = durationMs,
                        onSeek = { videoPlayerViewModel.seekBy(it) },
                        onPauseToggle = {
                            if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                        },
                        modifier = Modifier.focusRequester(seekBarFocusRequester)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Action Toolbar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play / Pause Button (56dp)
                        TvHudPlayPauseButton(
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            onClick = {
                                if (isPlaying) videoPlayerViewModel.pause() else videoPlayerViewModel.playFromExternal()
                            },
                            modifier = Modifier.focusRequester(playPauseFocusRequester)
                        )

                        // Rewind 10s
                        TvHudIconButton(
                            icon = Icons.Rounded.FastRewind,
                            contentDescription = "Rewind 10s",
                            onClick = { videoPlayerViewModel.seekBy(-10_000L) }
                        )

                        // Fast Forward 10s
                        TvHudIconButton(
                            icon = Icons.Rounded.FastForward,
                            contentDescription = "Forward 10s",
                            onClick = { videoPlayerViewModel.seekBy(10_000L) }
                        )

                        // Previous Video
                        TvHudIconButton(
                            icon = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Video",
                            onClick = { videoPlayerViewModel.playPreviousOrRestart() }
                        )

                        // Next Video
                        TvHudIconButton(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = "Next Video",
                            onClick = { videoPlayerViewModel.playNextOrRelated() }
                        )

                        Spacer(Modifier.weight(1f))

                        // Quality / Resolution Button
                        val qualityLabel = currentQuality?.resolution ?: "Auto"
                        TvHudTextButton(
                            label = qualityLabel,
                            onClick = { activeDialog = TvPlayerDialog.QUALITY }
                        )

                        // Captions Toggle / Track Selector
                        TvHudIconButton(
                            icon = Icons.Rounded.ClosedCaption,
                            contentDescription = "Subtitles",
                            isToggled = selectedCaption != null,
                            onClick = { activeDialog = TvPlayerDialog.CAPTIONS }
                        )

                        // Audio Dub / Language Selector (visible if multiple tracks exist)
                        if (audioTracks.size > 1) {
                            TvHudIconButton(
                                icon = Icons.Rounded.GraphicEq,
                                contentDescription = "Audio Track",
                                onClick = { activeDialog = TvPlayerDialog.AUDIO }
                            )
                        }

                        // Playback Speed Button (e.g. 1.0x)
                        TvHudTextButton(
                            label = "${playbackSpeed}x",
                            onClick = { activeDialog = TvPlayerDialog.SPEED }
                        )

                        // Video Scaling / Aspect Ratio
                        TvHudIconButton(
                            icon = Icons.Rounded.AspectRatio,
                            contentDescription = resizeModes[resizeModeIndex].second,
                            onClick = {
                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                            }
                        )

                        // Repeat / Loop
                        TvHudIconButton(
                            icon = Icons.Rounded.Repeat,
                            contentDescription = "Repeat Video",
                            isToggled = isLooping,
                            onClick = { videoPlayerViewModel.toggleLooping() }
                        )

                        // SponsorBlock Toggle
                        TvHudIconButton(
                            icon = Icons.Rounded.Security,
                            contentDescription = "SponsorBlock",
                            isToggled = sponsorBlockEnabled,
                            onClick = { themeViewModel.setSponsorBlockEnabled(!sponsorBlockEnabled) }
                        )

                        // Up Next / Queue Drawer Button
                        if (relatedVideos.isNotEmpty()) {
                            TvHudTextButton(
                                label = "Up Next",
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                onClick = { activeDialog = TvPlayerDialog.QUEUE }
                            )
                        }
                    }
                }
            }
        }

        // --- Dialogs & Panels ---

        // Quality Dialog
        if (activeDialog == TvPlayerDialog.QUALITY) {
            TvPlayerSelectionDialog(
                title = "Video Quality",
                options = availableQualities,
                selectedOption = currentQuality,
                labelProvider = { it.resolution + if (it.isHdr) " (HDR)" else "" },
                onSelect = {
                    videoPlayerViewModel.setQuality(it)
                    activeDialog = TvPlayerDialog.NONE
                },
                onDismiss = { activeDialog = TvPlayerDialog.NONE }
            )
        }

        // Speed Dialog
        if (activeDialog == TvPlayerDialog.SPEED) {
            val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            TvPlayerSelectionDialog(
                title = "Playback Speed",
                options = speedOptions,
                selectedOption = playbackSpeed,
                labelProvider = { if (it == 1.0f) "1.0x (Normal)" else "${it}x" },
                onSelect = {
                    videoPlayerViewModel.setPlaybackSpeed(it)
                    activeDialog = TvPlayerDialog.NONE
                },
                onDismiss = { activeDialog = TvPlayerDialog.NONE }
            )
        }

        // Captions Dialog
        if (activeDialog == TvPlayerDialog.CAPTIONS) {
            val captionOptions = listOf<CaptionTrack?>(null) + captionTracks
            TvPlayerSelectionDialog(
                title = "Subtitles & Captions",
                options = captionOptions,
                selectedOption = selectedCaption,
                labelProvider = { it?.let { track -> if (track.isAutoGenerated) "${track.name} (auto)" else track.name } ?: "Off" },
                onSelect = {
                    videoPlayerViewModel.setCaptionTrack(it)
                    activeDialog = TvPlayerDialog.NONE
                },
                onDismiss = { activeDialog = TvPlayerDialog.NONE }
            )
        }

        // Audio Track Dialog
        if (activeDialog == TvPlayerDialog.AUDIO) {
            TvPlayerSelectionDialog(
                title = "Audio Track",
                options = audioTracks,
                selectedOption = audioTracks.firstOrNull(),
                labelProvider = { it.label },
                onSelect = {
                    videoPlayerViewModel.setAudioTrack(it)
                    activeDialog = TvPlayerDialog.NONE
                },
                onDismiss = { activeDialog = TvPlayerDialog.NONE }
            )
        }

        // Up Next / Queue Shelf Overlay
        if (activeDialog == TvPlayerDialog.QUEUE) {
            TvUpNextQueueShelf(
                videos = relatedVideos,
                onSelectVideo = {
                    videoPlayerViewModel.playVideo(it)
                    activeDialog = TvPlayerDialog.NONE
                },
                onDismiss = { activeDialog = TvPlayerDialog.NONE },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Dedicated Interactive TV SeekBar with D-pad focus, live scrubbing, and expanding thumb.
 */
@Composable
private fun TvPlayerSeekBar(
    currentTimeMs: Long,
    durationMs: Long,
    onSeek: (deltaMs: Long) -> Unit,
    onPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(0L)
    val progress = if (duration > 0) (currentTimeMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        onSeek(-10_000L)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onSeek(10_000L)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        onPauseToggle()
                        true
                    }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
    ) {
        // Scrubber Bar
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackHeight = if (isFocused) 8.dp else 4.dp

            // Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // Progress Fill
            Box(
                modifier = Modifier
                    .width(maxWidth * progress)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(if (isFocused) accent else Color.White)
            )

            // Expanding Circular Thumb (visible when focused)
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .offset(x = (maxWidth * progress - 10.dp).coerceAtLeast(0.dp))
                        .size(20.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, accent, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Timestamps (Current / Total / Remaining)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(currentTimeMs),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
            )
            val remainingMs = (duration - currentTimeMs).coerceAtLeast(0L)
            Text(
                text = "-${formatMs(remainingMs)} / ${formatMs(duration)}",
                color = if (isFocused) Color.White else Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * 56dp Primary Play / Pause Button with buffering indicator.
 */
@Composable
private fun TvHudPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.20f))
            .tvFocusable(isFocused = isFocused, shape = CircleShape, focusedScale = 1.12f)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 44dp TV Icon Button with invert effect on focus.
 */
@Composable
private fun TvHudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToggled: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val containerColor = when {
        isFocused -> Color.White
        isToggled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    val tint = when {
        isFocused -> Color.Black
        isToggled -> Color.White
        else -> Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(containerColor)
            .tvFocusable(isFocused = isFocused, shape = CircleShape, focusedScale = 1.10f)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Pill Button for textual options like resolution (1080p) or speed (1.0x).
 */
@Composable
private fun TvHudTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.12f))
            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(22.dp), focusedScale = 1.08f)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = if (isFocused) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Reusable D-pad selection dialog for Quality, Speed, Captions, and Audio tracks.
 */
@Composable
private fun <T> TvPlayerSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T?,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.70f))
            .clickable(onClick = onDismiss)
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E2E))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { item ->
                    val isSelected = item == selectedOption
                    val interaction = remember { MutableInteractionSource() }
                    val isFocused by interaction.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isFocused -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(item) }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = labelProvider(item),
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Up Next / Queue Drawer Shelf at the bottom of the video player.
 */
@Composable
private fun TvUpNextQueueShelf(
    videos: List<VideoItem>,
    onSelectVideo: (VideoItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF14141E))
            .padding(vertical = 16.dp)
            .zIndex(10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Up Next & Related",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Press BACK to dismiss",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(videos, key = { it.videoId }) { video ->
                val durationFormatted = if (video.duration > 0) {
                    "${video.duration / 60}:${String.format("%02d", video.duration % 60)}"
                } else null

                TvMediaCard(
                    title = video.title,
                    subtitle = video.channelName,
                    artworkUrl = video.thumbnailUrl,
                    badgeText = durationFormatted,
                    aspectRatio = 16f / 9f,
                    cardWidth = 220.dp,
                    onClick = { onSelectVideo(video) }
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
