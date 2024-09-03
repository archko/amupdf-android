package cn.archko.pdf.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.CropUtils
import cn.archko.pdf.core.utils.FileUtils
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.github.axet.k2pdfopt.K2PdfOpt
import java.io.File

/**
 * @author: archko 2024/8/30 :08:39
 */
object ReflowHelper {

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
        dir: String,
        list: MutableList<String>,
    ): Int {
        val start = System.currentTimeMillis()
        //最终会形成,以viewWidth为基准的竖长图片,dpi=272/2时纸的大小是:9.4 x 10.2
        //dpi越大,时间越久,
        //count:2, dpi:272, view.w-h:720-1555, bmp.w-h:2550-2781, cos:640
        opt.create(viewWidth, viewHeight, densityDpi)
        opt.load(bitmap)
        /*BitmapUtils.saveBitmapToFile(
            bitmap,
            File("$dir/0.png")
        )*/

        val count = opt.count

        for (i in 0 until count) {
            val bmp = opt.renderPage(i)
            val cropBounds = CropUtils.getJavaCropBounds(
                bmp,
                Rect(0, 0, bmp.getWidth(), bmp.getHeight())
            )
            val nBitmap = Bitmap.createBitmap(
                bmp,
                cropBounds.left.toInt(),
                cropBounds.top.toInt(),
                cropBounds.width().toInt(),
                cropBounds.height().toInt()
            )
            Log.d(
                "TAG", String.format(
                    "index:%s, dpi:%s, view.w-h:%s-%s, bitmap.w-h:%s-%s, cos:%s, nBitmap.w-h:%s-%s, %s",
                    i,
                    densityDpi,
                    viewWidth,
                    viewHeight,
                    bitmap.width,
                    bitmap.height,
                    (System.currentTimeMillis() - start),
                    nBitmap.width,
                    nBitmap.height,
                    cropBounds
                )
            )
            val path = "$dir/${System.currentTimeMillis()}.jpg"
            BitmapUtils.saveBitmapToFile(
                nBitmap,
                File(path)
            )
            list.add(path)
            BitmapPool.getInstance().release(bmp)
            BitmapPool.getInstance().release(nBitmap)
        }
        BitmapPool.getInstance().release(bitmap)
        return count
    }

    /**
     * dpi虽然影响清晰度,但它是在图片大小固定的情况下处理缩放,所以决定性因素还是原图大小
     * 字体大小对实际的效果影响不大,目前分辨率再大也无法做到kindle的效果,不知道什么原因
     */
    fun k2pdf2bitmap(
        opt: K2PdfOpt,
        fontSize: Float,
        bitmap: Bitmap,
        viewWidth: Int,
        viewHeight: Int,
        densityDpi: Int,
    ): MutableList<Bitmap> {
        val list = mutableListOf<Bitmap>()
        var start = System.currentTimeMillis()
        opt.create(viewWidth, viewHeight, densityDpi)
        opt.fontSize = fontSize //调整字体大小
        opt.load(bitmap)

        val count = opt.count

        for (i in 0 until count) {
            start = System.currentTimeMillis()
            val bmp = opt.renderPage(i)
            Log.d(
                "reflow", String.format(
                    "index:%s, dpi:%s, font:%s, view.w-h:%s-%s, bmp.w-h:%s-%s cos:%s",
                    i, densityDpi, fontSize, viewWidth, viewHeight,
                    bmp.width, bmp.height,
                    (System.currentTimeMillis() - start)
                )
            )
            val end = System.currentTimeMillis() - start
            start = System.currentTimeMillis()
            //只需要切边上下就够了.
            val cropBounds = CropUtils.getJavaCropTopBottomBounds(
                bmp,
                Rect(0, 0, bmp.getWidth(), bmp.getHeight())
            )
            val nBitmap = Bitmap.createBitmap(
                bmp,
                cropBounds.left.toInt(),
                cropBounds.top.toInt(),
                cropBounds.width().toInt(),
                cropBounds.height().toInt()
            )
            val cropEnd = System.currentTimeMillis() - start
            Log.d(
                "reflow",
                String.format(
                    "index:%s, dpi:%s, font:%s, view.w-h:%s-%s, bitmap.w-h:%s-%s, cos:%s, cos2:%s, nBitmap.w-h:%s-%s, %s",
                    i,
                    densityDpi,
                    opt.fontSize,
                    viewWidth,
                    viewHeight,
                    bitmap.width,
                    bitmap.height,
                    end, cropEnd,
                    nBitmap.width,
                    nBitmap.height,
                    cropBounds
                )
            )
            bmp.recycle()
            list.add(nBitmap)
        }
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
            val bitmap = loadBitmapByPage(mupdfDocument, pageWidth, index)
            if (null != bitmap) {
                val result = k2pdf(
                    opt,
                    bitmap,
                    screenWidth,
                    screenHeight,
                    densityDpi,
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