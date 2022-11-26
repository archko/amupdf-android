package cn.archko.pdf.common

import android.content.Context
import cn.archko.pdf.App
import cn.archko.pdf.AppExecutors.Companion.instance
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import com.artifex.mupdf.fitz.Buffer
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.Rect
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
object PDFCreaterHelper {
    var mDocument: PDFDocument? = null
    var imgname = "DCIM/Camera/IMG_20181221_125752.jpg"
    var filename = "test.pdf"
    fun save() {
        instance.diskIO().execute {
            try {
                mDocument = PDFDocument()
                mDocument!!.addObject(mDocument!!.newString("test pdf"))
                val pdfObject = mDocument!!.newString("test2.pdf")
                val buffer = Buffer()
                buffer.writeLine("test2")
                val mediabox = Rect(0f, 0f, 1000f, 1000f)
                val pdfPage = mDocument!!.addPage(mediabox, 0, pdfObject, buffer)
                //mDocument.insertPage(0, pdfPage);
                val image = Image(FileUtils.getStoragePath(imgname))
                mDocument!!.addImage(image)
                //int save = mDocument.save(FileUtils.getStoragePath(filename), OPTS);
                //mDocument = (PDFDocument) PDFDocument.openDocument(FileUtils.getStoragePath(filename));
                d(String.format("%s,%s,%s", 0, mDocument.toString(), mDocument!!.countPages()))
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