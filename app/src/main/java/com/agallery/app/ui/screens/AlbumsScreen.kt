package com.agallery.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.agallery.app.data.model.AlbumUi
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    vm: GalleryViewModel = viewModel(),
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
                    .glass(RoundedCornerShape(22.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                Text("Albums", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, "New album") }
            }

            if (albums.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada album. Tambahkan foto dari galeri untuk memulai.")
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
                title = { Text("Album Baru") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama album") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.createAlbum(newName)
                        newName = ""; showCreate = false
                    }) { Text("Buat") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreate = false }) { Text("Batal") }
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
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
        ) {
            when {
                album.thumbnails.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) { Text("—") }
                }
                album.thumbnails.size < 4 -> {
                    // 1 foto besar
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(album.thumbnails.first())
                            .videoFrameMillis(1000)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // 4 foto grid 2x2
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
        Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(
            if (album.bucketId != null) "${album.photoCount} item (folder)"
            else "${album.photoCount} item",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun Thumb(uri: android.net.Uri, modifier: Modifier, ctx: android.content.Context) {
    AsyncImage(
        model = ImageRequest.Builder(ctx).data(uri).videoFrameMillis(1000).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
