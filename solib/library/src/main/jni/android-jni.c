#include <jni.h>
#include <time.h>
#include <pthread.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include <string.h>
#include <android/bitmap.h>

#include "smart-office-lib.h"
#include "smart-office-version.h"
#include "android-jni.h"

#define PACKAGEPATH "com/artifex/solib"

#define MIN(a,b) (((a)<(b))?(a):(b))
#define MAX(a,b) (((a)>(b))?(a):(b))


// JNI Type Signatures
//
// |----------------------------------------------------|
// | Type Signature            | Java Type              |
// |---------------------------+------------------------|
// | Z                         | boolean                |
// | B                         | byte                   |
// | C                         | char                   |
// | S                         | short                  |
// | I                         | int                    |
// | J                         | long                   |
// | F                         | float                  |
// | D                         | double                 |
// | L fully-qualified-class ; | fully-qualified-class  |
// | [ type                    | type[]                 |
// | ( arg-types ) ret-type    | method type            |
// |----------------------------------------------------|
//
// For example, the Java method:
//
//     long f (int n, String s, int[] arr);
//
// has the following type signature:
//
//     (ILjava/lang/String;[I)J

/* constants needed for SODoc_setSelectionListStyle */
typedef enum SOListStyle {
    SOListStyle_None=0,
    SOListStyle_Decimal=1,
    SOListStyle_Disc=2
} SOListStyle;

/* constants needed for SODoc_setSelectionAlignment */
typedef enum SOTextAlign {
    SOTextAlign_Left=0,
    SOTextAlign_Center=1,
    SOTextAlign_Right=2,
    SOTextAlign_Justify=3
} SOTextAlign;

typedef enum SOTextAlignV
{
    SOTextAlignV_Top=0,
    SOTextAlignV_Center=1,
    SOTextAlignV_Bottom=2
} SOTextAlignV;

/* Some Static variables. These are initialised on libInit, and never
 * change value. */
static JavaVM   *jvm = NULL;
static jfieldID  SOLib_internal_fid;
static jfieldID  SODoc_internal_fid;
static jfieldID  SOPage_internal_fid;
static jfieldID  SORender_internal_fid;
static jfieldID  SOBitmap_bitmap_fid;
static jfieldID  SOBitmap_rect_fid;
static jfieldID  SOSelectionLimits_internal_fid;
static jfieldID  SOSelectionTableRange_internal_fid;
static jfieldID  SOPoint_x_fid;
static jfieldID  SOPoint_y_fid;
static jfieldID  SOPoint_type_fid;
static jfieldID  SOSelectionContext_text_fid;
static jfieldID  SOSelectionContext_start_fid;
static jfieldID  SOSelectionContext_length_fid;
static jmethodID SODoc_ctor_mid;
static jmethodID SODoc_searchProgress_mid;
static jmethodID SODocLoadListener_progress_mid;
static jmethodID SODocLoadListener_onSelectionChanged_mid;
static jmethodID SODocLoadListener_onLayoutCompleted_mid;
static jmethodID SODocLoadListener_error_mid;
static jmethodID SOPage_ctor_mid;
static jmethodID SOPageLoadListener_update_mid;
static jmethodID SOBitmap_ctor_mid;
static jmethodID SODocSaveListener_onComplete_mid;
static jclass soRenderClass;
static jmethodID SORender_ctor_mid;
static jmethodID SORenderListener_progress_mid;
static jmethodID SOSelectionLimits_ctor_mid;
static jmethodID SOSelectionTableRange_ctor_mid;
static jmethodID RectF_ctor_mid;
static jmethodID PointF_ctor_mid;
static jmethodID Point_ctor_mid;
static jmethodID Rect_set_mid;
static jmethodID SOEnumerateTocListener_nextTocEntry_mid;
static jmethodID SOLinkData_ctor_mid;
static jmethodID SOSelectionContext_ctor_mid;
static jclass rectfClass;

static jclass rectClass;
static jfieldID  Rect_left_fid;
static jfieldID  Rect_top_fid;
static jfieldID  Rect_right_fid;
static jfieldID  Rect_bottom_fid;

/*
 *  allocator function for SmartOfficeibrary calls.
 */
static void *allocator(/*@unused@*/void *cookie, size_t size)
{
    /* Defeat malloc macro */
    return (malloc)(size);
}

/* use this function after a suspect call to see if
 * an exception has been raised.
 */
/*
static void checkForException(JNIEnv * env)
{
    jthrowable exc;
    exc = (*env)->ExceptionOccurred(env);
    if (exc)
    {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
}*/

/* Java cannot represent pointers in it's native types. We therefore
 * have to cast pointers to longs and back again. Rather than
 * doing this inline with repeated explicit casts, we do this using an
 * inline function and keep the nastiness in just 1 place.
 */
static jlong JPTR(void *so)
{
    return (jlong)(intptr_t)so;
}

/*
 * When making callbacks from C to java, we may be called on threads
 * internal to epage lib. As such, we have no JNIEnv. This function
 * handles getting us the required environment
 *
 * It's a common pattern to detach the thread immediately when the callback is
 * finished.  But if many callbacks are executed in a short space of time,
 * performance can suffer.  So instead, SmartOffice will do the detaching
 * when it finishes each of its threads, which happens here in the function
 * onThreadFinished().
 */
JNIEnv *ensureJniAttached()
{
    JNIEnv           *env = NULL;
    int               state;

    state = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
    if (state == JNI_EDETACHED)
    {
        /* The JVM is not attached to this thread. */
        if ((*jvm)->AttachCurrentThread(jvm, &env, NULL) < 0)
        {
            /* Failed to attach */
            return NULL;
        }
    }
    else if (state == JNI_OK)
    {
        /* Nothing to do! */
    }
    else if (state == JNI_EVERSION)
    {
        /* Bad version of jni interface. We're screwed. */
        return NULL;
    }
    return env;
}

/* Structures internal to this file */

typedef struct SODoc_s
{
    SmartOfficeDoc *doc;
    jobject        *listener;
    jobject        jdoc;
    jobject        *tocListener;
} SODoc;

typedef struct SOPage_s
{
    SmartOfficePage *page;
    jobject         *listener;
} SOPage;

typedef struct SORender_s
{
    SmartOfficeRender *render;
    jobject            listener;
    jobject            sorender;
    jobject            bitmap;
    jobject            alpha;
} SORender;

typedef struct SOSelectionLimits_s
{
    int                             hasStart;
    int                             hasEnd;
    SmartOfficePoint                startPt;
    SmartOfficePoint                endPt;
    SmartOfficePoint                handlePt;
    SmartOfficeBox                  area;
    SmartOfficeSelectionRegionFlags flags;
} SOSelectionLimits;

typedef struct SOSelectionTableRange_s
{
    int firstColumn;
    int columnCount;
    int firstRow;
    int rowCount;
} SOSelectionTableRange;


/* Utility functions for finding internal ptrs from given java level
 * pointers. */
static SmartOfficeLib *getSOLib(JNIEnv *env, jobject *thiz)
{
    return (SmartOfficeLib *)(intptr_t)((*env)->GetLongField(env, thiz, SOLib_internal_fid));
}

static SORender *getSORender(JNIEnv *env, jobject *thiz)
{
    return (SORender *)(intptr_t)((*env)->GetLongField(env, thiz, SORender_internal_fid));
}

static SODoc *getSODoc(JNIEnv *env, jobject *thiz)
{
    return (SODoc *)(intptr_t)((*env)->GetLongField(env, thiz, SODoc_internal_fid));
}

static SOPage *getSOPage(JNIEnv *env, jobject *thiz)
{
    return (SOPage *)(intptr_t)((*env)->GetLongField(env, thiz, SOPage_internal_fid));
}

static SOSelectionLimits *getSOSelectionLimits(JNIEnv *env, jobject *thiz)
{
    return (SOSelectionLimits *)(intptr_t)((*env)->GetLongField(env, thiz, SOSelectionLimits_internal_fid));
}

static SOSelectionTableRange *getSOSelectionTableRange(JNIEnv *env, jobject *thiz)
{
    return (SOSelectionTableRange *)(intptr_t)((*env)->GetLongField(env, thiz, SOSelectionTableRange_internal_fid));
}

/* SOLib */

JNIEXPORT jlong JNICALL
JNI_FN(SOLib_preInitLib)(JNIEnv * env, jobject thiz)
{
    jclass          clazz;

    if (jvm != NULL)
        return 0;

    /* Cache some fid's here. Do NOT cache class pointers without
     * taking references. */

    /* SOLib */
    clazz = (*env)->GetObjectClass(env, thiz);
    SOLib_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");

    /* SODoc */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SODoc");
    SODoc_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");
    SODoc_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(J)V");
    SODoc_searchProgress_mid = (*env)->GetMethodID(env, clazz, "searchProgress", "(IIFFFF)V");

    /* SODocLoadListener */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SODocLoadListenerInternal");
    SODocLoadListener_progress_mid = (*env)->GetMethodID(env, clazz, "progress", "(IZ)V");
    SODocLoadListener_error_mid = (*env)->GetMethodID(env, clazz, "error", "(II)V");
    SODocLoadListener_onSelectionChanged_mid = (*env)->GetMethodID(env, clazz, "onSelectionChanged", "(II)V");
    SODocLoadListener_onLayoutCompleted_mid = (*env)->GetMethodID(env, clazz, "onLayoutCompleted", "()V");

    /* SOEnumerateTocListener */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOEnumerateTocListener");
    SOEnumerateTocListener_nextTocEntry_mid = (*env)->GetMethodID(env, clazz, "nextTocEntry", "(IILjava/lang/String;Ljava/lang/String;)V");

    /* SOPage */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOPage");
    SOPage_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");
    SOPage_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(J)V");

    /* SORender */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SORender");
    soRenderClass = (jclass) (*env)->NewGlobalRef(env, clazz);
    SORender_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");
    SORender_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(J)V");

    /* SOBitmap */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOBitmap");
    SOBitmap_bitmap_fid = (*env)->GetFieldID(env, clazz, "bitmap", "Landroid/graphics/Bitmap;");
    SOBitmap_rect_fid = (*env)->GetFieldID(env, clazz, "rect", "Landroid/graphics/Rect;");
    SOBitmap_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(II)V");

    /* SOPoint */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOPoint");
    SOPoint_x_fid = (*env)->GetFieldID(env, clazz, "x", "F");
    SOPoint_y_fid = (*env)->GetFieldID(env, clazz, "y", "F");
    SOPoint_type_fid = (*env)->GetFieldID(env, clazz, "type", "I");

    /* SOLinkData */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOLinkData");
    SOLinkData_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(ILandroid/graphics/RectF;)V");

    /* SOSelectionLimits */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOSelectionLimits");
    SOSelectionLimits_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");
    SOSelectionLimits_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(J)V");

    /* SOSelectionTableRange */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOSelectionTableRange");
    SOSelectionTableRange_internal_fid = (*env)->GetFieldID(env, clazz, "internal", "J");
    SOSelectionTableRange_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(J)V");

    /* SOPageListener */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SOPageListener");
    SOPageLoadListener_update_mid = (*env)->GetMethodID(env, clazz, "update",
                                                        "(Landroid/graphics/RectF;)V");

    /* SODocSaveListener */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SODocSaveListener");
    SODocSaveListener_onComplete_mid = (*env)->GetMethodID(env, clazz, "onComplete", "(II)V");

    /* SORenderListenerInternal */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SORenderListenerInternal");
    SORenderListener_progress_mid = (*env)->GetMethodID(env, clazz, "progress", "(I)V");

    /* SODoc.SOSelectionContext */
    clazz = (*env)->FindClass(env, PACKAGEPATH"/SODoc$SOSelectionContext");
    SOSelectionContext_start_fid = (*env)->GetFieldID(env,clazz,"start","I");
    SOSelectionContext_length_fid = (*env)->GetFieldID(env,clazz,"length","I");
    SOSelectionContext_text_fid = (*env)->GetFieldID(env, clazz, "text", "Ljava/lang/String;");
    SOSelectionContext_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(Lcom/artifex/solib/SODoc;)V");

    /* RectF */
    clazz = (*env)->FindClass(env, "android/graphics/RectF");
    rectfClass = (jclass) (*env)->NewGlobalRef(env, clazz);
    RectF_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(FFFF)V");

    /* Rect */
    clazz = (*env)->FindClass(env, "android/graphics/Rect");
    rectClass = (jclass) (*env)->NewGlobalRef(env, clazz);
    Rect_left_fid = (*env)->GetFieldID(env, rectClass, "left", "I");
    Rect_top_fid = (*env)->GetFieldID(env, rectClass, "top", "I");
    Rect_right_fid = (*env)->GetFieldID(env, rectClass, "right", "I");
    Rect_bottom_fid = (*env)->GetFieldID(env, rectClass, "bottom", "I");

    /* PointF */
    clazz = (*env)->FindClass(env, "android/graphics/PointF");
    PointF_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(FF)V");

    /* Point */
    clazz = (*env)->FindClass(env, "android/graphics/Point");
    Point_ctor_mid = (*env)->GetMethodID(env, clazz, "<init>", "(II)V");

    /* Rect */
    clazz = (*env)->FindClass(env, "android/graphics/Rect");
    Rect_set_mid = (*env)->GetMethodID(env, clazz, "set", "(IIII)V");

    /* Get and store the main JVM pointer. We need this in order to get
     * JNIEnv pointers on callback threads. This is specifically
     * guaranteed to be safe to store in a static var. */
    (*env)->GetJavaVM(env, &jvm);

    return 0;
}

static void onThreadFinished(void)
{
    JNIEnv *env = NULL;
    int state;

    if (jvm != NULL)
    {
        state = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
        if (state == JNI_EDETACHED) {
            // detached
        } else {
            // attached
            (*jvm)->DetachCurrentThread(jvm);
        }
    }
}

/* Library instance creation */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_initLib)(JNIEnv * env, jobject thiz, jstring jlocale)
{
    SmartOfficeLib *so;
    const char     *locale;

    /* set the locale */
    locale = (*env)->GetStringUTFChars(env, jlocale, NULL);
    SmartOfficeLib_setLocale(locale);
    (*env)->ReleaseStringUTFChars(env, jlocale, locale);

    /* Create the library */
    if (SmartOfficeLib_create(&so, NULL) != 0)
        return 0;

    /* Add callback for thread terminations */
    SmartOfficeLib_setThreadTerminationCallback(so, onThreadFinished);

    return JPTR(so);
}

/* set the temp path */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_setTempPath)(JNIEnv * env, jobject thiz, jstring tempPath)
{
    int result;
    const char     *charPath;

    SmartOfficeLib *so = getSOLib(env, thiz);

    charPath = (*env)->GetStringUTFChars(env, tempPath, NULL);

    result = SmartOfficeLib_setTempPath(so, charPath);

    (*env)->ReleaseStringUTFChars(env, tempPath, charPath);

    return result;
}

/* set the system font path */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_installFonts)(JNIEnv * env, jobject thiz, jstring fontPath)
{
    int result;
    const char     *charPath;

    SmartOfficeLib *so = getSOLib(env, thiz);

    charPath = (*env)->GetStringUTFChars(env, fontPath, NULL);

    result = SmartOfficeLib_installFonts(so, charPath);

    (*env)->ReleaseStringUTFChars(env, fontPath, charPath);

    return result;
}


/* Library instance destruction */
JNIEXPORT void JNICALL
JNI_FN(SOLib_finLib)(JNIEnv * env, jobject thiz)
{
    SmartOfficeLib *so = getSOLib(env, thiz);

    SmartOfficeLib_destroy(so);

    /* drop global references */
    (*env)->DeleteGlobalRef(env, soRenderClass);
    (*env)->DeleteGlobalRef(env, rectfClass);
    (*env)->DeleteGlobalRef(env, rectClass);

    jvm = NULL;
}

static void openDocProgress(void *cookie, int pagesLoaded, int complete)
{
    SODoc  *doc = (SODoc *)cookie;
    JNIEnv *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, doc->listener, SODocLoadListener_progress_mid, pagesLoaded, complete);
    }
}

static void openDocError(void *cookie, SmartOfficeDocErrorType error, int errorNum)
{
    SODoc  *doc = (SODoc *)cookie;
    JNIEnv *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, doc->listener, SODocLoadListener_error_mid, error, errorNum);
    }
}

/* getDocTypeFromFileExtension */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_getDocTypeFromFileExtension)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return SmartOfficeDocType_Other;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    return docType;
}


/* isDocTypeDoc */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypeDoc)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_DOC || docType==SmartOfficeDocType_DOCX)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* isDocTypeExcel */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypeExcel)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_XLS || docType==SmartOfficeDocType_XLSX)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* isDocTypePowerPoint */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypePowerPoint)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_PPT || docType==SmartOfficeDocType_PPTX)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* isDocTypeOther */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypeOther)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_Other)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* isDocTypePdf */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypePdf)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_PDF)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* isDocTypeImage */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypeImage)(JNIEnv * env, jobject thiz, jstring path)
{
    const char     *charPath;
    SmartOfficeDocType docType;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed GetStringUTFChars");
        return JNI_FALSE;
    }

    docType = SmartOfficeLib_getDocTypeFromFileExtension(charPath);

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    if (docType==SmartOfficeDocType_IMG)
        return JNI_TRUE;
    return JNI_FALSE;
}

/// C callback function, called on a background thread
static void selectionChangeHandler(void *cookie, int startPage, int endPage)
{
    SODoc  *doc = (SODoc *)cookie;
    JNIEnv *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, doc->listener, SODocLoadListener_onSelectionChanged_mid, startPage, endPage);
    }
}

/// C callback function, called on a background thread
static void layoutCompleteHandler(void *cookie)
{
    SODoc  *doc = (SODoc *)cookie;
    JNIEnv *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, doc->listener, SODocLoadListener_onLayoutCompleted_mid);
    }
}

/* Document load */
JNIEXPORT jobject JNICALL
JNI_FN(SOLib_openDocumentInternal)(JNIEnv * env, jobject thiz, jstring path, jobject listener)
{
    SmartOfficeLib *so = getSOLib(env, thiz);
    const char     *charPath;
    jclass          docClass;
    SODoc          *doc;
    jobject         jdoc;

    doc = malloc(sizeof(*doc));
    if (doc == NULL)
        return NULL;

    docClass = (*env)->FindClass(env, PACKAGEPATH"/SODoc");
    if (docClass == NULL)
    {
        free(doc);
        return NULL;
    }

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
    {
        LOGE("Failed to get filename");
        free(doc);
        return NULL;
    }

    jdoc = (*env)->NewObject(env, docClass, SODoc_ctor_mid, JPTR(doc));
    if (jdoc == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        free(doc);
        return NULL;
    }

    doc->jdoc = (*env)->NewGlobalRef(env, jdoc);
    if (doc->jdoc == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        free(doc);
        return NULL;
    }

    doc->doc = NULL;
    doc->listener = (*env)->NewGlobalRef(env, listener);
    if (doc->listener == NULL)
    {
        (*env)->DeleteGlobalRef(env, doc->jdoc);
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        free(doc);
        return NULL;
    }

    if (SmartOfficeLib_loadDocument(so,
                                    charPath,
                                    SmartOfficeLib_getDocTypeFromFileExtension(charPath),
                                    openDocProgress,
                                    openDocError,
                                    doc,
                                    &doc->doc) != 0)
    {
        (*env)->DeleteGlobalRef(env, doc->jdoc);
        (*env)->DeleteGlobalRef(env, doc->listener);
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        free(doc);
        return NULL;
    }

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    //  get notified when the selection changes
    SmartOfficeDoc_monitorSelection(doc->doc, selectionChangeHandler, doc);
    //  get notified when a core layout is done
    SmartOfficeDoc_monitorLayoutComplete(doc->doc, layoutCompleteHandler, doc);

    return jdoc;
}

static int loggingOutput = 0;

static void doLog(JNIEnv *env, jobject thiz, const char *tag, int fileno)
{
    int             pipes[2];
    fd_set          readset;
    FILE           *inputFile;
    int             bufferSize = 256;
    char            readBuffer[bufferSize];
    int             fill = 0;
    struct timeval  timeout = { 0 };
    int             result;

    /* Set up the pipes */
    pipe(pipes);

    /* Make stderr or stdout  point to the input to the pipe */
    dup2(pipes[1], fileno);

    /* Make inputFile point to the output from the pipe */
    inputFile = fdopen(pipes[0], "r");

    do
    {
        /* Wait for there to be info to copy */
        FD_ZERO(&readset);
        FD_SET(pipes[0], &readset);
        timeout.tv_sec  = 0;
        timeout.tv_usec = 200;
        result = select(pipes[0]+1, &readset, NULL, NULL, &timeout);

        (void)result; // FIXME: should check error

        if (FD_ISSET(pipes[0], &readset))
        {
            /* At least 1 byte of data is available */
            int c = fgetc(inputFile);

            if (c == EOF)
                break; /* Should never happen */
            if (c < 32)
                c = 0;
            readBuffer[fill++] = c;

            /* Watch for overrunning the buffer. Just flush if we're reaching the end */
            if (c != 0 && fill == bufferSize-1)
            {
                readBuffer[fill] = 0;
                c = 0;
            }
            if (c == 0)
            {
                __android_log_write(2, tag, readBuffer);
                fill = 0;
            }
        }
        else
        {
        }
    }
    while (loggingOutput != 0);

    if (fill != 0)
    {
        readBuffer[fill] = 0;
        __android_log_write(2, tag, readBuffer);
    }

    /* Close the pipes when done */
    close(pipes[0]);
    close(pipes[1]);
}

/* Redirect stderr to the Android log */
JNIEXPORT void JNICALL
JNI_FN (SOLib_logStderr)(JNIEnv *env, jobject thiz)
{
    loggingOutput = 1;
    doLog(env, thiz, "stderr", STDERR_FILENO);
}

/* Redirect stdout to the Android log */
JNIEXPORT void JNICALL
JNI_FN (SOLib_logStdout)(JNIEnv *env, jobject thiz)
{
    loggingOutput = 1;
    doLog(env, thiz, "stdout", STDOUT_FILENO);
}

/* stop logging output */
JNIEXPORT void JNICALL
JNI_FN (SOLib_stopLoggingOutputInternal)(JNIEnv* env, jobject thiz)
{
    loggingOutput = 0;
}

/* get formula categories */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getFormulaeCategories)(JNIEnv * env, jobject thiz)
{
    SmartOfficeLib *lib = getSOLib(env, thiz);
    SmartOfficeFormulaeInfo *info;
    int i;
    jobjectArray ret = NULL;
    jclass stringClass;
    jobject string;

    if (lib == NULL)
        return NULL;

    stringClass = (*env)->FindClass(env, "java/lang/String");

    info = SmartOfficeLib_getFormulaeInfo(lib);

    if (info != NULL)
    {
        ret = (jobjectArray) (*env)->NewObjectArray(env, info->count, stringClass, NULL);
        if (ret != NULL)
        {
            for (i = 0; i < info->count; i++)
            {
                SmartOfficeFormulaeCategory *ccat = &info->categories[i];
                string = (*env)->NewStringUTF(env, ccat->title);
                if (string)
                {
                    (*env)->SetObjectArrayElement(env, ret, i, string);
                    (*env)->DeleteLocalRef(env, string);
                }
            }
        }

    }

    return ret;
}

JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getFormulae)(JNIEnv * env, jobject thiz, jstring category)
{
    SmartOfficeLib *lib = getSOLib(env, thiz);
    SmartOfficeFormulaeInfo *info;
    int i, j;
    jobjectArray ret = NULL;
    jclass stringClass;
    jobject string;
    const char *cat;
    char *result;

    if (lib == NULL)
        return NULL;

    stringClass = (*env)->FindClass(env, "java/lang/String");

    info = SmartOfficeLib_getFormulaeInfo(lib);
    if (info != NULL)
    {
        cat = (*env)->GetStringUTFChars(env, category, NULL);

        for (i = 0; i < info->count; i++)
        {
            SmartOfficeFormulaeCategory *ccat = &info->categories[i];
            if (strcmp(cat,ccat->title)==0)
            {
                ret = (jobjectArray) (*env)->NewObjectArray(env, ccat->count, stringClass, NULL);
                for (j = 0; j < ccat->count; j++)
                {
                    result = malloc(strlen(ccat->formulae[j])+strlen(ccat->descriptions[j])+2);
                    sprintf(result,"%s|%s",ccat->formulae[j],ccat->descriptions[j]);
                    string = (*env)->NewStringUTF(env, result);
                    if (string)
                    {
                        (*env)->SetObjectArrayElement(env, ret, j, string);
                        (*env)->DeleteLocalRef(env, string);
                    }
                }
            }
        }

        (*env)->ReleaseStringUTFChars(env, category, cat);
    }

    return ret;
}

/* get version information */

JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getVersionInfo)(JNIEnv * env, jobject thiz)
{
    SmartOfficeLib *lib = getSOLib(env, thiz);
    int i;
    jobjectArray ret = NULL;
    jclass stringClass;
    jobject string;

    if (lib == NULL)
        return NULL;

    stringClass = (*env)->FindClass(env, "java/lang/String");

    char **versionInfo = App_Version_getStrings();

    if (versionInfo != NULL)
    {
        ret = (jobjectArray) (*env)->NewObjectArray(env, 4, stringClass, NULL);
        if (ret != NULL)
        {
            for (i = 0; i < 4; i++)
            {
                string = (*env)->NewStringUTF(env, versionInfo[i]);
                if (string)
                {
                    (*env)->SetObjectArrayElement(env, ret, i, string);
                    (*env)->DeleteLocalRef(env, string);
                }
            }
        }

        App_Version_destroyStrings(versionInfo);
    }

    return ret;
}

/* isTrackChangesEnabled */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isTrackChangesEnabled)(JNIEnv * env, jobject thiz)
{
    SmartOfficeLib *so = getSOLib(env, thiz);

    int val = SmartOfficeLib_isTrackChangesEnabled(so);
    if (val == 1)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* setTrackChangesEnabled */
JNIEXPORT void JNICALL
JNI_FN(SOLib_setTrackChangesEnabled)(JNIEnv * env, jobject thiz, jboolean enabled)
{
    SmartOfficeLib *so = getSOLib(env, thiz);

    SmartOfficeLib_setTrackChangesEnabled(so, enabled ? 1 : 0);

    return;
}

/* isAnimationEnabled */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isAnimationEnabled)(JNIEnv *env, jobject thiz) {
    SmartOfficeLib *so = getSOLib(env, thiz);
    int val = SmartOfficeLib_isAnimationEnabled(so);
    if (val == 1)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* setAnimationEnabled */
JNIEXPORT void JNICALL
JNI_FN(SOLib_setAnimationEnabled)(JNIEnv * env, jobject thiz, jboolean enabled)
{
    SmartOfficeLib *so = getSOLib(env, thiz);

    SmartOfficeLib_setAnimationEnabled(so, enabled ? 1 : 0);

    return;
}


/* SODoc */

JNIEXPORT void JNICALL
JNI_FN(SODoc_destroy)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_destroy(doc->doc);
    (*env)->DeleteGlobalRef(env, doc->jdoc);
    (*env)->DeleteGlobalRef(env, doc->listener);
    (*env)->SetLongField(env, thiz, SODoc_internal_fid, 0);
    free(doc);
}

/* Some simple utility functions for finding styles in strings */
static char emptyString[1];

static char *findStyleInString(char *string, const char *style)
{
    const char *t;

    if (string == NULL || style == NULL)
        return NULL;

    /* Allow for style having leading whitespace. Should never happen,
     * but... */
    while (*style <= 32)
        style++;
    if (*style == 0)
        return NULL;

    t = style;
    while (*string)
    {
        /* Greedily take matching chars */
        while (*string == *t)
        {
            string++;
            t++;
        }
        /* If we matched the style all the way... */
        if (*t == 0)
        {
            /* Skip any whitespace after the string */
            while (*string && *string <= 32)
                string++;
            /* If we hit the end of the string, return it as an empty
             * match (but non NULL as it did match) */
            if (*string == 0)
                return emptyString;
            if (*string == ':')
            {
                string++;
                while (*string && *string <= 32)
                    string++;
                if (*string == ';')
                    return emptyString;
                return string;
            }
        }
        else
        {
            /* Run forward to the next possible start of string */
            while (*string && *string != ';')
                string++;
            if (*string == ';')
                string++;
            while (*string && *string <= 32)
                string++;
        }
        /* If we get here, then we failed to match */
        t = style;
    }
    return NULL;
}

static jboolean styleCmp(const char *value, const char *match)
{
    if (value == NULL || match == 0)
        return 0;
    while (*match != 0 && *value == *match)
    {
        value++;
        match++;
    }
    if (*match == 0)
    {
        while (*value && *value <= 32)
            value++;
        if (*value == 0 || *value == ';')
            return 1;
    }
    return 0;
}

/* getHasBeenModified */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getHasBeenModified)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!SmartOfficeDoc_hasBeenModified(doc->doc));
}

/* getSelectionCanHaveTextStyleApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveTextStyleApplied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_TextStyle));
}

/* getSelectionCanHaveTextAltered */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveTextAltered)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_TextChange));
}

/* getSelectionCanBeCopied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeCopied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Copy));
}

/* getSelectionCanBePasteTarget */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBePasteTarget)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Paste));
}

/* getSelectionCanBeAbsolutelyPositioned */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeAbsolutelyPositioned)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_AbsoluteMove));
}

/* getSelectionCanBeResized */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeResized)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Resize));
}

/* getSelectionIsTablePart */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsTablePart)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_TableChange));
}

/* getSelectionCanHaveForegroundColorApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveForegroundColorApplied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_ForegroundColor));
}

/* getSelectionCanHaveBackgroundColorApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveBackgroundColorApplied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_BackgroundColor));
}

/* getSelectionCanHaveHorizontalAlignmentApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveHorizontalAlignmentApplied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_HorizontalAlign));
}

/* getSelectionCanHaveVerticalAlignmentApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveVerticalAlignmentApplied)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_VerticalAlign));
}

/* getSelectionCanHavePictureInserted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHavePictureInserted)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_InsertPicture));
}

/* getSelectionCanHaveShapeInserted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveShapeInserted)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_InsertShape));
}

/* getSelectionCanBeRotated */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeRotated)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Rotate));
}

/* getSelectionIsAlterableTextSelection */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlterableTextSelection)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_TextSelect));
}

/* getSelectionPermitsInlineTextEntry */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionPermitsInlineTextEntry)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_InlineTextEntry));
}

/* getSelectionHasAssociatedPopup */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionHasAssociatedPopup)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Popup));
}

/* getSelectionCanCreateAnnotation */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanCreateAnnotation)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Annotate));
}

/* getSelectionCanBeDeleted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeDeleted)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_Delete));
}

/* getAnnotationCanBePlacedAtArbitraryPosition */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getAnnotationCanBePlacedAtArbitraryPosition)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_InsertAnnotation));
}

/* getSelectionIsAlterableAnnotation */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlterableAnnotation)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    return (jboolean)(doc && !!(SmartOfficeDoc_getSelCapabilities(doc->doc) &
                                SmartOfficeSelCapabilities_AnnotationContent));
}

static void *allocFn(void *cookie, size_t size)
{
    return malloc(size);
}

/* getSelectionFontColor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionFontColor)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jobject res;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "color", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "color");
    res = (*env)->NewStringUTF(env, value);
    free(utf8Style);
    return res;
}

/* getSelectionFontName */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionFontName)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jobject res;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "font-family", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "font-family");
    res = (*env)->NewStringUTF(env, value);
    free(utf8Style);
    return res;
}

/* setSelectionFontName */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontName)(JNIEnv * env, jobject thiz, jstring name)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charName;

    if (doc == NULL)
        return;

    charName = (*env)->GetStringUTFChars(env, name, NULL);
    if (charName)
    {
        char *text = malloc(strlen(charName) + 16);
        if (text)
        {
            sprintf(text, "font-family:%s", charName);
            SmartOfficeDoc_setSelectionStyle(doc->doc, text);
            free(text);
        }
        (*env)->ReleaseStringUTFChars(env, name, charName);
    }
}

/* setSelectionFontColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontColor)(JNIEnv * env, jobject thiz, jstring color)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charColor;

    if (doc == NULL)
        return;

    charColor = (*env)->GetStringUTFChars(env, color, NULL);
    if (charColor)
    {
        char *text = malloc(strlen(charColor) + 16);
        if (text)
        {
            sprintf(text, "color:%s", charColor);
            SmartOfficeDoc_setSelectionStyle(doc->doc, text);
            free(text);
        }
        (*env)->ReleaseStringUTFChars(env, color, charColor);
    }
}

/* setSelectionBackgroundTransparent */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionBackgroundTransparent)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, "background-color:transparent");
}

/* setSelectionBackgroundColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionBackgroundColor)(JNIEnv * env, jobject thiz, jstring color)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charColor;

    if (doc == NULL)
        return;

    charColor = (*env)->GetStringUTFChars(env, color, NULL);
    if (charColor)
    {
        char *text = malloc(strlen(charColor) + 32);
        if (text)
        {
            sprintf(text, "background-color:%s", charColor);
            SmartOfficeDoc_setSelectionStyle(doc->doc, text);
            free(text);
        }
        (*env)->ReleaseStringUTFChars(env, color, charColor);
    }
}



/* getSelectionIsBold */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsBold)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

//    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-bg-colors", &utf8Style, allocFn, NULL);
//    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-font-families", &utf8Style, allocFn, NULL);

    SmartOfficeDoc_getSelectionStyle(doc->doc, "font-weight", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "font-weight");
    res = styleCmp(value, "bold");
    if (!res && value)
    {
        int weight = atoi(value);
        if (weight >= 700)
            res = 1;
    }
    free(utf8Style);
    return res;
}

/* setSelectionIsBold */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsBold)(JNIEnv * env, jobject thiz, jboolean bold)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, bold ? "font-weight:bold" : "font-weight:normal");
}

/* getSelectionIsLinethrough */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsLinethrough)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-decoration", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-decoration");
    res = styleCmp(value, "line-through");
    free(utf8Style);
    return res;
}

/* setSelectionIsLinethrough */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsLinethrough)(JNIEnv * env, jobject thiz, jboolean bold)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, bold ? "text-decoration:line-through" : "text-decoration:none");
}

/* getSelectionIsItalic */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsItalic)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "font-style", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "font-style");
    res = styleCmp(value, "italic");
    free(utf8Style);
    return res;
}

/* setSelectionIsItalic */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsItalic)(JNIEnv * env, jobject thiz, jboolean italic)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;
    SmartOfficeDoc_setSelectionStyle(doc->doc, italic ? "font-style:italic" : "font-style:normal");
}

/* getSelectionIsUnderlined */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsUnderlined)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-underline", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "-epage-underline");
    res = styleCmp(value, "underline");
    free(utf8Style);
    return res;
}

/* setSelectionIsUnderlined */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsUnderlined)(JNIEnv * env, jobject thiz, jboolean underline)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, underline ? "-epage-underline:underline" : "-epage-underline:none");
}

/* getSelectionFontSize */
JNIEXPORT jdouble JNICALL
JNI_FN(SODoc_getSelectionFontSize)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    double res;

    if (doc == NULL)
        return 0.0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "font-size", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "font-size");
    if (value == NULL)
        res = 0.0;
    else
        res = strtod(value, NULL); /* Assumes points! */
    free(utf8Style);
    return res;
}

/* getSelectionNaturalDimensions */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionNaturalDimensionsInternal)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jstring res;

    if (doc == NULL)
        return NULL;

    res = NULL;
    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-natural-dimensions", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "-epage-natural-dimensions");
    if (value != NULL)
        res = (*env)->NewStringUTF(env, value);
    free(utf8Style);
    return res;
}

/* setSelectionFontSize */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontSize)(JNIEnv * env, jobject thiz, jdouble fontsize)
{
    SODoc *doc = getSODoc(env, thiz);
    char            text[32];

    if (doc == NULL)
        return;

    snprintf(text, sizeof(text), "font-size:%gpt", fontsize);
    SmartOfficeDoc_setSelectionStyle(doc->doc, text);
}

/* setSelectionText */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionText)(JNIEnv * env, jobject thiz, jstring text)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charText;

    if (doc == NULL)
        return;

    charText = (*env)->GetStringUTFChars(env, text, NULL);
    SmartOfficeDoc_setSelText(doc->doc, charText, 0, 1);
    (*env)->ReleaseStringUTFChars(env, text, charText);
}

/* deleteChar */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteChar)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deleteChar(doc->doc);
}

/* adjustSelecion */
JNIEXPORT void JNICALL
JNI_FN(SODoc_adjustSelection)(JNIEnv * env, jobject thiz, jint startOffset, jint endOffset, jint updateHighlight)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_adjustSelection(doc->doc, startOffset, endOffset,
                                   updateHighlight);
}

/* getSelectionContext */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getSelectionContext)(JNIEnv * env, jobject thiz)
{
    SODoc   *doc          = getSODoc(env, thiz);
    char    *ctextContext = NULL;
    int      selStart;
    int      selLen;
    SOError  err;
    jclass   jSelectionContextClass;
    jobject  jSelectionContext;
    jstring  jstr;

    err = SmartOfficeDoc_getSelectionContext(doc->doc,
                                            &ctextContext,
                                            &selStart,
                                            &selLen,
                                             allocator,
                                             NULL);
    if (err != 0)
    {
        goto error;
    }

    jSelectionContextClass =
        (*env)->FindClass(env, PACKAGEPATH"/SODoc$SOSelectionContext");

    if (jSelectionContextClass == NULL)
    {
        goto error;
    }

    // Obtain an instance of SODoc.SOSelectionContext
    jSelectionContext = (*env)->NewObject(env,
                                          jSelectionContextClass,
                                          SOSelectionContext_ctor_mid,
                                          thiz);

    if (jSelectionContext == NULL)
    {
        goto error;
    }

    // Copy the contextual text into the object.
    jstr = (*env)->NewStringUTF(env, ctextContext);
    (*env)->SetObjectField(env, jSelectionContext ,SOSelectionContext_text_fid, jstr);

    // Copy the selection start index into the object.
    (*env)->SetIntField(env, jSelectionContext, SOSelectionContext_start_fid, selStart);

    // Copy the selection length into the object.
    (*env)->SetIntField(env, jSelectionContext, SOSelectionContext_length_fid, selLen);

    free(ctextContext);
    return jSelectionContext;

error:
    free(ctextContext);
    return NULL;
}

/* getFontList */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getFontList)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *names;
    jobject res=NULL;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getFontNames(doc->doc, &names, allocFn, NULL);

    if (names==NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, names);
    return res;
}

/* getBgColorList */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SODoc_getBgColorList)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style=NULL;
    char *value;
    int numValues;
    char **values = NULL;
    char *s;
    int i;
    jobjectArray ret;
    jclass stringClass;
    char *token;
    char *last;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-bg-colors", &utf8Style, allocFn, NULL);
    if (utf8Style==NULL)
        return NULL;

    value = findStyleInString(utf8Style, "-epage-bg-colors");
    if (value==NULL || strlen(value)==0)
    {
        free(utf8Style);
        return NULL;
    }

    numValues = 0;
    s = value;
    token = strtok_r(s, " ", &last);
    while( token != NULL )
    {
        numValues++;
        if (numValues==1)
            values = malloc(sizeof(char *) * numValues);
        else
            values = realloc(values, sizeof(char *) * numValues);
        values[numValues-1] = token;
        token = strtok_r(NULL, " ", &last);
    }

    /* Now make a java array based upon those names */
    stringClass = (*env)->FindClass(env, "java/lang/String");
    /* Don't bother checking for stringClass being null. If that's not
     * there, we are screwed! */
    ret = (jobjectArray)(*env)->NewObjectArray(env, numValues, stringClass, NULL);
    if (ret != NULL)
    {
        /* Silently ignore java allocation problems */
        for (i = 0; i < numValues; i++)
        {
            jobject string = (*env)->NewStringUTF(env, values[i]);
            if (string)
            {
                (*env)->SetObjectArrayElement(env, ret, i, string);
                (*env)->DeleteLocalRef(env, string);
            }
        }
    }

    free(utf8Style);
    free(values);

    return ret;
}

static void getPageUpdate(void *cookie, const SmartOfficeBox *area)
{
    SOPage  *page = (SOPage *)cookie;
    JNIEnv  *env;
    jobject  jarea;

    env = ensureJniAttached();
    if (env != NULL)
    {
        jarea = (*env)->NewObject(env, rectfClass, RectF_ctor_mid, area->x, area->y, area->x + area->width, area->y + area->height);
        (*env)->CallVoidMethod(env, page->listener, SOPageLoadListener_update_mid, jarea);
        (*env)->DeleteLocalRef(env, jarea);
    }
}

/* getPage */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getPage)(JNIEnv * env, jobject thiz, int pageNumber, jobject listener)
{
    SODoc      *doc = getSODoc(env, thiz);
    jclass      pageClass;
    SOPage     *page;
    jobject     jpage;

    if (doc == NULL)
        return NULL;

    page = malloc(sizeof(*page));
    if (page == NULL)
        return NULL;

    pageClass = (*env)->FindClass(env, PACKAGEPATH"/SOPage");
    if (pageClass == NULL)
    {
        free(page);
        return NULL;
    }

    jpage = (*env)->NewObject(env, pageClass, SOPage_ctor_mid, JPTR(page));
    if (jpage == NULL)
    {
        free(page);
        return NULL;
    }

    page->page = NULL;
    page->listener = (*env)->NewGlobalRef(env, listener);
    if (page->listener == NULL)
    {
        free(page);
        return NULL;
    }

    if (SmartOfficeDoc_getPage(doc->doc,
                               pageNumber,
                               getPageUpdate,
                               page,
                               &page->page) != 0)
    {
        (*env)->DeleteGlobalRef(env, page->listener);
        free(page);
        return NULL;
    }

    return jpage;
}

static void saveHandler(void *cookie, SmartOfficeSaveResult res, SOError err)
{
    jobject listenerRef = (jobject)cookie;
    JNIEnv  *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, listenerRef, SODocSaveListener_onComplete_mid, (int)res, (int)err);
        (*env)->DeleteGlobalRef(env, listenerRef);
    }
}

/* saveTo */
JNIEXPORT int JNICALL
JNI_FN(SODoc_saveToInternal)(JNIEnv * env, jobject thiz, jstring path, jobject listener)
{
    SODoc      *doc = getSODoc(env, thiz);
    jobject     listenerRef;
    const char *charPath;
    SOError     err;

    if (doc == NULL)
        return 1;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
        return 1;

    listenerRef = (*env)->NewGlobalRef(env, listener);
    if (listenerRef == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        return 1;
    }

    err = SmartOfficeDoc_save(doc->doc,
                              charPath,
                              0,
                              saveHandler,
                              listenerRef);
    if (err != 0)
    {
        /* Report the error */
        saveHandler((void *)listenerRef, SmartOfficeSave_Error, err);
    }

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    return 0;
}

/* saveToPDF */
JNIEXPORT int JNICALL
JNI_FN(SODoc_saveToPDFInternal)(JNIEnv * env, jobject thiz, jstring path,
                                jboolean imagePerPage, jobject listener)
{
    SODoc      *doc = getSODoc(env, thiz);
    jobject     listenerRef;
    const char *charPath;
    SOError     err;

    if (doc == NULL)
        return 1;

    charPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (charPath == NULL)
        return 1;

    listenerRef = (*env)->NewGlobalRef(env, listener);
    if (listenerRef == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, path, charPath);
        return 1;
    }

    /*
     * Avoid saving to a temporary file then copying to 'path' as this
     * could require a copy between secure/native locations.
     */
    err = SmartOfficeDoc_exportAsPDF(doc->doc,
                                     charPath,
                                     SmartOfficeSaveFlags_NoTemporary,
                                     saveHandler,
                                     listenerRef,
                                     (imagePerPage) ? 1 : 0);
    if (err != 0)
    {
        /* Report the error */
        saveHandler((void *)listenerRef, SmartOfficeSave_Error, err);
    }

    (*env)->ReleaseStringUTFChars(env, path, charPath);

    return 0;
}

/* abortLoad */
JNIEXPORT void JNICALL
JNI_FN(SODoc_abortLoad)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_abortLoad(doc->doc);
}

/* getNumEdits */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getNumEdits)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return 0;

    return SmartOfficeDoc_getNumEdits(doc->doc);
}

/* getCurrentEdit */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getCurrentEdit)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return 0;

    return SmartOfficeDoc_getCurrentEdit(doc->doc);
}

/* setCurrentEdit */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setCurrentEdit)(JNIEnv *env, jobject thiz, int edit)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setCurrentEdit(doc->doc, edit);
}

/* getSelectionAsBitmap */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getSelectionAsBitmap)(JNIEnv *env, jobject thiz)
{
    SODoc             *doc = getSODoc(env, thiz);
    SmartOfficeBitmap  bitmap;
    int                err;
    jclass             sobitmapClass;
    jobject            sobitmap;
    jobject            nativeBitmap;
    void              *buffer;

    //  need a doc
    if (doc == NULL)
        return NULL;

    //  call the SO lib to get the SmartOfficeBitmap
    err = SmartOfficeDoc_getSelectionAsBitmap(doc->doc, &bitmap, NULL, NULL);
    if (err)
        return NULL;

    //  create a new SOBitmap
    sobitmapClass = (*env)->FindClass(env, PACKAGEPATH"/SOBitmap");
    if (sobitmapClass == NULL)
        return NULL;
    sobitmap = (*env)->NewObject(env, sobitmapClass, SOBitmap_ctor_mid, bitmap.width, bitmap.height);
    if (sobitmap == NULL)
        return NULL;

    //  get the native Bitmap field from the SOBitmap
    nativeBitmap = (*env)->GetObjectField(env, sobitmap, SOBitmap_bitmap_fid);
    if (nativeBitmap == NULL) {
        (*env)->DeleteLocalRef(env, sobitmap);
        return NULL;
    }

    //  lock the pixels
    if (AndroidBitmap_lockPixels(env, nativeBitmap, &buffer) < 0) {
        (*env)->DeleteLocalRef(env, sobitmap);
        return NULL;
    }

    //  copy the pixels.
    //  frp 5/28/2017
    //  I don't recall the reason for using (bitmap.width+1)&~1), but I find that this crashes
    //  unpredictably...
//    memcpy(buffer, bitmap.memptr, bitmap.height * ((bitmap.width+1)&~1) * 2);

    //  ... but using the actual bitmap width seems to work fine.
    memcpy(buffer, bitmap.memptr, bitmap.height * bitmap.width*2);

    //  unlock
    AndroidBitmap_unlockPixels(env, nativeBitmap);

    //  free the SmartOfficeBitmap buffer
    free(bitmap.memptr);

    //  done, return the SOBitmap
    return sobitmap;
}

/* insertImageAtSelection */
JNIEXPORT void JNICALL
JNI_FN(SODoc_insertImageAtSelection)(JNIEnv *env, jobject thiz, jstring jpath)
{
    SODoc      *doc = getSODoc(env, thiz);
    const char *path;

    if (doc == NULL)
        return;

    path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (path == NULL)
    {
        LOGE("Failed GetStringUTFChars for %s", path);
        return;
    }

    SmartOfficeDoc_insertImageAtSelection(doc->doc, path);

    (*env)->ReleaseStringUTFChars(env, jpath, path);

    return;
}

/* insertImageCenterPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_insertImageCenterPage)(JNIEnv *env, jobject thiz, int pageNum, jobject jpath)
{
    SODoc      *doc = getSODoc(env, thiz);
    const char *path;

    if (doc == NULL)
        return;

    path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (path == NULL)
        /* FIXME */
        return;

    (void)SmartOfficeDoc_insertImageCenterPage(doc->doc, pageNum, path);

    (*env)->ReleaseStringUTFChars(env, jpath, path);
}

/* nativeInsertAutoshapeCenterPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_nativeInsertAutoshapeCenterPage)(JNIEnv *env, jobject thiz, int pageNum, jobject jshape, jobject jproperties, jboolean centerPage, float x, float y)
{
    SODoc            *doc = getSODoc(env, thiz);
    const char       *shape;
    const char       *properties;
    SmartOfficePoint  pos;

    if (doc == NULL)
        return;

    shape = (*env)->GetStringUTFChars(env, jshape, NULL);
    if (shape == NULL)
        /* FIXME */
        return;

    if (jproperties == NULL)
    {
        properties = NULL;
    }
    else
    {
        properties = (*env)->GetStringUTFChars(env, jproperties, NULL);
        if (properties == NULL)
        {
            (*env)->ReleaseStringUTFChars(env, jshape, shape);
            /* FIXME */
            return;
        }
    }

    pos.x = x;
    pos.y = y;
    (void)SmartOfficeDoc_insertAutoshape(doc->doc, pageNum, shape, properties, centerPage, &pos);

    if (jproperties != NULL)
        (*env)->ReleaseStringUTFChars(env, jproperties, properties);
    (*env)->ReleaseStringUTFChars(env, jshape, shape);
}

/* setSearchStart */
JNIEXPORT void JNICALL
JNI_FN(SODoc_nativeSetSearchStart)(JNIEnv *env, jobject thiz, int page, jfloat x, jfloat y)
{
    SODoc *doc = getSODoc(env, thiz);
    SmartOfficePoint offset = { (float)x, (float)y };

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSearchStart(doc->doc, page, offset);
}

static void searchProgressHandler(void *cookie, SmartOfficeSearchStatus status, int page, SmartOfficeBox *box)
{
    jobject jdoc = (jobject)cookie;

    JNIEnv *env;
    float   x0, y0, x1, y1;

    if (box != NULL)
    {
        x0 = box->x;
        y0 = box->y;
        x1 = x0 + box->width;
        y1 = y0 + box->height;
    }
    else
    {
        x0 = 0;
        y0 = 0;
        x1 = 0;
        y1 = 0;
    }

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, jdoc, SODoc_searchProgress_mid, status, page, x0, y0, x1, y1);
    }
}

/* nativeSearch */
JNIEXPORT int JNICALL
JNI_FN(SODoc_nativeSearch)(JNIEnv *env, jobject thiz, jobject jtext, jboolean matchCase, jboolean backwards)
{
    SODoc *doc = getSODoc(env, thiz);
    int    res;
    const char *text;

    if (doc == NULL)
        return 0;

    text = (*env)->GetStringUTFChars(env, jtext, NULL);

    res = SmartOfficeDoc_search(doc->doc, text, (int)matchCase,
                                (SmartOfficeSearchDirection)backwards,
                                searchProgressHandler, doc->jdoc);

    (*env)->ReleaseStringUTFChars(env, jtext, text);

    return res;
}

/* cancelSearch */
JNIEXPORT void JNICALL
JNI_FN(SODoc_cancelSearch)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_cancelSearch(doc->doc);
}

/* closeSearch */
JNIEXPORT void JNICALL
JNI_FN(SODoc_nativeCloseSearch)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_closeSearch(doc->doc);
}

/* getIndentationLevel */
JNIEXPORT jintArray JNICALL
JNI_FN(SODoc_getIndentationLevel)(JNIEnv *env, jobject thiz)
{
    SODoc     *doc = getSODoc(env, thiz);
    int        indents[2];
    jintArray *arr;

    if (doc == NULL)
        return NULL;

    if (SmartOfficeDoc_getIndentationLevel(doc->doc, &indents[0], &indents[1]) != 0)
        return NULL;

    arr = (*env)->NewIntArray(env, 2);
    if (arr != NULL)
        (*env)->SetIntArrayRegion(env, arr, 0, 2, indents);

    return arr;
}

/* setIndentationLevel */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setIndentationLevel)(JNIEnv *env, jobject thiz, int indent)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setIndentationLevel(doc->doc, indent);
}

/* getSelectedObjectBounds */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getSelectedObjectBounds)(JNIEnv *env, jobject thiz)
{
    SODoc          *doc = getSODoc(env, thiz);
    SmartOfficeBox  bounds;
    jobject        *jbounds;

    if (doc == NULL)
        return NULL;

    if (SmartOfficeDoc_getSelectedObjectBounds(doc->doc, &bounds) != 0)
        return NULL;

    jbounds = (*env)->NewObject(env, rectfClass, RectF_ctor_mid, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);

    return jbounds;
}

/* setSelectedObjectBounds */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedObjectBounds)(JNIEnv *env, jobject thiz, jfloat x0, jfloat y0, jfloat x1, jfloat y1)
{
    SODoc          *doc = getSODoc(env, thiz);
    SmartOfficeBox  bounds;

    if (doc == NULL)
        return;

    bounds.x      = x0;
    bounds.y      = y0;
    bounds.width  = x1-x0;
    bounds.height = y1-y0;

    SmartOfficeDoc_setSelectedObjectBounds(doc->doc, &bounds);
}

/* setSelectedObjectPosition */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedObjectPosition)(JNIEnv *env, jobject thiz, jfloat x, jfloat y)
{
    SODoc            *doc = getSODoc(env, thiz);
    SmartOfficePoint  pos;

    if (doc == NULL)
        return;

    pos.x = x;
    pos.y = y;
    SmartOfficeDoc_setSelectedObjectPosition(doc->doc, &pos);
}

/* getNumPagesInternal */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getNumPagesInternal)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    int    pages;

    if (doc == NULL)
        return 0;

    if (SmartOfficeDoc_getNumPages(doc->doc, &pages) < 0)
        return 0;

    return pages;
}

/* getDocType */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getDocType)(JNIEnv *env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return -1;

    return SmartOfficeDoc_docType(doc->doc);
}

/* clearSelection */
JNIEXPORT void JNICALL
JNI_FN(SODoc_clearSelection)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_clearSelection(doc->doc);
}

/* selectionDelete */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionDelete)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deleteSelection(doc->doc);
}

/* selectionCutToClip */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionCutToClip)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    // Cut the selection into the internal clipboard
    SmartOfficeDoc_cutSelectionToClipboard(doc->doc);
}

/* selectionCopyToClip */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionCopyToClip)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    // Copy the selection into the internal clipboard
    SmartOfficeDoc_copySelectionToClipboard(doc->doc);
}

/* clipboardHasData */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_clipboardHasData)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    return SmartOfficeDoc_clipboardHasData(doc->doc);
}

/* selectionPaste */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionPaste)(JNIEnv * env, jobject thiz, int pageNum)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    // Paste our internal clipboard into the document
    SmartOfficeDoc_pasteClipboard(doc->doc, (int)pageNum);
}

/* getClipboardAsText */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getClipboardAsText)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *clipText = NULL;
    jobject res=NULL;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getClipboardAsText(doc->doc, &clipText, NULL, NULL);
    if (clipText==NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, clipText);
    return res;
}

/* setClipboardFromText */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setClipboardFromText)(JNIEnv * env, jobject thiz, jstring text)
{
    SODoc *doc = getSODoc(env, thiz);
    const char* utext;

    if (doc == NULL)
        return;

    utext = (*env)->GetStringUTFChars(env, text, NULL);
    SmartOfficeDoc_setClipboardFromText(doc->doc, utext);
    (*env)->ReleaseStringUTFChars(env, text, utext);
}

/* addBlankPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addBlankPage)(JNIEnv * env, jobject thiz, int pageNumber)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addBlankPage(doc->doc, (int)pageNumber);
}

/* deletePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deletePage)(JNIEnv * env, jobject thiz, int pageNumber)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deletePage(doc->doc, (int)pageNumber);
}


/* addRowsAbove */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addRowsAbove)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addRowAbove(doc->doc);
}

/* addRowsBelow */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addRowsBelow)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addRowBelow(doc->doc);
}

/* addColumnsLeft */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addColumnsLeft)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addColumnLeft(doc->doc);
}

/* addColumnsRight */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addColumnsRight)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addColumnRight(doc->doc);
}

/* deleteRows */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteRows)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deleteRows(doc->doc);
}

/* deleteColumns */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteColumns)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deleteColumns(doc->doc);
}


/* getSelectionIsAlignLeft */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignLeft)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-align");
    res = styleCmp(value, "left");
    free(utf8Style);
    return res;
}

/* getSelectionIsAlignCenter */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignCenter)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-align");
    res = styleCmp(value, "center");
    free(utf8Style);
    return res;
}

/* getSelectionIsAlignRight */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignRight)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-align");
    res = styleCmp(value, "right");
    free(utf8Style);
    return res;
}

/* getSelectionIsAlignJustify */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignJustify)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-align");
    res = styleCmp(value, "justify");
    free(utf8Style);
    return res;
}


/* getSelectionAsText */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAsText)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *text;
    jobject res=NULL;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getSelAsText(doc->doc, &text, allocFn, NULL);

    if (text==NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, text);
    return res;
}

/* selectionTableRange */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_selectionTableRange)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    SOSelectionTableRange *range;
    jclass             sosRangeClass;
    jobject            sosRange;

    if (doc == NULL)
        return NULL;

    range = malloc(sizeof(*range));
    if (range == NULL)
        return NULL;

    SmartOfficeDoc_getTableRange(doc->doc,
                                 &range->firstColumn,
                                 &range->columnCount,
                                 &range->firstRow,
                                 &range->rowCount);

    sosRangeClass = (*env)->FindClass(env, PACKAGEPATH"/SOSelectionTableRange");
    if (sosRangeClass == NULL)
    {
        free(range);
        return NULL;
    }
    sosRange = (*env)->NewObject(env, sosRangeClass, SOSelectionTableRange_ctor_mid, JPTR(range));
    if (sosRange == NULL)
    {
        free(range);
        return NULL;
    }

    return sosRange;
}

/* moveTableSelectionUp */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionUp)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return;
    SmartOfficeDoc_moveTableSelectionToUp(doc->doc);
}

/* moveTableSelectionDown */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionDown)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return;
    SmartOfficeDoc_moveTableSelectionToDown(doc->doc);
}

/* moveTableSelectionLeft */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionLeft)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return;
    SmartOfficeDoc_moveTableSelectionToLeft(doc->doc);
}

/* moveTableSelectionRight */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionRight)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return;
    SmartOfficeDoc_moveTableSelectionToRight(doc->doc);
}

JNIEXPORT jfloat JNICALL
JNI_FN(SODoc_getSelectedRowHeight)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    float height=0;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getRowHeight(doc->doc, &height);

    return height;
}

JNIEXPORT jfloat JNICALL
JNI_FN(SODoc_getSelectedColumnWidth)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    float width=0;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getColumnWidth(doc->doc, &width);

    return width;
}

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedRowHeight)(JNIEnv * env, jobject thiz, float value)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setRowHeight(doc->doc, value);
}

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedColumnWidth)(JNIEnv * env, jobject thiz, float value)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setColumnWidth(doc->doc, value);
}


JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getTableCellsMerged)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style = NULL;
    char *value = NULL;
    jboolean result;

    if (doc == NULL)
        return JNI_FALSE;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-cell-merged", &utf8Style, allocFn, NULL);
    if (utf8Style == NULL)
        return JNI_FALSE;

    value = findStyleInString(utf8Style, "-epage-cell-merged");
    result = styleCmp(value, "merged");
    free(utf8Style);
    return result;
}

JNIEXPORT void JNICALL
JNI_FN(SODoc_setTableCellsMerged)(JNIEnv * env, jobject thiz, jboolean tableCellsMerged)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    if (tableCellsMerged)
        SmartOfficeDoc_mergeTableCells(doc->doc);
    else
        SmartOfficeDoc_unmergeTableCells(doc->doc);
}

JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedCellFormat)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *text;
    jobject res=NULL;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getSelectionCellFormat(doc->doc, &text, allocFn, NULL);

    if (text==NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, text);
    return res;
}

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedCellFormat)(JNIEnv * env, jobject thiz, jstring cellFormat)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charText;

    if (doc == NULL)
        return;

    charText = (*env)->GetStringUTFChars(env, cellFormat, NULL);
    SmartOfficeDoc_setSelectionCellFormat(doc->doc, charText);
    (*env)->ReleaseStringUTFChars(env, cellFormat, charText);
}

JNIEXPORT void JNICALL
JNI_FN(SODoc_setFlowModeInternal)(JNIEnv * env, jobject thiz, int mode, float width)
{
    SODoc *doc = getSODoc(env, thiz);
    SmartOfficeFlowMode soMode;

    if (doc == NULL)
        return;

    if (mode==1)
        soMode = SmartOfficeFlowMode_Normal;
    else if (mode==2)
        soMode = SmartOfficeFlowMode_Reflow;
    else
    {
        LOGE("SODoc_setFlowMode bad mode=%d", mode);
        return;
    }

    //  TODO: restrictions/checking of width?

    SmartOfficeDoc_setFlowMode(doc->doc, soMode, width);
}

/* processKeyCommand */
JNIEXPORT void JNICALL
JNI_FN(SODoc_processKeyCommand)(JNIEnv * env, jobject thiz, int inCommand)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    //  TODO: we assume that the incoming int values match.
    SmartOfficeCmd command = inCommand;

    SmartOfficeDoc_processKeyCommand(doc->doc, command);
}

/* movePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_movePage)(JNIEnv * env, jobject thiz, int pageNumber, int newNumber)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
     return;

    SmartOfficeDoc_movePage(doc->doc, pageNumber, newNumber);
}

/* duplicatePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_duplicatePage)(JNIEnv * env, jobject thiz, int pageNumber)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
     return;

    SmartOfficeDoc_duplicatePage(doc->doc, pageNumber);
}

static void tocCallbackFn(void           *handle,
                          void           *parentHandle,
                          const char     *label,
                          const char     *url,
                          int             open,
                          void           *cookie)
{
    SODoc  *doc = (SODoc *)cookie;
    JNIEnv *env;
    jobject urlString;
    jobject labelString;

    env = ensureJniAttached();
    if (env != NULL)
    {
        urlString = (*env)->NewStringUTF(env, url);
        labelString = (*env)->NewStringUTF(env, label);

        (*env)->CallVoidMethod(env, doc->tocListener, SOEnumerateTocListener_nextTocEntry_mid, handle, parentHandle, labelString, urlString);

        (*env)->DeleteLocalRef(env, urlString);
        (*env)->DeleteLocalRef(env, labelString);
    }
}


/* enumerateToc */
JNIEXPORT jlong JNICALL
JNI_FN(SODoc_enumerateToc)(JNIEnv * env, jobject thiz, jobject listener)
{
    int result;

    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return 0;

    doc->tocListener = (*env)->NewGlobalRef(env, listener);
    if (doc->tocListener == NULL)
    {
        return 0;
    }

    result = SmartOfficeDoc_enumerateToc(doc->doc, tocCallbackFn, doc);
    return result;
}

/* interpretLinkUrl */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_interpretLinkUrl)(JNIEnv * env, jobject thiz, jstring url)
{
    int err;
    int page;
    SmartOfficeBox box;
    const char *charUrl;

    jclass soLinkDataClass=NULL;
    jobject soLinkData=NULL;
    jobject rectf=NULL;

    SODoc *doc = getSODoc(env, thiz);
    if (doc == NULL)
        return NULL;

    charUrl = (*env)->GetStringUTFChars(env, url, NULL);
    if (!charUrl)
        return NULL;

    err = SmartOfficeDoc_interpretLinkUrl(doc->doc, charUrl, &page, &box);
    (*env)->ReleaseStringUTFChars(env, url, charUrl);

    if (err)
        return NULL;

    rectf = (*env)->NewObject(env, rectfClass, RectF_ctor_mid, box.x, box.y, box.x + box.width, box.y + box.height);
    if (rectf == NULL)
        goto error;

    soLinkDataClass = (*env)->FindClass(env, PACKAGEPATH"/SOLinkData");
    if (soLinkDataClass == NULL)
        goto error;

    soLinkData = (*env)->NewObject(env, soLinkDataClass, SOLinkData_ctor_mid, page, rectf);
    if (soLinkData == NULL)
        goto error;

    (*env)->DeleteLocalRef(env, soLinkDataClass);

    return soLinkData;

error:
    if (rectf!=NULL)
        (*env)->DeleteLocalRef(env, rectf);
    if (soLinkData!=NULL)
        (*env)->DeleteLocalRef(env, soLinkData);
    if (soLinkDataClass!=NULL)
        (*env)->DeleteLocalRef(env, soLinkDataClass);

    return NULL;
}


/* SOPage */

/* setSelectionLimitsBox */
JNIEXPORT void JNICALL
JNI_FN(SOPage_setSelectionLimitsBox)(JNIEnv *env, jobject thiz, jfloat left, jfloat top, jfloat right, jfloat bottom)
{
    SOPage *page = getSOPage(env, thiz);

    SmartOfficeBox  box;

    if (page == NULL)
        return;

    box.x      = left;
    box.y      = top;
    box.width  = right - left;
    box.height = bottom - top;

    SmartOfficePage_setSelectionLimitsBox(page->page, box);
}

/* discard */
JNIEXPORT void JNICALL
JNI_FN(SOPage_discard)(JNIEnv * env, jobject thiz)
{
    SOPage *page = getSOPage(env, thiz);

    if (page == NULL)
        return;

    SmartOfficePage_destroy(page->page);
    (*env)->DeleteGlobalRef(env, page->listener);
    (*env)->SetLongField(env, thiz, SOPage_internal_fid, 0);
    free(page);
}

/* zoomToFitRect */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_zoomToFitRect)(JNIEnv * env, jobject thiz, int w, int h)
{
    SOPage *page = getSOPage(env, thiz);
    float fw, fh;
    jclass pointfClass;
    int err;

    if (page == NULL)
        return NULL;

    err = SmartOfficePage_calculateZoom(page->page, w, h, &fw, &fh);
    if (err)
    {
        fw = fh = 0.0f;
    }
    pointfClass = (*env)->FindClass(env, "android/graphics/PointF");
    return (*env)->NewObject(env, pointfClass, PointF_ctor_mid, fw, fh);
}

/* sizeAtZoom */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_sizeAtZoom)(JNIEnv * env, jobject thiz, double zoom)
{
    SOPage *page = getSOPage(env, thiz);
    int w, h, err;
    jclass pointClass;

    if (page == NULL)
        return NULL;

    err = SmartOfficePage_getSizeForZoom(page->page, zoom, &w, &h);
    if (err)
    {
        w = h = 0;
    }

    pointClass = (*env)->FindClass(env, "android/graphics/Point");
    return (*env)->NewObject(env, pointClass, Point_ctor_mid, w, h);
}

static jfloatArray convertRuler(JNIEnv *env, jobject thiz, SmartOfficeRuler *ruler)
{
    jfloatArray arr;

    if (ruler == NULL)
        return NULL;

    arr = (*env)->NewFloatArray(env, ruler->numGraduations);
    if (arr == NULL)
        /* FIXME: Not ideal, as NULL means 'no ruler' */
        return NULL;

    (*env)->SetFloatArrayRegion(env, arr, 0, ruler->numGraduations, ruler->graduations);

    SmartOfficeRuler_destroy(ruler);

    return arr;
}

JNIEXPORT jfloatArray JNICALL
JNI_FN(SOPage_getHorizontalRuler)(JNIEnv *env, jobject thiz)
{
    SOPage           *page = getSOPage(env, thiz);

    if (page == NULL)
        return NULL;

    return convertRuler(env, thiz, SmartOfficePage_getHorizontalRuler(page->page));
}

JNIEXPORT jfloatArray JNICALL
JNI_FN(SOPage_getVerticalRuler)(JNIEnv *env, jobject thiz)
{
    SOPage           *page = getSOPage(env, thiz);

    if (page == NULL)
        return NULL;

    return convertRuler(env, thiz, SmartOfficePage_getVerticalRuler(page->page));
}

static void renderProgressHandler(void *cookie, int error)
{
    SORender *render = (SORender *)cookie;
    JNIEnv   *env;

    env = ensureJniAttached();
    if (env != NULL)
    {
        (*env)->CallVoidMethod(env, render->listener, SORenderListener_progress_mid, error);
        (*env)->DeleteGlobalRef(env, render->listener);
        (*env)->DeleteGlobalRef(env, render->sorender);
        AndroidBitmap_unlockPixels(env, render->bitmap);
        (*env)->DeleteGlobalRef(env, render->bitmap);
    }
}

/* renderAtZoom */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_nativeRenderAtZoom)(JNIEnv *env,
                                  jobject thiz,
                                  int     layer,
                                  double  zoom,
                                  double  originX,
                                  double  originY,
                                  jobject bitmap,
                                  int     bitmapW,
                                  jobject alpha,
                                  int     x,
                                  int     y,
                                  int     w,
                                  int     h,
                                  jobject listener)
{
    int                    err;
    SOPage                *page;
    SORender              *render;
    char                  *bmptr;
    char                  *amptr = NULL;
    AndroidBitmapInfo      alphaInfo;
    jobject                sorender;
    SmartOfficeRenderArea  area;
    SmartOfficeBitmap      sob; /* bitmap */
    SmartOfficeBitmap      soa; /* alpha */

    if (bitmap == NULL)
        return NULL;

    /* alpha may be NULL */

    page = getSOPage(env, thiz);
    if (page == NULL)
        goto exit;

    render = malloc(sizeof(*render));
    if (render == NULL)
        goto exit;

    if (AndroidBitmap_lockPixels(env, bitmap, (void **)&bmptr) < 0)
        goto exit10;

    if (alpha) {
        if (AndroidBitmap_lockPixels(env, alpha, (void **) &amptr) < 0)
            goto exit15;

        if (AndroidBitmap_getInfo(env, alpha, &alphaInfo) < 0)
            goto exit17;
    }

    render->render   = NULL;
    render->sorender = NULL;
    render->listener = (*env)->NewGlobalRef(env, listener);
    render->bitmap   = (*env)->NewGlobalRef(env, bitmap);
    render->alpha    = (*env)->NewGlobalRef(env, alpha);

    sorender = (*env)->NewObject(env,
                                 soRenderClass,
                                 SORender_ctor_mid,
                                 JPTR(render));
    if (sorender == NULL)
        goto exit20;

    render->sorender = (*env)->NewGlobalRef(env, sorender);

    area.origin.x          = originX;
    area.origin.y          = originY;
    area.renderArea.x      = 0;
    area.renderArea.y      = 0;
    area.renderArea.width  = w;
    area.renderArea.height = h;

    sob.width       = w;
    sob.height      = h;
#ifndef SCREENS_ARE_R8G8B8X8
    sob.memptr      = bmptr + 2 * (x + y * bitmapW);
    sob.lineSkip    = bitmapW * 2;
    sob.type        = SmartOfficeBitmapType_RGB565;
#else
    sob.memptr      = bmptr + 4 * (x + y * bitmapW);
    sob.lineSkip    = bitmapW * 4;
    sob.type        = SmartOfficeBitmapType_RGBA8888;
#endif

    if (alpha)
    {
        soa.memptr      = amptr + (x + y * alphaInfo.stride);
        soa.width       = w;
        soa.height      = h;
        soa.lineSkip    = alphaInfo.stride;
        soa.type        = SmartOfficeBitmapType_A8;
    }

    err = SmartOfficePage_renderLayerAlpha(page->page,
                                           layer,
                                           zoom,
                                          &sob,
                                           (alpha) ? &soa : NULL,
                                          &area,
                                           renderProgressHandler,
                                           render,
                                          &render->render);
    if (err)
        goto exit30;

    return sorender;


exit30:
    (*env)->DeleteGlobalRef(env, render->sorender);
exit20:
    (*env)->DeleteGlobalRef(env, render->listener);
    (*env)->DeleteGlobalRef(env, render->bitmap);
    (*env)->DeleteGlobalRef(env, render->alpha);
exit17:
    if (alpha)
        AndroidBitmap_unlockPixels(env, amptr);
exit15:
    AndroidBitmap_unlockPixels(env, bmptr);
exit10:
    free(render);
exit:
    return NULL;
}

/* select */
JNIEXPORT void JNICALL
JNI_FN(SOPage_select)(JNIEnv * env, jobject thiz, int mode, double pointX, double pointY)
{
    SOPage           *page   = getSOPage(env, thiz);
    SmartOfficePoint  point = {pointX, pointY};

    if (page == NULL)
        return;

    SmartOfficePage_selectAtPoint(page->page, &point, mode, NULL);
}

/* selectionLimits */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_selectionLimits)(JNIEnv * env, jobject thiz)
{
    SOPage            *page   = getSOPage(env, thiz);
    SOSelectionLimits *limits;
    jclass             soslimClass;
    jobject            soslim;

    if (page == NULL)
        return NULL;

    limits = malloc(sizeof(*limits));
    if (limits == NULL)
        return NULL;

    SmartOfficePage_getSelectionLimits(page->page,
                                       &limits->hasStart,
                                       &limits->hasEnd,
                                       &limits->startPt,
                                       &limits->endPt,
                                       &limits->handlePt,
                                       &limits->area,
                                       &limits->flags);

    soslimClass = (*env)->FindClass(env, PACKAGEPATH"/SOSelectionLimits");
    if (soslimClass == NULL)
    {
        free(limits);
        return NULL;
    }
    soslim = (*env)->NewObject(env, soslimClass, SOSelectionLimits_ctor_mid, JPTR(limits));
    if (soslim == NULL)
    {
        free(limits);
        return NULL;
    }

    return soslim;
}

/* getPageTitle */
JNIEXPORT jstring JNICALL
JNI_FN(SOPage_getPageTitle)(JNIEnv * env, jobject thiz)
{
    SOPage *page = getSOPage(env, thiz);
    SOError err = 0;
    char *title=NULL;
    jobject res;

    if (page == NULL)
        return NULL;

    err = SmartOfficePage_getPageTitle(page->page, &title, allocator, NULL);
    if (err!=0)
    {
        free(title);
        return NULL;
    }

    res = (*env)->NewStringUTF(env, title);

    return res;
}

/* objectAtPoint */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_objectAtPoint)(JNIEnv * env, jobject thiz, float x, float y)
{
    SOPage *page = getSOPage(env, thiz);
    SmartOfficePoint point = {x, y};
    SmartOfficeBox bbox;
    int pageNum = -1;
    char *url = NULL;
    SOError err;
    jclass class;
    jmethodID ctor;
    jobject object;
    jfieldID fid;

    //  gotta have a page
    if (page == NULL)
        return NULL;

    //  call the library
    err = SmartOfficePage_objectAtPoint(page->page, &point, NULL, NULL, &url, &pageNum, &bbox);
    if (err!=0)
        return NULL;

    //  create a blank object
    class = (*env)->FindClass(env, PACKAGEPATH"/SOHyperlink");
    ctor = (*env)->GetMethodID(env, class, "<init>", "()V");
    object = (*env)->NewObject(env, class, ctor);

    //  copy the page number
    fid = (*env)->GetFieldID(env,class,"pageNum","I");
    (*env)->SetIntField(env, object ,fid, pageNum);

    //  copy the url
    fid = (*env)->GetFieldID(env, class, "url", "Ljava/lang/String;");
    jstring jstr = (*env)->NewStringUTF(env,url);
    (*env)->SetObjectField(env, object ,fid, jstr);
    free(url);

    //  copy bbox as a Rect
    jclass rectClass = (*env)->FindClass(env, "android/graphics/Rect");
    jmethodID rectCtor = (*env)->GetMethodID(env, rectClass, "<init>", "(IIII)V");
    jobject rect = (*env)->NewObject(env, rectClass, rectCtor,
                                     (int)bbox.x, (int)bbox.y, (int)(bbox.x+bbox.width), (int)(bbox.y+bbox.height));
    fid = (*env)->GetFieldID(env, class, "bbox", "Landroid/graphics/Rect;");
    (*env)->SetObjectField(env, object ,fid, rect);

    //  done
    return object;
}

JNIEXPORT jstring JNICALL
JNI_FN(SOPage_getSlideTransitionInternal)(JNIEnv * env, jobject thiz)
{
    SOPage *page = getSOPage(env, thiz);
    SOError err;
    char *trans=NULL;
    jobject res;

    //  must have  page
    if (page == NULL)
        return NULL;

    //  call the library
    err = SmartOfficePage_getSlideTransition(page->page, &trans, NULL, NULL);
    if (err!=0)
        return NULL;

    res = (*env)->NewStringUTF(env, trans);
    free(trans);

    return res;
}

static jobject objectForAnimCmd(JNIEnv                            *env,
                                const SmartOfficeAnimCmdSpecifier *spec)
{
    jclass    clazz;
    jmethodID mid;
    jobject   anim_obj  = NULL;
    jobject   point_obj = NULL;
    jobject   point_obj2 = NULL;

    switch (spec->command)
    {
        case SmartOfficeAnimCmd_Render:
            {
                const SmartOfficeAnimCmdRenderData *renderData;

                renderData = &spec->data.render;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              renderData->renderArea.origin.x,
                                              renderData->renderArea.origin.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationRenderCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIFLandroid/graphics/PointF;FFFF)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             renderData->renderable,
                                             renderData->zoom,
                                             point_obj,
                                             renderData->renderArea.renderArea.x,
                                             renderData->renderArea.renderArea.y,
                                             renderData->renderArea.renderArea.width,
                                             renderData->renderArea.renderArea.height);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Dispose:
            {
                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationDisposeCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(I)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_WaitForTime:
            {
                const SmartOfficeAnimCmdWaitForTimeData *timeData;

                timeData = &spec->data.waitForTime;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationWaitForTimeCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IF)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             timeData->delay);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_WaitForLayer:
            {
                const SmartOfficeAnimCmdWaitForLayerData *layerData;

                layerData = &spec->data.waitForLayer;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationWaitForLayerCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(III)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             layerData->layer,
                                             layerData->whence);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_WaitForEvent:
            {
                const SmartOfficeAnimCmdWaitForEventData *eventData;

                eventData = &spec->data.waitForEvent;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationWaitForEventCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(II)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             eventData->event);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Plot:
            {
                const SmartOfficeAnimCmdPlotData *plotData;

                plotData = &spec->data.plot;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              plotData->position.x,
                                              plotData->position.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationPlotCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IFLandroid/graphics/PointF;I)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             plotData->delay,
                                             point_obj,
                                             plotData->zPosition);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_SetVisibility:
            {
                const SmartOfficeAnimCmdVisibilityData *visibilityData;

                visibilityData = &spec->data.visibility;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationSetVisibilityCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IFZ)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             visibilityData->delay,
                                             !!visibilityData->visible);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_SetPosition:
            {
                const SmartOfficeAnimCmdPositionData *positionData;

                positionData = &spec->data.position;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              positionData->newOrigin.x,
                                              positionData->newOrigin.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationSetPositionCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IFLandroid/graphics/PointF;)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             positionData->delay,
                                             point_obj);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_SetOpacity:
            {
                const SmartOfficeAnimCmdOpacityData *opacityData;

                opacityData = &spec->data.opacity;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationSetOpacityCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IFF)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             opacityData->delay,
                                             opacityData->opacity);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_SetTransform:
            {
                const SmartOfficeAnimCmdTransformData *trfmData;

                trfmData = &spec->data.transform;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationSetTransformCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IFFFFFFF)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             trfmData->delay,
                                             trfmData->transform.a,
                                             trfmData->transform.b,
                                             trfmData->transform.c,
                                             trfmData->transform.d,
                                             trfmData->transform.tx,
                                             trfmData->transform.ty);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Move:
            {
                const SmartOfficeAnimCmdMoveData *moveData;

                moveData = &spec->data.move;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              moveData->start.x,
                                              moveData->start.y);
                if (point_obj == NULL)
                    return NULL;

                point_obj2 = (*env)->NewObject(env,
                                               clazz, PointF_ctor_mid,
                                               moveData->end.x,
                                               moveData->end.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationMoveCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIZZFFLandroid/graphics/PointF;Landroid/graphics/PointF;I)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             moveData->looping.turns,
                                             !!moveData->looping.reversed,
                                             !!moveData->looping.bouncing,
                                             moveData->delay,
                                             moveData->duration,
                                             point_obj,
                                             point_obj2,
                                             moveData->profile);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Fade:
            {
                const SmartOfficeAnimCmdFadeData *fadeData;

                fadeData = &spec->data.fade;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationFadeCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIZZFFIIFFI)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             fadeData->looping.turns,
                                             !!fadeData->looping.reversed,
                                             !!fadeData->looping.bouncing,
                                             fadeData->delay,
                                             fadeData->duration,
                                             fadeData->effect,
                                             fadeData->subType,
                                             fadeData->startOpacity,
                                             fadeData->endOpacity,
                                             fadeData->profile);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Scale:
            {
                const SmartOfficeAnimCmdScaleData *scaleData;

                scaleData = &spec->data.scale;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              scaleData->centre.x,
                                              scaleData->centre.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationScaleCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIZZFFFFFFLandroid/graphics/PointF;I)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             scaleData->looping.turns,
                                             !!scaleData->looping.reversed,
                                             !!scaleData->looping.bouncing,
                                             scaleData->delay,
                                             scaleData->duration,
                                             scaleData->startX, scaleData->startY,
                                             scaleData->endX, scaleData->endY,
                                             point_obj,
                                             scaleData->profile);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        case SmartOfficeAnimCmd_Rotate:
            {
                const SmartOfficeAnimCmdRotateData *rotateData;

                rotateData = &spec->data.rotate;

                clazz = (*env)->FindClass(env, "android/graphics/PointF");
                if (clazz == NULL)
                    return NULL;

                point_obj = (*env)->NewObject(env,
                                              clazz, PointF_ctor_mid,
                                              rotateData->origin.x,
                                              rotateData->origin.y);
                if (point_obj == NULL)
                    return NULL;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationRotateCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIZZFFLandroid/graphics/PointF;FFI)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             rotateData->looping.turns,
                                             !!rotateData->looping.reversed,
                                             !!rotateData->looping.bouncing,
                                             rotateData->delay,
                                             rotateData->duration,
                                             point_obj,
                                             rotateData->startAngle,
                                             rotateData->endAngle,
                                             rotateData->profile);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;
        case SmartOfficeAnimCmd_ColourEffect:
            {
                const SmartOfficeAnimCmdColourEffectData *effectData;

                effectData = &spec->data.colourEffect;

                clazz = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationColourEffectCommand");
                if (clazz == NULL)
                    return NULL;

                mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIZZFFI)V");

                anim_obj = (*env)->NewObject(env,
                                             clazz, mid,
                                             spec->layer,
                                             effectData->looping.turns,
                                             !!effectData->looping.reversed,
                                             !!effectData->looping.bouncing,
                                             effectData->delay,
                                             effectData->duration,
                                             effectData->effect);
                if (anim_obj == NULL)
                    return NULL;
            }
            break;

        // Note: No composite command (yet)

        default:
            return NULL;
    }

    return anim_obj;
}

JNIEXPORT jobjectArray JNICALL
JNI_FN(SOPage_getAnimations)(JNIEnv *env, jobject thiz)
{
    SOError          err;
    SOPage          *page;
    SmartOfficeAnim *anims = NULL;
    jclass           animationClass;
    jobjectArray     ary   = NULL;
    int              i;

    page = getSOPage(env, thiz);
    if (page == NULL)
        return NULL;

    err = SmartOfficePage_getAnimations(page->page, &anims);
    if (err || anims == NULL)
        return NULL;

    animationClass = (*env)->FindClass(env, PACKAGEPATH"/animation/SOAnimationCommand");

    if (animationClass == NULL)
        goto exit;

    // build an array of SOAnimationCommands
    ary = (*env)->NewObjectArray(env, anims->ncommands, animationClass, NULL);
    if (ary == NULL)
        goto exit;

    for (i = 0; i < anims->ncommands; i++)
    {
        jobject janim;

        janim = objectForAnimCmd(env, &anims->commands[i]);
        if (janim == NULL)
            break; // error

        (*env)->SetObjectArrayElement(env, ary, i, janim);
        (*env)->DeleteLocalRef(env, janim);
    }

exit:
    SmartOfficePage_destroyAnimations(anims);

    return ary;
}

/* SelectionLimits */

/* getStart */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getStart)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);
    jclass pointfClass;
    jobject res;

    if (slim == NULL)
        return NULL;

    pointfClass = (*env)->FindClass(env, "android/graphics/PointF");
    res = (*env)->NewObject(env, pointfClass, PointF_ctor_mid, slim->startPt.x, slim->startPt.y);
    return res;
}

/* getEnd */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getEnd)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);
    jclass pointfClass;
    jobject res;

    if (slim == NULL)
        return NULL;

    pointfClass = (*env)->FindClass(env, "android/graphics/PointF");
    res = (*env)->NewObject(env, pointfClass, PointF_ctor_mid, slim->endPt.x, slim->endPt.y);
    return res;
}

/* getHandle */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getHandle)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);
    jclass pointfClass;
    jobject res;

    if (slim == NULL)
        return NULL;

    pointfClass = (*env)->FindClass(env, "android/graphics/PointF");
    res = (*env)->NewObject(env, pointfClass, PointF_ctor_mid, slim->handlePt.x, slim->handlePt.y);
    return res;
}

/* getBox */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getBox)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);
    jobject res;

    if (slim == NULL)
        return NULL;

    res = (*env)->NewObject(env, rectfClass, RectF_ctor_mid, slim->area.x, slim->area.y, slim->area.x + slim->area.width, slim->area.y + slim->area.height);
    return res;
}

/* getHasSelectionStart */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasSelectionStart)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!slim->hasStart);
}

/* getHasSelectionEnd */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasSelectionEnd)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!slim->hasEnd);
}

/* getIsActive */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsActive)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    /* TODO: this is temporary until we gigure out what's broken */
    return (jboolean)(slim && slim->hasStart && slim->hasEnd);
//    return (jboolean)(slim && !!(slim->flags & SmartOfficeSelectionRegion_Active));
}

/* getIsCaret */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsCaret)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!(slim->flags & SmartOfficeSelectionRegion_Caret));
}

/* getIsExtensible */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsExtensible)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!(slim->flags & SmartOfficeSelectionRegion_Extensible));
}

/* getHasPendingVisualChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasPendingVisualChanges)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!(slim->flags & SmartOfficeSelectionRegion_PendingVisualChanges));
}

/* getIsComposing */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsComposing)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    return (jboolean)(slim && !!(slim->flags & SmartOfficeSelectionRegion_Composing));
}

/* scaleBy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_scaleBy)(JNIEnv * env, jobject thiz, double scale)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    if (slim == NULL)
        return;

    slim->startPt.x *= scale;
    slim->startPt.y *= scale;
    slim->endPt.x *= scale;
    slim->endPt.y *= scale;
    slim->handlePt.x *= scale;
    slim->handlePt.y *= scale;
    slim->area.x *= scale;
    slim->area.y *= scale;
    slim->area.width *= scale;
    slim->area.height *= scale;
}

/* offsetBy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_offsetBy)(JNIEnv * env, jobject thiz, double offX, double offY)
{
    SOSelectionLimits *slim = getSOSelectionLimits(env, thiz);

    if (slim == NULL)
        return;

    slim->startPt.x += offX;
    slim->startPt.y += offY;
    slim->endPt.x += offX;
    slim->endPt.y += offY;
    slim->handlePt.x += offX;
    slim->handlePt.y += offY;
    slim->area.x += offX;
    slim->area.y += offY;
}

/* combineWith */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_combineWith)(JNIEnv * env, jobject thiz, jobject that)
{
    SOSelectionLimits *slim  = getSOSelectionLimits(env, thiz);
    SOSelectionLimits *slim2 = getSOSelectionLimits(env, that);

    if (slim2->hasStart && !slim->hasStart)
    {
        slim->hasStart = 1;
        slim->startPt = slim2->startPt;
    }

    if (slim2->hasEnd)
    {
        slim->hasEnd = 1;
        slim->endPt =  slim2->endPt;
        slim->handlePt = slim2->handlePt;
    }

    if (slim2->area.width > 0 && slim2->area.height > 0)
    {
        if (slim->area.width > 0 && slim->area.height > 0)
        {
            float xmin = MIN(slim->area.x, slim2->area.x);
            float ymin = MIN(slim->area.y, slim2->area.y);
            float xmax = MAX(slim->area.x + slim->area.width, slim2->area.x + slim2->area.width);
            float ymax = MAX(slim->area.y + slim->area.height, slim2->area.y + slim2->area.height);
            slim->area.x = xmin;
            slim->area.y = ymin;
            slim->area.width = xmax - xmin;
            slim->area.height = ymax - ymin;
        }
        else
        {
            slim->area = slim2->area;
        }
    }

    slim->flags |= slim2->flags;
}

/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_destroy)(JNIEnv * env, jobject thiz)
{
    SOSelectionLimits *slim  = getSOSelectionLimits(env, thiz);

    free(slim);
}


/* SOSelectionTableRange */

/* firstColumn */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_firstColumn)(JNIEnv * env, jobject thiz)
{
    SOSelectionTableRange *srange = getSOSelectionTableRange(env, thiz);
    if (srange==NULL)
        return 0;
    return srange->firstColumn;
}

/* columnCount */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_columnCount)(JNIEnv * env, jobject thiz)
{
    SOSelectionTableRange *srange = getSOSelectionTableRange(env, thiz);
    if (srange==NULL)
        return 0;
    return srange->columnCount;
}

/* firstRow */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_firstRow)(JNIEnv * env, jobject thiz)
{
    SOSelectionTableRange *srange = getSOSelectionTableRange(env, thiz);
    if (srange==NULL)
        return 0;
    return srange->firstRow;
}

/* rowCount */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_rowCount)(JNIEnv * env, jobject thiz)
{
    SOSelectionTableRange *srange = getSOSelectionTableRange(env, thiz);
    if (srange==NULL)
        return 0;
    return srange->rowCount;
}


/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionTableRange_destroy)(JNIEnv * env, jobject thiz)
{
    SOSelectionTableRange *slim  = getSOSelectionTableRange(env, thiz);

    free(slim);
}


/* SOBitmap */

/* nativeCopyPixels */
JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeCopyPixels565)(JNIEnv *env, jobject thiz, int dstX, int dstY, int dstW, int w, int h, int srcX, int srcY, int srcW, jobject dstBitmap, jobject srcBitmap)
{
    char *d, *s;
    int y;

    if (AndroidBitmap_lockPixels(env, dstBitmap, (void **)&d) < 0)
        return;
    if (AndroidBitmap_lockPixels(env, srcBitmap, (void **)&s) < 0)
        return;

    d += (dstX + dstY * dstW) * 2;
    s += (srcX + srcY * srcW) * 2;

    for (y = 0; y < h; y++)
    {
        memcpy(d, s, w * 2);
        d += dstW * 2;
        s += srcW * 2;
    }

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);
}

void
darken_bitmap_565(uint8_t  *bitmap_,
                  uint32_t  w,
                  uint32_t  h,
                  ptrdiff_t span)
{
    uint16_t *bitmap = (uint16_t *)(void *)bitmap_;
    uint32_t x;

    span >>= 1;
    span -= w;
    for (; h > 0; h--)
    {
        for (x = w; x > 0; x--)
        {
            int v = *bitmap;

            //  get the color values
            int r = ((v & 0xF800)) >> 11;
            int g = ((v & 0x07E0)) >> 5;
            int b = ((v & 0x001F));

            //  invert
            int y = (39970*r + 38442*g + 15140*b + 16384)>>15;
            y = 64-y;
            r += (y>>1);
            if (r < 0) r = 0; else if (r > 31) r = 31;
            g += y;
            if (g < 0) g = 0; else if (g > 63) g = 63;
            b += (y>>1);
            if (b < 0) b = 0; else if (b > 31) b = 31;

            //  put the values back
            *bitmap++ = b | (g<<5) | (r<<11);
        }
        bitmap += span;
    }
}

void
darken_bitmap_888(uint8_t  *bitmap,
                  uint32_t  w,
                  uint32_t  h,
                  ptrdiff_t span)
{
    uint32_t x;

    span -= w*4;
    for (; h > 0; h--)
    {
        for (x = w; x > 0; x--)
        {
            int r = bitmap[0];
            int g = bitmap[1];
            int b = bitmap[2];
            int y = (39336 * r + 76884 * g + 14900 * b + 32768)>>16;
            y = 259-y;
            r += y;
            if (r < 0) r = 0; else if (r > 255) r = 255;
            g += y;
            if (g < 0) g = 0; else if (g > 255) g = 255;
            b += y;
            if (b < 0) b = 0; else if (b > 255) b = 255;
            bitmap[0] = r;
            bitmap[1] = g;
            bitmap[2] = b;
            bitmap += 4;
        }
        bitmap += span;
    }
}

JNIEXPORT void JNICALL
JNI_FN(SOBitmap_invertLuminance)(JNIEnv *env, jobject thiz)
{
    jobject            nativeBitmap;
    jobject            bitmapRect;
    AndroidBitmapInfo  bitmapInfo;
    uint8_t            *bitmapPixels;
    int                 width, height;
    int                 left, top, right, bottom;

    //  get the native Bitmap field from the SOBitmap
    nativeBitmap = (*env)->GetObjectField(env, thiz, SOBitmap_bitmap_fid);
    if (nativeBitmap == NULL)
        return;

    //  get native bitmap info
    if (AndroidBitmap_getInfo(env, nativeBitmap, &bitmapInfo) < 0)
        return;

    //  get the native Rect field from the SOBitmap
    bitmapRect = (*env)->GetObjectField(env, thiz, SOBitmap_rect_fid);
    if (bitmapRect == NULL)
        return;

    //  get the dimensions from the Rect.
    left = (*env)->GetIntField(env, bitmapRect, Rect_left_fid);
    top = (*env)->GetIntField(env, bitmapRect, Rect_top_fid);
    right = (*env)->GetIntField(env, bitmapRect, Rect_right_fid);
    bottom = (*env)->GetIntField(env, bitmapRect, Rect_bottom_fid);
    width = right-left;
    height = bottom-top;

    //  Lock the bitmap
    if (AndroidBitmap_lockPixels(env, nativeBitmap, (void **)&bitmapPixels) < 0)
        return;

    //  do it
    switch (bitmapInfo.format)
    {
        case ANDROID_BITMAP_FORMAT_RGB_565:
            darken_bitmap_565(bitmapPixels + bitmapInfo.stride*top + left*2, width, height, bitmapInfo.stride);
            break;

        case ANDROID_BITMAP_FORMAT_RGBA_8888:
            darken_bitmap_888(bitmapPixels + bitmapInfo.stride*top + left*4, width, height, bitmapInfo.stride);
            break;

        default:
            break;
    }

    //  unlock pixels
    AndroidBitmap_unlockPixels(env, nativeBitmap);
}

JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeCopyPixels888)(JNIEnv *env, jobject thiz, int dstX, int dstY, int dstW, int w, int h, int srcX, int srcY, int srcW, jobject dstBitmap, jobject srcBitmap)
{
    char *d, *s;
    int y;

    if (AndroidBitmap_lockPixels(env, dstBitmap, (void **)&d) < 0)
        return;
    if (AndroidBitmap_lockPixels(env, srcBitmap, (void **)&s) < 0)
        return;

    d += (dstX + dstY * dstW) * 4;
    s += (srcX + srcY * srcW) * 4;

    for (y = 0; y < h; y++)
    {
        memcpy(d, s, w * 4);
        d += dstW * 4;
        s += srcW * 4;
    }

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);
}

/* nativeMergePixels */
JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeMergePixels565)(JNIEnv *env, jobject thiz,
                                      jobject dstBitmap, int dx, int dy, int dw, int dh,
                                      jobject srcBitmap, jobject srcAlpha, int sx, int sy, int sw, int sh)
{
    int                w, h;
    AndroidBitmapInfo  srcbInfo;
    AndroidBitmapInfo  srcaInfo;
    AndroidBitmapInfo  dstInfo;
    uint32_t           srcbInfoStride;
    uint32_t           srcaInfoStride;
    uint32_t           dstInfoStride;
    void              *srcb; /* source bitmap */
    void              *srca; /* source alpha */
    void              *dst;  /* destination bitmap */
    uint16_t          *cd;
    uint8_t           *ad;
    uint32_t          *dd;
    int                y;

    if (dstBitmap == NULL || srcBitmap == NULL || srcAlpha  == NULL)
        return;

    /* Use the smaller output area */
    if (sw > dw) w = dw; else w = sw;
    if (sh > dh) h = dh; else h = sh;

    if (AndroidBitmap_getInfo(env, srcBitmap, &srcbInfo) < 0)
        return;
    if (AndroidBitmap_getInfo(env, srcAlpha,  &srcaInfo) < 0)
        return;
    if (AndroidBitmap_getInfo(env, dstBitmap, &dstInfo)  < 0)
        return;

    /* Cache the stride values */
    srcbInfoStride = srcbInfo.stride / 2;
    srcaInfoStride = srcaInfo.stride / 1;
    dstInfoStride  = dstInfo.stride  / 4;

    /* Lock down the bitmap memory prior to processing */
    if (AndroidBitmap_lockPixels(env, srcBitmap, (void **)&srcb) < 0)
        return;
    if (AndroidBitmap_lockPixels(env, srcAlpha,  (void **)&srca) < 0)
        goto exit10;
    if (AndroidBitmap_lockPixels(env, dstBitmap, (void **)&dst)  < 0)
        goto exit20;

    /* Merge the b5g6r5 + g8 pixels into r8g8b8a8 (not premultiplied) */
    cd = ((uint16_t *) srcb) + sy * srcbInfoStride + sx; /*   b5g6r5 colour pixels */
    ad = ((uint8_t *)  srca) + sy * srcaInfoStride + sx; /*       g8  alpha pixels */
    dd = ((uint32_t *) dst)  + dy * dstInfoStride  + dx; /* r8g8b8a8 output pixels */

    for (y = 0; y < h; y++)
    {
        uint16_t *cp = cd;
        uint8_t  *ap = ad;
        uint32_t *dp = dd;
        int       x;

        for (x = 0; x < w; x++)
        {
            uint16_t cpx = *cp++;
            uint8_t  apx = *ap++;
            uint32_t r,g,b;

            /* Pixel_Format_b5g6r5:
             * 5-bits blue in lowest bits, then 6 of green, then red */
            r = (cpx & 0xF800u) >> 8; r |= (r >> 5);
            g = (cpx & 0x07E0u) >> 3; g |= (g >> 6);
            b = (cpx & 0x001Fu) << 3; b |= (b >> 5);

            /* Android's 32-bit format is RGBA (0xAABBGGRR) */
            *dp++ = (r << 0) | (g << 8) | (b << 16) | (apx << 24);
        }

        cd += srcbInfoStride; /* advance by a packed row */
        ad += srcaInfoStride; /* advance by a packed row */
        dd += dstInfoStride;  /* advance by a 4-byte aligned row */
    }

    AndroidBitmap_unlockPixels(env, dstBitmap);
exit20:
    AndroidBitmap_unlockPixels(env, srcAlpha);
exit10:
    AndroidBitmap_unlockPixels(env, srcBitmap);
}

JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeMergePixels888)(JNIEnv *env, jobject thiz,
                                      jobject dstBitmap, int dx, int dy, int dw, int dh,
                                      jobject srcBitmap, jobject srcAlpha, int sx, int sy, int sw, int sh)
{
    int                w, h;
    AndroidBitmapInfo  srcbInfo;
    AndroidBitmapInfo  srcaInfo;
    AndroidBitmapInfo  dstInfo;
    uint32_t           srcbInfoStride;
    uint32_t           srcaInfoStride;
    uint32_t           dstInfoStride;
    void              *srcb; /* source bitmap */
    void              *srca; /* source alpha */
    void              *dst;  /* destination bitmap */
    uint32_t          *cd;
    uint8_t           *ad;
    uint32_t          *dd;
    int                y;

    if (dstBitmap == NULL || srcBitmap == NULL || srcAlpha  == NULL)
        return;

    /* Use the smaller output area */
    if (sw > dw) w = dw; else w = sw;
    if (sh > dh) h = dh; else h = sh;

    if (AndroidBitmap_getInfo(env, srcBitmap, &srcbInfo) < 0)
        return;
    if (AndroidBitmap_getInfo(env, srcAlpha,  &srcaInfo) < 0)
        return;
    if (AndroidBitmap_getInfo(env, dstBitmap, &dstInfo)  < 0)
        return;

    /* Cache the stride values */
    srcbInfoStride = srcbInfo.stride / 4;
    srcaInfoStride = srcaInfo.stride / 1;
    dstInfoStride  = dstInfo.stride  / 4;

    /* Lock down the bitmap memory prior to processing */
    if (AndroidBitmap_lockPixels(env, srcBitmap, (void **)&srcb) < 0)
        return;
    if (AndroidBitmap_lockPixels(env, srcAlpha,  (void **)&srca) < 0)
        goto exit10;
    if (AndroidBitmap_lockPixels(env, dstBitmap, (void **)&dst)  < 0)
        goto exit20;

    /* Merge the r8g8b8x8 + g8 pixels into r8g8b8a8 (not premultiplied) */
    cd = ((uint32_t *) srcb) + sy * srcbInfoStride + sx; /*   b5g6r5 colour pixels */
    ad = ((uint8_t *)  srca) + sy * srcaInfoStride + sx; /*       g8  alpha pixels */
    dd = ((uint32_t *) dst)  + dy * dstInfoStride  + dx; /* r8g8b8a8 output pixels */

    for (y = 0; y < h; y++)
    {
        uint32_t *cp = cd;
        uint8_t  *ap = ad;
        uint32_t *dp = dd;
        int       x;

        for (x = 0; x < w; x++)
        {
            uint16_t cpx = *cp++;
            uint8_t  apx = *ap++;

            /* Android's 32-bit format is RGBA (0xAABBGGRR) */
            *dp++ = (cpx & ~0xFF000000) | (apx<<24);
        }

        cd += srcbInfoStride; /* advance by a packed row */
        ad += srcaInfoStride; /* advance by a packed row */
        dd += dstInfoStride;  /* advance by a 4-byte aligned row */
    }

    AndroidBitmap_unlockPixels(env, dstBitmap);
exit20:
    AndroidBitmap_unlockPixels(env, srcAlpha);
exit10:
    AndroidBitmap_unlockPixels(env, srcBitmap);
}


/* SORender */

/* abort */
JNIEXPORT void JNICALL
JNI_FN(SORender_abort)(JNIEnv * env, jobject thiz)
{
    SORender *render = getSORender(env, thiz);

    if (render) {
        /* abort, not destroy */
        SmartOfficeRender_abort(render->render);
    }
}

/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SORender_destroy)(JNIEnv * env, jobject thiz)
{
    SORender *render = getSORender(env, thiz);

    if (render) {
        /* destroy, and free the internal object. */
        SmartOfficeRender_destroy(render->render);
        (*env)->SetLongField(env, thiz, SORender_internal_fid, 0);
        free(render);
    }
}

/* getSelectionListStyleIsDecimal */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsDecimal)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "list-style-type", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "list-style-type");
    res = styleCmp(value, "decimal");
    free(utf8Style);
    return res;
}

/* getSelectionListStyleIsDisc */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsDisc)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "list-style-type", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "list-style-type");
    res = styleCmp(value, "disc");
    free(utf8Style);
    return res;
}

/* getSelectionListStyleIsNone */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsNone)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    jboolean res;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "list-style-type", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "list-style-type");
    res = styleCmp(value, "none");
    free(utf8Style);
    return res;
}

/* setSelectionListStyle */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionListStyle)(JNIEnv * env, jobject thiz, jint style)
{
    SODoc *doc = getSODoc(env, thiz);
    char text[32];

    if (doc == NULL)
        return;

    switch (style)
    {
        case SOListStyle_Decimal:
            sprintf(text,"list-style-type:decimal");
            break;

        case SOListStyle_Disc:
            sprintf(text,"list-style-type:disc");
            break;

        case SOListStyle_None:
            sprintf(text,"list-style-type:none");
            break;

        default:
            //  TODO
            break;
    }

    SmartOfficeDoc_setSelectionStyle(doc->doc, text);
}

/* setSelectionAlignment */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAlignment)(JNIEnv * env, jobject thiz, jint alignment)
{
    SODoc *doc = getSODoc(env, thiz);
    char *text=NULL;

    if (doc == NULL)
        return;

    switch (alignment)
    {
        case SOTextAlign_Left:
            text = "text-align:left";
            break;

        case SOTextAlign_Center:
            text = "text-align:center";
            break;

        case SOTextAlign_Right:
            text = "text-align:right";
            break;

        case SOTextAlign_Justify:
            text = "text-align:justify";
            break;

        default:
            //  TODO
            return;
            break;
    }

    SmartOfficeDoc_setSelectionStyle(doc->doc, text);
}

/* setSelectionAlignmentV */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAlignmentV)(JNIEnv * env, jobject thiz, jint alignment)
{
    SODoc *doc = getSODoc(env, thiz);
    char *text=NULL;

    if (doc == NULL)
        return;

    switch (alignment)
    {
        case SOTextAlignV_Top:
            text = "-epage-textbox-vertical-align:top";
            break;

        case SOTextAlignV_Center:
            text = "-epage-textbox-vertical-align:center";
            break;

        case SOTextAlignV_Bottom:
            text = "-epage-textbox-vertical-align:bottom";
            break;

        default:
            //  TODO
            return;
            break;
    }

    SmartOfficeDoc_setSelectionStyle(doc->doc, text);
}

/* getSelectionAlignment */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getSelectionAlignment)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    int result;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "text-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "text-align");

    result = 0;
    if (styleCmp(value, "left")==JNI_TRUE)
        result = SOTextAlign_Left;
    else if (styleCmp(value, "center")==JNI_TRUE)
        result = SOTextAlign_Center;
    else if (styleCmp(value, "right")==JNI_TRUE)
        result = SOTextAlign_Right;

    free(utf8Style);
    return result;
}

/* getSelectionRotation */
JNIEXPORT float JNICALL
JNI_FN(SODoc_getSelectionRotation)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    float result=0;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "shape-rotation", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "shape-rotation");

    //  value make be null if object can't be rotated
    if (value != NULL)
        result = atof(value);

    free(utf8Style);
    return result;
}

/* setSelectionRotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionRotation)(JNIEnv * env, jobject thiz, float angle)
{
    SODoc *doc = getSODoc(env, thiz);
    char val[100];

    if (doc == NULL)
        return;

    sprintf(val, "shape-rotation:%.2f", angle);

    SmartOfficeDoc_setSelectionStyle(doc->doc, val);
}


/* getSelectionAlignmentV */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getSelectionAlignmentV)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    int result;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "-epage-textbox-vertical-align", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "-epage-textbox-vertical-align");

    result = 0;
    if (styleCmp(value, "top")==JNI_TRUE)
        result = SOTextAlignV_Top;
    else if (styleCmp(value, "center")==JNI_TRUE)
        result = SOTextAlignV_Center;
    else if (styleCmp(value, "bottom")==JNI_TRUE)
        result = SOTextAlignV_Bottom;

    free(utf8Style);
    return result;
}

/* setSelectionArrangeForwards */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeForwards)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, "shape-zorder:forwards");
}

/* setSelectionArrangeFront */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeFront)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, "shape-zorder:front");
}

/* setSelectionArrangeBackwards */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeBackwards)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, "shape-zorder:backwards");
}

/* setSelectionArrangeBack */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeBack)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setSelectionStyle(doc->doc, "shape-zorder:back");
}

/* setSelectionFillColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFillColor)(JNIEnv * env, jobject thiz, jstring color)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charColor;

    if (doc == NULL)
        return;

    charColor = (*env)->GetStringUTFChars(env, color, NULL);
    if (charColor)
    {
        char *text = malloc(strlen(charColor) + 16);
        if (text)
        {
            sprintf(text, "fill-color:%s", charColor);
            SmartOfficeDoc_setSelectionStyle(doc->doc, text);
            free(text);
        }
        (*env)->ReleaseStringUTFChars(env, color, charColor);
    }
}

/* setSelectionLineColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineColor)(JNIEnv * env, jobject thiz, jstring color)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *charColor;

    if (doc == NULL)
        return;

    charColor = (*env)->GetStringUTFChars(env, color, NULL);
    if (charColor)
    {
        char *text = malloc(strlen(charColor) + 16);
        if (text)
        {
            sprintf(text, "line-color:%s", charColor);
            SmartOfficeDoc_setSelectionStyle(doc->doc, text);
            free(text);
        }
        (*env)->ReleaseStringUTFChars(env, color, charColor);
    }
}

/* setSelectionLineWidth */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineWidth)(JNIEnv * env, jobject thiz, float width)
{
    SODoc *doc = getSODoc(env, thiz);
    char swidth[100];

    if (doc == NULL)
        return;

    sprintf(swidth, "line-width:%fpt", width);
    SmartOfficeDoc_setSelectionStyle(doc->doc, swidth);
}

/* getSelectionLineWidth */
JNIEXPORT float JNICALL
JNI_FN(SODoc_getSelectionLineWidth)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    float result=0;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "line-width", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "line-width");

    result = atof(value)/12700.0;  //  EMUs per pt

    free(utf8Style);
    return result;
}

/* setSelectionLineType */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineType)(JNIEnv * env, jobject thiz, int type)
{
    SODoc *doc = getSODoc(env, thiz);
    char stype[100];

    if (doc == NULL)
        return;

    sprintf(stype, "line-type:%d", type);
    SmartOfficeDoc_setSelectionStyle(doc->doc, stype);
}

/* createInkAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_createInkAnnotation)(JNIEnv * env, jobject thiz,
                                  int page, jobjectArray jpoints, float thickness, int color)
{
    SODoc *doc = getSODoc(env, thiz);
    size_t pointsCount;
    int i;
    SmartOfficePathPoint *cPoints;
    SmartOfficePathPoint *p;
    jobject jpoint;
    unsigned int ucolor;
    unsigned int r, g, b, a;

    if (doc == NULL)
        return;

    //  allocate an array of points
    pointsCount = (*env)->GetArrayLength( env, jpoints );
    cPoints = calloc(sizeof(*cPoints), pointsCount);
    if (cPoints == NULL)
        return;

    //  loop thru the points
    for (i = 0; i < pointsCount; i++)
    {
        p = &cPoints[i];

        //  set the coordinates
        jpoint = (jobject) (*env)->GetObjectArrayElement(env, jpoints, i);
        p->x = (*env)->GetFloatField(env, jpoint, SOPoint_x_fid);
        p->y = (*env)->GetFloatField(env, jpoint, SOPoint_y_fid);

        //  set the type
        p->type = (*env)->GetIntField(env, jpoint, SOPoint_type_fid);
    }

    //  rearrange the colors.  The caller gives us argb, but we want rgba.
    ucolor = color & 0xffffffff;
    b =  ucolor & 0x000000ff;
    g = (ucolor & 0x0000ff00) >> 8;
    r = (ucolor & 0x00ff0000) >> 16;
    a = (ucolor & 0xff000000) >> 24;
    ucolor = (r<<24) + (g<<16) + (b<<8) + a;

    //  create!
    SmartOfficeDoc_createInkAnnotation(doc->doc, page, cPoints, pointsCount, thickness, ucolor);

    //  free allocated memory
    free(cPoints);
}

/* createTextAnnotationAt */
JNIEXPORT void JNICALL
JNI_FN(SODoc_createTextAnnotationAt)(JNIEnv * env, jobject thiz, jobject jpoint, int page)
{
    SODoc *doc = getSODoc(env, thiz);
    jclass pointfClass;
    jfieldID xfid, yfid;
    SmartOfficePoint spt = {0, 0};

    if (doc == NULL)
        return;

    //  get info about the PointF class
    pointfClass = (*env)->FindClass(env, "android/graphics/PointF");
    xfid = (*env)->GetFieldID(env, pointfClass, "x", "F");
    yfid = (*env)->GetFieldID(env, pointfClass, "y", "F");

    //  set the point values
    spt.x = (*env)->GetFloatField(env, jpoint, xfid);
    spt.y = (*env)->GetFloatField(env, jpoint, yfid);

    //  go!
    SmartOfficeDoc_createTextAnnotation(doc->doc, page, &spt);
}


/* addHighlightAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addHighlightAnnotation)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_addHighlightAnnotation(doc->doc);
}

/* deleteHighlightAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteHighlightAnnotation)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_deleteHighlightAnnotation(doc->doc);
}

/* getSelectionAnnotationAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationAuthor)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    int annotationId;
    char *author = NULL;
    jobject res;

    if (doc == NULL)
        return NULL;

    SOError e = SmartOfficeDoc_getSelectionAnnotationId(doc->doc, &annotationId);
    if (e)
        return NULL;

    SmartOfficeDoc_getAnnotationAuthor(doc->doc, annotationId, &author, NULL, NULL);
    if (author == NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, author);
    free(author);
    return res;
}

/* getSelectionAnnotationDate */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationDate)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    int annotationId;
    char *date = NULL;
    jobject res;

    if (doc == NULL)
        return NULL;

    SOError e = SmartOfficeDoc_getSelectionAnnotationId(doc->doc, &annotationId);
    if (e)
        return NULL;

    SmartOfficeDoc_getAnnotationDate(doc->doc, annotationId, &date, NULL, NULL);
    if (date == NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, date);
    free(date);
    return res;
}

/* getSelectionAnnotationComment */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationComment)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    int annotationId;
    char *comment = NULL;
    jobject res;

    if (doc == NULL)
        return NULL;

    SOError e = SmartOfficeDoc_getSelectionAnnotationId(doc->doc, &annotationId);
    if (e)
        return NULL;

    SmartOfficeDoc_getAnnotationComment(doc->doc, annotationId, &comment, NULL, NULL);
    if (comment == NULL)
        return NULL;

    res = (*env)->NewStringUTF(env, comment);
    free(comment);
    return res;
}

/* setSelectionAnnotationComment */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAnnotationComment)(JNIEnv * env, jobject thiz, jstring jcomment)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *comment;
    SOError e;

    if (doc == NULL)
        return;

    int annotationId;

    e = SmartOfficeDoc_getSelectionAnnotationId(doc->doc, &annotationId);
    if (e) {
        return;
    }

    comment = (*env)->GetStringUTFChars(env, jcomment, NULL);
    e = SmartOfficeDoc_setAnnotationComment(doc->doc, annotationId, comment);
    (*env)->ReleaseStringUTFChars(env, jcomment, comment);
}

/* getSelectionLineType */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getSelectionLineType)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *utf8Style;
    char *value;
    int result=0;

    if (doc == NULL)
        return 0;

    SmartOfficeDoc_getSelectionStyle(doc->doc, "line-type", &utf8Style, allocFn, NULL);
    value = findStyleInString(utf8Style, "line-type");

    result = atoi(value);

    free(utf8Style);
    return result;
}

/* providePassword */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_providePassword)(JNIEnv * env, jobject thiz, jstring jpassword)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *password;
    int result;

    if (doc == NULL)
        return JNI_FALSE;

    password = (*env)->GetStringUTFChars(env, jpassword, NULL);
    result = SmartOfficeDoc_providePassword(doc->doc, password);
    (*env)->ReleaseStringUTFChars(env, jpassword, password);

    if (result == 0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* docSupportsReview */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_docSupportsReview)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if ((SmartOfficeDoc_getDocCapabilities(doc->doc) & SmartOfficeDocCapabilities_Review) != 0)
        return JNI_TRUE;

    return JNI_FALSE;
}

/* setAuthor */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_setAuthor)(JNIEnv * env, jobject thiz, jstring jauthor)
{
    SODoc *doc = getSODoc(env, thiz);
    const char *author;
    int result;

    if (doc == NULL)
        return JNI_FALSE;

    author = (*env)->GetStringUTFChars(env, jauthor, NULL);
    result = SmartOfficeDoc_setDocumentAuthor(doc->doc, author);
    (*env)->ReleaseStringUTFChars(env, jauthor, author);

    if (result == 0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* getAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getAuthor)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *cstr = NULL;
    jobject res;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getDocumentAuthor(doc->doc, &cstr, NULL, NULL);

    if (cstr == NULL)
        res = (*env)->NewStringUTF(env, "");
    else
    {
        res = (*env)->NewStringUTF(env, cstr);
        free(cstr);
    }

    return res;
}

/* getTrackingChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getTrackingChanges)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (SmartOfficeDoc_getTrackingChanges(doc->doc) != 0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* setTrackingChanges */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setTrackingChanges)(JNIEnv * env, jobject thiz, jboolean track)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setTackingChanges(doc->doc, track?1:0);
}

/* getShowingTrackedChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getShowingTrackedChanges)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (SmartOfficeDoc_getShowingTrackedChanges(doc->doc) != 0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* setShowingTrackedChanges */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setShowingTrackedChanges)(JNIEnv * env, jobject thiz, jboolean show)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_setShowingTrackedChanges(doc->doc, show?1:0);
}

/* selectionIsAutoshapeOrImage */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_selectionIsAutoshapeOrImage)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (0 != (SmartOfficeDoc_getSelCapabilities(doc->doc)
              & SmartOfficeSelCapabilities_AutoshapeOrImageChange))
        return JNI_TRUE;

    return JNI_FALSE;
}

/* selectionIsReviewable */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_selectionIsReviewable)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (0 != (SmartOfficeDoc_getSelCapabilities(doc->doc)
              & SmartOfficeSelCapabilities_ReviewContent))
        return JNI_TRUE;

    return JNI_FALSE;
}

/* nextTrackedChange */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_nextTrackedChange)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (SmartOfficeDoc_nextTrackedChange(doc->doc)!=0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* previousTrackedChange */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_previousTrackedChange)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return JNI_FALSE;

    if (SmartOfficeDoc_previousTrackedChange(doc->doc)!=0)
        return JNI_TRUE;
    return JNI_FALSE;
}

/* acceptTrackedChange */
JNIEXPORT void JNICALL
JNI_FN(SODoc_acceptTrackedChange)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_acceptTrackedChange(doc->doc);
}

/* rejectTrackedChange */
JNIEXPORT void JNICALL
JNI_FN(SODoc_rejectTrackedChange)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);

    if (doc == NULL)
        return;

    SmartOfficeDoc_rejectTrackedChange(doc->doc);
}

/* getSelectedTrackedChangeAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeAuthor)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *author;
    jobject jauthor;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getTrackedChangeAuthor(doc->doc, &author, NULL, NULL);

    jauthor = (*env)->NewStringUTF(env, author);
    free(author);
    return jauthor;
}

/* getSelectedTrackedChangeDate */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeDate)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *date;
    jobject jdate;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getTrackedChangeDate(doc->doc, &date, NULL, NULL);

    jdate = (*env)->NewStringUTF(env, date);
    free(date);
    return jdate;
}

/* getSelectedTrackedChangeComment */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeComment)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    char *comment;
    jobject jcomment;

    if (doc == NULL)
        return NULL;

    SmartOfficeDoc_getTrackedChangeComment(doc->doc, &comment, NULL, NULL);

    jcomment = (*env)->NewStringUTF(env, comment);
    free(comment);
    return jcomment;
}

/* getSelectedTrackedChangeType */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeType)(JNIEnv * env, jobject thiz)
{
    SODoc *doc = getSODoc(env, thiz);
    SmartOfficeTrackedChangeType type = SmartOfficeTrackedChangeType_NoChange;

    if (doc == NULL)
        return type;

    SmartOfficeDoc_getTrackedChangeType(doc->doc, &type);
    return type;
}
