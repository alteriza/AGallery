package com.agallery.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
        top = GlassLightTop, bottom = GlassLightBottom, border = GlassLightBorder,
        bgTop = BgLightTop, bgBottom = BgLightBottom,
        content = Color(0xFF141428), subtle = Color(0x99141428)
    )
}

private val LightScheme = lightColorScheme(
    primary = AccentPurple,
    secondary = AccentCyan,
    tertiary = AccentPink,
    background = BgLightTop,
    surface = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentCyan,
    tertiary = AccentPink,
    background = BgDarkTop,
    surface = Color(0xFF14141F)
)

@Composable
fun AGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val glass = if (darkTheme) GlassColors(
        top = GlassDarkTop, bottom = GlassDarkBottom, border = GlassDarkBorder,
        bgTop = BgDarkTop, bgBottom = BgDarkBottom,
        content = Color.White, subtle = Color(0xB3FFFFFF)
    ) else GlassColors(
        top = GlassLightTop, bottom = GlassLightBottom, border = GlassLightBorder,
        bgTop = BgLightTop, bgBottom = BgLightBottom,
        content = Color(0xFF141428), subtle = Color(0x99141428)
    )

    CompositionLocalProvider(LocalGlassColors provides glass) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}