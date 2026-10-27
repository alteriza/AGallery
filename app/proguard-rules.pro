# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Kotlin + Compose
-keep class kotlin.Metadata { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil (ImageLoader)
-keep class coil.** { *; }
-keep class coil.decode.** { *; }
-keep class coil.request.** { *; }
-keep class coil.transform.** { *; }
-keepnames class coil.** { *; }
-dontwarn coil.**

# Media3 ExoPlayer (video playback + PlayerView)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# Haze (glassmorphism blur)
-keep class dev.chrisbanes.haze.** { *; }
-keepnames class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# Accompanist permissions
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# App sendiri (ViewModel, Repository, dll)
-keep class com.agallery.app.** { *; }

# OkHttp/Okio (transitive dari Coil)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
