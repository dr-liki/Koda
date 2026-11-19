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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.PlayHistoryEntry
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.ui.home.HomeViewModel

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
 * Shows user playlists, playback history, and profile sync options.
 */
@Composable
fun TvLibraryScreen(
    homeViewModel: HomeViewModel,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val userPlaylists by homeViewModel.userPlaylists.collectAsState()
    val playHistory by homeViewModel.playHistory.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = TvDimensions.OverscanVertical
            ),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Quick Sync Banner
        item {
            val syncInteraction = remember { MutableInteractionSource() }
            val isSyncFocused by syncInteraction.collectIsFocusedAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSyncFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color(0xFF1E1E2E))
                    .tvFocusable(isFocused = isSyncFocused, shape = RoundedCornerShape(16.dp))
                    .focusable(interactionSource = syncInteraction)
                    .clickable(interactionSource = syncInteraction, indication = null) {
                        // Open backup import / QR sync
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "Sync with Phone / Import Backup",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import your playlists, liked tracks, and YouTube sessions from phone",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Shelf 1: Playlists
        if (userPlaylists.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Your Playlists",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(userPlaylists, key = { it.url }) { playlist ->
                            TvMediaCard(
                                title = playlist.name,
                                subtitle = if (playlist.itemCount >= 0) "${playlist.itemCount} items" else "Playlist",
                                artworkUrl = playlist.thumbnailUrl,
                                aspectRatio = 1f,
                                cardWidth = 160.dp,
                                onClick = { /* open playlist */ }
                            )
                        }
                    }
                }
            }
        }

        // Shelf 2: Recent Listening History
        if (playHistory.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recent History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(playHistory, key = { it.songId }) { entry ->
                            TvMediaCard(
                                title = entry.title,
                                subtitle = entry.artist,
                                artworkUrl = entry.thumbnailUrl,
                                aspectRatio = 1f,
                                cardWidth = 150.dp,
                                onClick = {
                                    onPlaySong(entry.toSong())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
