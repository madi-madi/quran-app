# Retrofit, OkHttp, kotlinx.serialization, Room, WorkManager and Media3 all ship
# their own consumer R8 rules, so only project-specific rules belong here.

# Keep serializers of the API DTOs (accessed reflectively by the Retrofit converter).
-keepclassmembers @kotlinx.serialization.Serializable class sa.com.dreams.quran.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager instantiates workers reflectively by class name.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
