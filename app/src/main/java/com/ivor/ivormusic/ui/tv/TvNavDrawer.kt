package com.ivor.ivormusic.ui.tv

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TvDestination(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    SEARCH("Search", Icons.Rounded.Search),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

/**
 * TV Navigation Drawer that sits on the left of the screen.
 * Expands horizontally when focused to reveal text labels; collapses to icons when content is focused.
 */
@Composable
fun TvNavDrawer(
    selectedDestination: TvDestination,
    onDestinationSelected: (TvDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawerFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isDrawerFocused) 220.dp else 76.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .background(Color(0xFF0F0F16))
            .padding(vertical = TvDimensions.OverscanVertical, horizontal = 12.dp)
            .onFocusChanged { state ->
                isDrawerFocused = state.hasFocus
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo / Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            if (isDrawerFocused) {
                Text(
                    text = "Koda TV",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        // Navigation Items
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvDestination.values().forEach { destination ->
                TvNavItem(
                    destination = destination,
                    isSelected = destination == selectedDestination,
                    isExpanded = isDrawerFocused,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TvNavItem(
    destination: TvDestination,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> Color.White.copy(alpha = 0.25f)
        isSelected -> Color(0xFF1E1E2E)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> Color.White
        isSelected -> Color.White
        else -> Color.LightGray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .tvFocusable(
                isFocused = isFocused,
                shape = RoundedCornerShape(12.dp),
                focusedBorderColor = Color.White,
                focusedScale = 1.04f
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        if (isExpanded) {
            Text(
                text = destination.title,
                color = contentColor,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
