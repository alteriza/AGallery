package com.agallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    startIndex: Int,
    albumId: Long? = null,
    vm: GalleryViewModel = viewModel(),
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val photos by vm.viewerPhotos(albumId).collectAsState()

// Pager hanya dibuat SETELAH photos ter-load, sehingga initialPage akurat
    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val safeStart = startIndex.coerceIn(0, photos.size - 1)
    val pagerState = rememberPagerState(initialPage = safeStart) { photos.size }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (photos.isNotEmpty()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val photo = photos.getOrNull(page) ?: return@HorizontalPager
                if (photo.isVideo) {
                    VideoPage(photo)
                } else {
                    ImagePage(photo)
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopCenter)
                .padding(16.dp)
                .glass(RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (photos.isNotEmpty()) {
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
                    IconButton(onClick = { vm.moveToTrash(current); onBack() }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Image page: zoom hanya aktif saat scale > 1f, sehingga saat scale == 1f
 * horizontal pager bebas swipe (fix utama "viewer gak bisa digeser").
 */
@Composable
private fun ImagePage(photo: Photo) {
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
                            scale = 1f; offset = Offset.Zero
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
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            }
    )
}

@Composable
private fun VideoPage(photo: Photo) {
    val context = LocalContext.current
    val exo = remember(photo.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(photo.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(photo.uri) {
        onDispose { exo.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exo
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
