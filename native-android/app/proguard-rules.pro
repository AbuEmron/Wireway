# Keep kotlinx.serialization generated serializers (used by supabase-kt models).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable model classes in this app intact for reflection-free JSON.
-keep,includedescriptorclasses class com.wirewaypro.app.**$$serializer { *; }
-keepclassmembers class com.wirewaypro.app.** {
    *** Companion;
}
