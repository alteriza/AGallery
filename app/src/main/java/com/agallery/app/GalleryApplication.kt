package com.agallery.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GalleryApplication : Application(), ImageLoaderFactory {

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e("GalleryApplication", "Uncaught in appScope", e)
    }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override fun onCreate() {
        super.onCreate()
        try {
            val repo = GalleryRepository(this, AppDatabase.get(this))
            appScope.launch {
                try {
                    repo.purgeExpired()
                } catch (e: Exception) {
                    Log.e("GalleryApplication", "purgeExpired failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("GalleryApplication", "init repo failed", e)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
