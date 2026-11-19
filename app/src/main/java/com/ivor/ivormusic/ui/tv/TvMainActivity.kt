package com.ivor.ivormusic.ui.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.ivor.ivormusic.ui.home.HomeViewModel
import com.ivor.ivormusic.ui.player.PlayerViewModel
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel

/**
 * Android TV Main Activity.
 * Entry point launched on Android TV / Google TV via Leanback Launcher.
 */
@OptIn(UnstableApi::class)
class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android TV runs in landscape; keep window flags clean
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6C5CE7),
                    secondary = Color(0xFFA29BFE),
                    background = Color(0xFF0F0F16),
                    surface = Color(0xFF1E1E2E)
                )
            ) {
                val homeViewModel: HomeViewModel = viewModel()
                val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
                val playerViewModel: PlayerViewModel = viewModel {
                    PlayerViewModel(applicationContext)
                }

                TvMainApp(
                    homeViewModel = homeViewModel,
                    videoPlayerViewModel = videoPlayerViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@Composable
private fun TvMainApp(
    homeViewModel: HomeViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    playerViewModel: PlayerViewModel
) {
    var selectedDestination by remember { mutableStateOf(TvDestination.HOME) }
    var isVideoPlayerFullscreen by remember { mutableStateOf(false) }
    var isMusicPlayerFullscreen by remember { mutableStateOf(false) }

    val currentVideo by videoPlayerViewModel.currentVideo.collectAsState()
    val isVideoPlaying by videoPlayerViewModel.isPlaying.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isMusicPlaying by playerViewModel.isPlaying.collectAsState()

    // When video starts playing, automatically enter fullscreen video player
    LaunchedEffect(currentVideo, isVideoPlaying) {
        if (currentVideo != null && isVideoPlaying) {
            isVideoPlayerFullscreen = true
            isMusicPlayerFullscreen = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F16))
    ) {
        // Base Browse Screen: Left Nav Drawer + Content Body
        Row(modifier = Modifier.fillMaxSize()) {
            // Collapsible Left Nav Drawer
            TvNavDrawer(
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                }
            )

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectedDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tv_destination_transition"
                ) { destination ->
                    when (destination) {
                        TvDestination.HOME -> TvHomeScreen(
                            homeViewModel = homeViewModel,
                            onPlaySong = { song ->
                                videoPlayerViewModel.pause()
                                isVideoPlayerFullscreen = false
                                playerViewModel.playSong(song)
                                isMusicPlayerFullscreen = true
                            },
                            onPlayVideo = { video ->
                                playerViewModel.pause()
                                isMusicPlayerFullscreen = false
                                videoPlayerViewModel.playVideo(video)
                                isVideoPlayerFullscreen = true
                            }
                        )
                        TvDestination.SEARCH -> TvSearchScreen(
                            homeViewModel = homeViewModel,
                            onPlayVideo = { video ->
                                playerViewModel.pause()
                                isMusicPlayerFullscreen = false
                                videoPlayerViewModel.playVideo(video)
                                isVideoPlayerFullscreen = true
                            }
                        )
                        TvDestination.LIBRARY -> TvLibraryScreen(
                            homeViewModel = homeViewModel,
                            onPlaySong = { song ->
                                videoPlayerViewModel.pause()
                                isVideoPlayerFullscreen = false
                                playerViewModel.playSong(song)
                                isMusicPlayerFullscreen = true
                            }
                        )
                        TvDestination.SETTINGS -> TvSettingsScreen()
                    }
                }

                // Mini Now-Playing Strip if audio is playing in background
                if (currentSong != null && !isVideoPlayerFullscreen && !isMusicPlayerFullscreen) {
                    TvMiniAudioBar(
                        title = currentSong?.title.orEmpty(),
                        artist = currentSong?.artist.orEmpty(),
                        isPlaying = isMusicPlaying,
                        onClick = { isMusicPlayerFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp, end = 24.dp)
                    )
                }
            }
        }

        // Fullscreen Overlays
        if (isVideoPlayerFullscreen && currentVideo != null) {
            TvVideoPlayer(
                videoPlayerViewModel = videoPlayerViewModel,
                onClose = { isVideoPlayerFullscreen = false },
                modifier = Modifier.fillMaxSize()
            )
        } else if (isMusicPlayerFullscreen && currentSong != null) {
            TvMusicPlayer(
                playerViewModel = playerViewModel,
                onClose = { isMusicPlayerFullscreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TvMiniAudioBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) MaterialTheme.colorScheme.primary else Color(0xFF1E1E2E))
            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(14.dp), focusedScale = 1.02f)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = if (isPlaying) "Playing" else "Paused",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
