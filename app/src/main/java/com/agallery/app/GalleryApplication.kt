package com.agallery.app

import android.app.Application
import com.agallery.app.data.local.AppDatabase
import com.agallery.app.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GalleryApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val repo = GalleryRepository(this, AppDatabase.get(this))
        appScope.launch { repo.purgeExpired() }
    }
}
