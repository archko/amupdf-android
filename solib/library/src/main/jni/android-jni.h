#ifndef ANDROID_JNI_H
#define ANDROID_JNI_H

#include <android/log.h>
#include <android/bitmap.h>

/* JNI Utils */
#define PACKAGEPATH "com/artifex/solib"

#define LOG_TAG "solib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGT(...) __android_log_print(ANDROID_LOG_INFO,"alert",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEnv *ensureJniAttached();

/* SOLib */
#define JNI_FN(A) Java_com_artifex_solib_ ## A

/* Library method caching */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_preInitLib)(JNIEnv * env, jobject thiz);

/* Library instance creation */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_initLib)(JNIEnv * env, jobject thiz, jstring locale);

/* set the temp path */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_setTempPath)(JNIEnv * env, jobject thiz, jstring tempPath);

/* set the system font path */
JNIEXPORT jlong JNICALL
JNI_FN(SOLib_installFonts)(JNIEnv * env, jobject thiz, jstring fontPath);

/* Library instance destruction */
JNIEXPORT void JNICALL
JNI_FN(SOLib_finLib)(JNIEnv * env, jobject thiz);

/* Document load */
JNIEXPORT jobject JNICALL
JNI_FN(SOLib_openDocumentInternal)(JNIEnv * env, jobject thiz, jstring path, jobject listener);

/* getDocTypeFromFileExtension */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_getDocTypeFromFileExtension)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypeDoc */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypeDoc)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypeExcel */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypeExcel)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypePowerPoint */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isDocTypePowerPoint)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypeOther */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypeOther)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypePdf */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypePdf)(JNIEnv * env, jobject thiz, jstring path);

/* isDocTypeImage */
JNIEXPORT jint JNICALL
JNI_FN(SOLib_isDocTypeImage)(JNIEnv * env, jobject thiz, jstring path);

/* Redirect stderr to the Android log */
JNIEXPORT void JNICALL
JNI_FN (SOLib_logStderr)(JNIEnv* env, jobject thiz);

/* Redirect stdout to the Android log */
JNIEXPORT void JNICALL
JNI_FN (SOLib_logStdout)(JNIEnv* env, jobject thiz);

/* stop logging output */
JNIEXPORT void JNICALL
JNI_FN (SOLib_stopLoggingOutputInternal)(JNIEnv* env, jobject thiz);

/* get formula categories */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getFormulaeCategories)(JNIEnv * env, jobject thiz);

/* get formulas */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getFormulae)(JNIEnv * env, jobject thiz, jstring category);

/* get version information */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SOLib_getVersionInfo)(JNIEnv * env, jobject thiz);

/* isTrackChangesEnabled */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isTrackChangesEnabled)(JNIEnv * env, jobject thiz);

/* setTrackChangesEnabled */
JNIEXPORT void JNICALL
JNI_FN(SOLib_setTrackChangesEnabled)(JNIEnv * env, jobject thiz, jboolean enabled);

/* isAnimationEnabled */
JNIEXPORT jboolean JNICALL
JNI_FN(SOLib_isAnimationEnabled)(JNIEnv * env, jobject thiz);

/* setAnimationEnabled */
JNIEXPORT void JNICALL
JNI_FN(SOLib_setAnimationEnabled)(JNIEnv * env, jobject thiz, jboolean enabled);


/* SODoc */

JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getSelectionAsBitmap)(JNIEnv *env, jobject thiz);

/* getHasBeenModified */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getHasBeenModified)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveTextStyleApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveTextStyleApplied)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveTextAltered */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveTextAltered)(JNIEnv * env, jobject thiz);

/* getSelectionCanBeCopied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeCopied)(JNIEnv * env, jobject thiz);

/* getSelectionCanBePasteTarget */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBePasteTarget)(JNIEnv * env, jobject thiz);

/* getSelectionCanBeAbsolutelyPositioned */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeAbsolutelyPositioned)(JNIEnv * env, jobject thiz);

/* getSelectionCanBeResized */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeResized)(JNIEnv * env, jobject thiz);
/* getSelectionIsTablePart */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsTablePart)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveForegroundColourApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveForegroundColourApplied)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveBackgroundColourApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveBackgroundColourApplied)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveHorizontalAlignmentApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveHorizontalAlignmentApplied)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveVerticalAlignmentApplied */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveVerticalAlignmentApplied)(JNIEnv * env, jobject thiz);

/* getSelectionCanHavePictureInserted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHavePictureInserted)(JNIEnv * env, jobject thiz);

/* getSelectionCanHaveShapeInserted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanHaveShapeInserted)(JNIEnv * env, jobject thiz);

/* getSelectionCanBeRotated */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeRotated)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlterableTextSelection */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlterableTextSelection)(JNIEnv * env, jobject thiz);

/* getSelectionPermitsInlineTextEntry */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionPermitsInlineTextEntry)(JNIEnv * env, jobject thiz);

/* getSelectionHasAssociatedPopup */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionHasAssociatedPopup)(JNIEnv * env, jobject thiz);

/* getSelectionCanCreateAnnotation */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanCreateAnnotation)(JNIEnv * env, jobject thiz);

/* getSelectionCanBeDeleted */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionCanBeDeleted)(JNIEnv * env, jobject thiz);

/* getAnnotationCanBePlacedAtArbitraryPosition */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getAnnotationCanBePlacedAtArbitraryPosition)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlterableAnnotation */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlterableAnnotation)(JNIEnv * env, jobject thiz);

/* getSelectionFontName */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionFontName)(JNIEnv * env, jobject thiz);

/* setSelectionFontName */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontName)(JNIEnv * env, jobject thiz, jstring name);

/* setSelectionFontColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontColor)(JNIEnv * env, jobject thiz, jstring color);

/* setSelectionBackgroundColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionBackgroundColor)(JNIEnv * env, jobject thiz, jstring color);

/* setSelectionBackgroundTransparent */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionBackgroundTransparent)(JNIEnv * env, jobject thiz);

/* getSelectionFontColor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionFontColor)(JNIEnv * env, jobject thiz);

/* getSelectionIsBold */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsBold)(JNIEnv * env, jobject thiz);

/* setSelectionIsBold */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsBold)(JNIEnv * env, jobject thiz, jboolean bold);

/* getSelectionIsLinethrough */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsLinethrough)(JNIEnv * env, jobject thiz);

/* setSelectionIsLinethrough */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsLinethrough)(JNIEnv * env, jobject thiz, jboolean bold);

/* getSelectionIsItalic */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsItalic)(JNIEnv * env, jobject thiz);

/* setSelectionIsItalic */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsItalic)(JNIEnv * env, jobject thiz, jboolean italic);

/* getSelectionIsUnderlined */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsUnderlined)(JNIEnv * env, jobject thiz);

/* setSelectionIsUnderlined */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionIsUnderlined)(JNIEnv * env, jobject thiz, jboolean underline);

/* getSelectionFontSize */
JNIEXPORT jdouble JNICALL
JNI_FN(SODoc_getSelectionFontSize)(JNIEnv * env, jobject thiz);

/* setSelectionFontSize */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFontSize)(JNIEnv * env, jobject thiz, jdouble fontsize);

/* setSelectionText */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionText)(JNIEnv * env, jobject thiz, jstring text);

/* getSelectionNaturalDimensions */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionNaturalDimensionsInternal)(JNIEnv * env, jobject thiz);

/* deleteChar */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteChar)(JNIEnv * env, jobject thiz);

/* adjustSelection */
JNIEXPORT void JNICALL
JNI_FN(SODoc_adjustSelection)(JNIEnv * env, jobject thiz, jint startOffset, jint endOffset, jint updateHighlight);

/* getSelectionContext */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getSelectionContext)(JNIEnv * env, jobject thiz);

/* getFontList */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getFontList)(JNIEnv * env, jobject thiz);

/* getBgColorList */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SODoc_getBgColorList)(JNIEnv * env, jobject thiz);

/* getNumPagesInternal */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getNumPagesInternal)(JNIEnv *env, jobject thiz);

/* getPage */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_getPage)(JNIEnv * env, jobject thiz, int pageNumber, jobject listener);

/* saveTo */
JNIEXPORT int JNICALL
JNI_FN(SODoc_saveTo)(JNIEnv * env, jobject thiz, jstring path, jobject listener);

/* saveToPDF */
JNIEXPORT int JNICALL
JNI_FN(SODoc_saveToPDF)(JNIEnv * env, jobject thiz, jstring path, jobject listener);

/* abortLoad */
JNIEXPORT void JNICALL
JNI_FN(SODoc_abortLoad)(JNIEnv *env, jobject thiz);

/* getDocType */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getDocType)(JNIEnv *env, jobject thiz);

/* clearSelection */
JNIEXPORT void JNICALL
JNI_FN(SODoc_clearSelection)(JNIEnv * env, jobject thiz);

/* selectionDelete */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionDelete)(JNIEnv * env, jobject thiz);

/* selectionCutToClip */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionCutToClip)(JNIEnv * env, jobject thiz);

/* selectionCopyToClip */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionCopyToClip)(JNIEnv * env, jobject thiz);

/* clipboardHasData */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_clipboardHasData)(JNIEnv * env, jobject thiz);

/* selectionPaste */
JNIEXPORT void JNICALL
JNI_FN(SODoc_selectionPaste)(JNIEnv * env, jobject thiz, int pageNumber);

/* getClipboardAsText */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getClipboardAsText)(JNIEnv * env, jobject thiz);

/* setClipboardFromText */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setClipboardFromText)(JNIEnv * env, jobject thiz, jstring text);

/* addBlankPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addBlankPage)(JNIEnv * env, jobject thiz, int pageNumber);

/* deletePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deletePage)(JNIEnv * env, jobject thiz, int pageNumber);

/* addRowsAbove */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addRowsAbove)(JNIEnv * env, jobject thiz);

/* addRowsBelow */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addRowsBelow)(JNIEnv * env, jobject thiz);

/* addColumnsLeft */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addColumnsLeft)(JNIEnv * env, jobject thiz);

/* addColumnsRight */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addColumnsRight)(JNIEnv * env, jobject thiz);

/* deleteRows */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteRows)(JNIEnv * env, jobject thiz);

/* deleteColumns */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteColumns)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlignLeft */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignLeft)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlignCenter */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignCenter)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlignRight */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignRight)(JNIEnv * env, jobject thiz);

/* getSelectionIsAlignJustify */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionIsAlignJustify)(JNIEnv * env, jobject thiz);

/* getSelectionAsText */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAsText)(JNIEnv * env, jobject thiz);

/* table range*/
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_selectionTableRange)(JNIEnv * env, jobject thiz);

/* moveTableSelectionUp */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionUp)(JNIEnv * env, jobject thiz);

/* moveTableSelectionDown */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionDown)(JNIEnv * env, jobject thiz);

/* moveTableSelectionLeft */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionLeft)(JNIEnv * env, jobject thiz);

/* moveTableSelectionRight */
JNIEXPORT void JNICALL
JNI_FN(SODoc_moveTableSelectionRight)(JNIEnv * env, jobject thiz);

JNIEXPORT jfloat JNICALL
JNI_FN(SODoc_getSelectedRowHeight)(JNIEnv * env, jobject thiz);

JNIEXPORT jfloat JNICALL
JNI_FN(SODoc_getSelectedColumnWidth)(JNIEnv * env, jobject thiz);

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedRowHeight)(JNIEnv * env, jobject thiz, float value);

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedColumnWidth)(JNIEnv * env, jobject thiz, float value);

JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getTableCellsMerged)(JNIEnv * env, jobject thiz);

JNIEXPORT void JNICALL
JNI_FN(SODoc_setTableCellsMerged)(JNIEnv * env, jobject thiz, jboolean tableCellsMerged);

JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedCellFormat)(JNIEnv * env, jobject thiz);

JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectedCellFormat)(JNIEnv * env, jobject thiz, jstring format);

/* insertImageAtSelection */
JNIEXPORT void JNICALL
JNI_FN(SODoc_insertImageAtSelection)(JNIEnv *env, jobject thiz, jstring jpath);

JNIEXPORT void JNICALL
JNI_FN(SODoc_setFlowModeInternal)(JNIEnv * env, jobject thiz, int mode, float width);

/* insertImageCenterPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_insertImageCenterPage)(JNIEnv *env, jobject thiz, int pageNum, jobject jpath);

/* nativeInsertAutoshapeCenterPage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_nativeInsertAutoshapeCenterPage)(JNIEnv *env, jobject thiz, int pageNum, jobject jshape, jobject jproperties, jboolean centerPage, float x, float y);

/* setSelectionArrangeForwards */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeForwards)(JNIEnv * env, jobject thiz);

/* setSelectionArrangeFront */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeFront)(JNIEnv * env, jobject thiz);

/* setSelectionArrangeBackwards */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeBackwards)(JNIEnv * env, jobject thiz);

/* setSelectionArrangeBack */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionArrangeBack)(JNIEnv * env, jobject thiz);

/* setSelectionFillColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionFillColor)(JNIEnv * env, jobject thiz, jstring color);

/* setSelectionLineColor */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineColor)(JNIEnv * env, jobject thiz, jstring color);

/* providePassword */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_providePassword)(JNIEnv * env, jobject thiz, jstring jpassword);

/* docSupportsReview */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_docSupportsReview)(JNIEnv * env, jobject thiz);

/* setAuthor */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_setAuthor)(JNIEnv * env, jobject thiz, jstring jauthor);

/* getAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getAuthor)(JNIEnv * env, jobject thiz);

/* getTrackingChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getTrackingChanges)(JNIEnv * env, jobject thiz);

/* setTrackingChanges */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setTrackingChanges)(JNIEnv * env, jobject thiz, jboolean track);

/* getShowingTrackedChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getShowingTrackedChanges)(JNIEnv * env, jobject thiz);

/* setShowingTrackedChanges */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setShowingTrackedChanges)(JNIEnv * env, jobject thiz, jboolean show);

/* selectionIsReviewable */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_selectionIsReviewable)(JNIEnv * env, jobject thiz);

/* selectionIsAutoshapeOrImage */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_selectionIsAutoshapeOrImage)(JNIEnv * env, jobject thiz);

/* nextTrackedChange */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_nextTrackedChange)(JNIEnv * env, jobject thiz);

/* previousTrackedChange */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_previousTrackedChange)(JNIEnv * env, jobject thiz);

/* acceptTrackedChange */
JNIEXPORT void JNICALL
JNI_FN(SODoc_acceptTrackedChange)(JNIEnv * env, jobject thiz);

/* rejectTrackedChange */
JNIEXPORT void JNICALL
JNI_FN(SODoc_rejectTrackedChange)(JNIEnv * env, jobject thiz);

/* getSelectedTrackedChangeAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeAuthor)(JNIEnv * env, jobject thiz);

/* getSelectedTrackedChangeDate */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeDate)(JNIEnv * env, jobject thiz);

/* getSelectedTrackedChangeComment */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeComment)(JNIEnv * env, jobject thiz);

/* getSelectedTrackedChangeType */
JNIEXPORT int JNICALL
JNI_FN(SODoc_getSelectedTrackedChangeType)(JNIEnv * env, jobject thiz);

/* processKeyCommand */
JNIEXPORT void JNICALL
JNI_FN(SODoc_processKeyCommand)(JNIEnv * env, jobject thiz, int inCommand);

/* movePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_movePage)(JNIEnv * env, jobject thiz, int pageNumber, int newNumber);

/* duplicatePage */
JNIEXPORT void JNICALL
JNI_FN(SODoc_duplicatePage)(JNIEnv * env, jobject thiz, int pageNumber);

/* enumerateToc */
JNIEXPORT jlong JNICALL
JNI_FN(SODoc_enumerateToc)(JNIEnv * env, jobject thiz, jobject listener);

/* interpretLinkUrl */
JNIEXPORT jobject JNICALL
JNI_FN(SODoc_interpretLinkUrl)(JNIEnv * env, jobject thiz, jstring url);


/* SOPage */

JNIEXPORT void JNICALL
JNI_FN(SOPage_setSelectionLimitsBox)(JNIEnv *env, jobject thiz, jfloat x0, jfloat y0, jfloat x1, jfloat y1);

/* discard */
JNIEXPORT void JNICALL
JNI_FN(SOPage_discard)(JNIEnv * env, jobject thiz);

/* zoomToFitRect */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_zoomToFitRect)(JNIEnv * env, jobject thiz, int w, int h);

/* sizeAtZoom */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_sizeAtZoom)(JNIEnv * env, jobject thiz, double zoom);

/* renderAtZoom */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_nativeRenderAtZoom)(JNIEnv * env, jobject thiz, int layer, double zoom, double originX, double originY, jobject bitmap, int bitmapW, jobject alpha, int x, int y, int w, int h, jobject listener);

/* select */
JNIEXPORT void JNICALL
JNI_FN(SOPage_select)(JNIEnv * env, jobject thiz, int mode, double pointX, double pointY);

/* selectionLimits */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_selectionLimits)(JNIEnv * env, jobject thiz);

/* getPageTitle */
JNIEXPORT jstring JNICALL
JNI_FN(SOPage_getPageTitle)(JNIEnv * env, jobject thiz);

/* objectAtPoint */
JNIEXPORT jobject JNICALL
JNI_FN(SOPage_objectAtPoint)(JNIEnv * env, jobject thiz, float x, float y);

/* getSlideTransition */
JNIEXPORT jstring JNICALL
JNI_FN(SOPage_getSlideTransitionInternal)(JNIEnv * env, jobject thiz);

/* getAnimations */
JNIEXPORT jobjectArray JNICALL
JNI_FN(SOPage_getAnimations)(JNIEnv *env, jobject thiz);

/* SelectionLimits */

/* getStart */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getStart)(JNIEnv * env, jobject thiz);

/* getEnd */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getEnd)(JNIEnv * env, jobject thiz);

/* getHandle */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getHandle)(JNIEnv * env, jobject thiz);

/* getBox */
JNIEXPORT jobject JNICALL
JNI_FN(SOSelectionLimits_getBox)(JNIEnv * env, jobject thiz);

/* getHasSelectionStart */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasSelectionStart)(JNIEnv * env, jobject thiz);

/* getHasSelectionEnd */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasSelectionEnd)(JNIEnv * env, jobject thiz);

/* getIsActive */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsActive)(JNIEnv * env, jobject thiz);

/* getIsCaret */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsCaret)(JNIEnv * env, jobject thiz);

/* getIsExtensible */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsExtensible)(JNIEnv * env, jobject thiz);

/* getHasPendingVisualChanges */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getHasPendingVisualChanges)(JNIEnv * env, jobject thiz);

/* getIsComposing */
JNIEXPORT jboolean JNICALL
JNI_FN(SOSelectionLimits_getIsComposing)(JNIEnv * env, jobject thiz);

/* scaleBy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_scaleBy)(JNIEnv * env, jobject thiz, double scale);

/* offsetBy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_offsetBy)(JNIEnv * env, jobject thiz, double offX, double offY);

/* combineWith */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_combineWith)(JNIEnv * env, jobject thiz, jobject that);

/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionLimits_destroy)(JNIEnv * env, jobject thiz);

/* SOSelectionTableRange */

/* firstColumn */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_firstColumn)(JNIEnv * env, jobject thiz);

/* columnCount */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_columnCount)(JNIEnv * env, jobject thiz);

/* firstRow */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_firstRow)(JNIEnv * env, jobject thiz);

/* rowCount */
JNIEXPORT jint JNICALL
JNI_FN(SOSelectionTableRange_rowCount)(JNIEnv * env, jobject thiz);

/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SOSelectionTableRange_destroy)(JNIEnv * env, jobject thiz);


/* SOBitmap */

/* nativeCopyPixels */
JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeCopyPixels)(JNIEnv *env, jobject thiz, int dstX, int dstY, int dstW, int w, int h, int srcX, int srcY, int srcW, jobject dstBitmap, jobject srcBitmap);

/* invertLuminance */
JNIEXPORT void JNICALL
JNI_FN(SOBitmap_invertLuminance)(JNIEnv *env, jobject thiz);

/* nativeMergePixels */
JNIEXPORT void JNICALL
JNI_FN(SOBitmap_nativeMergePixels)(JNIEnv *env, jobject thiz, jobject dstBitmap, int dx, int dy, int dw, int dh, jobject srcBitmap, jobject srcAlpha, int sx, int sy, int sw, int sh);

/* SORender */

/* abort */
JNIEXPORT void JNICALL
JNI_FN(SORender_abort)(JNIEnv * env, jobject thiz);

/* destroy */
JNIEXPORT void JNICALL
JNI_FN(SORender_destroy)(JNIEnv * env, jobject thiz);

/* setSelectionListStyle */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionListStyle)(JNIEnv * env, jobject thiz, jint style);

/* getSelectionListStyleIsDecimal */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsDecimal)(JNIEnv * env, jobject thiz);

/* getSelectionListStyleIsDisc */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsDisc)(JNIEnv * env, jobject thiz);

/* getSelectionListStyleIsNone */
JNIEXPORT jboolean JNICALL
JNI_FN(SODoc_getSelectionListStyleIsNone)(JNIEnv * env, jobject thiz);

/* setSelectionAlignment */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAlignment)(JNIEnv * env, jobject thiz, jint alignment);

/* setSelectionAlignmentV */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAlignmentV)(JNIEnv * env, jobject thiz, jint alignment);

/* getSelectionAlignment */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getSelectionAlignment)(JNIEnv * env, jobject thiz);

/* getSelectionAlignmentV */
JNIEXPORT jint JNICALL
JNI_FN(SODoc_getSelectionAlignmentV)(JNIEnv * env, jobject thiz);

/* getSelectionRotation */
JNIEXPORT float JNICALL
JNI_FN(SODoc_getSelectionRotation)(JNIEnv * env, jobject thiz);

/* setSelectionRotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionRotation)(JNIEnv * env, jobject thiz, float angle);

/* setSelectionLineWidth */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineWidth)(JNIEnv * env, jobject thiz, float width);

/* setSelectionLineType */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionLineType)(JNIEnv * env, jobject thiz, int type);

/* createInkAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_createInkAnnotation)(JNIEnv * env, jobject thiz, int page, jobjectArray points, float width, int color);

/* addHighlightAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_addHighlightAnnotation)(JNIEnv * env, jobject thiz);

/* deleteHighlightAnnotation */
JNIEXPORT void JNICALL
JNI_FN(SODoc_deleteHighlightAnnotation)(JNIEnv * env, jobject thiz);

/* createTextAnnotationAt */
JNIEXPORT void JNICALL
JNI_FN(SODoc_createTextAnnotationAt)(JNIEnv * env, jobject thiz, jobject point, int page);

/* getSelectionAnnotationAuthor */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationAuthor)(JNIEnv * env, jobject thiz);

/* getSelectionAnnotationDate */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationDate)(JNIEnv * env, jobject thiz);

/* getSelectionAnnotationComment */
JNIEXPORT jstring JNICALL
JNI_FN(SODoc_getSelectionAnnotationComment)(JNIEnv * env, jobject thiz);

/* setSelectionAnnotationComment */
JNIEXPORT void JNICALL
JNI_FN(SODoc_setSelectionAnnotationComment)(JNIEnv * env, jobject thiz, jstring jcomment);


#endif /* ANDROID_JNI_H */
