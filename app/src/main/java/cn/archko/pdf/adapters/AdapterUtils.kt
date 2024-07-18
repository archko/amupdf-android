package cn.archko.pdf.adapters

import android.widget.ImageView
import android.widget.ProgressBar
import cn.archko.mupdf.R
import java.util.Locale

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
        exts.add("ppt")
        exts.add("pptx")
        exts.add("doc")
        exts.add("docx")
        exts.add("xls")
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
        var drawableId = R.drawable.app_pdf
        if (".pdf".equals(ext)) {
            drawableId = R.drawable.app_pdf
        } else if (".djvu".equals(ext) || ".djv".equals(ext)) {
            drawableId = R.drawable.image_djvu
        } else if (".epub".equals(ext) || ".mobi".equals(ext)
        ) {
            drawableId = R.drawable.app_epub_zip
        } else if (".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)
            || ".bmp".equals(ext) || ".svg".equals(ext) || ".gif".equals(ext)
            || ".jfif".equals(ext) || ".jfif-tbnl".equals(ext)
            || ".tif".equals(ext) || ".tiff".equals(ext)
            || ".heic".equals(ext) || ".webp".equals(ext)
        ) {
            drawableId = R.drawable.image
        } /*else if ("txt".equals(ext) || "log".equals(ext)
            || "js".equals(ext) || "json".equals(ext)
            || "html".equals(ext) || "xhtml".equals(ext)
        ) {
            drawableId = R.drawable.browser_icon_txt
        }*/ else if (".ppt".equals(ext)) {
            drawableId = R.drawable.office_ppt
        } else if (".pptx".equals(ext)) {
            drawableId = R.drawable.office_ppt
        } else if (".doc".equals(ext)) {
            drawableId = R.drawable.office_word
        } else if (".docx".equals(ext)) {
            drawableId = R.drawable.office_word
        } else if (".xls".equals(ext)) {
            drawableId = R.drawable.office_excel
        } else if (".xlsx".equals(ext)) {
            drawableId = R.drawable.office_excel
        }
        imageView.setImageResource(drawableId)
    }

    fun setProgress(progressbar: ProgressBar, progress: Int, count: Int) {
        progressbar.setMax(progress)
        progressbar.progress = count
    }

    fun isSupportExt(fname: String): Boolean {
        return exts.contains(getExtension(fname))
    }

    fun isPlainTxt(name: String): Boolean {
        val fname = name.lowercase(Locale.getDefault())
        return fname.endsWith("txt") || fname.endsWith("log") || fname.endsWith("json") || fname.endsWith(
            "js"
        ) || fname.endsWith("html") || fname.endsWith("xhtml")
    }

    fun supportImage(path: String): Boolean {
        return path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".gif", true)
    }
}
