package com.ivor.ivormusic.ui.tv

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.LocalVideo
import com.ivor.ivormusic.data.PlayHistoryEntry
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel
import com.ivor.ivormusic.ui.player.PlayerViewModel
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel
import kotlinx.coroutines.launch

private enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED_SONGS("Liked Songs"),
    HISTORY("History"),
    DEVICE_MEDIA("Device Media")
}

private fun PlayHistoryEntry.toSong(): Song = Song(
    id = songId,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    source = source,
    albumId = albumId
)

/**
 * 10-foot Android TV Library Screen.
 * Full feature parity: User Playlists, Liked Songs, History, and Local Device Media.
 */
@Composable
fun TvLibraryScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    val scope = rememberCoroutineScope()

    val userPlaylists by homeViewModel.userPlaylists.collectAsState()
    val likedSongs by homeViewModel.likedSongs.collectAsState()
    val playHistory by homeViewModel.playHistory.collectAsState()
    val localSongs by homeViewModel.songs.collectAsState()
    val deviceVideos by homeViewModel.deviceVideos.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F16))
            .padding(
                start = TvLayout.ContentStart,
                top = 24.dp,
                end = TvDimensions.OverscanHorizontal,
                bottom = TvDimensions.OverscanVertical
            )
    ) {
        // Top Header & Tab Chips
        Text(
            text = "Library",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(LibraryTab.values()) { tab ->
                val isSelected = tab == selectedTab
                val interaction = remember { MutableInteractionSource() }
                val isFocused by interaction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isFocused -> Color.White
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF1E1E2E)
                            }
                        )
                        .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(8.dp), focusedScale = 1.06f)
                        .focusable(interactionSource = interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { selectedTab = tab }
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Tab Content Grid
        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (userPlaylists.isEmpty()) {
                    EmptyLibraryPlaceholder("No playlists created yet.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 190.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(userPlaylists, key = { it.id }) { playlist ->
                            val countSubtitle = if (playlist.itemCount > 0) "${playlist.itemCount} items" else playlist.uploaderName
                            TvMediaCard(
                                title = playlist.name,
                                subtitle = countSubtitle,
                                artworkUrl = playlist.thumbnailUrl,
                                aspectRatio = 1f,
                                cardWidth = 190.dp,
                                onClick = {
                                    scope.launch {
                                        val songs = homeViewModel.fetchPlaylistSongs(playlist.id)
                                        if (songs.isNotEmpty()) {
                                            playerViewModel.playQueue(songs)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            LibraryTab.LIKED_SONGS -> {
                if (likedSongs.isEmpty()) {
                    EmptyLibraryPlaceholder("No liked songs yet. Like songs with the heart button while listening.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(likedSongs, key = { it.id }) { song ->
                            TvMediaCard(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUrl = song.thumbnailUrl,
                                aspectRatio = 1f,
                                cardWidth = 180.dp,
                                onClick = { playerViewModel.playSong(song) }
                            )
                        }
                    }
                }
            }

            LibraryTab.HISTORY -> {
                if (playHistory.isEmpty()) {
                    EmptyLibraryPlaceholder("No playback history yet.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 210.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playHistory, key = { "${it.songId}_${it.timestamp}" }) { entry ->
                            val song = entry.toSong()
                            val durationFormatted = if (song.duration > 0) {
                                "${song.duration / 60}:${String.format("%02d", song.duration % 60)}"
                            } else null

                            TvMediaCard(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUrl = song.thumbnailUrl,
                                badgeText = durationFormatted,
                                aspectRatio = 16f / 9f,
                                cardWidth = 210.dp,
                                onClick = { playerViewModel.playSong(song) }
                            )
                        }
                    }
                }
            }

            LibraryTab.DEVICE_MEDIA -> {
                val totalItems = localSongs.size + deviceVideos.size
                if (totalItems == 0) {
                    EmptyLibraryPlaceholder("No local audio or video files found on this TV storage.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 190.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Local Songs
                        items(localSongs, key = { "local_s_" + it.id }) { song ->
                            TvMediaCard(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUrl = song.thumbnailUrl,
                                badgeText = "Audio",
                                aspectRatio = 1f,
                                cardWidth = 180.dp,
                                onClick = { playerViewModel.playSong(song) }
                            )
                        }

                        // Local Videos
                        items(deviceVideos, key = { "local_v_" + it.id }) { video ->
                            val durationFormatted = if (video.durationMs > 0) {
                                "${video.durationMs / 60000}:${String.format("%02d", (video.durationMs % 60000) / 1000)}"
                            } else null

                            TvMediaCard(
                                title = video.title,
                                subtitle = "Local File",
                                artworkUrl = video.uri.toString(),
                                badgeText = durationFormatted ?: "Video",
                                aspectRatio = 16f / 9f,
                                cardWidth = 210.dp,
                                onClick = {
                                    videoPlayerViewModel.playDeviceVideos(deviceVideos, video)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}
