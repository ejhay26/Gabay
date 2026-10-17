# ProGuard / R8 rules for Gabay (dev.gabay.app)

# UniFFI FFI Bridge & JNA
-keep class uniffi.** { *; }
-keep class dev.gabay.app.uniffi.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
    <methods>;
}
-keep interface * extends com.sun.jna.Library { *; }

# MapLibre GL Native
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# Kotlin Coroutines & Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn sun.misc.Unsafe
