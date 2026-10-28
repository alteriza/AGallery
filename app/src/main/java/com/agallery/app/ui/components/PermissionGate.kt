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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.agallery.app.util.hasManageStoragePermission
import com.agallery.app.util.openManageStorageSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    onPermissionsGranted: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val perms = remember {
        when {
            Build.VERSION.SDK_INT >= 33 -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val state = rememberMultiplePermissionsState(perms)
    val runtimeGranted = state.permissions.any { it.status.isGranted }

    var manageGranted by remember { mutableStateOf(hasManageStoragePermission(context)) }

    DisposableEffect(Unit) {
        val owner = context as? LifecycleOwner
        if (owner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    manageGranted = hasManageStoragePermission(context)
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
    }

    val needsManage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val allGranted = runtimeGranted && (!needsManage || manageGranted)

    LaunchedEffect(allGranted) {
        if (allGranted) onPermissionsGranted()
    }

    if (allGranted) {
        content()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                when {
                    !runtimeGranted -> {
                        Text(
                            "Media access is required to show your photos and videos.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { state.launchMultiplePermissionRequest() }) {
                            Text("Grant Media Access")
                        }
                    }
                    needsManage && !manageGranted -> {
                        Text(
                            "All Files Access is required for silent deletion and Trash Bin management.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { openManageStorageSettings(context) }) {
                            Text("Grant All Files Access")
                        }
                    }
                }
            }
        }
    }
}