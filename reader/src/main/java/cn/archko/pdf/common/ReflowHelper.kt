package cn.archko.pdf.common

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.FileUtils
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.archko.reader.k2pdfopt.K2PdfOpt
import java.io.File
import kotlin.math.sqrt

/**
 * @author: archko 2024/8/30 :08:39
 */
object ReflowHelper {

    const val MAX_REFLOW_PIXELS = 8_000_000f
    const val MIN_RENDER_DPI = 120f
    const val MAX_RENDER_DPI = 400f

    @JvmStatic
    fun renderDpiForPage(pageWidthPts: Float, pageHeightPts: Float): Float {
        if (pageWidthPts <= 0f || pageHeightPts <= 0f) {
            return MAX_RENDER_DPI
        }
        val areaInches = pageWidthPts / 72f * (pageHeightPts / 72f)
        if (areaInches <= 0f) {
            return MAX_RENDER_DPI
        }
        val dpi = sqrt(MAX_REFLOW_PIXELS / areaInches)
        return dpi.coerceIn(MIN_RENDER_DPI, MAX_RENDER_DPI)
    }

    @JvmStatic
    fun sourceDpiForBitmap(bitmapWidth: Int, pageWidthPts: Float): Float {
        if (bitmapWidth <= 0 || pageWidthPts <= 0f) {
            return 0f
        }
        return 72f * bitmapWidth / pageWidthPts
    }

    @JvmStatic
    fun getPageWidthPts(mupdfDocument: MupdfDocument?, pageNo: Int): Float {
        val page = mupdfDocument?.loadPage(pageNo) ?: return 0f
        return try {
            page.bounds.x1 - page.bounds.x0
        } finally {
            page.destroy()
        }
    }

    fun loadBitmapByPage(mupdfDocument: MupdfDocument?, viewWidth: Int, pageNo: Int): Bitmap? {
        val start = System.currentTimeMillis()
        val page = mupdfDocument?.loadPage(pageNo)
        if (page != null) {
            val pageWidth = page.bounds.x1 - page.bounds.x0
            val pageHeight = page.bounds.y1 - page.bounds.y0

            val scale: Float = viewWidth / pageWidth

            val bWidth = viewWidth
            val bHeight = Math.round(pageHeight * scale)

            val bitmap = BitmapPool.getInstance().acquire(bWidth, bHeight)

            val ctm = Matrix(scale)
            val dev = AndroidDrawDevice(bitmap, 0, 0, 0, 0, bitmap.width, bitmap.height)
            page.run(dev, ctm, null)
            page.destroy()
            dev.close()
            dev.destroy()
            Logcat.d(
                "reflow",
                String.format(
                    "loadBitmapByPage.scale:%s, %s, width-height:%s-%s, bmp.w-h:%s-%s, cos:%s",
                    scale,
                    pageNo,
                    pageWidth,
                    pageHeight,
                    bWidth,
                    bHeight,
                    (System.currentTimeMillis() - start)
                )
            )
            return bitmap
        } else {
            return null
        }
    }

    fun k2pdf(
        opt: K2PdfOpt,
        bitmap: Bitmap,
        viewWidth: Int,
        viewHeight: Int,
        densityDpi: Int,
        sourceDpi: Float,
        dir: String,
        list: MutableList<String>,
    ): Int {
        val start = System.currentTimeMillis()
        val devDpi = if (sourceDpi > 1f) {
            Math.min(densityDpi.toFloat(), sourceDpi).toInt()
        } else {
            densityDpi
        }
        opt.create(viewWidth, viewHeight, devDpi)
        opt.setSourceDpi(sourceDpi)
        opt.load(bitmap)

        val bmp = opt.renderPage()
        if (null == bmp) {
            Log.d("reflow", "k2pdf.renderPage null")
            opt.releasePage()
            BitmapPool.getInstance().release(bitmap)
            return 0
        }
        val path = "$dir/${System.currentTimeMillis()}.jpg"
        BitmapUtils.saveBitmapToFile(
            bmp,
            File(path)
        )
        list.add(path)
        Log.d(
                "reflow", String.format(
                "dpi:%s, srcDpi:%s, view.w-h:%s-%s, page.w-h:%s-%s, cos:%s",
                devDpi,
                sourceDpi,
                viewWidth,
                viewHeight,
                bmp.width,
                bmp.height,
                (System.currentTimeMillis() - start)
            )
        )
        bmp.recycle()
        opt.releasePage()
        BitmapPool.getInstance().release(bitmap)
        return 1
    }

    /**
     * koreader 方式: 一张源页重排成一张竖长图, 再按屏幕高度切块返回.
     * 切块只是显示需要, 不会再重新缩放或分页.
     */
    fun k2pdf2bitmap(
        opt: K2PdfOpt,
        fontSize: Float,
        bitmap: Bitmap,
        viewWidth: Int,
        viewHeight: Int,
        densityDpi: Int,
        sourceDpi: Float,
        wordSpacing: Float,
    ): MutableList<Bitmap> {
        val list = mutableListOf<Bitmap>()
        val start = System.currentTimeMillis()
        val devDpi = if (sourceDpi > 1f) {
            Math.min(densityDpi.toFloat(), sourceDpi).toInt()
        } else {
            densityDpi
        }
        opt.create(viewWidth, viewHeight, devDpi)
        opt.setSourceDpi(sourceDpi)
        opt.setWordSpacing(wordSpacing)
        opt.fontSize = fontSize
        opt.load(bitmap)

        val pageWidth = opt.pageWidth
        val pageHeight = opt.pageHeight
        if (pageWidth <= 0 || pageHeight <= 0) {
            Log.d("reflow", "reflow failed:$pageWidth-$pageHeight")
            return list
        }
        val tileHeight = if (viewHeight > 0) viewHeight else 1024
        var y = 0
        while (y < pageHeight) {
            val height = Math.min(tileHeight, pageHeight - y)
            val bmp = opt.renderTile(0, y, pageWidth, height)
            if (null == bmp) {
                Log.d("reflow", "renderTile null:$y, $pageWidth-$height")
                break
            }
            list.add(bmp)
            y += height
        }
        opt.releasePage()
        Log.d(
            "reflow", String.format(
                "font:%s, dpi:%s, srcDpi:%s, view.w-h:%s-%s, page.w-h:%s-%s, tiles:%s, cos:%s",
                fontSize,
                devDpi,
                sourceDpi,
                viewWidth,
                viewHeight,
                pageWidth,
                pageHeight,
                list.size,
                (System.currentTimeMillis() - start)
            )
        )
        return list
    }

    fun reflow(
        mupdfDocument: MupdfDocument?,
        opt: K2PdfOpt,
        context: Context,
        parent: ViewGroup,
        start: Int,
        end: Int,
        screenWidth: Int,
        screenHeight: Int,
        pageWidth: Int,
        dir: String,
        name: String,
        densityDpi: Int,
    ): Int {
        FileUtils.cleanDir(File(dir))
        val list = mutableListOf<String>()
        for (index in start until end) {
            val pageWidthPts = getPageWidthPts(mupdfDocument, index)
            val bitmap = loadBitmapByPage(mupdfDocument, pageWidth, index)
            if (null != bitmap) {
                val sourceDpi = sourceDpiForBitmap(bitmap.width, pageWidthPts).let {
                    if (it > 1f) it else densityDpi.toFloat()
                }
                val result = k2pdf(
                    opt,
                    bitmap,
                    screenWidth,
                    screenHeight,
                    densityDpi,
                    sourceDpi,
                    dir,
                    list
                )
                Log.d("reflow", String.format("process.page:%s, count:%s", index, result))
            }
        }
        PDFCreaterHelper.createPdfFromFormatedImages(context, parent, "/$dir/$name.pdf", list)
        return list.size
    }
}