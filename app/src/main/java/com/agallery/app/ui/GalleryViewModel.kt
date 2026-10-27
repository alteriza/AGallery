package com.agallery.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agallery.app.data.local.*
import com.agallery.app.data.model.AlbumUi
import com.agallery.app.data.model.Photo
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GalleryRepository(app.applicationContext, AppDatabase.get(app.applicationContext))

    private val _raw = MutableStateFlow<List<Photo>>(emptyList())
    val raw: StateFlow<List<Photo>> = _raw.asStateFlow()

    /** Foto untuk grid & viewer: exclude yang ada di trash. */
    val photos: StateFlow<List<Photo>> = combine(_raw, repo.trash) { all, trash ->
        val trashUris = trash.map { it.photoUri }.toSet()
        all.filter { it.uri.toString() !in trashUris }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val trash: StateFlow<List<TrashEntity>> = repo.trash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Gabungan manual + auto bucket album, urut pinned dulu. */
    val albums: StateFlow<List<AlbumUi>> = combine(
        repo.albumsFlow, photos, repo.albumRefs
    ) { albumEntities, photoList, refs ->
        val byUri = photoList.associateBy { it.uri.toString() }
        val refsByAlbum = refs.groupBy({ it.albumId }, { it.photoUri })
        albumEntities.mapNotNull { row ->
            val entity = row.album
            val photosInAlbum: List<Photo> = if (entity.bucketId != null) {
                photoList.filter { it.bucketId == entity.bucketId }
            } else {
                refsByAlbum[entity.id].orEmpty().mapNotNull { byUri[it] }
            }
            if (photosInAlbum.isEmpty() && !entity.isPinned) return@mapNotNull null
            AlbumUi(
                id = entity.id,
                name = entity.name,
                bucketId = entity.bucketId,
                photoCount = photosInAlbum.size,
                isPinned = entity.isPinned,
                thumbnails = photosInAlbum.take(4).map { it.uri }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _raw.value = withContext(Dispatchers.IO) { repo.loadMedia() }
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _raw.value = withContext(Dispatchers.IO) { repo.loadMedia() }
            _isLoading.value = false
        }
    }

    /** Foto dalam konteks viewer: semua, atau filter per album (bucket / manual). */
    fun viewerPhotos(albumId: Long?): StateFlow<List<Photo>> {
        if (albumId == null) return photos
        return combine(photos, albums, repo.albumRefs) { photoList, albumList, refs ->
            val album = albumList.find { it.id == albumId } ?: return@combine emptyList()
            if (album.bucketId != null) {
                photoList.filter { it.bucketId == album.bucketId }
            } else {
                val uriSet = refs.filter { it.albumId == albumId }
                    .map { it.photoUri }.toSet()
                photoList.filter { it.uri.toString() in uriSet }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun createAlbum(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.createAlbum(name.trim())
        _message.value = "Album \"${name.trim()}\" dibuat"
    }

    fun deleteAlbum(id: Long) = viewModelScope.launch {
        repo.deleteAlbum(id)
        _message.value = "Album dihapus"
    }

    fun togglePin(albumId: Long, pinned: Boolean) = viewModelScope.launch {
        repo.setPinned(albumId, pinned)
        _message.value = if (pinned) "Album di-pin" else "Pin dilepas"
    }

    fun addPhotoToAlbum(albumId: Long, uri: Uri) = viewModelScope.launch {
        repo.addPhotoToAlbum(albumId, uri)
        _message.value = "Foto ditambahkan ke album"
    }

    fun moveToTrash(photo: Photo) = viewModelScope.launch {
        repo.moveToTrash(photo)
        _message.value = "Dipindahkan ke Trash (7 hari)"
    }

    fun restoreFromTrash(uri: String) = viewModelScope.launch {
        repo.restoreFromTrash(uri)
        _message.value = "Foto dipulihkan"
    }

    fun deletePermanently(uri: String) = viewModelScope.launch {
        val ok = repo.deletePermanently(uri)
        _message.value = if (ok) "Foto dihapus permanen"
        else "Tidak bisa hapus — file milik aplikasi lain"
    }

    fun saveEditedBitmap(bitmap: android.graphics.Bitmap, onSaved: (Uri?) -> Unit) =
        viewModelScope.launch {
            val uri = repo.saveBitmapToGallery(bitmap, "AGallery_${System.currentTimeMillis()}.jpg")
            refresh()
            onSaved(uri)
        }

    fun clearMessage() { _message.value = null }
}
