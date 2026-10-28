package com.agallery.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlassColors(
    val top: Color,
    val bottom: Color,
    val border: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val content: Color,
    val subtle: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        top = GlassDarkTop,
        bottom = GlassDarkBottom,
        border = GlassDarkBorder,
        bgTop = BgDarkTop,
        bgBottom = BgDarkBottom,
        content = Color.White,
        subtle = Color(0xB3FFFFFF)
    )
}

private val DarkScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentCyan,
    tertiary = AccentPink,
    background = BgDarkTop,
    surface = Color(0xFF14141F),
    surfaceVariant = Color(0xFF1E1E2E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xB3FFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White
)

@Composable
fun AGalleryTheme(content: @Composable () -> Unit) {
    val glass = GlassColors(
        top = GlassDarkTop,
        bottom = GlassDarkBottom,
        border = GlassDarkBorder,
        bgTop = BgDarkTop,
        bgBottom = BgDarkBottom,
        content = Color.White,
        subtle = Color(0xB3FFFFFF)
    )

    CompositionLocalProvider(
        LocalGlassColors provides glass,
        LocalContentColor provides Color.White
    ) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = Typography,
            content = content
        )
    }
}
