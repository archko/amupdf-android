package cn.archko.pdf.common

import android.content.Context
import cn.archko.pdf.AppExecutors.Companion.instance
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import com.artifex.mupdf.fitz.*
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
object PDFCreaterHelper {

    //var imgname = "/sdcard/DCIM/Camera/IMG_20200912_072822.jpg"
    var imgname = "/sdcard/DCIM/Camera/IMG20230111162944.jpg"
    var filename = "/book/test.pdf"

    fun save() {
        instance.diskIO().execute {
            try {
                val mDocument = PDFDocument()
                val mediabox = Rect(0f, 0f, 300f, 500f)
                val image = Image(imgname)
                val resources = mDocument.newDictionary()
                val xobj = mDocument.newDictionary()
                val obj = mDocument.addImage(image)
                xobj.put("I", obj)
                resources.put("XObject", xobj)

                val contents = "q 300 0 0 500 0 0 cm /I Do Q\n"

                val page = mDocument.addPage(mediabox, 0, resources, contents)

                mDocument.insertPage(-1, page)
                val destPdfPath = FileUtils.getStoragePath(filename)
                mDocument.save(destPdfPath, OPTS);
                val newPdfDoc = PDFDocument.openDocument(destPdfPath)
                d(String.format("save,%s,%s", newPdfDoc.toString(), newPdfDoc?.countPages()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createTextPage(text: String) {
        val mDocument = PDFDocument()
        val mediabox = Rect(0f, 0f, 300f, 500f)
        val fontSize = 16       //字号
        val leftPadding = 20    //左侧的距离
        val height = 500        //这个是倒着的,如果是0,则在底部
        val contents = "BT /Tm $fontSize Tf $leftPadding $height TD ($text) Tj ET\n"

        //没有resources,则字体是默认的,resources用于存储字体与图片

        val page = mDocument.addPage(mediabox, 0, null, contents)

        mDocument.insertPage(-1, page)
        val destPdfPath = FileUtils.getStoragePath(filename)
        mDocument.save(destPdfPath, OPTS);
    }

    fun createPdf(pdfPath: String, imagePaths: List<String>) {
        instance.diskIO().execute {
            try {
                d(String.format("imagePaths:%s", imagePaths))
                var mDocument: PDFDocument? = PDFDocument.openDocument(pdfPath) as PDFDocument
                if (mDocument == null) {
                    mDocument = PDFDocument()
                }
                for (path in imagePaths) {
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
                    d(String.format("page,%s", contents))

                    mDocument.insertPage(-1, page)
                }
                mDocument.save(pdfPath, OPTS);
                val newPdfDoc = PDFDocument.openDocument(pdfPath);
                d(String.format("save,%s,%s", newPdfDoc.toString(), newPdfDoc?.countPages()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        Logcat.d("saveBooksToHtml:$dir,${files.size}")
        if (files != null) {
            for (file in files) {
                Logcat.d("saveBooksToHtml:$file")
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
            Logcat.d("save book:$path")
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

    const val OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts"
}