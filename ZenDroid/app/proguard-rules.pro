# Add project specific ProGuard rules here.
# Keep Room entities
-keep class com.zendroid.nmapgui.data.model.** { *; }

# Keep Gson serialization classes
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep XML Parser classes
-keep class org.xmlpull.** { *; }

# Keep NmapExecutor and related classes
-keep class com.zendroid.nmapgui.domain.executor.** { *; }

# Keep data classes for Room
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
