package com.agallery.app.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import coil.request.videoFrameMillis
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass
import java.util.concurrent.TimeUnit

@Composable
fun TrashScreen(
    vm: GalleryViewModel,
    onBack: () -> Unit
) {
    val trash by vm.trash.collectAsState()
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<String?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = pendingUri
        pendingUri = null
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            vm.finalizeTrashDeletion(uri)
        }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
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
                    "Trash (7 days)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            if (trash.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trash is empty.", color = Color.White)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trash, key = { it.photoUri }) { item ->
                        val daysLeft = TimeUnit.MILLISECONDS
                            .toDays(item.expiresAt - System.currentTimeMillis())
                            .coerceAtLeast(0)
                        Row(
                            Modifier.fillMaxWidth()
                                .glass(RoundedCornerShape(16.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.photoUri)
                                    .videoFrameMillis(1000)
                                    .build(),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    color = Color.White
                                )
                                Text(
                                    "Deletes in $daysLeft days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { vm.restoreFromTrash(item.photoUri) }) {
                                Icon(Icons.Filled.Restore, "Restore", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val pi = MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        listOf(Uri.parse(item.photoUri))
                                    )
                                    pendingUri = item.photoUri
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(pi.intentSender).build()
                                    )
                                } else {
                                    vm.deleteFileDirect(item.photoUri)
                                }
                            }) {
                                Icon(
                                    Icons.Filled.DeleteForever,
                                    "Delete permanently",
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
