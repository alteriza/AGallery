package com.agallery.app.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.agallery.app.data.model.Photo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun photoFromUri(context: Context, uri: Uri): Photo? {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: return null
    val isVideo = mime.startsWith("video/")

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
    val fullProjection = if (isVideo) projection + "duration" else projection

    return runCatching {
        resolver.query(uri, fullProjection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                ?: uri.lastPathSegment
                ?: "unknown"
            val taken = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN))
            val added = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
            val durCol = if (isVideo) c.getColumnIndex("duration") else -1
            Photo(
                id = id,
                uri = uri,
                name = name,
                dateTaken = taken.takeIf { it > 0 } ?: (added * 1000L),
                size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                width = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)),
                height = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)),
                bucketId = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID))
                    ?: "0",
                bucket = c.getString(
                    c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                ) ?: "Unknown",
                isVideo = isVideo,
                duration = if (durCol >= 0) c.getLong(durCol) else 0L
            )
        }
    }.getOrNull()
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "—"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%d:%02d", m, s)
}

fun formatDate(ms: Long): String {
    if (ms <= 0) return "—"
    val fmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
    return fmt.format(Date(ms))
}
