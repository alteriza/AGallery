package com.agallery.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agallery.app.data.local.AlbumWithCount
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.local.AlbumPhotoEntity
import com.agallery.app.data.local.TrashEntity
import com.agallery.app.data.model.Photo
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GalleryRepository(app.applicationContext, AppDatabase.get(app.applicationContext))

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val albums: StateFlow<List<AlbumWithCount>> = repo.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trash: StateFlow<List<TrashEntity>> = repo.trash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _photos.value = withContext(Dispatchers.IO) { repo.loadPhotos() }
            _isLoading.value = false
        }
    }

    fun observeAlbumPhotos(albumId: Long): StateFlow<List<AlbumPhotoEntity>> =
        repo.observeAlbumPhotos(albumId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createAlbum(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.createAlbum(name.trim())
        _message.value = "Album \"${name.trim()}\" dibuat"
    }

    fun deleteAlbum(id: Long) = viewModelScope.launch {
        repo.deleteAlbum(id)
        _message.value = "Album dihapus"
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
        repo.deletePermanently(uri)
        _message.value = "Foto dihapus permanen"
    }

    fun saveEditedBitmap(bitmap: android.graphics.Bitmap, onSaved: (Uri?) -> Unit) =
        viewModelScope.launch {
            val uri = repo.saveBitmapToGallery(bitmap, "AGallery_${System.currentTimeMillis()}.jpg")
            refresh()
            onSaved(uri)
        }

    fun clearMessage() { _message.value = null }
}