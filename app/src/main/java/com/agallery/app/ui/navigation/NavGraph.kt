package com.agallery.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agallery.app.IncomingRequest
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.EditorOverlay
import com.agallery.app.ui.components.PermissionGate
import com.agallery.app.ui.components.ViewerOverlay
import com.agallery.app.ui.screens.AlbumDetailScreen
import com.agallery.app.ui.screens.AlbumsScreen
import com.agallery.app.ui.screens.PhotoGridScreen
import com.agallery.app.ui.screens.TrashScreen

data class ViewerRequest(
    val startIndex: Int,
    val albumId: Long?,
    val incomingUri: String?
)

@Composable
fun AGalleryNavHost(
    incomingRequest: IncomingRequest? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val vm: GalleryViewModel = viewModel()

    var viewerState by remember { mutableStateOf<ViewerRequest?>(null) }
    var editorState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(incomingRequest) {
        incomingRequest?.let { req ->
            viewerState = ViewerRequest(startIndex = 0, albumId = null, incomingUri = req.uri)
        }
    }

    val overlayActive = viewerState != null || editorState != null

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (overlayActive) Modifier.blur(10.dp) else Modifier)
                .background(
                    if (overlayActive) Color.Black.copy(alpha = 0.25f) else Color.Transparent
                )
        ) {
            PermissionGate(onPermissionsGranted = { vm.refresh() }) {
                NavHost(navController = navController, startDestination = "grid") {

                    composable("grid") {
                        PhotoGridScreen(
                            vm = vm,
                            onPhotoClick = { index ->
                                viewerState = ViewerRequest(index, null, null)
                            },
                            onNavigateAlbums = { navController.navigate("albums") },
                            onNavigateTrash = { navController.navigate("trash") }
                        )
                    }

                    composable("albums") {
                        AlbumsScreen(
                            vm = vm,
                            onBack = { navController.popBackStack() },
                            onOpenAlbum = { id, name ->
                                navController.navigate(
                                    "album/$id?name=${android.net.Uri.encode(name)}"
                                )
                            }
                        )
                    }

                    composable(
                        "album/{id}?name={name}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.LongType },
                            navArgument("name") {
                                type = NavType.StringType
                                defaultValue = "Album"
                            }
                        )
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        val name = entry.arguments?.getString("name") ?: "Album"
                        AlbumDetailScreen(
                            vm = vm,
                            albumId = id,
                            albumName = name,
                            onBack = { navController.popBackStack() },
                            onOpenViewer = { index ->
                                viewerState = ViewerRequest(index, id, null)
                            }
                        )
                    }

                    composable("trash") {
                        TrashScreen(
                            vm = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        viewerState?.let { state ->
            ViewerOverlay(
                vm = vm,
                startIndex = state.startIndex,
                albumId = state.albumId,
                incomingUri = state.incomingUri,
                onDismiss = { viewerState = null },
                onEdit = { uri ->
                    viewerState = null
                    editorState = uri
                }
            )
        }

        editorState?.let { uri ->
            EditorOverlay(
                vm = vm,
                uriString = uri,
                onDismiss = { editorState = null }
            )
        }
    }
}