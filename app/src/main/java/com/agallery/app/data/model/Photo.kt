package com.agallery.app.data.model

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val bucket: String,
    val isVideo: Boolean = false,
    val duration: Long = 0L
)

data class AlbumUi(
    val id: Long,
    val name: String,
    val bucketId: String?,
    val photoCount: Int,
    val isPinned: Boolean,
    val thumbnails: List<Uri>
)
