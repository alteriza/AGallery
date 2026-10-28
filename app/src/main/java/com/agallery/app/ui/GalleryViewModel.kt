package com.agallery.app.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agallery.app.data.local.AlbumPhotoEntity
import com.agallery.app.data.local.AlbumWithCount
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.local.TrashEntity
import com.agallery.app.data.model.AlbumUi
import com.agallery.app.data.model.Photo
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GalleryRepository(
        app.applicationContext,
        AppDatabase.get(app.applicationContext)
    )

    private val _raw = MutableStateFlow<List<Photo>>(emptyList())
    val raw: StateFlow<List<Photo>> = _raw.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val safeTrash = repo.trash.catch { e ->
        Log.e("GalleryViewModel", "trash flow failed", e)
        emit(emptyList())
    }
    private val safeAlbumsFlow = repo.albumsFlow.catch { e ->
        Log.e("GalleryViewModel", "albumsFlow failed", e)
        emit(emptyList<AlbumWithCount>())
    }
    private val safeAlbumRefs = repo.albumRefs.catch { e ->
        Log.e("GalleryViewModel", "albumRefs flow failed", e)
        emit(emptyList<AlbumPhotoEntity>())
    }

    val trash: StateFlow<List<TrashEntity>> = safeTrash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photos: StateFlow<List<Photo>> =
        combine(_raw, safeTrash) { all, trashItems ->
            val trashUris = trashItems.map { it.photoUri }.toSet()
            all.filter { it.uri.toString() !in trashUris }
        }.catch { e ->
            Log.e("GalleryViewModel", "photos flow failed", e)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<AlbumUi>> =
        combine(safeAlbumsFlow, photos, safeAlbumRefs) { rows, photoList, refs ->
            runCatching {
                val byUri = photoList.associateBy { it.uri.toString() }
                val refsByAlbum = refs.groupBy({ it.albumId }, { it.photoUri })
                rows.mapNotNull { row ->
                    val e = row.album
                    val items: List<Photo> =
                        if (e.bucketId != null) photoList.filter { it.bucketId == e.bucketId }
                        else refsByAlbum[e.id].orEmpty().mapNotNull { byUri[it] }
                    if (items.isEmpty() && !e.isPinned) return@mapNotNull null
                    AlbumUi(
                        id = e.id,
                        name = e.name,
                        bucketId = e.bucketId,
                        photoCount = items.size,
                        isPinned = e.isPinned,
                        thumbnails = items.take(4).map { it.uri }
                    )
                }
            }.getOrElse { e ->
                Log.e("GalleryViewModel", "build albums failed", e)
                emptyList()
            }
        }.catch { e ->
            Log.e("GalleryViewModel", "albums flow failed", e)
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val viewerCache = ConcurrentHashMap<Long, StateFlow<List<Photo>>>()

    fun viewerPhotos(albumId: Long?): StateFlow<List<Photo>> {
        if (albumId == null) return photos
        return viewerCache.getOrPut(albumId) {
            combine(photos, albums, safeAlbumRefs) { photoList, albumList, refs ->
                runCatching {
                    val album = albumList.find { it.id == albumId } ?: return@runCatching emptyList()
                    if (album.bucketId != null) {
                        photoList.filter { it.bucketId == album.bucketId }
                    } else {
                        val uriSet = refs.filter { it.albumId == albumId }
                            .map { it.photoUri }
                            .toSet()
                        photoList.filter { it.uri.toString() in uriSet }
                    }
                }.getOrElse { emptyList() }
            }.catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _raw.value = withContext(Dispatchers.IO) { repo.loadMedia() }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "refresh failed", e)
                _raw.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAlbum(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        try {
            repo.createAlbum(name.trim())
            _message.value = "Album created"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "createAlbum failed", e)
        }
    }

    fun deleteAlbum(id: Long) = viewModelScope.launch {
        try {
            repo.deleteAlbum(id)
            viewerCache.remove(id)
            _message.value = "Album deleted"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "deleteAlbum failed", e)
        }
    }

    fun togglePin(albumId: Long, pinned: Boolean) = viewModelScope.launch {
        try {
            repo.setPinned(albumId, pinned)
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "togglePin failed", e)
        }
    }

    fun addPhotoToAlbum(albumId: Long, uri: Uri) = viewModelScope.launch {
        try {
            repo.addPhotoToAlbum(albumId, uri)
            _message.value = "Added to album"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "addPhotoToAlbum failed", e)
        }
    }

    fun moveToTrash(photo: Photo) = viewModelScope.launch {
        try {
            repo.moveToTrash(photo)
            _message.value = "Moved to Trash"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "moveToTrash failed", e)
        }
    }

    fun moveMultipleToTrash(items: List<Photo>) = viewModelScope.launch {
        if (items.isEmpty()) return@launch
        try {
            items.forEach { repo.moveToTrash(it) }
            _message.value = "${items.size} items moved to Trash"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "moveMultipleToTrash failed", e)
        }
    }

    fun restoreFromTrash(uri: String) = viewModelScope.launch {
        try {
            repo.restoreFromTrash(uri)
            _message.value = "Restored"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "restoreFromTrash failed", e)
        }
    }

    fun finalizeTrashDeletion(uri: String) = finalizeMultipleTrashDeletion(listOf(uri))

    fun finalizeMultipleTrashDeletion(uris: List<String>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        try {
            uris.forEach { uri ->
                repo.deleteFilePermanently(uri, attemptMediaStoreDelete = false)
            }
            _raw.value = _raw.value.filter { it.uri.toString() !in uris }
            _message.value = if (uris.size == 1) "Deleted permanently"
            else "${uris.size} items deleted"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "finalizeMultipleTrashDeletion failed", e)
        }
    }

    fun deleteFileDirect(uri: String) = viewModelScope.launch {
        try {
            val ok = repo.deleteFilePermanently(uri, attemptMediaStoreDelete = true)
            _raw.value = _raw.value.filter { it.uri.toString() != uri }
            _message.value = if (ok) "Deleted permanently"
            else "Removed from Trash (file not deleted)"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "deleteFileDirect failed", e)
        }
    }

    fun deleteMultiplePermanently(uris: List<String>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        try {
            var allOk = true
            uris.forEach { uri ->
                val ok = repo.deleteFilePermanently(uri, attemptMediaStoreDelete = true)
                if (!ok) allOk = false
            }
            _raw.value = _raw.value.filter { it.uri.toString() !in uris }
            _message.value = if (allOk) "${uris.size} items deleted"
            else "${uris.size} items processed"
        } catch (e: Exception) {
            Log.e("GalleryViewModel", "deleteMultiplePermanently failed", e)
        }
    }

    fun saveEditedBitmap(bitmap: android.graphics.Bitmap, onSaved: (Uri?) -> Unit) =
        viewModelScope.launch {
            try {
                val uri = repo.saveBitmapToGallery(
                    bitmap,
                    "AGallery_${System.currentTimeMillis()}.jpg"
                )
                refresh()
                onSaved(uri)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "saveEditedBitmap failed", e)
                onSaved(null)
            }
        }

    fun clearMessage() { _message.value = null }
}
