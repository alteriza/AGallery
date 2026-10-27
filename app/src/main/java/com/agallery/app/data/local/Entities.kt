package com.agallery.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bucketId: String? = null,
    val coverUri: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "album_photos", primaryKeys = ["albumId", "photoUri"])
data class AlbumPhotoEntity(
    val albumId: Long,
    val photoUri: String,
    val addedAt: Long = System.currentTimeMillis()
)

/** Soft-delete: foto tetap di MediaStore, hanya dicatat di tabel trash. */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val photoUri: String,
    val displayName: String,
    val deletedAt: Long,
    val expiresAt: Long
)

data class AlbumWithCount(
    @Embedded val album: AlbumEntity,
    val photoCount: Int
)
