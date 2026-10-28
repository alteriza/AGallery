package com.agallery.app.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
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
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.GlassBackground
import com.agallery.app.ui.components.glass
import com.agallery.app.util.hasManageStoragePermission
import com.agallery.app.util.openManageStorageSettings
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    vm: GalleryViewModel,
    onBack: () -> Unit
) {
    val trash by vm.trash.collectAsState()
    val context = LocalContext.current

    var selectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uris = pendingUris
        pendingUris = emptyList()
        if (result.resultCode == Activity.RESULT_OK && uris.isNotEmpty()) {
            vm.finalizeMultipleTrashDeletion(uris)
        }
    }

    fun exit() {
        selectionMode = false
        selectedUris = emptySet()
    }

    fun toggle(uri: String) {
        selectedUris = if (uri in selectedUris) selectedUris - uri
        else selectedUris + uri
    }

    fun deleteUris(uris: List<String>) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStoragePermission(context)) {
            val pi = MediaStore.createDeleteRequest(
                context.contentResolver,
                uris.map { Uri.parse(it) }
            )
            pendingUris = uris
            deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        } else {
            vm.deleteMultiplePermanently(uris)
            exit()
        }
    }

    LaunchedEffect(trash) {
        if (selectionMode) {
            val validUris = trash.map { it.photoUri }.toSet()
            val cleaned = selectedUris.intersect(validUris)
            if (cleaned != selectedUris) selectedUris = cleaned
            if (cleaned.isEmpty()) {
                selectionMode = false
            }
        }
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
                        selectedUris = trash.map { it.photoUri }.toSet()
                    }) { Text("All", color = Color.White) }
                    IconButton(onClick = {
                        deleteUris(selectedUris.toList())
                    }) {
                        Icon(Icons.Filled.DeleteForever, "Delete", tint = Color.White)
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
                        "Trash (7 days)",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = Color.White
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        !hasManageStoragePermission(context)
                    ) {
                        IconButton(onClick = { openManageStorageSettings(context) }) {
                            Icon(
                                Icons.Filled.Settings,
                                "Storage settings",
                                tint = Color.White
                            )
                        }
                    }
                }
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
                        val uri = item.photoUri
                        val selected = uri in selectedUris
                        val daysLeft = TimeUnit.MILLISECONDS
                            .toDays(item.expiresAt - System.currentTimeMillis())
                            .coerceAtLeast(0)
                        Row(
                            Modifier.fillMaxWidth()
                                .glass(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) toggle(uri)
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedUris = setOf(uri)
                                        } else {
                                            toggle(uri)
                                        }
                                    }
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uri)
                                        .videoFrameMillis(1000)
                                        .build(),
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selected) {
                                    Box(
                                        Modifier.fillMaxSize()
                                            .background(Color(0x557C5CFF))
                                    )
                                    Box(
                                        Modifier.align(Alignment.Center).size(22.dp)
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
                            IconButton(onClick = { vm.restoreFromTrash(uri) }) {
                                Icon(Icons.Filled.Restore, "Restore", tint = Color.White)
                            }
                            IconButton(onClick = { deleteUris(listOf(uri)) }) {
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
