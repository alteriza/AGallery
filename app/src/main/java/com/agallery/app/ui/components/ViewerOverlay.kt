package com.agallery.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.theme.LocalGlassColors
import com.agallery.app.util.photoFromUri
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun ViewerOverlay(
    vm: GalleryViewModel,
    startIndex: Int,
    albumId: Long?,
    incomingUri: String?,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit
) {
    BackHandler { onDismiss() }

    val flowPhotos by vm.viewerPhotos(albumId).collectAsState()
    val context = LocalContext.current
    val localHazeState = remember { HazeState() }

    val photos: List<Photo> = remember(flowPhotos, incomingUri) {
        if (incomingUri == null) flowPhotos
        else {
            val incoming = Uri.parse(incomingUri)
            if (flowPhotos.any { it.uri == incoming }) flowPhotos
            else {
                val extra = photoFromUri(context, incoming)
                if (extra != null) listOf(extra) + flowPhotos else flowPhotos
            }
        }
    }

    LaunchedEffect(photos.isEmpty()) {
        if (photos.isEmpty()) onDismiss()
    }

    if (photos.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val actualStart = remember(photos, incomingUri, startIndex) {
        if (incomingUri != null) {
            val incoming = Uri.parse(incomingUri)
            photos.indexOfFirst { it.uri == incoming }.coerceAtLeast(0)
        } else {
            startIndex.coerceIn(0, photos.size - 1)
        }
    }

    CompositionLocalProvider(LocalHazeState provides localHazeState) {
        ViewerContent(
            photos = photos,
            startIndex = actualStart,
            hazeState = localHazeState,
            onDismiss = onDismiss,
            onEdit = onEdit,
            onDelete = { photo ->
                vm.moveToTrash(photo)
                onDismiss()
            }
        )
    }
}

@Composable
private fun ViewerContent(
    photos: List<Photo>,
    startIndex: Int,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }
    val context = LocalContext.current

    var showActions by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty() && pagerState.currentPage >= photos.size) {
            pagerState.scrollToPage(photos.size - 1)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            HorizontalPager(
                state = pagerState,
                key = { page -> photos.getOrNull(page)?.uri?.toString() ?: "p_$page" },
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos.getOrNull(page)
                if (photo != null) {
                    if (photo.isVideo) {
                        VideoPage(photo = photo, isActive = page == pagerState.currentPage)
                    } else {
                        ZoomableImage(photo)
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .hazeEffect(state = hazeState, style = glassHazeStyle())
                .background(LocalGlassColors.current.bgTop.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${pagerState.currentPage + 1}/${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(8.dp))
            val current = photos.getOrNull(pagerState.currentPage)
            if (current != null) {
                IconButton(onClick = {
                    val mime = context.contentResolver.getType(current.uri)
                        ?: if (current.isVideo) "video/*" else "image/*"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, current.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }) {
                    Icon(Icons.Filled.Share, "Share", tint = Color.White)
                }
                IconButton(onClick = { showActions = true }) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                }
            }
        }

        val current = photos.getOrNull(pagerState.currentPage)

        if (showActions && current != null) {
            AlertDialog(
                onDismissRequest = { showActions = false },
                title = { Text("Actions", color = Color.White) },
                text = {
                    Column {
                        if (!current.isVideo) {
                            TextButton(onClick = {
                                showActions = false
                                onEdit(current.uri.toString())
                            }) { Text("Edit") }
                        }
                        TextButton(onClick = {
                            showActions = false
                            showDetails = true
                        }) { Text("Details") }
                        TextButton(onClick = {
                            showActions = false
                            onDelete(current)
                        }) { Text("Delete") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActions = false }) { Text("Cancel") }
                }
            )
        }

        if (showDetails && current != null) {
            MediaDetailsDialog(photo = current, onDismiss = { showDetails = false })
        }
    }
}

@Composable
private fun ZoomableImage(photo: Photo) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = photo.uri,
        contentDescription = photo.name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) offset += pan else offset = Offset.Zero
                                event.changes.forEach { it.consume() }
                            }
                        } else if (scale > 1f) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                offset += pan
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    )
}

@Composable
private fun VideoPage(photo: Photo, isActive: Boolean) {
    if (!isActive) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PlayArrow,
                null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(72.dp)
            )
        }
        return
    }
    key(photo.uri.toString()) {
        ActiveVideo(photo)
    }
}

@Composable
private fun ActiveVideo(photo: Photo) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            setMediaItem(MediaItem.fromUri(photo.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { exo.release() } }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) offset += pan else offset = Offset.Zero
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.player = exo }
        )
    }
}
