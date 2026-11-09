# The app's own R8 rules. The release build in app/build.gradle.kts applies them after the Android plugin's
# proguard-android-optimize.txt (its proguardFiles setting).

# Keep code readable (no obfuscation), but allow shrinking/optimization
-dontobfuscate

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Parcelable and Serializable (Intents, Navigation)
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Realm model (RealmObject)
-keep class com.erman.usurf.home.data.Favorite { *; }

# External logging bindings referenced but not packaged
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { *; }

# Navigation Safe Args generated (Directions/Args)
-keep class com.erman.usurf.*Directions
-keep class com.erman.usurf.*Args
