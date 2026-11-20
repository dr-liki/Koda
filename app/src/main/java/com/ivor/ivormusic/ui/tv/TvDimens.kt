package com.ivor.ivormusic.ui.tv

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 10-foot television scale dimensions for Koda, modeled after OpenStream's TvDimens.
 *
 * Based on the Android TV reference canvas (960x540 dp) and derived from the real window,
 * with 5% overscan margins and horizontal card peek affordances (0.4 card peeking on screen edge).
 */
@Immutable
data class TvDimens(
    val scale: Float,
    val gutter: Dp,
    val gutterVertical: Dp,
    val gutterTight: Dp,
    val railGap: Dp,
    val rowGap: Dp,
    val topBarHeight: Dp,
    /** Portrait poster / square card. Six fit in the rails. */
    val posterCardWidth: Dp,
    val posterCardHeight: Dp,
    /** Landscape 16:9 card. Four fit in the rails. */
    val landscapeCardWidth: Dp,
    val landscapeCardHeight: Dp,
    val cardTitleHeight: Dp,
    val heroHeight: Dp,
    val buttonHeight: Dp,
    val chipHeight: Dp,
    val focusRingWidth: Dp,
    val focusScale: Float,
    val minTarget: Dp,
    val navRailWidth: Dp,
    val navRailItemHeight: Dp,
    val sidePanelWidth: Dp,
    val listRowHeight: Dp,
    val listRowGap: Dp,
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val cardElevation: Dp,
    val keyboardKeyWidth: Dp,
    val keyboardKeyHeight: Dp,
    val progressBarHeight: Dp,
    val avatarSize: Dp,
    val dividerThickness: Dp
) {
    companion object {
        const val POSTERS_PER_RAIL = 6
        const val LANDSCAPE_PER_RAIL = 4
        private const val PEEK = 0.4f

        fun of(widthDp: Int, heightDp: Int): TvDimens {
            val scale = (widthDp / 960f).coerceIn(0.78f, 1.35f)
            val short = (heightDp / 540f).coerceIn(0.62f, 1.3f)
            val s = minOf(scale, short)

            fun v(base: Float): Dp = (base * s).dp

            val gutter = (widthDp * 0.05f).dp
            val railGap = v(14f)
            val safeWidth = widthDp.dp - gutter * 2
            val posterCardWidth = (safeWidth - railGap * POSTERS_PER_RAIL) / (POSTERS_PER_RAIL + PEEK)
            val landscapeCardWidth = (safeWidth - railGap * LANDSCAPE_PER_RAIL) / (LANDSCAPE_PER_RAIL + PEEK)
            val listRowHeight = v(64f)

            return TvDimens(
                scale = s,
                gutter = gutter,
                gutterVertical = (heightDp * 0.05f).dp,
                gutterTight = v(24f),
                railGap = railGap,
                rowGap = v(24f),
                topBarHeight = v(56f),
                posterCardWidth = posterCardWidth,
                posterCardHeight = posterCardWidth, // 1:1 square for album artwork
                landscapeCardWidth = landscapeCardWidth,
                landscapeCardHeight = landscapeCardWidth * 9 / 16, // 16:9 for videos
                cardTitleHeight = v(24f),
                heroHeight = (heightDp * 0.48f).dp,
                buttonHeight = v(48f),
                chipHeight = v(38f),
                focusRingWidth = v(3.5f),
                focusScale = 1.08f,
                minTarget = v(48f),
                navRailWidth = v(76f),
                navRailItemHeight = v(52f),
                sidePanelWidth = v(420f),
                listRowHeight = listRowHeight,
                listRowGap = v(10f),
                paddingSmall = v(8f),
                paddingMedium = v(16f),
                paddingLarge = v(24f),
                cardElevation = v(16f),
                keyboardKeyWidth = v(52f),
                keyboardKeyHeight = v(44f),
                progressBarHeight = v(6f),
                avatarSize = v(64f),
                dividerThickness = v(1f)
            )
        }
    }
}

val LocalTvDimens = staticCompositionLocalOf { TvDimens.of(960, 540) }

/** Standard layout offsets and overscan values for 1080p TV reference space. */
object TvDimensions {
    val OverscanHorizontal: Dp = 48.dp
    val OverscanVertical: Dp = 27.dp
    val CardSpacing: Dp = 16.dp
    val CardCornerRadius: Dp = 12.dp
    val FocusBorderWidth: Dp = 3.5.dp
    const val FocusedScale: Float = 1.08f
}

object TvLayout {
    val ContentStart: Dp = 96.dp
    val PageStart: Dp = 56.dp
    val ContentEnd: Dp = 48.dp
    val PosterWidth: Dp = 104.dp
    val PosterHeight: Dp = 156.dp
    val LandscapeWidth: Dp = 196.dp
    val LandscapeHeight: Dp = 110.dp
    val CardGap: Dp = 14.dp
    val CardShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
}

