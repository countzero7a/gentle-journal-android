# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin serialization
# SQLCipher
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# AndroidX Security / Crypto
-keep class androidx.security.crypto.** { *; }
# Tink (used by security-crypto) references compile-time-only annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Default Android rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
