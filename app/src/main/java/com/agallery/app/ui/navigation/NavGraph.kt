package com.agallery.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agallery.app.ui.GalleryViewModel
import com.agallery.app.ui.components.PermissionGate
import com.agallery.app.ui.screens.*

@Composable
fun AGalleryNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // ViewModel di-scope ke Activity (bukan per NavBackStackEntry).
    // Semua screen memakai instance yang sama → data tidak reload saat navigasi.
    val vm: GalleryViewModel = viewModel()

    PermissionGate {
        NavHost(navController = navController, startDestination = "grid", modifier = modifier) {

            composable("grid") {
                PhotoGridScreen(
                    vm = vm,
                    onPhotoClick = { index -> navController.navigate("viewer/$index") },
                    onNavigateAlbums = { navController.navigate("albums") },
                    onNavigateTrash = { navController.navigate("trash") }
                )
            }

            composable(
                "viewer/{index}?albumId={albumId}",
                arguments = listOf(
                    navArgument("index") { type = NavType.IntType },
                    navArgument("albumId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                val index = entry.arguments?.getInt("index") ?: 0
                val albumId = entry.arguments?.getLong("albumId")?.takeIf { it >= 0 }
                PhotoViewerScreen(
                    vm = vm,
                    startIndex = index,
                    albumId = albumId,
                    onBack = { navController.popBackStack() },
                    onEdit = { uri -> navController.navigate("editor?uri=${android.net.Uri.encode(uri)}") }
                )
            }

            composable("albums") {
                AlbumsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { id, name ->
                        navController.navigate("album/$id?name=${android.net.Uri.encode(name)}")
                    }
                )
            }

            composable(
                "album/{id}?name={name}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "Album" }
                )
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val name = entry.arguments?.getString("name") ?: "Album"
                AlbumDetailScreen(
                    vm = vm,
                    albumId = id,
                    albumName = name,
                    onBack = { navController.popBackStack() },
                    onOpenViewer = { index -> navController.navigate("viewer/$index?albumId=$id") }
                )
            }

            composable("trash") {
                TrashScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable(
                "editor?uri={uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                PhotoEditorScreen(
                    vm = vm,
                    uriString = uri,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
