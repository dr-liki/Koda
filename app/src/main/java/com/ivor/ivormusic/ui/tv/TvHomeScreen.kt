package com.ivor.ivormusic.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel
import kotlinx.coroutines.delay

/**
 * 10-foot Android TV Home Screen displaying full Koda features:
 * - Mode Switcher: Music Mode vs Video Mode
 * - In Video Mode: Recommended Videos, Trending Videos
 * - In Music Mode: Quick Picks, Mixes, Playlists, Recently Played
 * - Responsive 10-foot dimensions with 0.4 card peek affordance
 */
@Composable
fun TvHomeScreen(
    homeViewModel: HomeViewModel,
    onPlaySong: (Song) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalTvDimens.current

    val youtubeSongs by homeViewModel.youtubeSongs.collectAsState()
    val trendingVideos by homeViewModel.trendingVideos.collectAsState()
    val userPlaylists by homeViewModel.userPlaylists.collectAsState()
    val songsHistory by homeViewModel.songs.collectAsState()

    var isVideoMode by remember { mutableStateOf(false) }

    val firstCardFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadYouTubeRecommendations()
        homeViewModel.applyVideoRecommendationsPreference(true)
    }

    LaunchedEffect(trendingVideos, youtubeSongs, isVideoMode) {
        val hasContent = if (isVideoMode) trendingVideos.isNotEmpty() else youtubeSongs.isNotEmpty()
        if (hasContent && !hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            delay(150)
            runCatching { firstCardFocusRequester.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = dimens.gutterVertical, bottom = dimens.gutterVertical),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.rowGap)
    ) {
        // Top Header: Mode Switcher Chips (Music Mode vs Video Mode)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.gutter),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvButton(
                    onClick = { isVideoMode = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (!isVideoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (!isVideoMode) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Music Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                TvButton(
                    onClick = { isVideoMode = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isVideoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isVideoMode) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Video Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // ==================== VIDEO MODE CONTENT ====================
        if (isVideoMode) {
            // Shelf 1: Recommended Videos
            item {
                TvShelf(title = "Recommended Videos", gutter = dimens.gutter) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dimens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(dimens.railGap)
                    ) {
                        if (trendingVideos.isNotEmpty()) {
                            itemsIndexed(trendingVideos, key = { _, video -> video.videoId }) { index, video ->
                                val durationFormatted = if (video.duration > 0) {
                                    "${video.duration / 60}:${String.format("%02d", video.duration % 60)}"
                                } else null

                                TvMediaCard(
                                    title = video.title,
                                    subtitle = video.channelName,
                                    artworkUrl = video.thumbnailUrl,
                                    badgeText = durationFormatted,
                                    aspectRatio = 16f / 9f,
                                    cardWidth = dimens.landscapeCardWidth,
                                    modifier = if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier,
                                    onClick = { onPlayVideo(video) }
                                )
                            }
                        } else {
                            items(5) {
                                TvMediaCardSkeleton(cardWidth = dimens.landscapeCardWidth, aspectRatio = 16f / 9f)
                            }
                        }
                    }
                }
            }

            // Shelf 2: Trending Across YouTube
            item {
                TvShelf(title = "Trending", gutter = dimens.gutter) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dimens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(dimens.railGap)
                    ) {
                        if (trendingVideos.size > 5) {
                            items(trendingVideos.drop(5), key = { it.videoId }) { video ->
                                TvMediaCard(
                                    title = video.title,
                                    subtitle = video.channelName,
                                    artworkUrl = video.thumbnailUrl,
                                    aspectRatio = 16f / 9f,
                                    cardWidth = dimens.landscapeCardWidth,
                                    onClick = { onPlayVideo(video) }
                                )
                            }
                        } else {
                            items(4) {
                                TvMediaCardSkeleton(cardWidth = dimens.landscapeCardWidth, aspectRatio = 16f / 9f)
                            }
                        }
                    }
                }
            }
        }

        // ==================== MUSIC MODE CONTENT ====================
        if (!isVideoMode) {
            // Shelf 1: Quick Picks For You
            item {
                TvShelf(title = "Quick Picks", gutter = dimens.gutter) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dimens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(dimens.railGap)
                    ) {
                        if (youtubeSongs.isNotEmpty()) {
                            itemsIndexed(youtubeSongs, key = { _, song -> song.id }) { index, song ->
                                TvMediaCard(
                                    title = song.title,
                                    subtitle = song.artist,
                                    artworkUrl = song.thumbnailUrl,
                                    aspectRatio = 1f,
                                    cardWidth = dimens.posterCardWidth,
                                    modifier = if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier,
                                    onClick = { onPlaySong(song) }
                                )
                            }
                        } else {
                            items(6) {
                                TvMediaCardSkeleton(cardWidth = dimens.posterCardWidth, aspectRatio = 1f)
                            }
                        }
                    }
                }
            }

            // Shelf 2: Playlists & Personal Mixes
            if (userPlaylists.isNotEmpty()) {
                item {
                    TvShelf(title = "Playlists & Mixes", gutter = dimens.gutter) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = dimens.gutter),
                            horizontalArrangement = Arrangement.spacedBy(dimens.railGap)
                        ) {
                            items(userPlaylists, key = { it.url }) { playlist ->
                                TvMediaCard(
                                    title = playlist.name,
                                    subtitle = if (playlist.itemCount >= 0) "${playlist.itemCount} items" else "Playlist",
                                    artworkUrl = playlist.thumbnailUrl,
                                    aspectRatio = 1f,
                                    cardWidth = dimens.posterCardWidth,
                                    onClick = {
                                        // Load and play playlist
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Shelf 3: Recently Played Songs
            if (songsHistory.isNotEmpty()) {
                item {
                    TvShelf(title = "Recently Played", gutter = dimens.gutter) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = dimens.gutter),
                            horizontalArrangement = Arrangement.spacedBy(dimens.railGap)
                        ) {
                            items(songsHistory.take(15), key = { it.id }) { song ->
                                TvMediaCard(
                                    title = song.title,
                                    subtitle = song.artist,
                                    artworkUrl = song.thumbnailUrl,
                                    aspectRatio = 1f,
                                    cardWidth = dimens.posterCardWidth,
                                    onClick = { onPlaySong(song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TvShelf(
    title: String,
    gutter: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = gutter, bottom = 12.dp)
        )
        content()
    }
}
