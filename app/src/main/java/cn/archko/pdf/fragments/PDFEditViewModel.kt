package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.text.TextUtils
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import cn.archko.mupdf.R
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.listeners.MupdfListener
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.CropUtils
import cn.archko.pdf.core.utils.FileUtils
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.github.axet.k2pdfopt.K2PdfOpt
import java.io.File

class PDFEditViewModel : MupdfListener {

    var pdfPath: String? = null
    var mupdfDocument: MupdfDocument? = null
    val aPageList = mutableListOf<APage>()
    var zoom = 1.0f
    var isEdit = false
    val opt = K2PdfOpt()

    fun loadPdfDoc(context: Context, path: String, password: String?): List<APage>? {
        try {
            pdfPath = path
            if (null == mupdfDocument) {
                mupdfDocument = MupdfDocument(context)
            }
            Log.d("TAG", "loadPdfDoc.password:$password")
            mupdfDocument!!.newDocument(path, password)
            mupdfDocument!!.let {
                if (it.getDocument()!!.needsPassword()) {
                    Log.d("TAG", "needsPassword")
                    if (TextUtils.isEmpty(password)) {
                        return null
                    }
                    it.getDocument()!!.authenticatePassword(password)
                }
            }

            val cp = mupdfDocument!!.countPages()
            loadAllPageSize(cp)
            Log.d("TAG", "loadPdfDoc.cp:$cp, size:${aPageList.size}")
            return aPageList
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadAllPageSize(cp: Int): List<APage> {
        val pointF = loadPageSize(0)
        if (pointF != null) {
            for (i in 0 until cp) {
                val page = APage(i, pointF.width, pointF.height, 1.0f)
                aPageList.add(page)
            }
        }
        return aPageList
    }

    private fun loadPageSize(page: Int): APage? {
        val p = mupdfDocument?.loadPage(page) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(page, w, h, 1.0f)
    }

    fun destroy() {
        mupdfDocument?.destroy()
    }

    fun loadPage(page: Int): Page? {
        return mupdfDocument?.loadPage(page)
    }

    fun countPages(): Int {
        return if (null == mupdfDocument) {
            0
        } else {
            mupdfDocument!!.countPages()
        }
    }

    fun loadOutline(): Array<Outline>? {
        return mupdfDocument?.loadOutline()
    }

    fun deletePage(page: Int) {
        if (null != mupdfDocument) {
            isEdit = true
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            pdfDocument.deletePage(page)
            aPageList.removeAt(page)
            Log.d(
                "TAG",
                "deletePage.$page, cp:${pdfDocument.countPages()}, size:${aPageList.size}"
            )
        }
    }

    fun insertPage(page: Int, path: String) {
        if (null != mupdfDocument) {
            isEdit = true
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            Log.d(
                "TAG",
                "insertPage.$page, cp:${pdfDocument.countPages()}, size:${aPageList.size}"
            )
        }
    }

    fun save() {
        if (null != mupdfDocument && isEdit) {
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            pdfDocument.save(pdfPath, PDFCreaterHelper.OPTS)
            Toast.makeText(App.instance, R.string.edit_save_success, Toast.LENGTH_SHORT).show()
            isEdit = false
        } else {
            Toast.makeText(App.instance, R.string.edit_no_modification, Toast.LENGTH_SHORT).show()
        }
    }

    fun extract(position: Int) {
    }

    override fun getPageCount(): Int {
        return return aPageList.size
    }

    override fun getDocument(): MupdfDocument? {
        return mupdfDocument
    }

    override fun getPageList(): List<APage> {
        return aPageList
    }

    fun loadBitmapByPage(viewWidth: Int, pageNo: Int): Bitmap? {
        val page = mupdfDocument?.loadPage(pageNo)
        if (page != null) {
            val pageWidth = page.bounds.x1 - page.bounds.x0
            val pageHeight = page.bounds.y1 - page.bounds.y0

            val scale: Float = viewWidth / pageWidth

            val bWidth = viewWidth
            val bHeight = Math.round(pageHeight * scale)

            val bitmap = BitmapPool.getInstance().acquire(bWidth, bHeight)
            Log.d(
                "TAG",
                "loadBitmapByPage.scale:$scale, width-height:$pageWidth-$pageHeight, bmp.w-h:$bWidth, $bHeight"
            )

            val ctm = Matrix(scale)
            val dev = AndroidDrawDevice(bitmap, 0, 0, 0, 0, bitmap.width, bitmap.height)
            page.run(dev, ctm, null)
            page.destroy()
            dev.close()
            dev.destroy()
            return bitmap
        } else {
            return null
        }
    }

    fun k2pdf(
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
            BitmapPool.getInstance().release(bitmap)
            BitmapPool.getInstance().release(bmp)
            BitmapPool.getInstance().release(nBitmap)
        }
        return count
    }

    fun reflow(
        context: Context,
        parent: ViewGroup,
        start: Int,
        end: Int,
        screenWidth: Int,
        screenHeight: Int,
        pageWidth: Int,
        dir: String,
        name: String
    ): Int {
        FileUtils.cleanDir(File(dir))
        val list = mutableListOf<String>()
        for (index in start until end) {
            val bitmap = loadBitmapByPage(pageWidth, index)
            if (null != bitmap) {
                val result = k2pdf(
                    bitmap,
                    screenWidth,
                    screenHeight,
                    250,
                    dir,
                    list
                )
                Log.d("TAG", String.format("process.page:%s, count:%s", index, result))
            }
        }
        PDFCreaterHelper.createPdfFromFormatedImages(context, parent, "/$dir/$name.pdf", list)
        return list.size
    }
}