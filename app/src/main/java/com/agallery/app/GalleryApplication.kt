package com.agallery.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GalleryApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val repo = GalleryRepository(this, AppDatabase.get(this))
        appScope.launch { repo.purgeExpired() }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
