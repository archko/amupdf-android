package cn.archko.pdf.adapters

import android.widget.ImageView
import cn.archko.pdf.R

/**
 * @author: archko 2024/3/12 :13:39
 */
object AdapterUtils {
    val exts: MutableList<String> = ArrayList()

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
    }

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
        var drawableId = R.drawable.ic_book_text
        if (".pdf".equals(ext)) {
            drawableId = R.drawable.ic_book_pdf
        } else if (".djvu".equals(ext) || ".djv".equals(ext)) {
            drawableId = R.drawable.ic_book_djvu
        } else if (".epub".equals(ext) || ".mobi".equals(ext)
        ) {
            drawableId = R.drawable.ic_book_epub_zip
        } else if (".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)
            || ".bmp".equals(ext) || ".svg".equals(ext) || ".gif".equals(ext)
            || ".jfif".equals(ext) || ".jfif-tbnl".equals(ext)
            || ".tif".equals(ext) || ".tiff".equals(ext)
            || ".heic".equals(ext) || ".webp".equals(ext)
        ) {
            drawableId = R.drawable.ic_book_image
        } /*else if ("txt".equals(ext) || "log".equals(ext)
            || "js".equals(ext) || "json".equals(ext)
            || "html".equals(ext) || "xhtml".equals(ext)
        ) {
            drawableId = R.drawable.browser_icon_txt
        }*/ else if (".pptx".equals(ext)) {
            drawableId = R.drawable.ic_book_ppt
        } else if (".docx".equals(ext)) {
            drawableId = R.drawable.ic_book_word
        } else if (".xlsx".equals(ext)) {
            drawableId = R.drawable.ic_book_excel
        }
        imageView.setImageResource(drawableId)
    }
}