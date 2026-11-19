package com.ivor.ivormusic.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

object TvDimensions {
    /**
     * Standard TV overscan margins to prevent clipping on television screens.
     */
    val OverscanHorizontal: Dp = 48.dp
    val OverscanVertical: Dp = 27.dp

    val CardCornerRadius: Dp = 12.dp
    val FocusedScale: Float = 1.06f
}

/**
 * Reusable focus highlight modifier for TV interactive elements.
 * Provides a 1.06x scale spring, elevation shadow, and bright focus outline.
 */
fun Modifier.tvFocusable(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(TvDimensions.CardCornerRadius),
    focusedBorderColor: Color = Color(0xFF6C5CE7),
    focusedScale: Float = TvDimensions.FocusedScale
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_card_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier
                    .shadow(elevation = 12.dp, shape = shape)
                    .border(BorderStroke(2.5.dp, focusedBorderColor), shape)
            } else {
                Modifier
            }
        )
        .clip(shape)
}
