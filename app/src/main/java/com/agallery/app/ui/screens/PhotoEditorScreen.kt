package com.agallery.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.glass
import com.agallery.app.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoEditorScreen(
    uriString: String,
    vm: GalleryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var crop by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        bitmap = withContext(Dispatchers.IO) { BitmapUtils.decode(context, Uri.parse(uriString)) }
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview + crop overlay
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    var mode = 0 // 1=move, 2=TL,3=TR,4=BL,5=BR
                    detectDragGestures(
                        onDragStart = { pos ->
                            val r = Rect(crop.left * size.width, crop.top * size.height,
                                         crop.right * size.width, crop.bottom * size.height)
                            val t = 40f
                            mode = when {
                                (pos - Offset(r.left, r.top)).getDistance() < t -> 2
                                (pos - Offset(r.right, r.top)).getDistance() < t -> 3
                                (pos - Offset(r.left, r.bottom)).getDistance() < t -> 4
                                (pos - Offset(r.right, r.bottom)).getDistance() < t -> 5
                                else -> 1
                            }
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            val dx = drag.x / size.width
                            val dy = drag.y / size.height
                            crop = when (mode) {
                                1 -> crop.translate(dx, dy).clampToUnit()
                                2 -> Rect((crop.left + dx).coerceIn(0f, crop.right - 0.1f),
                                          (crop.top + dy).coerceIn(0f, crop.bottom - 0.1f),
                                          crop.right, crop.bottom)
                                3 -> Rect(crop.left, (crop.top + dy).coerceIn(0f, crop.bottom - 0.1f),
                                          (crop.right + dx).coerceIn(crop.left + 0.1f, 1f), crop.bottom)
                                4 -> Rect((crop.left + dx).coerceIn(0f, crop.right - 0.1f), crop.top,
                                          crop.right, (crop.bottom + dy).coerceIn(crop.top + 0.1f, 1f))
                                else -> Rect(crop.left, crop.top,
                                             (crop.right + dx).coerceIn(crop.left + 0.1f, 1f),
                                             (crop.bottom + dy).coerceIn(crop.top + 0.1f, 1f))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                AsyncImage(
                    model = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToScale(brightness, brightness, brightness, 1f) }
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotation
                            scaleX = if (flipH) -1f else 1f
                        }
                )
            }
            if (containerSize != IntSize.Zero) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = Rect(crop.left * size.width, crop.top * size.height,
                                 crop.right * size.width, crop.bottom * size.height)
                    drawRect(Color.Black.copy(alpha = 0.55f))
                    drawRect(
                        color = Color.White,
                        topLeft = r.topLeft,
                        size = r.size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                }
            }
        }

        // Toolbar glass
        Column(
            Modifier.fillMaxWidth().padding(12.dp).glass(RoundedCornerShape(24.dp)).padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                    Icon(Icons.Filled.RotateRight, "Rotate", tint = Color.White)
                }
                IconButton(onClick = { flipH = !flipH }) {
                    Icon(Icons.Filled.Flip, "Flip", tint = Color.White)
                }
                IconButton(onClick = { crop = Rect(0f, 0f, 1f, 1f) }) {
                    Icon(Icons.Filled.CropFree, "Reset crop", tint = Color.White)
                }
            }
            Text("Brightness", color = Color.White, style = MaterialTheme.typography.labelSmall)
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = 0.4f..1.6f
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Batal", color = Color.White) }
                Button(
                    enabled = !saving && bitmap != null,
                    onClick = {
                        val src = bitmap ?: return@Button
                        saving = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                var out = BitmapUtils.rotateAndFlip(src, rotation, flipH)
                                out = BitmapUtils.crop(out, crop.left, crop.top, crop.right, crop.bottom)
                                out = BitmapUtils.applyBrightness(out, brightness)
                                out
                            }
                            vm.saveEditedBitmap(result) {
                                saving = false
                                onBack()
                            }
                        }
                    }
                ) { Text(if (saving) "Menyimpan..." else "Simpan") }
            }
        }
    }
}

private fun Rect.translate(dx: Float, dy: Float): Rect =
    Rect(left + dx, top + dy, right + dx, bottom + dy).clampToUnit()

private fun Rect.clampToUnit(): Rect {
    val w = width; val h = height
    val l = left.coerceIn(0f, 1f - w)
    val t = top.coerceIn(0f, 1f - h)
    return Rect(l, t, l + w, t + h)
}
