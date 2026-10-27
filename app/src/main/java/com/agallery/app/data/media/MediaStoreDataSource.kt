package com.agallery.app.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.agallery.app.data.model.Photo

class MediaStoreDataSource(private val context: Context) {

    fun queryMedia(): List<Photo> {
        val images = queryCollection(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            isVideo = false
        )
        val videos = queryCollection(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            isVideo = true
        )
        return (images + videos).sortedByDescending { it.dateTaken }
    }

    private fun queryCollection(collection: Uri, isVideo: Boolean): List<Photo> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val durationCol = if (isVideo) "duration" else null
        val fullProjection = if (durationCol != null) projection + durationCol else projection

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val result = mutableListOf<Photo>()

        context.contentResolver.query(collection, fullProjection, null, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val wCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val bucketIdCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durCol = durationCol?.let { c.getColumnIndex(it) } ?: -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result += Photo(
                    id = id,
                    uri = uri,
                    name = c.getString(nameCol) ?: (if (isVideo) "VID_$id" else "IMG_$id"),
                    dateTaken = c.getLong(takenCol).takeIf { it > 0 }
                        ?: (c.getLong(addedCol) * 1000L),
                    size = c.getLong(sizeCol),
                    width = c.getInt(wCol),
                    height = c.getInt(hCol),
                    bucketId = c.getString(bucketIdCol) ?: "0",
                    bucket = c.getString(bucketCol) ?: "Unknown",
                    isVideo = isVideo,
                    duration = if (durCol >= 0) c.getLong(durCol) else 0L
                )
            }
        }
        return result
    }
}
