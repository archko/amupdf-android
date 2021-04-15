package cn.archko.pdf.common

import cn.archko.pdf.utils.FileUtils
import com.artifex.mupdf.fitz.Document
import java.io.File
import java.io.FileWriter

/**
 * @author: archko 2019/6/25 :14:28
 */
class TextReflowHelper(var mCore: Document?) {

    fun export() {
        //doAsync {
        //    Logcat.d("exportToText")
        //    exportToText()
        //    Logcat.d("exportToHtml")
        //    exportToHtml()
        //    Logcat.d("exportToXHtml")
        //    exportToXHtml()
        //    Logcat.d("end")
        //}
    }

    fun exportToText() {
        var file =
            File(FileUtils.getStorageDir("amupdf").absolutePath + File.separator + "book.txt")
        var fw = FileWriter(file, true)
        try {
            for (index in 0 until 30) {
                val result = mCore?.loadPage(index)
                    ?.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")
                fw.write(String(result!!))
            }
        } catch (e: Exception) {
        } finally {
            fw.close()
        }
    }

    fun exportToHtml() {
        var file =
            File(FileUtils.getStorageDir("amupdf").absolutePath + File.separator + "book.html")
        var fw = FileWriter(file, true)
        try {
            for (index in 0 until 30) {
                val result = mCore?.loadPage(index)
                    ?.textAsHtml2("preserve-whitespace,inhibit-spaces,preserve-images")
                fw.write(String(result!!))
            }
        } catch (e: Exception) {
        } finally {
            fw.close()
        }
    }

    fun exportToXHtml() {
        var file =
            File(FileUtils.getStorageDir("amupdf").absolutePath + File.separator + "book.xhtml")
        var fw = FileWriter(file, true)
        try {
            for (index in 0 until 30) {
                val result = mCore?.loadPage(index)
                    ?.textAsXHtml("preserve-whitespace,inhibit-spaces,preserve-images")
                fw.write(String(result!!))
            }
        } catch (e: Exception) {
        } finally {
            fw.close()
        }
    }
}
