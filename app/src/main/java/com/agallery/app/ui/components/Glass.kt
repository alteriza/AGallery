package com.agallery.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.agallery.app.ui.theme.LocalGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = compositionLocalOf<HazeState?> { null }

@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    intensity: Float = 1f
): Modifier {
    val g = LocalGlassColors.current
    val hazeState = LocalHazeState.current
    val top = g.top.copy(alpha = (g.top.alpha * intensity).coerceIn(0f, 1f))
    val bottom = g.bottom.copy(alpha = (g.bottom.alpha * intensity).coerceIn(0f, 1f))

    var m = this.clip(shape)
    if (hazeState != null) {
        m = m.hazeEffect(state = hazeState)
    }
    return m
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

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.glass(shape), content = content)
}

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
                        colors = listOf(g.top.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(200f, 300f),
                        radius = 900f
                    )
                )
        )
        content()
    }
}
