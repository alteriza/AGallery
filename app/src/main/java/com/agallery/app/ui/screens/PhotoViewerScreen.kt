package com.agallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.agallery.app.ui.components.glassHazeStyle
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.LocalHazeState
import com.agallery.app.ui.theme.LocalGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun PhotoViewerScreen(
    vm: GalleryViewModel,
    startIndex: Int,
    albumId: Long? = null,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val photos by vm.viewerPhotos(albumId).collectAsState()
    val localHazeState = remember { HazeState() }

    var hadPhotos by remember { mutableStateOf(false) }
    LaunchedEffect(photos.isNotEmpty()) {
        if (photos.isNotEmpty()) {
            hadPhotos = true
        } else if (hadPhotos) {
            onBack()
        }
    }

    if (photos.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val safeStart = startIndex.coerceIn(0, photos.size - 1)

    CompositionLocalProvider(LocalHazeState provides localHazeState) {
        ViewerContent(
            photos = photos,
            startIndex = safeStart,
            hazeState = localHazeState,
            onBack = onBack,
            onEdit = onEdit,
            onDelete = { photo ->
                vm.moveToTrash(photo)
                onBack()
            }
        )
    }
}

@Composable
private fun ViewerContent(
    photos: List<Photo>,
    startIndex: Int,
    hazeState: HazeState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty() && pagerState.currentPage >= photos.size) {
            pagerState.scrollToPage(photos.size - 1)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                        VideoPlayer(photo = photo, isActive = page == pagerState.currentPage)
                    } else {
                        ZoomableImage(photo)
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .hazeEffect(
                    state = hazeState,
                    style = glassHazeStyle()
                )
                .background(LocalGlassColors.current.bgTop.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
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
            if (current != null && !current.isVideo) {
                IconButton(onClick = { onEdit(current.uri.toString()) }) {
                    Icon(Icons.Filled.Tune, "Edit", tint = Color.White)
                }
            }
            if (current != null) {
                IconButton(onClick = { onDelete(current) }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                }
            }
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
            .pointerInput(scale) {
                if (scale > 1f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
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
private fun VideoPlayer(photo: Photo, isActive: Boolean) {
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
