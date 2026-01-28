package cn.archko.pdf.adapters

import android.widget.ImageView
import cn.archko.pdf.R

/**
 * @author: archko 2024/3/12 :13:39
 */
object AdapterUtils {
    /*val exts: MutableList<String> = ArrayList()

    init {
        exts.add("pdf")
        exts.add("xps")
        exts.add("cbz")
        exts.add("png")
        exts.add("jpe")
        exts.add("jpeg")
        exts.add("jpg")
        exts.add("jfif")
        exts.add("jfif-tbnl")
        exts.add("tif")
        exts.add("tiff")
        exts.add("bmp")
        exts.add("epub")
        exts.add("mobi")
        exts.add("txt")
        exts.add("log")
        exts.add("pptx")
        exts.add("docx")
        exts.add("xlsx")
        exts.add("json")
        exts.add("js")
    }*/

    fun getExtensionWithDot(name: String?): String {
        if (name == null) {
            return ""
        }
        val index = name.lastIndexOf(".")
        return if (index == -1) {
            ""
        } else name.substring(index)
    }

    fun getExtension(name: String?): String {
        if (name == null) {
            return ""
        }
        val index = name.lastIndexOf(".")
        return if (index == -1) {
            ""
        } else name.substring(index + 1)
    }

    fun setIcon(ext: String?, imageView: ImageView) {
        var drawableId = R.drawable.ic_book_unknown
        if (".pdf" == ext) {
            drawableId = R.drawable.ic_book_pdf
        } else if (".djvu" == ext || ".djv" == ext) {
            drawableId = R.drawable.ic_book_djvu
        } else if (".epub" == ext) {
            drawableId = R.drawable.ic_book_epub
        } else if (".mobi" == ext) {
            drawableId = R.drawable.ic_book_mobi
        } else if (".fb2" == ext || ".fb3" == ext) {
            drawableId = R.drawable.ic_book_fb2
        } else if (".azw" == ext || ".azw2" == ext
            || ".azw3" == ext || ".azw4" == ext
        ) {
            drawableId = R.drawable.ic_book_azw3
        } else if (".png" == ext || ".jpg" == ext || ".jpeg" == ext
            || ".bmp" == ext || ".svg" == ext || ".gif" == ext
            || ".heic" == ext || ".webp" == ext
        ) {
            drawableId = R.drawable.ic_book_image
        } else if (".dng" == ext || ".nef" == ext
            || ".cr2" == ext || ".cr3" == ext
            || ".arw" == ext || ".raf" == ext
            || ".orf" == ext
        ) {
            drawableId = R.drawable.ic_book_raw
        } else if (".jfif" == ext || ".jfif-tbnl" == ext
            || ".tif" == ext || ".tiff" == ext
        ) {
            drawableId = R.drawable.ic_book_tif
        } else if (".txt" == ext || ".log" == ext
            || ".js" == ext || ".json" == ext
        ) {
            drawableId = R.drawable.ic_book_text
        } else if (".html" == ext || ".xhtml" == ext) {
            drawableId = R.drawable.ic_book_html
        } else if (".pptx" == ext) {
            drawableId = R.drawable.ic_book_ppt
        } else if (".docx" == ext) {
            drawableId = R.drawable.ic_book_docx
        } else if (".doc" == ext) {
            drawableId = R.drawable.ic_book_doc
        } else if (".xlsx" == ext) {
            drawableId = R.drawable.ic_book_excel
        }
        imageView.setImageResource(drawableId)
    }
}