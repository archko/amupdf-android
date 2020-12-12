# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

##

-keep, includedescriptorclasses class com.artifex.solib.SODoc { com.artifex.solib.SOPage getPage(...); }
-keep, includedescriptorclasses class com.artifex.solib.SODoc { int saveToInternal(...); }
-keep, includedescriptorclasses class com.artifex.solib.SODoc { int saveToPDFInternal(...); }
-keep, includedescriptorclasses class com.artifex.solib.SODoc { void createInkAnnotation(...); }
-keep, includedescriptorclasses class com.artifex.solib.SODoc { int enumerateToc(...); }
-keep, includedescriptorclasses class com.artifex.solib.SOLib { com.artifex.solib.SODoc openDocumentInternal(...); }
-keep, includedescriptorclasses class com.artifex.solib.SOPage { com.artifex.solib.SORender nativeRenderAtZoom(...); }
-keep, includedescriptorclasses class com.artifex.sonui.SOFileGrid { void setListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.SortOrderMenu { void setSortOrderListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocListPagesView { void setMainView(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocView { void setHost(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DocView { void setDocConfigOptions(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.DragHandle { void setDragHandleListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.NUIView { void setOnDoneListener(...); }
-keep, includedescriptorclasses class com.artifex.sonui.editor.Slider { void setSliderEventListener(...); }

## JNI referenced classes


-keepclasseswithmembernames,includedescriptorclasses class com.artifex.solib.* {
    native <methods>;
    public <fields>;
}

-keep class com.artifex.solib.animation.SOAnimation* {
     <init>(...);
}

-keep class com.artifex.solib.SOLib {
  <init>(...);
  private long internal;
}

-keep, includedescriptorclasses class com.artifex.solib.SOSecureFS {
  <methods>;
  <fields>;
 }
-keep class com.artifex.solib.SOSecureFS$FileAttributes {
  <methods>;
  <fields>;
 }

-keep class com.artifex.solib.SODoc$SOSelectionContext {
  <methods>;
  <fields>;
 }

-keepclassmembers class com.artifex.solib.SODoc {
  private long internal;
  <init>(...);
  *** searchProgress(...);
}

-keepclassmembers class com.artifex.solib.SODocLoadListenerInternal {
  *** progress(...);
  *** error(...);
  *** onSelectionChanged(...);
  *** onLayoutCompleted(...);
  *** setDoc(...);
}

-keepclassmembers class com.artifex.solib.SOEnumerateTocListener {
    *** nextTocEntry(...);
}

-keepclassmembers class com.artifex.solib.SOPage {
    private long internal;
    <init>(...);
    *** objectAtPoint(...);
}

-keep, includedescriptorclasses class com.artifex.solib.SOHyperlink {
    <fields>;
    <init>(...);
}

-keepclassmembers class com.artifex.solib.SORender {
    private long internal;
    <init>(...);
}

-keep, includedescriptorclasses class com.artifex.solib.SOBitmap {
    <init>(...);
    protected android.graphics.Bitmap   bitmap;
    protected android.graphics.Rect     rect;
}

-keep, includedescriptorclasses class com.artifex.solib.SOPoint {
    public float x;
    public float y;
    public int type;
    <init>(...);
}

-keep, includedescriptorclasses class com.artifex.solib.SOLinkData {
    <init>(...);
}

-keepclassmembers class com.artifex.solib.SOSelectionLimits {
    private long internal;
    <init>(...);
}

-keepclassmembers class com.artifex.solib.SOSelectionTableRange {
    private long internal;
    <init>(...);
}

-keepclassmembers class com.artifex.solib.SOPageListener {
    *** update(...);
}

-keepclassmembers class com.artifex.solib.SODocSaveListener {
    *** onComplete(...);
}

-keepclassmembers class com.artifex.solib.SORenderListenerInternal {
    *** progress(...);
}

-keep, includedescriptorclasses class android.graphics.Bitmap {
    <init>(...);
    <fields>;
}

-keep, includedescriptorclasses class android.graphics.RectF {
    <init>(...);
    <fields>;
}

-keep, includedescriptorclasses class android.graphics.Rect {
    *** set(...);
    <fields>;
}

-keep, includedescriptorclasses class android.graphics.PointF {
    <init>(...);
    <fields>;
}

-keep, includedescriptorclasses class android.graphics.Point {
    <init>(...);
    <fields>;
}

# After splitting up some of the solib classes, the fllowing
# became necessary, but I don't know why.

-keep, includedescriptorclasses  class com.artifex.solib.SODocLoadListenerInternal{
<methods>;
<fields>;
}

-keep, includedescriptorclasses  class com.artifex.solib.SORender{
<methods>;
<fields>;
}

-keep, includedescriptorclasses  class com.artifex.solib.SORenderListenerInternal{
<methods>;
<fields>;
}
