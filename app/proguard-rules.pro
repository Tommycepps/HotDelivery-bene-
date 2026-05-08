# Mantieni i modelli Gson
-keepclassmembers class com.deliverymap.models.** { *; }
-keep class com.deliverymap.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# MapLibre
-keep class org.maplibre.** { *; }
