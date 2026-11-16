# R8 / ProGuard hardening rules for PulseArena release builds

# Repackage obfuscated classes and hide original source filenames
-repackageclasses 'p'
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod

# Strip debug, verbose, and info logging from release APKs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep model classes used with reflection / serialization if needed
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Media3 / ExoPlayer HLS
-keep class androidx.media3.exoplayer.hls.** { *; }
-dontwarn androidx.media3.**

# Firebase AI Logic & App Check
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
