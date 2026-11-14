#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

extern "C" {
#include <willus.h>
#include <k2pdfopt.h>
#include "context.h"
#include "koptreflow.h"
#include "setting.h"
}

#define LOG_TAG "K2PdfOpt"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    int dev_width;
    int dev_height;
    int dev_dpi;
    int has_page;
    KOPTContext kctx;
} K2PDFOPT_JNI;

static jfieldID handle_field(JNIEnv *env, jobject thiz) {
    jclass cls = env->GetObjectClass(thiz);
    if (cls == NULL) {
        return NULL;
    }
    jfieldID fid = env->GetFieldID(cls, "handle", "J");
    env->DeleteLocalRef(cls);
    return fid;
}

static K2PDFOPT_JNI *get_handle(JNIEnv *env, jobject thiz) {
    jfieldID fid = handle_field(env, thiz);
    if (fid == NULL) {
        return NULL;
    }
    return (K2PDFOPT_JNI *) (intptr_t) env->GetLongField(thiz, fid);
}

static void set_handle(JNIEnv *env, jobject thiz, K2PDFOPT_JNI *handle) {
    jfieldID fid = handle_field(env, thiz);
    if (fid != NULL) {
        env->SetLongField(thiz, fid, (jlong) (intptr_t) handle);
    }
}

static void free_page(K2PDFOPT_JNI *handle) {
    if (handle == NULL || !handle->has_page) {
        return;
    }
    bmp_free(&handle->kctx.dst);
    wrectmaps_free(&handle->kctx.rectmaps);
    wrectmaps_init(&handle->kctx.rectmaps);
    handle->has_page = 0;
}

static void throw_illegal_argument(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls != NULL) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}

/*
** Android Bitmap -> WILLUSBITMAP (24 bpp, alpha composited on white).
*/
static int bitmap_to_bmp(JNIEnv *env, jobject bitmap, WILLUSBITMAP *bmp) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return -1;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888
        && info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        return -1;
    }

    void *pixels = NULL;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return -1;
    }

    int width = (int) info.width;
    int height = (int) info.height;
    bmp_init(bmp);
    bmp->width = width;
    bmp->height = height;
    bmp->bpp = 24;
    for (int i = 0; i < 256; i++) {
        bmp->red[i] = (unsigned char) i;
        bmp->green[i] = (unsigned char) i;
        bmp->blue[i] = (unsigned char) i;
    }
    bmp_alloc(bmp);

    for (int row = 0; row < height; row++) {
        unsigned char *src = (unsigned char *) pixels + row * info.stride;
        unsigned char *dst = bmp_rowptr_from_top(bmp, row);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            for (int col = 0; col < width; col++, src += 4, dst += 3) {
                int alpha = src[3];
                if (alpha >= 255) {
                    dst[0] = src[0];
                    dst[1] = src[1];
                    dst[2] = src[2];
                } else {
                    dst[0] = (unsigned char) ((src[0] * alpha + 255 * (255 - alpha)) / 255);
                    dst[1] = (unsigned char) ((src[1] * alpha + 255 * (255 - alpha)) / 255);
                    dst[2] = (unsigned char) ((src[2] * alpha + 255 * (255 - alpha)) / 255);
                }
            }
        } else {
            uint16_t *p = (uint16_t *) src;
            for (int col = 0; col < width; col++, dst += 3) {
                uint16_t v = p[col];
                unsigned char r5 = (unsigned char) ((v >> 11) & 0x1F);
                unsigned char g6 = (unsigned char) ((v >> 5) & 0x3F);
                unsigned char b5 = (unsigned char) (v & 0x1F);
                dst[0] = (unsigned char) ((r5 * 527 + 23) >> 6);
                dst[1] = (unsigned char) ((g6 * 259 + 33) >> 6);
                dst[2] = (unsigned char) ((b5 * 527 + 23) >> 6);
            }
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return 0;
}

/*
** Crop a rectangle out of WILLUSBITMAP into an Android ARGB_8888 Bitmap.
*/
static jobject bmp_to_bitmap(JNIEnv *env, WILLUSBITMAP *bmp, int x, int y, int w, int h, int dpi) {
    if (bmp == NULL || bmp->data == NULL) {
        return NULL;
    }
    if (x < 0) {
        w += x;
        x = 0;
    }
    if (y < 0) {
        h += y;
        y = 0;
    }
    if (x >= bmp->width || y >= bmp->height) {
        return NULL;
    }
    if (x + w > bmp->width) {
        w = bmp->width - x;
    }
    if (y + h > bmp->height) {
        h = bmp->height - y;
    }
    if (w <= 0 || h <= 0) {
        return NULL;
    }

    jclass bitmap_cls = env->FindClass("android/graphics/Bitmap");
    jclass config_cls = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb_field = env->GetStaticFieldID(config_cls, "ARGB_8888",
                                                "Landroid/graphics/Bitmap$Config;");
    jobject config = env->GetStaticObjectField(config_cls, argb_field);
    jmethodID create = env->GetStaticMethodID(bitmap_cls, "createBitmap",
                                              "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject result = env->CallStaticObjectMethod(bitmap_cls, create, (jint) w, (jint) h, config);
    if (result == NULL) {
        return NULL;
    }

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, result, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return result;
    }
    void *pixels = NULL;
    if (AndroidBitmap_lockPixels(env, result, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return result;
    }

    int bytes_per_pixel = bmp->bpp / 8;
    for (int row = 0; row < h; row++) {
        unsigned char *src = bmp_rowptr_from_top(bmp, y + row) + x * bytes_per_pixel;
        unsigned char *dst = (unsigned char *) pixels + row * info.stride;
        if (bmp->bpp == 24) {
            for (int col = 0; col < w; col++, src += 3, dst += 4) {
                dst[0] = src[0];
                dst[1] = src[1];
                dst[2] = src[2];
                dst[3] = 255;
            }
        } else {
            for (int col = 0; col < w; col++, dst += 4) {
                unsigned char v = src[col];
                dst[0] = bmp->red[v];
                dst[1] = bmp->green[v];
                dst[2] = bmp->blue[v];
                dst[3] = 255;
            }
        }
    }
    AndroidBitmap_unlockPixels(env, result);

    jmethodID set_density = env->GetMethodID(bitmap_cls, "setDensity", "(I)V");
    if (set_density != NULL) {
        env->CallVoidMethod(result, set_density, (jint) dpi);
    }
    return result;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_create(JNIEnv *env, jobject thiz, jint width, jint height,
                                              jint dpi) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL) {
        handle = (K2PDFOPT_JNI *) calloc(1, sizeof(K2PDFOPT_JNI));
        if (handle == NULL) {
            throw_illegal_argument(env, "out of memory");
            return;
        }
        set_handle(env, thiz, handle);
    } else {
        free_page(handle);
    }

    memset(&handle->kctx, 0, sizeof(KOPTContext));
    bmp_init(&handle->kctx.dst);
    bmp_init(&handle->kctx.src);
    wrectmaps_init(&handle->kctx.rectmaps);

    handle->kctx.trim = 0;
    handle->kctx.wrap = 2;
    handle->kctx.white_threshold = -1;
    handle->kctx.paint_white_threshold = 0;
    handle->kctx.indent = 1;
    handle->kctx.rotate = 0;
    handle->kctx.columns = 1;
    handle->kctx.straighten = 0;
    handle->kctx.justification = -1;
    handle->kctx.writing_direction = 0;
    handle->kctx.zoom = 1.0;
    handle->kctx.margin = 0.02;
    handle->kctx.quality = 1.0;
    handle->kctx.contrast = 1.0;
    handle->kctx.defect_size = 1.0;
    handle->kctx.line_spacing = -1.2;
    handle->kctx.word_spacing = -0.2;
    handle->kctx.shrink_factor = 1.0;
    handle->kctx.source_dpi = 0.0;
    handle->kctx.font_magnification = 1.0;
    handle->kctx.cjkchar = 1;

    handle->dev_width = width;
    handle->dev_height = height;
    handle->dev_dpi = dpi;
    handle->has_page = 0;
    handle->kctx.dev_width = width;
    handle->kctx.dev_height = height;
    handle->kctx.dev_dpi = dpi;
    handle->kctx.page_width = width;
    handle->kctx.page_height = height;
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setFontSize(JNIEnv *env, jobject thiz, jfloat size) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.font_magnification = size;
    }
}

JNIEXPORT jfloat JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_getFontSize(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    return handle == NULL ? 1.0f : (jfloat) handle->kctx.font_magnification;
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setSourceDpi(JNIEnv *env, jobject thiz, jfloat dpi) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.source_dpi = dpi;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setMargin(JNIEnv *env, jobject thiz, jfloat margin) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.margin = margin;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setColumns(JNIEnv *env, jobject thiz, jint columns) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.columns = columns;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setQuality(JNIEnv *env, jobject thiz, jfloat quality) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.quality = quality;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setWrap(JNIEnv *env, jobject thiz, jboolean wrap) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.wrap = wrap ? 1 : 0;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setTrim(JNIEnv *env, jobject thiz, jboolean trim) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.trim = trim ? 1 : 0;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_setWordSpacing(JNIEnv *env, jobject thiz, jfloat spacing) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        handle->kctx.word_spacing = spacing;
    }
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_load(JNIEnv *env, jobject thiz, jobject bitmap) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL) {
        throw_illegal_argument(env, "create() must be called before load()");
        return;
    }
    if (bitmap == NULL) {
        throw_illegal_argument(env, "bitmap is null");
        return;
    }

    free_page(handle);

    WILLUSBITMAP src;
    if (bitmap_to_bmp(env, bitmap, &src) != 0) {
        throw_illegal_argument(env, "unsupported bitmap format");
        return;
    }

    handle->kctx.src = src;
    handle->kctx.page_width = src.width;
    handle->kctx.page_height = handle->dev_height;
    handle->kctx.bbox.x0 = 0.0f;
    handle->kctx.bbox.y0 = 0.0f;
    handle->kctx.bbox.x1 = (float) src.width;
    handle->kctx.bbox.y1 = (float) src.height;
    handle->kctx.zoom = 1.0;

    k2pdfopt_reflow_bmp(&handle->kctx);

    handle->kctx.src.data = NULL;
    handle->kctx.src.size_allocated = 0;
    handle->has_page = 1;

    LOGI("reflow: %dx%d -> %dx%d, srcDpi=%.1f, rectmaps=%d",
         src.width, src.height,
         handle->kctx.dst.width, handle->kctx.dst.height,
         handle->kctx.source_dpi, handle->kctx.rectmaps.n);
}

JNIEXPORT jint JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_getPageWidth(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    return handle == NULL || !handle->has_page ? 0 : handle->kctx.dst.width;
}

JNIEXPORT jint JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_getPageHeight(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    return handle == NULL || !handle->has_page ? 0 : handle->kctx.dst.height;
}

JNIEXPORT jobject JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_renderPage(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL || !handle->has_page) {
        return NULL;
    }
    return bmp_to_bitmap(env, &handle->kctx.dst, 0, 0, handle->kctx.dst.width,
                         handle->kctx.dst.height, handle->dev_dpi);
}

JNIEXPORT jobject JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_renderTile(JNIEnv *env, jobject thiz, jint x, jint y,
                                                  jint width, jint height) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL || !handle->has_page) {
        return NULL;
    }
    return bmp_to_bitmap(env, &handle->kctx.dst, x, y, width, height, handle->dev_dpi);
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_releasePage(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle != NULL) {
        free_page(handle);
    }
}

JNIEXPORT jobject JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_getRectMaps(JNIEnv *env, jobject thiz) {
    jclass map_cls = env->FindClass("java/util/HashMap");
    jmethodID map_init = env->GetMethodID(map_cls, "<init>", "()V");
    jobject map = env->NewObject(map_cls, map_init);
    jmethodID map_put = env->GetMethodID(map_cls, "put",
                                         "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jclass rect_cls = env->FindClass("android/graphics/Rect");
    jmethodID rect_init = env->GetMethodID(rect_cls, "<init>", "(IIII)V");

    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL || !handle->has_page) {
        return map;
    }

    double src_dpi = handle->kctx.source_dpi > 1.0
                     ? handle->kctx.source_dpi
                     : handle->dev_dpi * handle->kctx.quality;
    WRECTMAPS *rects = &handle->kctx.rectmaps;
    for (int i = 0; i < rects->n; i++) {
        WRECTMAP *rectmap = &rects->wrectmap[i];
        double kx = rectmap->srcdpiw > 0.0 ? src_dpi / rectmap->srcdpiw : 1.0;
        double ky = rectmap->srcdpih > 0.0 ? src_dpi / rectmap->srcdpih : 1.0;
        double sx = rectmap->coords[0].x * kx;
        double sy = rectmap->coords[0].y * ky;
        double sw = rectmap->coords[2].x * kx;
        double sh = rectmap->coords[2].y * ky;

        jobject src = env->NewObject(rect_cls, rect_init, (jint) sx, (jint) sy,
                                     (jint) (sx + sw), (jint) (sy + sh));
        jobject dst = env->NewObject(rect_cls, rect_init,
                                     (jint) rectmap->coords[1].x, (jint) rectmap->coords[1].y,
                                     (jint) (rectmap->coords[1].x + rectmap->coords[2].x),
                                     (jint) (rectmap->coords[1].y + rectmap->coords[2].y));
        env->CallObjectMethod(map, map_put, src, dst);
        env->DeleteLocalRef(src);
        env->DeleteLocalRef(dst);
    }
    return map;
}

JNIEXPORT void JNICALL
Java_com_archko_reader_k2pdfopt_K2PdfOpt_close(JNIEnv *env, jobject thiz) {
    K2PDFOPT_JNI *handle = get_handle(env, thiz);
    if (handle == NULL) {
        return;
    }
    free_page(handle);
    wrectmaps_free(&handle->kctx.rectmaps);
    set_handle(env, thiz, NULL);
    free(handle);
}

}
