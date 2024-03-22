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
        var drawableId = R.drawable.browser_icon_any
        if ("pdf".equals(ext, ignoreCase = true)) {
            drawableId = R.drawable.browser_icon_pdf
        } else if ("epub".equals(ext, ignoreCase = true) || "mobi".equals(ext, ignoreCase = true)) {
            drawableId = R.drawable.browser_icon_epub
        } else if ("png".equals(ext, ignoreCase = true) || "jpg".equals(
                ext,
                ignoreCase = true
            ) || "jpeg".equals(ext, ignoreCase = true)
        ) {
            drawableId = R.drawable.browser_icon_image
        } else if ("txt".equals(ext, ignoreCase = true) || "log".equals(ext, ignoreCase = true)
            || "js".equals(ext, ignoreCase = true) || "json".equals(ext, ignoreCase = true)
            || "html".equals(ext, ignoreCase = true) || "xhtml".equals(ext, ignoreCase = true)
        ) {
            drawableId = R.drawable.browser_icon_txt
        } /*else if ("ppt".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_ppt;
        } else if ("pptx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_pptx;
        } else if ("doc".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_doc;
        } else if ("docx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_docx;
        } else if ("xls".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_xls;
        } else if ("xlsx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_xlsx;
        } */
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
