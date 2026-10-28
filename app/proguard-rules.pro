# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room / Entities
-keep class com.kishan9565.expensetracker.data.db.** { *; }

# Serialization / Networking
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Domain Models
-keep class com.kishan9565.expensetracker.domain.model.** { *; }
