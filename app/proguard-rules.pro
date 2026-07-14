-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.impl.**
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.goterl.lazysodium.**

# JNA loads its native bridge and maps structures via reflection.
# Without these rules R8 strips/renames JNA in release builds and the app
# crashes on startup while StealthApp initializes libsodium.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure { *; }
-keepclassmembers class * implements com.sun.jna.Library { *; }
-keepclassmembers class * implements com.sun.jna.Callback { *; }
-dontwarn com.sun.jna.**
-dontwarn java.awt.**
