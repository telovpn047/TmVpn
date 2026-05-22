# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.seyit474.tmvpn.**$$serializer { *; }
-keepclassmembers class com.seyit474.tmvpn.** {
    *** Companion;
}
-keepclasseswithmembers class com.seyit474.tmvpn.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-keep class androidx.compose.** { *; }

# ServerConfig - Aşama 2'de libXray ile reflection olabilir
-keep class com.seyit474.tmvpn.model.** { *; }
