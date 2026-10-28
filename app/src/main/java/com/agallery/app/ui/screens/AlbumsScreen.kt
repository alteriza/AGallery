package com.agallery.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import coil.request.videoFramePercent
import com.agallery.app.data.model.AlbumUi
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    vm: GalleryViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (Long, String) -> Unit
) {
    val albums by vm.albums.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp)
                    .glass(RoundedCornerShape(22.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    color = Color.White
                )
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, "New album", tint = Color.White)
                }
            }

            if (albums.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No albums yet.", color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onOpenAlbum(album.id, album.name) },
                            onTogglePin = { vm.togglePin(album.id, !album.isPinned) }
                        )
                    }
                }
            }
        }

        if (showCreate) {
            AlertDialog(
                onDismissRequest = { showCreate = false },
                title = { Text("New Album", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Album name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.createAlbum(newName)
                        newName = ""
                        showCreate = false
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreate = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(album: AlbumUi, onClick: () -> Unit, onTogglePin: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .glass(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onTogglePin)
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            when {
                album.thumbnails.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) { Text("—", color = Color.White) }
                }
                album.thumbnails.size < 4 -> {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(album.thumbnails.first())
                            .size(450)
                            .videoFramePercent(0.15)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.weight(1f)) {
                            Thumb(album.thumbnails[0], Modifier.weight(1f).fillMaxHeight(), ctx)
                            Spacer(Modifier.width(2.dp))
                            Thumb(album.thumbnails[1], Modifier.weight(1f).fillMaxHeight(), ctx)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.weight(1f)) {
                            Thumb(album.thumbnails[2], Modifier.weight(1f).fillMaxHeight(), ctx)
                            Spacer(Modifier.width(2.dp))
                            Thumb(album.thumbnails[3], Modifier.weight(1f).fillMaxHeight(), ctx)
                        }
                    }
                }
            }
            if (album.isPinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            color = Color.White
        )
        Text(
            if (album.bucketId != null) "${album.photoCount} items (folder)"
            else "${album.photoCount} items",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun Thumb(uri: android.net.Uri, modifier: Modifier, ctx: android.content.Context) {
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(uri)
            .size(450)
            .videoFramePercent(0.15)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}