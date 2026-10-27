package com.agallery.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agallery.app.ui.theme.LocalGlassColors

/**
 * Glass surface: translucent vertical gradient + subtle 1dp border + soft highlight.
 * Efek "backdrop blur" realistis hanya tersedia API 31+ (RenderEffect).
 * Untuk <31 kita pakai translucent gradient saja — secara visual tetap glassmorphism.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    intensity: Float = 1f
): Modifier {
    val g = LocalGlassColors.current
    val top = g.top.copy(alpha = (g.top.alpha * intensity).coerceIn(0f, 1f))
    val bottom = g.bottom.copy(alpha = (g.bottom.alpha * intensity).coerceIn(0f, 1f))

    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                0f to g.border.copy(alpha = 0.55f),
                0.5f to g.border.copy(alpha = 0.10f),
                1f to g.border.copy(alpha = 0.35f),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            shape = shape
        )
}

/** Backdrop blur opsional — otomatis no-op di API < 31. */
@Composable
fun Modifier.backdropBlur(radius: Dp = 24.dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blur(radius) else this

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.glass(shape), content = content)
}

/** Gradient background + decorative blurred blobs (light/dark aware). */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val g = LocalGlassColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(g.bgTop, g.bgBottom)))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(g.top.copy(alpha = 0.35f), androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(200f, 300f),
                        radius = 900f
                    )
                )
        )
        content()
    }
}