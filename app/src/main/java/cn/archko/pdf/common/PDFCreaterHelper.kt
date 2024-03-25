package cn.archko.pdf.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.archko.mupdf.R
import cn.archko.pdf.core.App
import cn.archko.pdf.adapters.AdapterUtils
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.EncodingDetect
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.StreamUtils
import cn.archko.pdf.core.utils.Utils
import com.artifex.mupdf.fitz.Device
import com.artifex.mupdf.fitz.DocumentWriter
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFObject
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.Story
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
object PDFCreaterHelper {

    const val OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts"
    private const val PAPER_WIDTH = 1080f
    private const val PAPER_HEIGHT = 1800f
    private const val PAPER_PADDING = 40f
    private const val PAPER_FONT_SIZE = 17f

    /**
     * 用这个创建,带cj字体的内容,会产生很大体积,而且字体不好看
     */
    fun createPdfFromText(sourcePath: String, destPath: String): Boolean {
        val text = EncodingDetect.readFile(sourcePath)
        val mediabox = Rect(0f, 0f, 500f, 707f) //A2
        val margin = 10f
        var writer = DocumentWriter(destPath, "PDF", "")

        var snark = "<!DOCTYPE html>" +
                "<style>" +
                "#body { font-family: \"Droid Sans\", sans-serif; }" +
                "</style>" +
                "<body>" +
                text +
                "</body></html>"
        val story = Story(snark, "", 12f)

        var more: Boolean

        do {
            val filled = Rect()
            val where = Rect(
                mediabox.x0 + margin,
                mediabox.y0 + margin,
                mediabox.x1 - margin,
                mediabox.y1 - margin
            )
            val dev: Device = writer.beginPage(mediabox)
            more = story.place(where, filled)
            story.draw(dev, Matrix.Identity())
            writer.endPage()
        } while (more)

        writer.close()
        writer.destroy()
        story.destroy()

        return true
    }

    /**
     * 如果是原图的高宽,不经过缩放,pdf的页面高宽设置与图片大小一致,得到的pdf会很大.
     * 图片是否超过指定值,都应该做一次压缩
     */
    fun createPdfFromImages(pdfPath: String?, imagePaths: List<String>): Boolean {
        Log.d("TAG", String.format("imagePaths:%s", imagePaths))
        var mDocument: PDFDocument? = null
        try {
            mDocument = PDFDocument.openDocument(pdfPath) as PDFDocument
        } catch (e: Exception) {
            Log.d("TAG", "could not open:$pdfPath")
        }
        if (mDocument == null) {
            mDocument = PDFDocument()
        }

        val resultPaths = processLargeImage(imagePaths)

        //空白页面必须是-1,否则会崩溃,但插入-1的位置的页面会成为最后一个,所以追加的时候就全部用-1就行了.
        var index = -1
        for (path in resultPaths) {
            val page = addPage(path, mDocument, index++)

            mDocument.insertPage(-1, page)
        }
        mDocument.save(pdfPath, OPTS);
        Log.d("TAG", String.format("save,%s,%s", mDocument.toString(), mDocument.countPages()))
        val cacheDir = FileUtils.getExternalCacheDir(App.instance).path + File.separator + "create"
        val dir = File(cacheDir)
        if (dir.isDirectory) {
            dir.deleteRecursively()
        }
        return mDocument.countPages() > 0
    }

    private fun addPage(
        path: String,
        mDocument: PDFDocument,
        index: Int
    ): PDFObject? {
        val image = Image(path)
        val resources = mDocument.newDictionary()
        val xobj = mDocument.newDictionary()
        val obj = mDocument.addImage(image)
        xobj.put("I", obj)
        resources.put("XObject", xobj)

        val w = image.width
        val h = image.height
        val mediabox = Rect(0f, 0f, w.toFloat(), h.toFloat())
        val contents = "q $w 0 0 $h 0 0 cm /I Do Q\n"
        val page = mDocument.addPage(mediabox, 0, resources, contents)
        Log.d("TAG", String.format("index:%s,page,%s", index, contents))
        return page
    }

    /**
     * 将大图片切割成小图片,以长图片切割,不处理宽图片
     */
    private fun processLargeImage(imagePaths: List<String>): List<String> {
        val options = BitmapFactory.Options()
        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = true
        val maxHeight = 6000

        val result = arrayListOf<String>()
        for (path in imagePaths) {
            try {
                BitmapFactory.decodeFile(path, options)
                if (options.outHeight > maxHeight) {
                    //split image,maxheight=PAPER_HEIGHT
                    splitImages(result, path, options.outWidth, options.outHeight)
                } else {
                    if (AdapterUtils.supportImage(path)) {
                        result.add(path)
                    } else {
                        convertImageToJpeg(result, path)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * 默认不支持bmp,svg,heic,webp这些直接转换,所以先解析为jpg
     */
    private fun convertImageToJpeg(result: java.util.ArrayList<String>, path: String) {
        val bm: Bitmap = BitmapFactory.decodeFile(path)
        val file =
            File(
                FileUtils.getExternalCacheDir(App.instance).path
                        //FileUtils.getStorageDirPath() + "/amupdf"
                        + File.separator + "create" + File.separator + System.currentTimeMillis() + ".jpg"
            )
        BitmapUtils.saveBitmapToFile(bm, file, Bitmap.CompressFormat.JPEG, 100)
        Log.d("TAG", "convertImageToJpeg path:${file.absolutePath}")

        result.add(file.absolutePath)
    }

    private fun splitImages(
        result: ArrayList<String>,
        path: String,
        width: Int,
        height: Int,
    ) {
        var top = 0f
        val right = 0 + width
        var bottom = PAPER_HEIGHT

        val decoder = BitmapRegionDecoder.newInstance(path, true)

        while (bottom < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, bottom.toInt())
            splitImage(path, rect, result, decoder)

            top = bottom
            bottom += PAPER_HEIGHT
        }
        if (top < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, height)
            splitImage(path, rect, result, decoder)
        }
        decoder.recycle()
    }

    private fun splitImage(
        path: String,
        rect: android.graphics.Rect,
        result: ArrayList<String>,
        decoder: BitmapRegionDecoder
    ) {
        val bm: Bitmap = decoder.decodeRegion(rect, null)
        val file =
            File(
                FileUtils.getExternalCacheDir(App.instance).path
                        //FileUtils.getStorageDirPath() + "/amupdf"
                        + File.separator + "create" + File.separator + System.currentTimeMillis() + ".jpg"
            )
        BitmapUtils.saveBitmapToFile(bm, file, Bitmap.CompressFormat.JPEG, 100)
        Log.d("TAG", "new file:height:${rect.bottom - rect.top}, path:${file.absolutePath}")

        result.add(file.absolutePath)
        //val bitmapPath = compressImageFitPage(file.absolutePath, bm.width, bm.height)
        //result.add(bitmapPath)
    }

    var canExtract: Boolean = true

    fun extractToImages(context: Context, screenWidth: Int, dir: String, pdfPath: String): Int {
        return extractToImages(context, screenWidth, dir, pdfPath, 0, -1)
    }

    fun extractToImages(
        context: Context, screenWidth: Int, dir: String, pdfPath: String,
        start: Int,
        end: Int
    ): Int {
        try {
            Log.d(
                "TAG",
                "extractToImages:$screenWidth, start:$start, end:$end dir:$dir, dst:$pdfPath"
            )
            val mupdfDocument = MupdfDocument(context)
            mupdfDocument.newDocument(pdfPath, null)
            val count: Int = mupdfDocument.countPages()
            var startPage = start
            if (startPage < 0) {
                startPage = 0
            } else if (startPage >= count) {
                startPage = 0
            }
            var endPage = end
            if (end > count) {
                endPage = count
            } else if (endPage < 0) {
                endPage = count
            }
            for (i in startPage until endPage) {
                if (!canExtract) {
                    Log.d("TAG", "extractToImages.stop")
                    return i
                }
                val page = mupdfDocument.loadPage(i)
                if (null != page) {
                    val pageWidth = page.bounds.x1 - page.bounds.x0
                    val pageHeight = page.bounds.y1 - page.bounds.y0

                    var exportWidth = screenWidth
                    if (exportWidth == -1) {
                        exportWidth = pageWidth.toInt()
                    }
                    val scale = exportWidth / pageWidth
                    val width = exportWidth
                    val height = pageHeight * scale
                    val bitmap = BitmapPool.getInstance().acquire(width, height.toInt())
                    val ctm = Matrix(scale)
                    MupdfDocument.render(page, ctm, bitmap, 0, 0, 0)
                    page.destroy()
                    BitmapUtils.saveBitmapToFile(bitmap, File("$dir/${i + 1}.png"))
                    BitmapPool.getInstance().release(bitmap)
                }
                Log.d("TAG", "extractToImages:page:${i + 1}.png")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -2
        }
        return 0
    }

    fun extractToHtml(context: Context, path: String, pdfPath: String): Boolean {
        try {
            val mupdfDocument = MupdfDocument(context)
            mupdfDocument.newDocument(pdfPath, null)
            val cp: Int = mupdfDocument.countPages()
            val stringBuilder = StringBuilder()
            for (i in 0 until cp) {
                val page = mupdfDocument.loadPage(i)
                if (null != page) {
                    val content =
                        String(page.textAsHtml2("preserve-whitespace,inhibit-spaces,preserve-images"))
                    stringBuilder.append(content)
                    page.destroy()
                    StreamUtils.appendStringToFile(stringBuilder.toString(), path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    //==================


    /**
     * Page width for our PDF.
     */
    const val PDF_PAGE_WIDTH = 8.3 * 72 * 2

    /**
     * Page height for our PDF.
     */
    const val PDF_PAGE_HEIGHT = 11.7 * 72 * 2

    fun createPdfUseSystemFromTxt(
        context: Context?,
        parent: ViewGroup?,
        sourcePath: String?,
        destPath: String?
    ): Boolean {
        val content: String = EncodingDetect.readFile(sourcePath)
        try {
            return doCreatePdfUseSystemFromTxt(context, parent, content, destPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * @param context
     * @param parent  需要有一个临时的布局
     * @param path    保存的文件名
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    fun doCreatePdfUseSystemFromTxt(
        context: Context?,
        parent: ViewGroup?,
        content: String,
        path: String?
    ): Boolean {
        val pdfDocument = PdfDocument()
        val pageWidth = PDF_PAGE_WIDTH.toInt()
        val pageHeight = PDF_PAGE_HEIGHT.toInt()
        val contentView =
            LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false) as TextView
        contentView.text = content
        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        contentView.measure(measureWidth, measuredHeight)
        contentView.layout(0, 0, pageWidth, pageHeight)

        //contentView.setPadding(Utils.dipToPixel(20), Utils.dipToPixel(20), Utils.dipToPixel(20), Utils.dipToPixel(20));
        //contentView.setTextSize(Utils.dipToPixel(12));
        val lineCount = contentView.lineCount
        var lineHeight = contentView.lineHeight
        if (contentView.lineSpacingMultiplier > 0) {
            lineHeight = (lineHeight * contentView.lineSpacingMultiplier).toInt()
        }
        val layout = contentView.layout
        var start = 0
        var end: Int
        var pageH = 0
        val paddingTopAndBottom: Int = Utils.dipToPixel(context, 40f)
        //循环遍历打印每一行
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < lineCount) {
            end = layout.getLineEnd(i)
            val line = content.substring(start, end) //指定行的内容
            start = end
            sb.append(line)
            pageH += lineHeight
            if (pageH >= pageHeight - paddingTopAndBottom) {
                Log.d(
                    "TextView",
                    String.format(
                        "============page line:%s,lh:%s,ph:%s==========",
                        i,
                        lineHeight,
                        pageHeight
                    )
                )
                createTxtPage(
                    context,
                    parent,
                    pdfDocument,
                    pageWidth,
                    pageHeight,
                    i + 1,
                    sb.toString()
                )
                pageH = 0
                sb.setLength(0)
            }
            i++
        }
        if (sb.length > 0) {
            Log.d("TextView", "last line ===")
            createTxtPage(context, parent, pdfDocument, pageWidth, pageHeight, i, sb.toString())
        }
        return savePdf(path, pdfDocument)
    }

    private fun createTxtPage(
        context: Context?,
        parent: ViewGroup?,
        pdfDocument: PdfDocument,
        pageWidth: Int,
        pageHeight: Int,
        pageNo: Int,
        content: String?
    ) {
        val contentView =
            LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false) as TextView
        contentView.text = content
        val pageInfo: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo)
                .create()
        val page: PdfDocument.Page = pdfDocument.startPage(pageInfo)
        val pageCanvas: Canvas = page.getCanvas()
        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        contentView.measure(measureWidth, measuredHeight)
        contentView.layout(0, 0, pageWidth, pageHeight)
        contentView.draw(pageCanvas)

        // finish the page
        pdfDocument.finishPage(page)
    }

    @Throws(FileNotFoundException::class)
    private fun savePdf(path: String?, document: PdfDocument): Boolean {
        val outputStream = FileOutputStream(path)
        try {
            document.writeTo(outputStream)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }
        return false
    }
}