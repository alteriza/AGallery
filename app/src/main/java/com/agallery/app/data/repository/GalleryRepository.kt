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
            Log.e("GalleryRepository", "queryMedia failed", e)
            emptyList()
        }
        syncBucketAlbums(photos)
        photos
    }

    private suspend fun syncBucketAlbums(photos: List<Photo>) {
        try {
            val buckets = photos.groupBy { it.bucketId }
            for ((bucketId, items) in buckets) {
                try {
                    val existing = db.albumDao().findByBucket(bucketId)
                    if (existing == null) {
                        val name = items.firstOrNull()?.bucket?.ifBlank { "Folder $bucketId" }
                            ?: "Folder $bucketId"
                        db.albumDao().insertIgnore(AlbumEntity(name = name, bucketId = bucketId))
                    }
                } catch (e: Exception) {
                    Log.e("GalleryRepository", "sync bucket $bucketId failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("GalleryRepository", "syncBucketAlbums failed", e)
        }
    }

    suspend fun setPinned(albumId: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        runCatching { db.albumDao().setPinned(albumId, pinned) }
    }

    fun observeAlbumPhotos(albumId: Long): Flow<List<AlbumPhotoEntity>> =
        db.albumDao().observePhotosInAlbum(albumId)

    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        runCatching { db.albumDao().insert(AlbumEntity(name = name)) }.getOrDefault(-1L)
    }

    suspend fun deleteAlbum(id: Long) = withContext(Dispatchers.IO) {
        runCatching {
            db.albumDao().deleteRefsForAlbum(id)
            db.albumDao().deleteAlbum(id)
        }
    }

    suspend fun addPhotoToAlbum(albumId: Long, uri: Uri) = withContext(Dispatchers.IO) {
        runCatching {
            db.albumDao().addPhoto(AlbumPhotoEntity(albumId, uri.toString()))
        }
    }

    suspend fun moveToTrash(photo: Photo) = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            db.trashDao().insert(
                TrashEntity(
                    photoUri = photo.uri.toString(),
                    displayName = photo.name,
                    deletedAt = now,
                    expiresAt = now + TRASH_TTL_MS
                )
            )
        }
    }

    suspend fun restoreFromTrash(uri: String) = withContext(Dispatchers.IO) {
        runCatching { db.trashDao().remove(uri) }
    }

    suspend fun removeFromTrashDb(uriString: String) = withContext(Dispatchers.IO) {
        runCatching { db.trashDao().remove(uriString) }
    }

    suspend fun deleteFilePermanently(
        uriString: String,
        attemptMediaStoreDelete: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        var mediaStoreOk = true
        if (attemptMediaStoreDelete) {
            mediaStoreOk = runCatching {
                context.contentResolver.delete(Uri.parse(uriString), null, null) > 0
            }.getOrDefault(false)
        }
        runCatching { db.trashDao().remove(uriString) }
        runCatching { db.albumDao().removePhotoByUri(uriString) }
        mediaStoreOk
    }

    suspend fun deleteFileDirect(uriString: String): Boolean = withContext(Dispatchers.IO) {
        deleteFilePermanently(uriString, attemptMediaStoreDelete = true)
    }

    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        runCatching {
            val expired = db.trashDao().getExpired(System.currentTimeMillis())
            expired.forEach { item ->
                val deleted = runCatching {
                    context.contentResolver.delete(Uri.parse(item.photoUri), null, null) > 0
                }.getOrDefault(false)
                if (deleted) {
                    runCatching { db.trashDao().remove(item.photoUri) }
                    runCatching { db.albumDao().removePhotoByUri(item.photoUri) }
                }
            }
        }.onFailure { Log.e("GalleryRepository", "purgeExpired failed", it) }
    }

    suspend fun saveBitmapToGallery(bitmap: Bitmap, displayName: String): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AGallery")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = resolver.insert(collection, values) ?: return@runCatching null
                resolver.openOutputStream(uri)?.use { out: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            }.getOrNull()
        }
}
