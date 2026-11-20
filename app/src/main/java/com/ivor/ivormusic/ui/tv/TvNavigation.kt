package com.ivor.ivormusic.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel
import com.ivor.ivormusic.ui.player.PlayerViewModel
import com.ivor.ivormusic.ui.theme.ThemeViewModel
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel

enum class TvNavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    SEARCH("Search", Icons.Rounded.Search),
    SUBSCRIPTIONS("Subscriptions", Icons.Rounded.Subscriptions),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    DOWNLOADS("Downloads", Icons.Rounded.Download),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

/**
 * Unified Android TV Root Navigation Shell modeled after OpenStream.
 * Handles the collapsible left rail, destination switching, mini now-playing bar,
 * and fullscreen 10-foot Video and Music player overlays.
 */
@Composable
fun TvNavigation(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    TvTheme {
        val configuration = LocalConfiguration.current
        val contentWidth = configuration.screenWidthDp - TvLayout.ContentStart.value.toInt()
        val metrics = remember(contentWidth, configuration.screenHeightDp) {
            TvDimens.of(contentWidth, configuration.screenHeightDp)
        }

        CompositionLocalProvider(LocalTvDimens provides metrics) {
            TvAppScaffold(
                homeViewModel = homeViewModel,
                playerViewModel = playerViewModel,
                videoPlayerViewModel = videoPlayerViewModel,
                themeViewModel = themeViewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TvAppScaffold(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedDestination by remember { mutableStateOf(TvNavDestination.HOME) }
    var isVideoPlayerActive by remember { mutableStateOf(false) }
    var isMusicPlayerActive by remember { mutableStateOf(false) }

    val currentVideo by videoPlayerViewModel.currentVideo.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isMusicPlaying by playerViewModel.isPlaying.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val requesters = remember { TvNavDestination.values().associateWith { FocusRequester() } }

    fun playVideo(video: VideoItem) {
        // Pause music if running
        if (isMusicPlaying) playerViewModel.pause()
        videoPlayerViewModel.playVideo(video)
        isVideoPlayerActive = true
    }

    fun playSong(song: Song) {
        // Close video player if running
        if (isVideoPlayerActive) {
            videoPlayerViewModel.closePlayer()
            isVideoPlayerActive = false
        }
        playerViewModel.playSong(song)
        isMusicPlayerActive = true
    }

    // Top-level Back navigation handling
    BackHandler(enabled = isVideoPlayerActive || isMusicPlayerActive || selectedDestination != TvNavDestination.HOME) {
        when {
            isVideoPlayerActive -> {
                videoPlayerViewModel.closePlayer()
                isVideoPlayerActive = false
            }
            isMusicPlayerActive -> {
                isMusicPlayerActive = false
            }
            selectedDestination != TvNavDestination.HOME -> {
                selectedDestination = TvNavDestination.HOME
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0A10))) {
        // Navigation Drawer + Screen Content
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimBrush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF0F0F16),
                    Color(0xFF0F0F16).copy(alpha = 0.7f),
                    Color.Transparent
                )
            ),
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .selectableGroup()
                        .focusRestorer(requesters.getValue(selectedDestination)),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    TvNavDestination.values().forEach { destination ->
                        val isSelected = destination == selectedDestination
                        NavigationDrawerItem(
                            selected = isSelected,
                            onClick = {
                                drawerState.setValue(DrawerValue.Closed)
                                selectedDestination = destination
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color.Black
                            ),
                            modifier = Modifier.focusRequester(requesters.getValue(destination))
                        ) {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        ) {
            // Main Destination Screen
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedDestination) {
                    TvNavDestination.HOME -> {
                        TvHomeScreen(
                            homeViewModel = homeViewModel,
                            onPlaySong = { playSong(it) },
                            onPlayVideo = { playVideo(it) }
                        )
                    }
                    TvNavDestination.SEARCH -> {
                        TvSearchScreen(
                            homeViewModel = homeViewModel,
                            onPlaySong = { playSong(it) },
                            onPlayVideo = { playVideo(it) }
                        )
                    }
                    TvNavDestination.SUBSCRIPTIONS -> {
                        TvSubscriptionsScreen(
                            homeViewModel = homeViewModel,
                            onPlayVideo = { playVideo(it) }
                        )
                    }
                    TvNavDestination.LIBRARY -> {
                        TvLibraryScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            videoPlayerViewModel = videoPlayerViewModel
                        )
                    }
                    TvNavDestination.DOWNLOADS -> {
                        TvDownloadsScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            videoPlayerViewModel = videoPlayerViewModel
                        )
                    }
                    TvNavDestination.SETTINGS -> {
                        TvSettingsScreen(
                            themeViewModel = themeViewModel
                        )
                    }
                }

                // Mini Now-Playing bar (when music is playing in background and full players are closed)
                val showMiniPlayer = currentSong != null && !isVideoPlayerActive && !isMusicPlayerActive
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 36.dp, bottom = 24.dp)
                ) {
                    val interaction = remember { MutableInteractionSource() }
                    val isFocused by interaction.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .width(340.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isFocused) Color.White else Color(0xFF1E1E2E).copy(alpha = 0.95f))
                            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(30.dp), focusedScale = 1.05f)
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { isMusicPlayerActive = true }
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentSong?.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong?.title.orEmpty(),
                                color = if (isFocused) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong?.artist.orEmpty(),
                                color = if (isFocused) Color.DarkGray else Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play / Pause Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isFocused) Color.Black else MaterialTheme.colorScheme.primary)
                                .clickable { playerViewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen Video Player Overlay
        if (isVideoPlayerActive) {
            TvVideoPlayer(
                videoPlayerViewModel = videoPlayerViewModel,
                onClose = { isVideoPlayerActive = false }
            )
        }

        // Fullscreen Music Player Overlay
        if (isMusicPlayerActive && currentSong != null) {
            TvMusicPlayer(
                playerViewModel = playerViewModel,
                onClose = { isMusicPlayerActive = false }
            )
        }
    }
}
