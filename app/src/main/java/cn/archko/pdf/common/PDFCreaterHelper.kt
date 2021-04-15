package cn.archko.pdf.common

import cn.archko.pdf.AppExecutors.Companion.instance
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.utils.FileUtils
import com.artifex.mupdf.fitz.Buffer
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.Rect

/**
 * @author: archko 2018/12/21 :1:03 PM
 */
class PDFCreaterHelper {
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

    companion object {
        const val OPTS = "compress-images;compress;incremental;linearize;pretty;compress-fonts"
    }
}