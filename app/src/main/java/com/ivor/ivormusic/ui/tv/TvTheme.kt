package com.ivor.ivormusic.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TV Theme wrapper that provides responsive 10-foot scale tokens and tv.material3 bridge.
 */
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val contentWidth = configuration.screenWidthDp - TvLayout.ContentStart.value.toInt()
    val metrics = remember(contentWidth, configuration.screenHeightDp) {
        TvDimens.of(contentWidth, configuration.screenHeightDp)
    }

    val density = LocalDensity.current
    val pinned = Density(density = density.density, fontScale = 1f)

    CompositionLocalProvider(
        LocalDensity provides pinned,
        LocalTvDimens provides metrics
    ) {
        val colors = MaterialTheme.colorScheme
        androidx.tv.material3.MaterialTheme(
            colorScheme = androidx.tv.material3.darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primaryContainer,
                onPrimaryContainer = colors.onPrimaryContainer,
                secondary = colors.secondary,
                onSecondary = colors.onSecondary,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surfaceContainer,
                onSurface = colors.onSurface
            ),
            content = content
        )
    }
}
