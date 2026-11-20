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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivor.ivormusic.BuildConfig
import com.ivor.ivormusic.ui.theme.ThemeViewModel

private enum class SettingsSection(val title: String, val icon: ImageVector) {
    PLAYBACK("Playback & Video", Icons.Rounded.PlayCircle),
    AUDIO("Audio & Sound", Icons.Rounded.GraphicEq),
    CACHE("Storage & Cache", Icons.Rounded.Storage),
    ABOUT("About & Info", Icons.Rounded.Info)
}

/**
 * 10-foot Android TV Settings Screen modeled after OpenStream.
 * Features a split two-pane layout: categories on the left, settings controls on the right.
 */
@Composable
fun TvSettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(SettingsSection.PLAYBACK) }

    val sponsorBlockEnabled by themeViewModel.sponsorBlockEnabled.collectAsState()
    val normalizeVolume by themeViewModel.normalizeVolume.collectAsState()
    val cacheSizeBytes by themeViewModel.currentCacheSizeBytes.collectAsState()
    val videoQualityWifi by themeViewModel.videoQualityWifi.collectAsState()

    val cacheSizeMb = remember(cacheSizeBytes) {
        String.format("%.1f MB", cacheSizeBytes / (1024.0 * 1024.0))
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
        // Left Column: Category Selection Pane
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingsSection.values().forEach { section ->
                val isSelected = section == selectedSection
                val interaction = remember { MutableInteractionSource() }
                val isFocused by interaction.collectIsFocusedAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isFocused -> Color.White
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF1E1E2E)
                            }
                        )
                        .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                        .focusable(interactionSource = interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { selectedSection = section }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = if (isFocused) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = section.title,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.width(36.dp))

        // Right Column: Settings Detail Pane
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedSection) {
                SettingsSection.PLAYBACK -> {
                    item {
                        TvSettingToggleItem(
                            title = "SponsorBlock Integration",
                            subtitle = "Automatically skip sponsor segments, intros, and non-music music video sections",
                            checked = sponsorBlockEnabled,
                            onCheckedChange = { themeViewModel.setSponsorBlockEnabled(it) }
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Default Video Resolution",
                            value = videoQualityWifi.ifEmpty { "1080p (Full HD)" }
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Media Engine",
                            value = "AndroidX Media3 ExoPlayer + Hardware Acceleration"
                        )
                    }
                }

                SettingsSection.AUDIO -> {
                    item {
                        TvSettingToggleItem(
                            title = "Audio Volume Normalization",
                            subtitle = "Balance sound dynamics to prevent sudden loud or quiet audio between tracks",
                            checked = normalizeVolume,
                            onCheckedChange = { themeViewModel.setNormalizeVolume(it) }
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Audio Decoding",
                            value = "High-Fidelity Opus / AAC 256kbps"
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Karaoke Lyrics",
                            value = "Synchronized scrolling with Romanization enabled"
                        )
                    }
                }

                SettingsSection.CACHE -> {
                    item {
                        TvSettingCard(
                            title = "Cached Media Size",
                            value = cacheSizeMb
                        )
                    }

                    item {
                        val interaction = remember { MutableInteractionSource() }
                        val isFocused by interaction.collectIsFocusedAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFocused) Color.White else Color(0xFF1E1E2E))
                                .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(12.dp), focusedScale = 1.02f)
                                .focusable(interactionSource = interaction)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = { themeViewModel.clearCacheAction() }
                                )
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Media Cache",
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Frees up temporary audio/video buffer data without affecting downloads",
                                    color = if (isFocused) Color.DarkGray else Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = null,
                                tint = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                SettingsSection.ABOUT -> {
                    item {
                        TvSettingCard(
                            title = "Koda Version",
                            value = "${BuildConfig.VERSION_NAME} (Release ${BuildConfig.VERSION_CODE})"
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Interface Mode",
                            value = "Unified Phone + Android TV (OpenStream Engine)"
                        )
                    }

                    item {
                        TvSettingCard(
                            title = "Fork Repository",
                            value = "https://github.com/dr-liki/Koda"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White else Color(0xFF1E1E2E))
            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(12.dp), focusedScale = 1.02f)
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = if (isFocused) Color.Black else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = if (isFocused) Color.DarkGray else Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null, // Controlled by Row click
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                checkedTrackColor = if (isFocused) Color.LightGray else MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun TvSettingCard(
    title: String,
    value: String
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White else Color(0xFF1E1E2E))
            .tvFocusable(isFocused = isFocused, shape = RoundedCornerShape(12.dp), focusedScale = 1.02f)
            .focusable(interactionSource = interaction)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = if (isFocused) Color.Black else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = if (isFocused) Color.DarkGray else MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
