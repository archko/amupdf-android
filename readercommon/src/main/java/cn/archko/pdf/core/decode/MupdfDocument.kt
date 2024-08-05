package cn.archko.pdf.core.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Environment
import android.util.Log
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.ParseTextMain
import cn.archko.pdf.core.entity.APage
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
    private var layoutW = 720
    private var layoutH = 1080
    private var layoutEM = 16
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

    /**
     * 渲染页面,传入一个Bitmap对象.使用硬件加速,虽然速度影响不大.
     *
     * @param bm     需要渲染的位图,配置为ARGB8888
     * @param page   当前渲染页面页码
     * @param pageW  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param pageH  页面的宽,由缩放级别计算得到的最后宽,由于这个宽诸页面的裁剪大小,如果不正确,得到的Tile页面是不正确的
     * @param patchX 裁剪的页面的左顶点
     * @param patchY 裁剪的页面的上顶点
     */
    fun drawPage(
        bm: Bitmap?, pageNum: Int,
        pageW: Int, pageH: Int,
        patchX: Int, patchY: Int,
        cookie: Cookie?
    ) {
        gotoPage(pageNum)
        if (displayList == null) displayList = page!!.toDisplayList(false)
        val ctm = Matrix(ZOOM, ZOOM)
        val bbox = RectI(page!!.bounds.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)
        val dev = AndroidDrawDevice(bm, patchX, patchY)
        displayList!!.run(dev, ctm, cookie)
        dev.close()
        dev.destroy()
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
    fun needsPassword(): Boolean {
        return document!!.needsPassword()
    }

    fun authenticatePassword(password: String?): Boolean {
        return document!!.authenticatePassword(password)
    }

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
        return if (document == null || pageIndex >= pageCount) null else document!!.loadPage(
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

    companion object {
        private const val TAG = "Mupdf"
        public var ZOOM = 160f / 72

        //File selectedFile = new File(filename);
        //String documentPath = selectedFile.getAbsolutePath();
        //String acceleratorPath = getAcceleratorPath(documentPath);
        //if (acceleratorValid(selectedFile, new File(acceleratorPath))) {
        //	doc = Document.openDocument(documentPath, acceleratorPath);
        //} else {
        //	doc = Document.openDocument(documentPath);
        //}
        //doc.saveAccelerator(acceleratorPath);
        fun getAcceleratorPath(documentPath: String): String {
            var acceleratorPath = documentPath.substring(1)
            acceleratorPath = acceleratorPath.replace(File.separatorChar, '%')
            acceleratorPath = acceleratorPath.replace('\\', '%')
            acceleratorPath = acceleratorPath.replace(':', '%')
            val tmpdir = Environment.getExternalStorageDirectory().path + "/amupdf"
            return StringBuffer(tmpdir).append(File.separatorChar).append(acceleratorPath)
                .append(".accel").toString()
        }

        fun acceleratorValid(documentFile: File, acceleratorFile: File): Boolean {
            val documentModified = documentFile.lastModified()
            val acceleratorModified = acceleratorFile.lastModified()
            return acceleratorModified != 0L && acceleratorModified > documentModified
        }

        //=================================================
        fun getArrByCrop(
            page: Page?,
            ctm: Matrix,
            pageW: Int,
            pageH: Int,
            leftBound: Int,
            topBound: Int
        ): FloatArray {
            var leftBound = leftBound
            var topBound = topBound
            val bWith = 200f
            val ratio = pageW / bWith
            val thumb =
                BitmapPool.getInstance().acquire((pageW / ratio).toInt(), (pageH / ratio).toInt())
            val matrix = Matrix(ctm.a / ratio, ctm.d / ratio)
            render(page, matrix, thumb, 0, leftBound, topBound)
            val rectF = getJavaCropRect(thumb)
            val xscale = thumb.getWidth() / rectF.width()
            leftBound = (rectF.left * ratio * xscale).toInt()
            topBound = (rectF.top * ratio * xscale).toInt()
            val height = (rectF.height() * ratio * xscale).toInt()
            ctm.scale(xscale, xscale)
            if (Logcat.loggable) {
                val tw = thumb.getWidth() * ratio
                val th = thumb.getHeight() * ratio
                val sw = xscale * pageW
                val sh = xscale * pageH
                Logcat.d(
                    TAG, String.format(
                        "decode crop.bitmap tw-th:%s-:%s, crop.w-h:%s-%s, sw-sh:%s-%s,xscale:%s, rect:%s-%s",
                        tw,
                        th,
                        sw,
                        sh,
                        xscale,
                        rectF.width(),
                        rectF.height(),
                        rectF.width() * ratio,
                        rectF.height() * ratio
                    )
                )

                //Logcat.d(TAG, String.format("bitmap:%s-%s,height:%s,thumb:%s-%s, crop rect:%s, xscale:%s,yscale:%s",
                //        pageW, pageH, height, thumb.getWidth(), thumb.getHeight(), rectF, xscale, yscale));
            }
            val arr =
                floatArrayOf(leftBound.toFloat(), topBound.toFloat(), height.toFloat(), xscale)
            BitmapPool.getInstance().release(thumb)
            return arr
        }

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

        /*public static RectF getNativeCropRect(Bitmap bitmap) {
        //long start = SystemClock.uptimeMillis();
        ByteBuffer byteBuffer = PageCropper.create(bitmap.getByteCount()).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        //Log.d("test", String.format("%s,%s,%s,%s", bitmap.getWidth(), bitmap.getHeight(), (SystemClock.uptimeMillis() - start), rectF));

        //view: view:Point(1920, 1080) patchX:71 mss:6.260591 mZoomSize:Point(2063, 3066) zoom:1.0749608
        //test: 2063,3066,261,RectF(85.0, 320.0, 1743.0, 2736.0)
        return PageCropper.getCropBounds(byteBuffer, bitmap.getWidth(), bitmap.getHeight(), new RectF(0f, 0f, bitmap.getWidth(), bitmap.getHeight()));
    }*/
        var useNewCropper = false
        fun getJavaCropRect(bitmap: Bitmap?): RectF {
            return if (useNewCropper) {
                CropUtils.getJavaCropRect(bitmap)
            } else CropUtils.getJavaCropBounds(bitmap)
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

        fun getArrByCrop(
            thumb: Bitmap,
            ratio: Float,
            pageW: Int,
            pageH: Int
        ): FloatArray {
            val start = System.currentTimeMillis()
            val rectF = CropUtils.getJavaCropBounds(
                thumb,
                Rect(0, 0, thumb.getWidth(), thumb.getHeight())
            )

            //scale to original image
            val xscale = 1f * pageW / rectF.width()
            val leftBound = (rectF.left * xscale).toInt()
            val topBound = (rectF.top * xscale).toInt()
            val resultH = (rectF.height() * xscale).toInt()
            val resultW = pageW

            Log.d(
                "TAG", String.format(
                    "crop.bitmap target.w-h:%s-%s, xscale:%s, rect:%s-%s, cos:%s",
                    resultW,
                    resultH,
                    xscale,
                    rectF.width() * ratio,
                    rectF.height() * ratio,
                    System.currentTimeMillis() - start
                )
            )

            return floatArrayOf(leftBound.toFloat(), topBound.toFloat(), resultH.toFloat(), xscale)
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

        fun decode(aPage: APage, decodeTask: DecodeParam) {
            if (!decodeTask.decodeCallback!!.shouldRender(decodeTask.pageNum, decodeTask)) {
                Log.d("TAG", String.format("decode.cancel:%s", aPage.index))
                return
            }

            if (decodeTask.document?.isDestroy == true) {
                Log.d("TAG", "decode.abort document.destroy")
                return
            }
            val page: Page = decodeTask.document?.loadPage(decodeTask.pageNum) ?: return

            var leftBound = 0
            var topBound = 0
            val pageW: Int = decodeTask.width
            var pageH: Int = decodeTask.height

            val ctm = Matrix(ZOOM)
            val bbox = RectI(page.bounds?.transform(ctm))
            val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
            val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
            ctm.scale(xscale, yscale)

            if (decodeTask.crop) {
                val arr = getArrByCrop(page, ctm, pageW, pageH)
                leftBound = arr[0].toInt()
                topBound = arr[1].toInt()
                pageH = arr[2].toInt()
                //val cropScale = arr[3]
                //pageSize.setCropHeight(pageH)
                //pageSize.setCropWidth(pageW)
                //val cropRectf = RectF(
                //    leftBound.toFloat(), topBound.toFloat(),
                //    (leftBound + pageW).toFloat(), (topBound + pageH).toFloat()
                //);
                //pageSize.setCropBounds(cropRectf, cropScale)
                if (!decodeTask.decodeCallback!!.shouldRender(decodeTask.pageNum, decodeTask)) {
                    Log.d("TAG", String.format("decode.cancel after crop:%s", aPage.index))
                    return
                }
            }

            var bitmap = BitmapCache.getInstance().getBitmap(decodeTask.key)
            if (null != bitmap) {
                upload(decodeTask, bitmap)
                return
            }

            bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
            if (!decodeTask.decodeCallback!!.shouldRender(decodeTask.pageNum, decodeTask)) {
                Log.d(
                    TAG, String.format("decode bitmap: cancel2:%s", decodeTask)
                )
                return
            }
            if (decodeTask.document?.isDestroy == true) {
                Log.d("TAG", "decode.abort document.destroy2")
                return
            }

            render(page, ctm, bitmap, 0, leftBound, topBound)
            page.destroy()
            Log.d(
                "TAG",
                String.format(
                    "decode.finish task:%s.view:%s, bmp.w-h:%s-%s",
                    decodeTask.pageNum, aPage.index,
                    bitmap.width, bitmap.height
                )
            )

            BitmapCache.getInstance().addBitmap(decodeTask.key, bitmap)

            upload(decodeTask, bitmap)
        }

        fun upload(
            decodeTask: DecodeParam,
            bitmap: Bitmap?
        ) {
            AppExecutors.instance.mainThread().execute {
                if (!decodeTask.decodeCallback!!.shouldRender(decodeTask.pageNum, decodeTask)) {
                    return@execute
                }

                decodeTask.decodeCallback?.decodeComplete(bitmap, decodeTask)
            }
        }
    }
}