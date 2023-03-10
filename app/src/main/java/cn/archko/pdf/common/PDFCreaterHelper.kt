package cn.archko.pdf.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Paint
import android.text.TextUtils
import cn.archko.pdf.App
import cn.archko.pdf.AppExecutors.Companion.instance
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.BitmapUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Font
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFObject
import com.artifex.mupdf.fitz.Rect
import java.io.File
import java.io.FileFilter
import java.util.Locale


/**
 * @author: archko 2018/12/21 :1:03 PM
 */
object PDFCreaterHelper {

    private const val OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts"
    private const val PAPER_WIDTH = 1280f
    private const val PAPER_HEIGHT = 2160f
    private const val PAPER_PADDING = 40f
    private const val PAPER_FONT_SIZE = 17f

    var filename = "/book/test.pdf"

    fun createTextPage(text: String) {
        val mDocument = PDFDocument()
        val mediabox = Rect(0f, 0f, PAPER_WIDTH, PAPER_HEIGHT)
        val fontSize = PAPER_FONT_SIZE       //字号
        val leftPadding = PAPER_PADDING      //左侧的距离
        val height = PAPER_HEIGHT           //这个是倒着的,如果是0,则在底部

        // /system/fonts/DroidSans.ttf");//load from system fonts.
        // /system/fonts/Roboto-Regular.ttf");
        // /system/fonts/DroidSansFallback.ttf");
        // /system/fonts/DroidSansChinese.ttf");
        // /system/fonts/NotoSansSC-Regular.otf");
        // /system/fonts/NotoSansTC-Regular.otf");
        // /system/fonts/NotoSansJP-Regular.otf");
        // /system/fonts/NotoSansKR-Regular.otf");
        // /system/fonts/NotoSansCJK-Regular.ttc");
        val resources = mDocument.newDictionary()
        val xobj = mDocument.newDictionary()
        var font = Font("/system/fonts/DroidSans.ttf")
        var song = mDocument.addFont(font)
        xobj.put("Tm", song)
        resources.put("Font", song)

        val str = "Hello, world!\n Hello, world!\n Hello, world!"
        val contents = "BT /Tm $fontSize Tf $leftPadding ${height - 200} TD ($str) Tj ET\n"

        val page = mDocument.addPage(mediabox, 0, null, contents)
        mDocument.insertPage(-1, page)

        val result = splitText(text, fontSize, PAPER_WIDTH - PAPER_PADDING * 2)

        var maxLine = 30
        var lineCount = 0
        val sb = StringBuilder()
        result.forEach {
            val contents = "BT /Tm $fontSize Tf $leftPadding ${height - 200} TD ($it) Tj ET\n"
            sb.append(contents)
            //没有resources,则字体是默认的,resources用于存储字体与图片
            val page = mDocument.addPage(mediabox, 0, resources, sb.toString())

            if (lineCount >= maxLine) {
                mDocument.insertPage(-1, page)
                sb.clear()
            }
            lineCount++
        }
        if (sb.isNotEmpty()) {
            val page = mDocument.addPage(mediabox, 0, resources, sb.toString())
            mDocument.insertPage(-1, page)
        }

        val destPdfPath = FileUtils.getStoragePath(filename)
        mDocument.save(destPdfPath, OPTS);
    }

    private fun splitText(text: String, fontSize: Float, width: Float): List<String> {
        val paint = Paint()
        paint.textSize = fontSize
        val wordW = paint.measureText("我") * Utils.getScale()
        val maxWord = width / wordW

        val pages = arrayListOf<String>()
        val sb = StringBuilder()
        var line = ""
        text.forEach {
            line += it
            if (line.length >= maxWord) {
                //sb.append(line).append("\n")
                pages.add(line)
                d("line:$line")
                line = ""
            }
        }
        if (!TextUtils.isEmpty(line)) {
            pages.add(line)
        }

        return pages
    }

    fun createPdf(pdfPath: String?, imagePaths: List<String>): Boolean {
        d(String.format("imagePaths:%s", imagePaths))
        var mDocument: PDFDocument? = null
        try {
            mDocument = PDFDocument.openDocument(pdfPath) as PDFDocument
        } catch (e: Exception) {
            Logcat.w(Logcat.TAG, "could not open:$pdfPath")
        }
        if (mDocument == null) {
            mDocument = PDFDocument()
        }

        val splitPaths = arrayListOf<File>()
        val resultPaths = processLargeImage(imagePaths, splitPaths)

        //空白页面必须是-1,否则会崩溃,但插入-1的位置的页面会成为最后一个,所以追加的时候就全部用-1就行了.
        var index = -1
        for (path in resultPaths) {
            val page = addPage(path, mDocument, index++)

            mDocument.insertPage(-1, page)
        }
        mDocument.save(pdfPath, OPTS);
        d(String.format("save,%s,%s", mDocument.toString(), mDocument.countPages()))
        if (splitPaths.size > 0) {
            splitPaths.forEach {
                it.delete()
            }
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
        d(String.format("index:%s,page,%s", index, contents))
        return page
    }

    /**
     * 将大图片切割成小图片,以长图片切割,不处理宽图片
     */
    private fun processLargeImage(
        imagePaths: List<String>,
        splitPaths: ArrayList<File>
    ): List<String> {
        val options = BitmapFactory.Options()
        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = true
        val maxHeight = PAPER_HEIGHT

        val result = arrayListOf<String>()
        for (path in imagePaths) {
            try {
                BitmapFactory.decodeFile(path, options)
                if (options.outHeight > maxHeight) {
                    //split image,maxheight=PAPER_HEIGHT
                    splitImages(result, path, options.outWidth, options.outHeight, splitPaths)
                } else {
                    result.add(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    private fun splitImages(
        result: ArrayList<String>,
        path: String,
        width: Int,
        height: Int,
        splitPaths: ArrayList<File>
    ) {
        var top = 0f
        val right = 0 + width
        var bottom = PAPER_HEIGHT

        while (bottom < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, bottom.toInt())
            splitImage(path, rect, result, splitPaths)

            top = bottom
            bottom += PAPER_HEIGHT
        }
        if (top < height) {
            val rect = android.graphics.Rect()
            rect.set(0, top.toInt(), right, height)
            splitImage(path, rect, result, splitPaths)
        }
    }

    private fun splitImage(
        path: String,
        rect: android.graphics.Rect,
        result: ArrayList<String>,
        splitPaths: ArrayList<File>
    ) {
        val mDecoder = BitmapRegionDecoder.newInstance(path, true)
        val bm: Bitmap = mDecoder.decodeRegion(rect, null)
        val file =
            File(
                FileUtils.getExternalCacheDir(App.instance).path
                        //FileUtils.getStorageDirPath() + "/amupdf"
                        + File.separator + System.currentTimeMillis() + ".jpg"
            )
        BitmapUtils.saveBitmapToFile(bm, file, Bitmap.CompressFormat.JPEG, 90)
        d("new file:height:${rect.bottom - rect.top}, path:${file.absolutePath}")
        result.add(file.absolutePath)
        splitPaths.add(file)
    }

    private val fileFilter: FileFilter = FileFilter { file ->
        if (file.isDirectory) {
            return@FileFilter false
        }
        val fname = file.name.lowercase(Locale.ROOT)
        return@FileFilter fname.startsWith("Peter")
    }

    fun saveBooksToHtml(context: Context) {
        val sdcardRoot = FileUtils.getStorageDirPath()
        val dir = "$sdcardRoot/book/股票"

        val files = File(dir).listFiles()
        d("saveBooksToHtml:$dir,${files.size}")
        if (files != null) {
            for (file in files) {
                d("saveBooksToHtml:$file")
                if (file.isFile &&
                    (file.name.startsWith("Peter") && !FileUtils.getExtension(file.name)
                        .equals("html"))
                ) {
                    instance.diskIO().execute {
                        kotlin.run { saveBookToHtml(context, sdcardRoot, file) }
                    }
                }
            }
        }
    }

    private fun saveBookToHtml(context: Context, sdcardRoot: String, file: File) {
        try {
            val path = "$sdcardRoot/amupdf/${file.name}.html"
            d("save book:$path")
            val mMupdfDocument = MupdfDocument(context)
            mMupdfDocument.newDocument(file.absolutePath, null)
            val cp: Int = mMupdfDocument.countPages()
            val stringBuilder = StringBuilder()
            for (i in 0 until cp) {
                val loadPage = mMupdfDocument.loadPage(i)
                val content =
                    String(loadPage.textAsHtml2("preserve-whitespace,inhibit-spaces,preserve-images"))
                stringBuilder.append(content)
                loadPage.destroy()
            }
            StreamUtils.saveStringToFile(stringBuilder.toString(), path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}