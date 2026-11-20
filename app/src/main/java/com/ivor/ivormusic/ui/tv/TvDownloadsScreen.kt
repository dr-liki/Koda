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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.DownloadedVideo
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.ui.home.HomeViewModel
import com.ivor.ivormusic.ui.player.PlayerViewModel
import com.ivor.ivormusic.ui.video.VideoPlayerViewModel

private enum class DownloadsFilter(val label: String) {
    ALL("All Downloads"),
    VIDEOS("Videos"),
    MUSIC("Songs")
}

/**
 * 10-foot Android TV Offline Downloads Screen.
 * Lists downloaded videos and music tracks with offline playback and D-pad deletion.
 */
@Composable
fun TvDownloadsScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(DownloadsFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    val downloadedSongs by homeViewModel.downloadedSongs.collectAsState()
    val downloadedVideos by homeViewModel.downloadedVideos.collectAsState()

    val totalItems = downloadedSongs.size + downloadedVideos.size

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val target = itemToDelete
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Download?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val title = when (target) {
                    is DownloadedVideo -> target.title
                    is Song -> target.title
                    else -> ""
                }
                Text(
                    text = "Are you sure you want to remove \"$title\" from TV storage?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            is DownloadedVideo -> playerViewModel.deleteVideoDownload(target.id)
                            is Song -> playerViewModel.deleteDownload(target.id)
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

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
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Downloads & Offline",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$totalItems items available offline",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(DownloadsFilter.values()) { filter ->
                val isSelected = filter == selectedFilter
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
                            onClick = { selectedFilter = filter }
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter.label,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Downloads Content Grid
        if (totalItems == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No offline media downloaded yet.\nUse the download icon on any song or video to save for offline playback.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 210.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Downloaded Videos
                if (selectedFilter != DownloadsFilter.MUSIC) {
                    items(downloadedVideos, key = { "dl_v_" + it.id }) { video ->
                        val durationFormatted = if (video.durationMs > 0) {
                            "${video.durationMs / 60000}:${String.format("%02d", (video.durationMs % 60000) / 1000)}"
                        } else null

                        TvMediaCard(
                            title = video.title,
                            subtitle = video.channelName,
                            artworkUrl = video.thumbnailUrl ?: video.uri.toString(),
                            badgeText = durationFormatted ?: video.quality ?: "Offline",
                            aspectRatio = 16f / 9f,
                            cardWidth = 220.dp,
                            onClick = {
                                videoPlayerViewModel.playDownloadedVideos(downloadedVideos, video)
                            }
                        )
                    }
                }

                // Downloaded Songs
                if (selectedFilter != DownloadsFilter.VIDEOS) {
                    items(downloadedSongs, key = { "dl_s_" + it.id }) { song ->
                        val durationFormatted = if (song.duration > 0) {
                            "${song.duration / 60}:${String.format("%02d", song.duration % 60)}"
                        } else null

                        TvMediaCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            badgeText = durationFormatted ?: "Audio",
                            aspectRatio = 1f,
                            cardWidth = 180.dp,
                            onClick = { playerViewModel.playSong(song) }
                        )
                    }
                }
            }
        }
    }
}
