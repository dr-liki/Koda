package com.ivor.ivormusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel

/**
 * Android TV Home Screen displaying horizontal shelves of content:
 * - Recommended Videos
 * - Music Quick Picks
 * - Library Playlists
 */
@Composable
fun TvHomeScreen(
    homeViewModel: HomeViewModel,
    onPlaySong: (Song) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val youtubeSongs by homeViewModel.youtubeSongs.collectAsState()
    val trendingVideos by homeViewModel.trendingVideos.collectAsState()
    val userPlaylists by homeViewModel.userPlaylists.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadYouTubeRecommendations()
        homeViewModel.applyVideoRecommendationsPreference(true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TvDimensions.OverscanVertical, bottom = TvDimensions.OverscanVertical),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Shelf 1: Recommended Videos
        if (trendingVideos.isNotEmpty()) {
            item {
                TvShelf(title = "Recommended Videos") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(trendingVideos, key = { it.videoId }) { video ->
                            val durationFormatted = if (video.duration > 0) {
                                "${video.duration / 60}:${String.format("%02d", video.duration % 60)}"
                            } else null

                            TvMediaCard(
                                title = video.title,
                                subtitle = video.channelName,
                                artworkUrl = video.thumbnailUrl,
                                badgeText = durationFormatted,
                                aspectRatio = 16f / 9f,
                                onClick = { onPlayVideo(video) }
                            )
                        }
                    }
                }
            }
        }

        // Shelf 2: Music Quick Picks
        if (youtubeSongs.isNotEmpty()) {
            item {
                TvShelf(title = "Music For You") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(youtubeSongs, key = { it.id }) { song ->
                            TvMediaCard(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUrl = song.thumbnailUrl,
                                aspectRatio = 1f, // Square for music art
                                cardWidth = 160.dp,
                                onClick = { onPlaySong(song) }
                            )
                        }
                    }
                }
            }
        }

        // Shelf 3: Your Playlists
        if (userPlaylists.isNotEmpty()) {
            item {
                TvShelf(title = "Playlists & Mixes") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(userPlaylists, key = { it.url }) { playlist ->
                            TvMediaCard(
                                title = playlist.name,
                                subtitle = if (playlist.itemCount >= 0) "${playlist.itemCount} items" else "Playlist",
                                artworkUrl = playlist.thumbnailUrl,
                                aspectRatio = 1f,
                                cardWidth = 160.dp,
                                onClick = {
                                    // Hand off or play playlist
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TvShelf(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )
        content()
    }
}
