package com.ivor.ivormusic.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ivor.ivormusic.data.SubscribedChannel
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel

/**
 * 10-foot Android TV Subscriptions Screen.
 * Shows a horizontal rail of subscribed channels with avatar chips,
 * and a responsive grid of latest video uploads from those channels.
 */
@Composable
fun TvSubscriptionsScreen(
    homeViewModel: HomeViewModel,
    onPlayVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val subscribedChannels by homeViewModel.subscribedChannels.collectAsState()
    val subscriptionFeed by homeViewModel.subscriptionFeed.collectAsState()
    val isFeedProgress by homeViewModel.subscriptionFeedProgress.collectAsState()

    var selectedChannelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (subscriptionFeed.isEmpty()) {
            homeViewModel.loadSubscriptionFeed(force = false)
        }
    }

    val displayedVideos = remember(subscriptionFeed, selectedChannelId) {
        if (selectedChannelId == null) {
            subscriptionFeed
        } else {
            subscriptionFeed.filter { it.channelId == selectedChannelId }
        }
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
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subscriptions",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            if (isFeedProgress != null) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Subscribed Channels Avatar Rail
        if (subscribedChannels.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                // "All" filter chip
                item {
                    val isAllSelected = selectedChannelId == null
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { selectedChannelId = null }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        focused -> Color.White
                                        isAllSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color(0xFF1E1E2E)
                                    }
                                )
                                .tvFocusable(isFocused = focused, shape = CircleShape, focusedScale = 1.12f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ALL",
                                color = if (focused) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "All Channels",
                            color = if (focused) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(subscribedChannels, key = { it.channelId }) { channel ->
                    val isSelected = selectedChannelId == channel.channelId
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = {
                                    selectedChannelId = if (isSelected) null else channel.channelId
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E2E))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .tvFocusable(isFocused = focused, shape = CircleShape, focusedScale = 1.12f)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(channel.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = channel.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = channel.name,
                            color = if (focused) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Uploads Feed Grid
        if (displayedVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (subscribedChannels.isEmpty()) {
                        "No subscribed channels yet. Subscribe to channels or import subscriptions in Settings."
                    } else {
                        "No new uploads from this channel."
                    },
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedVideos, key = { it.videoId }) { video ->
                    val durationFormatted = if (video.duration > 0) {
                        "${video.duration / 60}:${String.format("%02d", video.duration % 60)}"
                    } else null

                    TvMediaCard(
                        title = video.title,
                        subtitle = video.channelName,
                        artworkUrl = video.thumbnailUrl,
                        badgeText = durationFormatted,
                        aspectRatio = 16f / 9f,
                        cardWidth = 230.dp,
                        onClick = { onPlayVideo(video) }
                    )
                }
            }
        }
    }
}
