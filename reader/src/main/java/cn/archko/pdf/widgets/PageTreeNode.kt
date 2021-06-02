package cn.archko.pdf.widgets

import cn.archko.pdf.common.Logcat.d
import android.graphics.RectF
import android.os.AsyncTask
import cn.archko.pdf.common.BitmapCache
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import cn.archko.pdf.entity.APage
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.mupdf.MupdfDocument
import com.artifex.mupdf.fitz.RectI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PageTreeNode(localPageSliceBounds: RectF, page: APDFPage?, pageType: Int) {
    var pageType = PAGE_TYPE_LEFT_TOP
    private val pageSliceBounds: RectF?
    private val apdfPage: APDFPage?
    private val matrix: Matrix? = Matrix()
    private val cropMatrix = Matrix()
    private val bitmapPaint = Paint()

    //private final Paint strokePaint = strokePaint();
    //private final Paint strokePaint2 = strokePaint2();
    private var targetRect: Rect? = null
    private var cropTargetRect: Rect? = null

    //private var bitmapAsyncTask: AsyncTask<String, String, Bitmap>? = null
    private var isRecycle = false

    //private var boundsAsyncTask: AsyncTask<String, String, RectF>? = null
    fun updateVisibility() {
        if (isVisible) {
            if (bitmap != null) {
                apdfPage!!.documentView.postInvalidate()
            } else {
                decodePageTreeNode()
            }
        }
    }

    fun invalidateNodeBounds() {
        targetRect = null
    }

    private fun strokePaint(): Paint {
        val strokePaint = Paint()
        strokePaint.color = Color.GREEN
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 4f
        return strokePaint
    }

    private fun strokePaint2(): Paint {
        val strokePaint = Paint()
        strokePaint.color = Color.RED
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2f
        return strokePaint
    }

    fun draw(canvas: Canvas) {
        val bitmap = bitmap
        if (bitmap != null) {
            val tRect = getTargetRect()
            //Logcat.d(String.format("draw:%s-%s,w-h:%s-%s,rect:%s", tRect.width(), tRect.height(), bitmap.getWidth(), bitmap.getHeight(), tRect));
            canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), tRect, bitmapPaint)
            //canvas.drawRect(tRect, strokePaint);
            //canvas.drawRect(getCropTargetRect(), strokePaint2);
        }
    }

    private val isVisible: Boolean
        private get() = true

    /*|| (bitmap.getWidth() == -1 && bitmap.getHeight() == -1)*/
    var bitmap: Bitmap?
        get() {
            var bitmap = BitmapCache.getInstance().getBitmap(key)
            if (bitmap != null && bitmap.isRecycled) {
                bitmap = null
            }
            return bitmap
        }
        private set(bitmap) {
            if (isRecycle || key == null || bitmap == null /*|| (bitmap.getWidth() == -1 && bitmap.getHeight() == -1)*/) {
                return
            }
            BitmapCache.getInstance().addBitmap(key, bitmap)
            apdfPage!!.documentView.postInvalidate()
        }

    private fun decodePageTreeNode() {
        decode(0, apdfPage!!.aPage)
    }

    private fun evaluatePageSliceBounds(localPageSliceBounds: RectF, parent: PageTreeNode?): RectF {
        if (parent == null) {
            return localPageSliceBounds
        }
        val matrix = Matrix()
        matrix.postScale(parent.pageSliceBounds!!.width(), parent.pageSliceBounds.height())
        matrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top)
        val sliceBounds = RectF()
        matrix.mapRect(sliceBounds, localPageSliceBounds)
        return sliceBounds
    }

    private val key: String?
        private get() {
            if (targetRect == null) {
                getTargetRect()
            }
            return String.format(
                "key:%s-%s,%s-%s",
                apdfPage!!.aPage.index,
                targetRect!!.left,
                targetRect!!.top,
                targetRect!!.right,
                targetRect!!.bottom
            )
        }

    private fun getTargetRect(): Rect {
        if (targetRect == null) {
            matrix!!.reset()
            matrix.postScale(apdfPage!!.bounds.width(), apdfPage.bounds.height())
            matrix.postTranslate(apdfPage.bounds.left, apdfPage.bounds.top)
            val targetRectF = RectF()
            matrix.mapRect(targetRectF, pageSliceBounds)
            targetRect = Rect(
                targetRectF.left.toInt(),
                targetRectF.top.toInt(),
                targetRectF.right.toInt(),
                targetRectF.bottom.toInt()
            )
        }
        return targetRect!!
    }

    private fun getCropTargetRect(): Rect {
        if (cropTargetRect == null) {
            cropMatrix.reset()
            val cropBounds = apdfPage!!.getCropBounds()
            cropMatrix.postScale(cropBounds.width(), cropBounds.height())
            cropMatrix.postTranslate(cropBounds.left, cropBounds.top)
            val rectF = RectF()
            cropMatrix.mapRect(rectF, pageSliceBounds)
            cropTargetRect = Rect(
                rectF.left.toInt(),
                rectF.top.toInt(),
                rectF.right.toInt(),
                rectF.bottom.toInt()
            )
        }
        return cropTargetRect!!
    }

    fun recycle() {
        isRecycle = true
        /*if (null != bitmapAsyncTask) {
            bitmapAsyncTask!!.cancel(true)
            bitmapAsyncTask = null
        }
        if (null != boundsAsyncTask) {
            boundsAsyncTask!!.cancel(true)
            boundsAsyncTask = null
        }*/
        scope.cancel()
        //Bitmap bitmap = BitmapCache.getInstance().removeBitmap(getKey());
        //if (bitmap != null) {
        //    BitmapPool.getInstance().release(bitmap);
        //    bitmap = null;
        //}
        cropTargetRect = null
        targetRect = null
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as PageTreeNode
        if (isRecycle != that.isRecycle) return false
        if (if (pageSliceBounds != null) pageSliceBounds != that.pageSliceBounds else that.pageSliceBounds != null) return false
        if (if (apdfPage != null) apdfPage != that.apdfPage else that.apdfPage != null) return false
        if (if (matrix != null) matrix != that.matrix else that.matrix != null) return false
        return if (targetRect != null) targetRect == that.targetRect else that.targetRect == null
    }

    override fun hashCode(): Int {
        var result = pageSliceBounds?.hashCode() ?: 0
        result = 31 * result + (apdfPage?.hashCode() ?: 0)
        result = 31 * result + (matrix?.hashCode() ?: 0)
        result = 31 * result + if (targetRect != null) targetRect.hashCode() else 0
        result = 31 * result + if (isRecycle) 1 else 0
        return result
    }

    override fun toString(): String {
        return "PageTreeNode{" +
                ", pageSliceBounds=" + pageSliceBounds +
                ", targetRect=" + targetRect +
                ", matrix=" + matrix +
                '}'
    }

    @SuppressLint("StaticFieldLeak")
    fun decode(xOrigin: Int, pageSize: APage) {
        d(
            String.format(
                "task:%s,isRecycle:%s,decoding:%s,crop:%s,size:%s",
                scope, isRecycle, apdfPage!!.isDecodingCrop, apdfPage.cropBounds, pageSize
            )
        )
        if (scope.isActive) {
            scope.cancel()
            scope = CoroutineScope(Job() + Dispatchers.IO)
        }
        if (isRecycle) {
            return
        }
        if (apdfPage.isDecodingCrop) {
            return
        }
        if (apdfPage.crop && apdfPage.cropBounds == null) {
            apdfPage.isDecodingCrop = true
            decodeCropBounds(pageSize)
            return
        }
        scope.launch {
            val bm = decodeBitmap(pageSize, xOrigin)
            withContext(Dispatchers.Main) {
                bitmap = bm
            }
        }
    }

    private fun decodeBitmap(pageSize: APage, xOrigin: Int): Bitmap? {
        var rect = getTargetRect()
        var scale = 1.0f
        if (apdfPage != null) {
            if (apdfPage.crop && apdfPage.cropBounds != null) {
                rect = getCropTargetRect()
                scale = apdfPage.aPage.cropScale
            }
        }
        var leftBound = 0
        var topBound = 0
        val width = rect.width()
        val height = rect.height()
        leftBound = rect.left
        topBound = rect.top
        val bm = BitmapPool.getInstance().acquire(width, height)
        val page = apdfPage?.mupdfDocument?.loadPage(pageSize.index) ?: return null
        val ctm = com.artifex.mupdf.fitz.Matrix(pageSize.scaleZoom * scale)
        MupdfDocument.render(page, ctm, bm, xOrigin, leftBound, topBound)
        if (null != page) {
            page.destroy()
        }
        if (Logcat.loggable && pageType == PAGE_TYPE_LEFT_TOP) {
            d(
                String.format(
                    "decode bitmap:rect:%s-%s, width-height:%s-%s,xOrigin:%s, bound:%s-%s, page:%s",
                    getTargetRect(), rect,
                    width, height, xOrigin, leftBound, topBound, pageSize
                )
            )
        }

        //BitmapUtils.saveBitmapToFile(bitmap, new File(FileUtils.getStoragePath(pageType + "xx.png")));
        return bm
    }

    @SuppressLint("StaticFieldLeak")
    private fun decodeCropBounds(pageSize: APage) {
        if (isRecycle) {
            return
        }
        apdfPage!!.isDecodingCrop = true
        scope.launch {
            val rectF = decodeBounds(pageSize)
            withContext(Dispatchers.Main) {
                rectF?.run {
                    apdfPage.setCropBounds(rectF)
                }
            }
        }
    }

    private fun decodeBounds(pageSize: APage): RectF? {
        var leftBound = 0
        var topBound = 0
        val pageW = pageSize.zoomPoint.x
        var pageH = pageSize.zoomPoint.y
        val page = apdfPage?.mupdfDocument?.loadPage(pageSize.index) ?: return null
        val ctm = com.artifex.mupdf.fitz.Matrix(MupdfDocument.ZOOM)
        val bbox = RectI(page.bounds.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)
        val arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
        leftBound = arr[0].toInt()
        topBound = arr[1].toInt()
        pageH = arr[2].toInt()
        val cropScale = arr[3]
        if (null != page) {
            page.destroy()
        }
        pageSize.setCropWidth(pageW)
        pageSize.setCropHeight(pageH)
        val cropRectf = RectF(
            leftBound.toFloat(),
            topBound.toFloat(),
            (leftBound + pageW).toFloat(),
            (topBound + pageH).toFloat()
        )
        pageSize.setCropBounds(cropRectf, cropScale)
        return cropRectf
    }

    var scope = CoroutineScope(Job() + Dispatchers.IO)

    companion object {
        const val PAGE_TYPE_LEFT_TOP = 0
        const val PAGE_TYPE_RIGHT_TOP = 1
        const val PAGE_TYPE_LEFT_BOTTOM = 2
        const val PAGE_TYPE_RIGHT_BOTTOM = 3
    }

    init {
        pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, null)
        apdfPage = page
        this.pageType = pageType
    }
}