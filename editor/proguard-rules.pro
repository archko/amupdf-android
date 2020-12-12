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


#### General
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes Exceptions
-keepclassmembers enum com.artifex.sonui.editor.* {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}

-dontnote kankan.wheel.widget.**
-dontnote android.net.http.**
-dontnote org.apache.http.**

## library defaults (from ProGuard manual)

-keep public class com.artifex.sonui.editor.* {
    public protected *;
}

-keepclassmembernames class com.artifex.sonui.editor.* {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames,includedescriptorclasses class com.artifex.sonui.editor.* {
    native <methods>;
}

-keepclassmembers,allowoptimization enum com.artifex.sonui.editor.* {
    public static **[] values(); public static ** valueOf(java.lang.String);
}

-keepclassmembers class com.artifex.sonui.editor.* implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocListPagesView { void setMainView(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocView { void setHost(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DragHandle { void setDragHandleListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.NUIView { void setOnDoneListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.Slider { void setSliderEventListener(...); }

-keep, includedescriptorclasses class com.artifex.sonui.editor.AnimationLayerView { void render(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocPageView { void render(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocView { void setShowKeyboardListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.ListWheelDialog { void show(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormCheckboxEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormCheckboxEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormTextEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.PDFFormTextEditor { void start(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.SODocSession { SODocSession(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.SlideShowPageLayout { void render(...); }
-keepclassmembers class com.artifex.sonui.editor.NUICertificateAdapter {
    android.view.View itemView;
}
-keep, includedescriptorclasses class com.artifex.sonui.editor.NUICertificateAdapter { void notifyItemChanged(...);
    int getAdapterPosition();
    int getLayoutPosition();}

## Spongycastle rules
-keep class org.spongycastle.crypto.* {*;}
-keep class org.spongycastle.crypto.agreement.** {*;}
-keep class org.spongycastle.crypto.digests.* {*;}
-keep class org.spongycastle.crypto.ec.* {*;}
-keep class org.spongycastle.crypto.encodings.* {*;}
-keep class org.spongycastle.crypto.engines.* {*;}
-keep class org.spongycastle.crypto.macs.* {*;}
-keep class org.spongycastle.crypto.modes.* {*;}
-keep class org.spongycastle.crypto.paddings.* {*;}
-keep class org.spongycastle.crypto.params.* {*;}
-keep class org.spongycastle.crypto.prng.* {*;}
-keep class org.spongycastle.crypto.signers.* {*;}

-keep class org.spongycastle.jcajce.provider.** { *; }

-keep class org.spongycastle.jcajce.provider.digest.** {*;}
-keep class org.spongycastle.jcajce.provider.keystore.** {*;}
-keep class org.spongycastle.jcajce.provider.symmetric.** {*;}
-keep class org.spongycastle.jcajce.spec.* {*;}
-keep class org.spongycastle.jce.** {*;}

-dontnote org.spongycastle.**
-dontwarn org.junit.**

-dontwarn javax.naming.**



# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature


# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }


# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }

-dontwarn android.test.**

##
