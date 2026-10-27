package com.agallery.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass
import java.util.concurrent.TimeUnit

@Composable
fun TrashScreen(
    vm: GalleryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val trash by vm.trash.collectAsState()

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp)
                    .glass(RoundedCornerShape(22.dp)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                Text("Trash (7 hari)", style = MaterialTheme.typography.titleLarge)
            }

            if (trash.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trash kosong.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trash, key = { it.photoUri }) { item ->
                        val daysLeft = TimeUnit.MILLISECONDS.toDays(item.expiresAt - System.currentTimeMillis())
                            .coerceAtLeast(0)
                        Row(
                            Modifier.fillMaxWidth()
                                .glass(RoundedCornerShape(16.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.photoUri,
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text("Hapus dalam $daysLeft hari", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { vm.restoreFromTrash(item.photoUri) }) {
                                Icon(Icons.Filled.Restore, "Restore")
                            }
                            IconButton(onClick = { vm.deletePermanently(item.photoUri) }) {
                                Icon(Icons.Filled.DeleteForever, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}