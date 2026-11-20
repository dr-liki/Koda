package com.ivor.ivormusic.ui.tv

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.VideoItem
import com.ivor.ivormusic.ui.home.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KEYBOARD_KEYS: List<List<Char>> = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890").toList().chunked(6)
private val KEY_SIZE: Dp = 40.dp
private val KEY_GAP: Dp = 6.dp

private enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    VIDEOS("Videos"),
    PLAYLISTS("Playlists")
}

/**
 * 10-foot Android TV Search Screen modeled after OpenStream and YouTube TV.
 * Left: D-pad interactive on-screen keyboard + voice search.
 * Right: Filter chips, recent search history, and live search results grid.
 */
@Composable
fun TvSearchScreen(
    homeViewModel: HomeViewModel,
    onPlayVideo: (VideoItem) -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var videoResults by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var songResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val searchHistory by homeViewModel.searchHistory.collectAsState()
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val firstKeyRequester = remember { FocusRequester() }

    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            videoResults = emptyList()
            songResults = emptyList()
            isSearching = false
            return
        }
        isSearching = true
        searchJob = scope.launch {
            delay(300) // Debounce
            try {
                when (selectedFilter) {
                    SearchFilter.ALL -> {
                        val v = homeViewModel.searchVideos(query)
                        val s = homeViewModel.searchYouTube(query)
                        videoResults = v
                        songResults = s
                    }
                    SearchFilter.VIDEOS -> {
                        videoResults = homeViewModel.searchVideos(query)
                        songResults = emptyList()
                    }
                    SearchFilter.SONGS -> {
                        songResults = homeViewModel.searchYouTube(query)
                        videoResults = emptyList()
                    }
                    SearchFilter.PLAYLISTS -> {
                        val vPlaylists = homeViewModel.searchVideoPlaylists(query)
                        videoResults = vPlaylists.map { vp ->
                            VideoItem(
                                videoId = vp.playlistId,
                                title = vp.title,
                                channelName = vp.subtitle ?: vp.videoCountText ?: "Playlist",
                                thumbnailUrl = vp.thumbnailUrl,
                                duration = 0L,
                                viewCount = "",
                                uploadedDate = null
                            )
                        }
                        songResults = emptyList()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(selectedFilter) {
        if (searchQuery.isNotBlank()) performSearch(searchQuery)
    }

    // Voice Search Launcher (Google Assistant / Leanback Speech Recognizer)
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                performSearch(spokenText)
            }
        }
    }

    Row(
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
        // Left Column: TV Keyboard & Voice Search
        Column(
            modifier = Modifier
                .width(KEY_SIZE * 6 + KEY_GAP * 5)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(KEY_GAP)
        ) {
            // Search Query Display & Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E2E))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = searchQuery.ifEmpty { "Search..." },
                    color = if (searchQuery.isEmpty()) Color.Gray else Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Keyboard Keys Grid
            KEYBOARD_KEYS.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                    row.forEachIndexed { colIndex, key ->
                        val isFirst = rowIndex == 0 && colIndex == 0
                        TvKeyButton(
                            label = key.toString(),
                            width = KEY_SIZE,
                            height = KEY_SIZE,
                            modifier = if (isFirst) Modifier.focusRequester(firstKeyRequester) else Modifier,
                            onClick = {
                                searchQuery += key.lowercaseChar()
                                performSearch(searchQuery)
                            }
                        )
                    }
                }
            }

            // Action Row: Space, Backspace, Clear, Voice
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                // Space
                TvKeyButton(
                    icon = Icons.Rounded.SpaceBar,
                    width = KEY_SIZE * 2 + KEY_GAP,
                    height = KEY_SIZE,
                    onClick = {
                        searchQuery += " "
                        performSearch(searchQuery)
                    }
                )

                // Backspace
                TvKeyButton(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    width = KEY_SIZE,
                    height = KEY_SIZE,
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = searchQuery.dropLast(1)
                            performSearch(searchQuery)
                        }
                    }
                )

                // Clear
                TvKeyButton(
                    icon = Icons.Rounded.Clear,
                    width = KEY_SIZE,
                    height = KEY_SIZE,
                    onClick = {
                        searchQuery = ""
                        videoResults = emptyList()
                        songResults = emptyList()
                    }
                )

                // Voice Mic
                TvKeyButton(
                    icon = Icons.Rounded.Mic,
                    width = KEY_SIZE * 2 + KEY_GAP,
                    height = KEY_SIZE,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search Koda")
                        }
                        voiceSearchLauncher.launch(intent)
                    }
                )
            }
        }

        Spacer(Modifier.width(28.dp))

        // Right Column: Filters, History, Results
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(SearchFilter.values()) { filter ->
                    val selected = filter == selectedFilter
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    focused -> Color.White
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> Color(0xFF1E1E2E)
                                }
                            )
                            .tvFocusable(
                                isFocused = focused,
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.05f
                            )
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { selectedFilter = filter }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (focused) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Recent Searches Row (when query is empty)
            if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                Text(
                    text = "Recent Searches",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(searchHistory) { query ->
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (focused) Color.White else Color(0xFF1E1E2E))
                                .tvFocusable(isFocused = focused, shape = RoundedCornerShape(6.dp), focusedScale = 1.05f)
                            .focusable(interactionSource = interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = {
                                    searchQuery = query
                                    performSearch(query)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = query,
                                color = if (focused) Color.Black else Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Results Grid / Loading
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (videoResults.isEmpty() && songResults.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Video Results
                    items(videoResults, key = { "v_" + it.videoId }) { video ->
                        val durationFormatted = if (video.duration > 0) {
                            "${video.duration / 60}:${String.format("%02d", video.duration % 60)}"
                        } else null

                        TvMediaCard(
                            title = video.title,
                            subtitle = video.channelName,
                            artworkUrl = video.thumbnailUrl,
                            badgeText = durationFormatted,
                            aspectRatio = 16f / 9f,
                            cardWidth = 210.dp,
                            onClick = { onPlayVideo(video) }
                        )
                    }

                    // Song Results
                    items(songResults, key = { "s_" + it.id }) { song ->
                        TvMediaCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            aspectRatio = 1f,
                            cardWidth = 180.dp,
                            onClick = { onPlaySong(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    width: Dp = KEY_SIZE,
    height: Dp = KEY_SIZE,
    containerColor: Color = Color(0xFF1E1E2E)
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color.White else containerColor)
            .tvFocusable(isFocused = focused, shape = RoundedCornerShape(6.dp), focusedScale = 1.08f)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                color = if (focused) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
