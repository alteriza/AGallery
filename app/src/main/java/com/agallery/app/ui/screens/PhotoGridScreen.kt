package com.agallery.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.agallery.app.data.model.Photo
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glassHazeStyle
import com.agallery.app.ui.theme.LocalGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoGridScreen(
    vm: GalleryViewModel,
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
    val gridState = rememberLazyGridState()
    val hazeState = remember { HazeState() }

    var menuPhoto by remember { mutableStateOf<Photo?>(null) }
    var showAlbumPicker by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHost.showSnackbar(it)
            vm.clearMessage()
        }
    }

    GlassBackground {
        Box(Modifier.fillMaxSize()) {

            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                if (loading && photos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(
                            top = 88.dp,
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 104.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = photos,
                            key = { _, p -> p.uri.toString() }
                        ) { index, photo ->
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
                                    model = ImageRequest.Builder(context)
                                        .data(photo.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = photo.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (photo.isVideo) {
                                    Box(
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            "Video",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "AGallery",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .hazeEffect(state = hazeState, style = glassHazeStyle())
                    .background(LocalGlassColors.current.bgTop.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavPillButton(
                    icon = Icons.Filled.PhotoAlbum,
                    label = "Albums",
                    onClick = onNavigateAlbums
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(LocalGlassColors.current.border.copy(alpha = 0.3f))
                )
                NavPillButton(
                    icon = Icons.Filled.DeleteOutline,
                    label = "Trash",
                    onClick = onNavigateTrash
                )
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 104.dp)
            )
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
                            vm.moveToTrash(photo)
                            menuPhoto = null
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

@Composable
private fun NavPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
