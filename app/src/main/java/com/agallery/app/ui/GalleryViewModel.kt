package com.agallery.app.ui

import android.app.Application
import android.net.Uri
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

    val trash: StateFlow<List<TrashEntity>> = repo.trash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photos: StateFlow<List<Photo>> =
        combine(_raw, repo.trash) { all, trashItems ->
            val trashUris = trashItems.map { it.photoUri }.toSet()
            all.filter { it.uri.toString() !in trashUris }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<AlbumUi>> =
        combine(repo.albumsFlow, photos, repo.albumRefs) { rows, photoList, refs ->
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val viewerCache = ConcurrentHashMap<Long, StateFlow<List<Photo>>>()

    fun viewerPhotos(albumId: Long?): StateFlow<List<Photo>> {
        if (albumId == null) return photos
        return viewerCache.getOrPut(albumId) {
            combine(photos, albums, repo.albumRefs) { photoList, albumList, refs ->
                val album = albumList.find { it.id == albumId } ?: return@combine emptyList()
                if (album.bucketId != null) {
                    photoList.filter { it.bucketId == album.bucketId }
                } else {
                    val uriSet = refs
                        .filter { it.albumId == albumId }
                        .map { it.photoUri }
                        .toSet()
                    photoList.filter { it.uri.toString() in uriSet }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _raw.value = withContext(Dispatchers.IO) { repo.loadMedia() }
            _isLoading.value = false
        }
    }

    fun createAlbum(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.createAlbum(name.trim())
        _message.value = "Album dibuat"
    }

    fun deleteAlbum(id: Long) = viewModelScope.launch {
        repo.deleteAlbum(id)
        viewerCache.remove(id)
        _message.value = "Album dihapus"
    }

    fun togglePin(albumId: Long, pinned: Boolean) = viewModelScope.launch {
        repo.setPinned(albumId, pinned)
    }

    fun addPhotoToAlbum(albumId: Long, uri: Uri) = viewModelScope.launch {
        repo.addPhotoToAlbum(albumId, uri)
        _message.value = "Ditambahkan ke album"
    }

    fun moveToTrash(photo: Photo) = viewModelScope.launch {
        repo.moveToTrash(photo)
        _message.value = "Dipindahkan ke Trash"
    }

    fun restoreFromTrash(uri: String) = viewModelScope.launch {
        repo.restoreFromTrash(uri)
        _message.value = "Dipulihkan"
    }

    fun finalizeTrashDeletion(uri: String) = viewModelScope.launch {
        repo.removeFromTrashDb(uri)
        _message.value = "Dihapus permanen"
    }

    fun deleteFileDirect(uri: String) = viewModelScope.launch {
        val ok = repo.deleteFileDirect(uri)
        repo.removeFromTrashDb(uri)
        _message.value = if (ok) "Dihapus permanen" else "Hanya dihapus dari Trash"
    }

    fun saveEditedBitmap(bitmap: android.graphics.Bitmap, onSaved: (Uri?) -> Unit) =
        viewModelScope.launch {
            val uri = repo.saveBitmapToGallery(bitmap, "AGallery_${System.currentTimeMillis()}.jpg")
            refresh()
            onSaved(uri)
        }

    fun clearMessage() { _message.value = null }
}
