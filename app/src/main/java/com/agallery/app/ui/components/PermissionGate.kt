package com.agallery.app.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    onPermissionsGranted: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val perms = when {
        Build.VERSION.SDK_INT >= 34 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val state = rememberMultiplePermissionsState(perms)
    val granted = state.permissions.any { it.status.isGranted }

    var notified by remember { mutableStateOf(false) }
    LaunchedEffect(granted) {
        if (granted && !notified) {
            notified = true
            onPermissionsGranted()
        }
    }

    if (granted) {
        content()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Izin akses galeri & video diperlukan untuk menampilkan media.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { state.launchMultiplePermissionRequest() }) {
                    Text("Beri Izin")
                }
            }
        }
    }
}
