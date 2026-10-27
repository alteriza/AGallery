package com.agallery.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    startIndex: Int,
    vm: GalleryViewModel = viewModel(),
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val photos by vm.photos.collectAsState()
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = photos.getOrNull(page) ?: return@HorizontalPager
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            AsyncImage(
                model = photo.uri,
                contentDescription = photo.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(photo.uri) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offset.x; translationY = offset.y
                    }
            )
        }

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .glass(RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                photos.getOrNull(pagerState.currentPage)?.let { onEdit(it.uri.toString()) }
            }) { Icon(Icons.Filled.Tune, contentDescription = "Edit", tint = Color.White) }
            IconButton(onClick = {
                photos.getOrNull(pagerState.currentPage)?.let { vm.moveToTrash(it); onBack() }
            }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White) }
        }
    }
}