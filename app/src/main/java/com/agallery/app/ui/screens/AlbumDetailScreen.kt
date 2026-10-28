package com.agallery.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    vm: GalleryViewModel,
    albumId: Long,
    albumName: String,
    onBack: () -> Unit,
    onOpenViewer: (Int) -> Unit
) {
    val photosFlow = remember(albumId) { vm.viewerPhotos(albumId) }
    val photos by photosFlow.collectAsState()
    val ctx = LocalContext.current

    var selectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(photos) {
        if (selectionMode) {
            val validUris = photos.map { it.uri.toString() }.toSet()
            val cleaned = selectedUris.intersect(validUris)
            if (cleaned != selectedUris) selectedUris = cleaned
            if (cleaned.isEmpty()) {
                selectionMode = false
            }
        }
    }

    fun toggle(photo: Photo) {
        val uri = photo.uri.toString()
        selectedUris = if (uri in selectedUris) selectedUris - uri
        else selectedUris + uri
    }

    fun exit() {
        selectionMode = false
        selectedUris = emptySet()
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {

            if (selectionMode) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .glass(RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = ::exit) {
                        Icon(Icons.Filled.Close, "Cancel", tint = Color.White)
                    }
                    Text(
                        "${selectedUris.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        color = Color.White
                    )
                    TextButton(onClick = {
                        selectedUris = photos.map { it.uri.toString() }.toSet()
                    }) { Text("All", color = Color.White) }
                    IconButton(onClick = {
                        val items = photos.filter { it.uri.toString() in selectedUris }
                        vm.moveMultipleToTrash(items)
                        exit()
                    }) {
                        Icon(Icons.Filled.DeleteOutline, "Delete", tint = Color.White)
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .glass(RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        albumName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }

            if (photos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items in this album yet.", color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(photos, key = { _, p -> p.uri.toString() }) { index, photo ->
                        val uri = photo.uri.toString()
                        val selected = uri in selectedUris
                        Box(
                            Modifier.aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) toggle(photo)
                                        else onOpenViewer(index)
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedUris = setOf(uri)
                                        } else {
                                            toggle(photo)
                                        }
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(photo.uri)
                                    .apply { if (photo.isVideo) videoFrameMillis(1000) }
                                    .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (photo.isVideo) {
                                Box(
                                    Modifier.align(Alignment.Center).size(32.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                                }
                            }
                            if (selected) {
                                Box(
                                    Modifier.fillMaxSize().background(Color(0x557C5CFF))
                                )
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C5CFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
