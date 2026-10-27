package com.agallery.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("""
        SELECT albums.*, 
          (SELECT COUNT(*) FROM album_photos WHERE album_photos.albumId = albums.id) AS photoCount
        FROM albums 
        ORDER BY albums.isPinned DESC, albums.createdAt DESC
    """)
    fun observeAlbums(): Flow<List<AlbumWithCount>>

    @Query("SELECT * FROM albums WHERE bucketId = :bucketId LIMIT 1")
    suspend fun findByBucket(bucketId: String): AlbumEntity?

    @Query("UPDATE albums SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT * FROM album_photos")
    fun observeAllRefs(): Flow<List<AlbumPhotoEntity>>

    @Insert
    suspend fun insert(album: AlbumEntity): Long

    @Insert
    suspend fun insertIgnore(album: AlbumEntity): Long

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteAlbum(id: Long)

    @Query("DELETE FROM album_photos WHERE albumId = :id")
    suspend fun deleteRefsForAlbum(id: Long)

    @Query("SELECT * FROM album_photos WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun observePhotosInAlbum(albumId: Long): Flow<List<AlbumPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhoto(ref: AlbumPhotoEntity): Long

    @Query("DELETE FROM album_photos WHERE albumId = :albumId AND photoUri = :uri")
    suspend fun removePhoto(albumId: Long, uri: String)
}

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashEntity)

    @Query("DELETE FROM trash WHERE photoUri = :uri")
    suspend fun remove(uri: String)

    @Query("SELECT * FROM trash WHERE expiresAt < :now")
    suspend fun getExpired(now: Long): List<TrashEntity>
}
