# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Repackage classes into the top-level.
-repackageclasses
-keepattributes *Annotation*,Signature,InnerClasses,Exceptions
-optimizations !code/allocation/variable,!class/*,!method/*

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class * extends android.view.View


-keep class android.arch.core.** { *; }
-keep class android.arch.core.executor.** { *; }
-keep class android.arch.core.internal.** { *; }
-keep class android.arch.core.util.** { *; }
-keep class android.arch.lifecycle.** { *; }
-keep class android.arch.lifecycle.livedata.** { *; }
-keep class android.arch.lifecycle.livedata.core.** { *; }
-keep class android.arch.lifecycle.viewmodel.** { *; }

-keep class androidx.core.internal.** { *; }
-keep class androidx.versionedparcelable.** { *; }

-keep class com.google.android.material.internal.** { *; }


-keep class com.squareup.okhttp.** { *; }
-keep class com.squareup.okhttp.internal.** { *; }
-keep class com.squareup.okhttp.internal.framed.** { *; }
-keep class com.squareup.okhttp.internal.http.** { *; }
-keep class com.squareup.okhttp.internal.io.** { *; }
-keep class com.squareup.okhttp.internal.tls.** { *; }


-keep class javax.validation.** { *; }
-keep class javax.validation.bootstrap.** { *; }
-keep class javax.validation.executable.** { *; }
-keep class javax.validation.groups.** { *; }
-keep class javax.validation.metadata.** { *; }
-keep class javax.validation.spi.** { *; }
-keep class kotlin.** { *; }
-keep class kotlin.collections.** { *; }
-keep class kotlin.comparisons.** { *; }
-keep class kotlin.concurrent.** { *; }
-keep class kotlin.coroutines.experimental.** { *; }
-keep class kotlin.coroutines.experimental.intrinsics.** { *; }
-keep class kotlin.coroutines.experimental.jvm.internal.** { *; }
-keep class kotlin.experimental.** { *; }
-keep class kotlin.internal.** { *; }
-keep class kotlin.internal.contracts.** { *; }
-keep class kotlin.io.** { *; }
-keep class kotlin.jvm.** { *; }
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.internal.markers.** { *; }
-keep class kotlin.jvm.internal.unsafe.** { *; }
-keep class kotlin.math.** { *; }
-keep class kotlin.properties.** { *; }
-keep class kotlin.ranges.** { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.sequences.** { *; }
-keep class kotlin.system.** { *; }
-keep class kotlin.text.** { *; }


-keep class okhttp3.** { *; }
-keep class okhttp3.internal.** { *; }
-keep class okhttp3.internal.cache.** { *; }
-keep class okhttp3.internal.cache2.** { *; }
-keep class okhttp3.internal.connection.** { *; }
-keep class okhttp3.internal.http.** { *; }
-keep class okhttp3.internal.http1.** { *; }
-keep class okhttp3.internal.http2.** { *; }
-keep class okhttp3.internal.io.** { *; }
-keep class okhttp3.internal.platform.** { *; }
-keep class okhttp3.internal.proxy.** { *; }
-keep class okhttp3.internal.publicsuffix.** { *; }
-keep class okhttp3.internal.tls.** { *; }
-keep class okhttp3.internal.ws.** { *; }
-keep class org.intellij.lang.annotations.** { *; }

-keep class com.artifex.mupdf.fitz.** { *; }

#======================== =======================
-keep class cn.archko.pdf.entity.**{*;}
-keep class cn.archko.pdf.widgets.**{*;}

-keep class com.umeng.** {*;}

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#SDK 9.2.4及以上版本自带oaid采集模块，不再需要开发者再手动引用oaid库，所以可以不添加这些混淆
-keep class com.zui.**{*;}
-keep class com.miui.**{*;}
-keep class com.heytap.**{*;}
-keep class a.**{*;}
-keep class com.vivo.**{*;}

-keep public class cn.archko.mupdf.R$*{
public static final int *;
}