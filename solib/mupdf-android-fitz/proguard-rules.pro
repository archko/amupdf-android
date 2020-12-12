# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

## JNI referenced classes

-keepclassmembers public class com.artifex.mupdf.fitz.* {
    private long pointer;
    <init>(...);
    public <methods>;
    public <fields>;
    private <fields>;
}


## library defaults (from ProGuard manual)

-keep public class com.artifex.mupdf.fitz.* {
    public protected *;
}

-keepclassmembernames class com.artifex.mupdf.fitz.* {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames,includedescriptorclasses class com.artifex.mupdf.fitz.* {
    native <methods>;
}

-keepclassmembers,allowoptimization enum com.artifex.mupdf.fitz.* {
    public static **[] values(); public static ** valueOf(java.lang.String);
}

-keepclassmembers class com.artifex.mupdf.fitz.* implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

