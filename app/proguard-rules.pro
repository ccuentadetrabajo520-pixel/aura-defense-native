# Aura Defense — ProGuard / R8 Keep Rules

# Timber
-dontwarn timber.log.Timber
-keep class timber.log.TimberTree { *; }

# ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-dontwarn com.google.mlkit.vision.barcode.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keepclassmembers class * extends androidx.work.Worker { public <init>(android.content.Context,androidx.work.WorkerParameters); }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
        volatile <fields>;
}

# Kotlin Serialization (if used)
-dontwarn kotlinx.serialization.**

# Model classes used in serialization/parcelling
-keepclassmembers class com.aura.defense.** {
        <init>(...);
}

# Keep Compose runtime stable
-dontwarn androidx.compose.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
