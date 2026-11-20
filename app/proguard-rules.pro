# Room, CameraX and Compose ship their own consumer rules. Only kotlinx.serialization needs help
# for the backup model, which is (de)serialized reflectively through generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class io.github.petrf22.vernostnikarta.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.petrf22.vernostnikarta.**$$serializer { *; }

# Readable stack traces in Play Console crash reports.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
