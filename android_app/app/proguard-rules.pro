# ProGuard / R8 rules for Gabay (dev.gabay.app)

# UniFFI FFI Bridge & JNA
-keep class dev.gabay.app.uniffi.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# MapLibre GL Native
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# Kotlin Coroutines & Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn sun.misc.Unsafe
