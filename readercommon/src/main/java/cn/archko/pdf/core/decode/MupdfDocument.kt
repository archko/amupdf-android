package cn.archko.pdf.core.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.ParseTextMain
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.CropUtils
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Link
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File

/**
 * @author archko 2019/12/8 :12:43
 */
class MupdfDocument(private val context: Context) {
    private var resolution = 0
    private var document: Document? = null
    private var outline: Array<Outline>? = null
    private var pageCount = -1
    private var currentPage = 0
    private var page: Page? = null
    private var pageWidth = 0f
    private var pageHeight = 0f
    private var displayList: DisplayList? = null
    private var isDestroy = false

    /* Default to "A Format" pocket book size. */
    private var layoutW = LAYOUTW
    private var layoutH = LAYOUTH
    private var layoutEM = LAYOUTEM
    fun getDocument(): Document? {
        return document
    }

    fun setDocument(document: Document?) {
        this.document = document
        initDocument()
    }

    fun newDocument(pfd: String?, password: String?) {
        document = Document.openDocument(pfd)
        initDocument()
    }

    fun newDocument(data: ByteArray?, password: String?) {
        document = Document.openDocument(data, "magic")
        initDocument()
    }

    private fun initDocument() {
        document!!.layout(layoutW.toFloat(), layoutH.toFloat(), layoutEM.toFloat())
        pageCount = document!!.countPages()
        resolution = 160
        currentPage = -1
    }

    val title: String
        get() = document!!.getMetaData(Document.META_INFO_TITLE)

    fun countPages(): Int {
        return pageCount
    }

    val isReflowable: Boolean
        get() = document!!.isReflowable

    fun layout(oldPage: Int, w: Int, h: Int, em: Int): Int {
        if (w != layoutW || h != layoutH || em != layoutEM) {
            println("LAYOUT: $w,$h")
            layoutW = w
            layoutH = h
            layoutEM = em
            val mark = document!!.makeBookmark(document!!.locationFromPageNumber(oldPage))
            document!!.layout(layoutW.toFloat(), layoutH.toFloat(), layoutEM.toFloat())
            currentPage = -1
            pageCount = document!!.countPages()
            outline = null
            try {
                outline = document!!.loadOutline()
            } catch (ex: Exception) {
                /* ignore error */
            }
            return document!!.pageNumberFromLocation(document!!.findBookmark(mark))
        }
        return oldPage
    }

    fun gotoPage(pageNum: Int) {
        /* TODO: page cache */
        var pageNum = pageNum
        if (pageNum > pageCount - 1) pageNum = pageCount - 1 else if (pageNum < 0) pageNum = 0
        if (pageNum != currentPage) {
            currentPage = pageNum
            if (page != null) {
                page!!.destroy()
            }
            if (displayList != null) displayList!!.destroy()
            displayList = null
            page = document!!.loadPage(pageNum)
            val b = page!!.getBounds()
            pageWidth = b.x1 - b.x0
            pageHeight = b.y1 - b.y0
        }
    }

    fun getPageSize(pageNum: Int): PointF {
        gotoPage(pageNum)
        return PointF(pageWidth, pageHeight)
    }

    fun destroy() {
        isDestroy = true
        if (displayList != null) {
            displayList!!.destroy()
        }
        displayList = null
        if (page != null) {
            page!!.destroy()
        }
        page = null
        if (document != null) {
            document!!.destroy()
        }
        document = null
    }

    fun getPageLinks(pageNum: Int): Array<Link> {
        gotoPage(pageNum)
        return page!!.links
    }

    fun resolveLink(link: Link?): Int {
        return document!!.pageNumberFromLocation(document!!.resolveLink(link))
    }

    fun searchPage(pageNum: Int, text: String?): Array<Array<Quad>> {
        gotoPage(pageNum)
        return page!!.search(text)
    }

    fun hasOutline(): Boolean {
        if (outline == null) {
            try {
                outline = document!!.loadOutline()
            } catch (ex: Exception) {
                /* ignore error */
            }
        }
        return outline != null
    }

    /*private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline list[], String indent) {
        for (Outline node : list) {
            if (node.title != null) {
                int page = document.pageNumberFromLocation(document.resolveLink(node));
                result.add(new OutlineActivity.Item(indent + node.title, page));
            }
            if (node.down != null)
                flattenOutlineNodes(result, node.down, indent + "    ");
        }
    }

    public ArrayList<OutlineActivity.Item> getOutline() {
        ArrayList<OutlineActivity.Item> result = new ArrayList<OutlineActivity.Item>();
        flattenOutlineNodes(result, outline, "");
        return result;
    }*/

    fun renderBitmap(
        bitmap: Bitmap,
        pageNum: Int,
        autoCrop: Boolean,
        tb: RectF,
        bounds: Rect
    ): Bitmap {
        val page = document!!.loadPage(pageNum)
        val zoom = 2f
        val ctm = Matrix(zoom, zoom)
        val bbox = RectI(page.bounds.transform(ctm))
        val xscale = bounds.width().toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = bounds.height().toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)
        val patchX: Int
        val patchY: Int
        patchX = (tb.left * bounds.width()).toInt()
        patchY = (tb.top * bounds.height()).toInt()
        val dev =
            AndroidDrawDevice(bitmap, patchX, patchY, 0, 0, bitmap.getWidth(), bitmap.getHeight())
        page.run(dev, ctm, null as Cookie?)
        dev.close()
        dev.destroy()
        return bitmap
    }

    fun loadPage(pageIndex: Int): Page? {
        return if (document == null || pageIndex >= pageCount || pageIndex < 0) null
        else document!!.loadPage(
            pageIndex
        )
    }

    fun loadOutline(): Array<Outline>? {
        if (outline == null) {
            try {
                outline = document!!.loadOutline()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return outline
    }

    fun pageNumberFromLocation(node: Outline?): Int {
        return document!!.pageNumberFromLocation(document!!.resolveLink(node))
    }

    /**
     * 文本重排解析
     *
     * @param index
     * @return
     */
    fun decodeReflow(index: Int): List<ReflowBean>? {
        val p = loadPage(index) ?: return null
        val result = p.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")
        return if (null != result) {
            ParseTextMain.parseAsHtmlList(result, index)
        } else null
    }

    fun decodeReflowText(index: Int): List<ReflowBean>? {
        val p = loadPage(index) ?: return null
        val result = p.textAsText("preserve-whitespace,inhibit-spaces")
        return if (null != result) {
            ParseTextMain.parseAsTextList(result, index)
        } else null
    }

    companion object {
        private const val TAG = "Mupdf"
        const val ZOOM = 160f / 72

        const val LAYOUTW = 1080
        const val LAYOUTH = 1800
        const val LAYOUTEM = 42

        //=================================================
        fun render(
            page: Page?,
            ctm: Matrix?,
            bitmap: Bitmap?,
            xOrigin: Int,
            leftBound: Int,
            topBound: Int
        ) {
            if (page == null) {
                return
            }
            val dev = AndroidDrawDevice(bitmap, xOrigin + leftBound, topBound)
            page.run(dev, ctm, null)
            dev.close()
            dev.destroy()
        }

        fun crop(page: Page, viewWidth: Int) {
            val pageW = page.bounds.x1 - page.bounds.x0
            var pageH = page.bounds.y1 - page.bounds.y0
            val ctm = Matrix(2f)
            val bbox = RectI(page.bounds.transform(ctm))
            val xscale = pageW / (bbox.x1 - bbox.x0)
            val yscale = pageH / (bbox.y1 - bbox.y0)
            ctm.scale(xscale, yscale)
            /*if (viewWidth > 0) {
                pageW = viewWidth;
            }*/
            var leftBound = 0
            var topBound = 0
            val arr = getArrByCrop(page, ctm, pageW.toInt(), pageH.toInt())
            leftBound = arr[0].toInt()
            topBound = arr[1].toInt()
            pageH = arr[2]
            val cropRectf = RectF(
                leftBound.toFloat(),
                topBound.toFloat(),
                leftBound + pageW,
                topBound + pageH
            )
            val bitmap = BitmapPool.getInstance().acquire(pageW.toInt(), pageH.toInt())
            render(page, ctm, bitmap, 0, leftBound, topBound)
        }

        /**
         * @param pageW current page-view width
         * @param pageH current page-view height
         */
        fun getArrByCrop(
            page: Page?,
            ctm: Matrix,
            pageW: Int,
            pageH: Int,
        ): FloatArray {
            // decode thumb
            val ratio = 6f
            val thumb =
                BitmapPool.getInstance().acquire((pageW / ratio).toInt(), (pageH / ratio).toInt())
            val matrix =
                Matrix(ctm.a / ratio, ctm.d / ratio)
            render(page, matrix, thumb, 0, 0, 0)
            val rectF = CropUtils.getJavaCropBounds(
                thumb,
                Rect(0, 0, thumb.getWidth(), thumb.getHeight())
            )

            //scale to original image
            val xscale = 1f * pageW / rectF.width()
            ctm.scale(xscale / ratio, xscale / ratio)

            val leftBound = (rectF.left * xscale).toInt()
            val topBound = (rectF.top * xscale).toInt()
            val resultH = (rectF.height() * xscale).toInt()
            val resultW = pageW

            Log.d(
                "TAG", String.format(
                    "crop.bitmap target.w-h:%s-%s, xscale:%s, rect:%s-%s",
                    resultW, resultH, xscale, rectF.width() * ratio, rectF.height() * ratio
                )
            )

            //Logcat.d(TAG, String.format("bitmap:%s-%s,height:%s,thumb:%s-%s, crop rect:%s, xscale:%s,yscale:%s",
            //        pageW, pageH, height, thumb.getWidth(), thumb.getHeight(), rectF, xscale, yscale));
            //BitmapPool.getInstance().release(thumb);
            return floatArrayOf(leftBound.toFloat(), topBound.toFloat(), resultH.toFloat(), xscale)
        }

        /**
         * 根据页面240像素去缩放,将缩略图去切边,再缩放到原来图片大小.
         * 返回一个原图片大小的rect,包含切边的left,top大小
         */
        fun getArrByCrop(
            page: Page,
        ): RectF {
            val start = System.currentTimeMillis()
            // decode thumb
            val thumbw = 240f
            val pageW = page.bounds.x1 - page.bounds.x0
            val pageH = page.bounds.y1 - page.bounds.y0
            val ratio = pageW / thumbw
            val thumbh = (pageH / ratio)

            val thumb =
                BitmapPool.getInstance().acquire(thumbw.toInt(), thumbh.toInt())
            val matrix = Matrix(1 / ratio)
            render(page, matrix, thumb, 0, 0, 0)
            val rectF = CropUtils.getJavaCropBounds(
                thumb,
                Rect(0, 0, thumb.getWidth(), thumb.getHeight())
            )

            val leftBound = (rectF.left * ratio)
            val topBound = (rectF.top * ratio)
            val resultW = rectF.width() * ratio
            val resultH = rectF.height() * ratio

            Log.d(
                "TAG", String.format(
                    "decode.crop scale.w-h:%s-%s-%s, %s-%s, %s-%s,rect:%s, cos:%s",
                    resultW, resultH, ratio,
                    pageW, pageH, leftBound, topBound, rectF,
                    (System.currentTimeMillis() - start)
                )
            )

            //saveBitmap(thumb, rectF)
            BitmapPool.getInstance().release(thumb)
            return RectF(leftBound, topBound, leftBound + resultW, topBound + resultH)
        }

        private fun saveBitmap(thumb: Bitmap, rectF: RectF) {
            val paint = Paint()
            paint.setColor(Color.GREEN)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            val canvas = Canvas(thumb)
            canvas.drawRect(rectF, paint)
            BitmapUtils.saveBitmapToFile(
                thumb, File(
                    String.format(
                        "/sdcard/book/%s-%s-%s.jpg",
                        thumb.getWidth(), thumb.getHeight(), rectF
                    )
                )
            )
        }

        fun decodeReflowText(index: Int, document: Document): List<ReflowBean>? {
            val pageCount = document.countPages()
            val p = if (index >= pageCount) null else document.loadPage(
                index
            )
            val result = p?.textAsText("preserve-whitespace,inhibit-spaces")
            val list = if (null != result) {
                ParseTextMain.parseAsTextList(result, index)
            } else null

            p?.destroy()
            return list
        }
    }
}