package com.agallery.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.agallery.app.data.local.*
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

    val albums: Flow<List<AlbumWithCount>> = db.albumDao().observeAlbums()
    val trash: Flow<List<TrashEntity>> = db.trashDao().observeTrash()

    fun loadPhotos(): List<Photo> = mediaStore.queryPhotos()

    fun observeAlbumPhotos(albumId: Long): Flow<List<AlbumPhotoEntity>> =
        db.albumDao().observePhotosInAlbum(albumId)

    suspend fun createAlbum(name: String): Long =
        withContext(Dispatchers.IO) { db.albumDao().insert(AlbumEntity(name = name)) }

    suspend fun deleteAlbum(id: Long) = withContext(Dispatchers.IO) {
        db.albumDao().deleteRefsForAlbum(id)
        db.albumDao().deleteAlbum(id)
    }

    suspend fun addPhotoToAlbum(albumId: Long, uri: Uri) = withContext(Dispatchers.IO) {
        db.albumDao().addPhoto(AlbumPhotoEntity(albumId, uri.toString()))
        Unit
    }

    /** Soft delete: catat ke trash, foto asli tetap ada di MediaStore. */
    suspend fun moveToTrash(photo: Photo) = withContext(Dispatchers.IO) {
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

    suspend fun restoreFromTrash(uri: String) = withContext(Dispatchers.IO) {
        db.trashDao().remove(uri)
    }

    /** Hapus permanen dari device + dari tabel trash. */
    suspend fun deletePermanently(uri: String) = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) }
        db.trashDao().remove(uri)
    }

    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        val expired = db.trashDao().getExpired(System.currentTimeMillis())
        expired.forEach { item ->
            runCatching { context.contentResolver.delete(Uri.parse(item.photoUri), null, null) }
            db.trashDao().remove(item.photoUri)
        }
    }

    /** Simpan bitmap hasil edit ke Pictures/AGallery via MediaStore (tanpa izin tulis di Q+). */
    suspend fun saveBitmapToGallery(bitmap: Bitmap, displayName: String): Uri? =
        withContext(Dispatchers.IO) {
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

            val uri = resolver.insert(collection, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        }
}