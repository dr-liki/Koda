package com.ivor.ivormusic.ui.tv

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.ui.zIndex

/**
 * Shared TV focus animation: focused items zoom smoothly with a medium bouncy spring.
 */
@Composable
fun tvFocusScale(focused: Boolean, animated: Boolean): Float {
    if (!animated) return 1f
    val dimens = LocalTvDimens.current
    val target = if (focused) dimens.focusScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tvFocusScale"
    )
    return scale
}

/**
 * Reusable focus highlight modifier for TV interactive elements modeled after OpenStream.
 * Provides a 1.08x bouncy spring zoom, z-index lift, natural depth elevation, and a crisp 3dp border.
 */
fun Modifier.tvFocusable(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(TvDimensions.CardCornerRadius),
    focusedBorderColor: Color = Color.White,
    focusedScale: Float = TvDimensions.FocusedScale
): Modifier = composed {
    val animated = rememberTvAnimated()
    val scale = if (animated) {
        val target = if (isFocused) focusedScale else 1.0f
        val animatedScale by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "tv_card_scale"
        )
        animatedScale
    } else {
        if (isFocused) focusedScale else 1.0f
    }

    this
        .zIndex(if (isFocused) 2f else 0f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier
                    .shadow(elevation = 8.dp, shape = shape, clip = false)
                    .border(BorderStroke(3.dp, focusedBorderColor), shape)
            } else {
                Modifier
            }
        )
        .clip(shape)
}

/**
 * High-contrast TV Button with bright focus outline, bouncy zoom, and elevation.
 */
@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusRequester: FocusRequester? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = Color.White
    ),
    focusedColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.Black
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val animated = rememberTvAnimated()
    val dimens = LocalTvDimens.current
    val scale = tvFocusScale(isFocused, animated)
    val highlightBorder = if (isFocused) BorderStroke(dimens.focusRingWidth, Color.White) else null

    var buttonModifier = modifier
    if (focusRequester != null) buttonModifier = buttonModifier.focusRequester(focusRequester)

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = highlightBorder,
        elevation = if (isFocused) ButtonDefaults.buttonElevation(defaultElevation = dimens.cardElevation) else null,
        colors = if (isFocused) focusedColors else colors,
        contentPadding = contentPadding,
        modifier = buttonModifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        content()
    }
}

/**
 * Remote-focusable TV Icon Button with invert effect and white ring.
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val animated = rememberTvAnimated()
    val scale = tvFocusScale(isFocused, animated)

    var btnModifier = modifier
    if (focusRequester != null) btnModifier = btnModifier.focusRequester(focusRequester)

    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isFocused) Color.White else Color.White.copy(alpha = 0.15f),
            contentColor = if (isFocused) Color.Black else Color.White
        ),
        modifier = btnModifier
            .size(size)
            .scale(scale)
            .border(
                border = if (isFocused) BorderStroke(3.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}
