package cn.archko.pdf.common

import android.graphics.Bitmap
import android.graphics.RectF
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI

/**
 * 单页的pdf解码
 */
object PdfImageDecoder {
    fun decode(decodeParam: ImageWorker.DecodeParam): Bitmap? {
        try {
            //long start = SystemClock.uptimeMillis();
            val page: Page = decodeParam.document.loadPage(decodeParam.pageSize.index)
            var leftBound = 0
            var topBound = 0
            val pageSize: APage = decodeParam.pageSize
            var pageW = pageSize.zoomPoint.x
            var pageH = pageSize.zoomPoint.y
            val ctm = Matrix(MupdfDocument.ZOOM)
            val bbox = RectI(page.bounds.transform(ctm))
            val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
            val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
            ctm.scale(xscale, yscale)
            if (pageSize.getTargetWidth() > 0) {
                pageW = pageSize.getTargetWidth()
            }
            if (decodeParam.crop) {
                val arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
                leftBound = arr[0].toInt()
                topBound = arr[1].toInt()
                pageH = arr[2].toInt()
                val cropScale = arr[3]
                pageSize.setCropHeight(pageH)
                pageSize.setCropWidth(pageW)
                val cropRectf = RectF(
                    leftBound.toFloat(),
                    topBound.toFloat(),
                    (leftBound + pageW).toFloat(),
                    (topBound + pageH).toFloat()
                );
                pageSize.setCropBounds(cropRectf, cropScale);
            }
            if (Logcat.loggable) {
                d(
                    String.format(
                        "decode bitmap: %s-%s,page:%s-%s,xOrigin:%s, bound(left-top):%s-%s, page:%s",
                        pageW, pageH, pageSize.zoomPoint.x, pageSize.zoomPoint.y,
                        decodeParam.xOrigin, leftBound, topBound, pageSize
                    )
                )
            }
            val bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
            //Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
            MupdfDocument.render(page, ctm, bitmap, decodeParam.xOrigin, leftBound, topBound)
            page.destroy()
            //Logcat.d(TAG, "decode:" + (SystemClock.uptimeMillis() - start));
            //BitmapCache.getInstance().addBitmap(decodeParam.key, bitmap)
            return bitmap
        } catch (e: Exception) {
            if (Logcat.loggable) {
                d(
                    String.format(
                        "decode bitmap error:countPages-page:%s-%s",
                        decodeParam.document.countPages(),
                        decodeParam
                    )
                )
            }
            e.printStackTrace()
        }
        return null
    }
}
