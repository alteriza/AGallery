-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn androidx.media3.**

-keep class androidx.compose.** { *; }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}