package com.agallery.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.ui.platform.LocalContext
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoGridScreen(
    vm: GalleryViewModel = viewModel(),
    onPhotoClick: (Int) -> Unit,
    onNavigateAlbums: () -> Unit,
    onNavigateTrash: () -> Unit
) {
    val photos by vm.photos.collectAsState()
    val albums by vm.albums.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    // LazyGridState di-remember melewati recomposition & nav return
    val gridState = rememberLazyGridState()

    var menuPhoto by remember { mutableStateOf<Photo?>(null) }
    var showAlbumPicker by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            vm.clearMessage()
        }
    }

    // NOTE: Tidak ada `LaunchedEffect(Unit) { vm.refresh() }` — ViewModel loads di init.
    // Ini yang mencegah grid reset ke atas setelah kembali dari viewer.

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .glass(RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AGallery", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onNavigateAlbums) { Icon(Icons.Filled.PhotoAlbum, "Albums") }
                    IconButton(onClick = onNavigateTrash) { Icon(Icons.Filled.DeleteOutline, "Trash") }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (loading && photos.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(photos, key = { _, p -> p.uri.toString() }) { index, photo ->
                            val model = ImageRequest.Builder(context)
                                .data(if (photo.isVideo) photo.uri else photo.uri)
                                .apply { if (photo.isVideo) videoFrameMillis(1000) }
                                .crossfade(true)
                                .build()

                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .combinedClickable(
                                        onClick = { onPhotoClick(index) },
                                        onLongClick = { menuPhoto = photo }
                                    )
                            ) {
                                AsyncImage(
                                    model = model,
                                    contentDescription = photo.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (photo.isVideo) {
                                    Box(
                                        Modifier.align(Alignment.Center)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        menuPhoto?.let { photo ->
            AlertDialog(
                onDismissRequest = { menuPhoto = null },
                title = { Text(photo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column {
                        TextButton(onClick = { showAlbumPicker = true }) {
                            Text("Tambahkan ke Album")
                        }
                        TextButton(onClick = {
                            vm.moveToTrash(photo); menuPhoto = null
                        }) { Text("Hapus (Trash)") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { menuPhoto = null }) { Text("Tutup") }
                }
            )
        }

        if (showAlbumPicker) {
            AlertDialog(
                onDismissRequest = { showAlbumPicker = false },
                title = { Text("Pilih Album") },
                text = {
                    Column {
                        if (albums.isEmpty()) Text("Belum ada album.")
                        albums.forEach { a ->
                            TextButton(onClick = {
                                menuPhoto?.let { vm.addPhotoToAlbum(a.id, it.uri) }
                                showAlbumPicker = false
                                menuPhoto = null
                            }) { Text("${a.name} (${a.photoCount})") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAlbumPicker = false }) { Text("Batal") }
                }
            )
        }
    }
}
