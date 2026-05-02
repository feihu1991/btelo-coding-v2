# Keep Tink classes
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Keep OkHttp classes
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep GitHub API models for app update
-keep class com.btelo.coding.data.remote.GitHubRelease { *; }
-keep class com.btelo.coding.data.remote.GitHubAsset { *; }
