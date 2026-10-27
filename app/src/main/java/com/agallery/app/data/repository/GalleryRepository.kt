package com.agallery.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.agallery.app.data.local.AlbumEntity
import com.agallery.app.data.local.AlbumPhotoEntity
import com.agallery.app.data.local.AlbumWithCount
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.local.TrashEntity
import com.agallery.app.data.media.MediaStoreDataSource
import com.agallery.app.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.OutputStream

class GalleryRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val mediaStore = MediaStoreDataSource(context)
    private val TRASH_TTL_MS = 7L * 24 * 60 * 60 * 1000

    val albumsFlow: Flow<List<AlbumWithCount>> = db.albumDao().observeAlbums()
    val trash: Flow<List<TrashEntity>> = db.trashDao().observeTrash()
    val albumRefs: Flow<List<AlbumPhotoEntity>> = db.albumDao().observeAllRefs()

    suspend fun loadMedia(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = try {
            mediaStore.queryMedia()
        } catch (e: Exception) {
            Log.e("GalleryRepository",
