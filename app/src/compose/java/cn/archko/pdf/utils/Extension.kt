package cn.archko.pdf.utils

import cn.archko.mupdf.R
import cn.archko.pdf.entity.FileBean
import java.util.Locale

/**
 * @author: archko 2021/4/14 :16:25
 */
fun FileBean.getIcon(): Int {
    var iconId = R.drawable.browser_icon_folder
    if (type == FileBean.HOME || type == FileBean.CURRENT) {
    } else if (type == FileBean.NORMAL && isDirectory && !isUpFolder) {
    } else if (isUpFolder) {
    } else {
        bookProgress?.let {
            if (null != bookProgress!!.ext) {
                val ext: String = bookProgress!!.ext!!.lowercase(Locale.ROOT)

                if ("pdf".equals(ext)) {
                    iconId = R.drawable.browser_icon_pdf
                } else if ("epub".equals(ext) || "mobi".equals(ext)) {
                    iconId = R.drawable.browser_icon_epub
                } else if (ext.contains("txt") || ext.contains("log")
                    || ext.contains("js") || ext.contains("json")
                    || ext.contains("html") || ext.contains("xhtml")
                ) {
                    iconId = R.drawable.browser_icon_txt
                } else if (ext.contains("png") || ext.contains("jpg") || ext.contains("jpeg")) {
                    iconId = R.drawable.browser_icon_image
                } else if ("ppt".equals(ext)) {
                    iconId = R.drawable.browser_icon_ppt
                } else if ("pptx".equals(ext)) {
                    iconId = R.drawable.browser_icon_pptx
                } else if ("doc".equals(ext)) {
                    iconId = R.drawable.browser_icon_doc
                } else if ("docx".equals(ext)) {
                    iconId = R.drawable.browser_icon_docx
                } else if ("xls".equals(ext)) {
                    iconId = R.drawable.browser_icon_xls
                } else if ("xlsx".equals(ext)) {
                    iconId = R.drawable.browser_icon_xlsx
                } else {
                    iconId = R.drawable.browser_icon_any
                }
            }
        }
    }

    return iconId
}

fun FileBean.getProgress(): Float {
    val bookProgress = bookProgress
    if (null != bookProgress) {
        if (bookProgress.page > 0) {
            return bookProgress.page * 100F / bookProgress.pageCount
        }
    }
    return 0f
}

fun FileBean.getSize(): String {
    if (!isDirectory && null != bookProgress) {
        return Utils.getFileSize(bookProgress!!.size)
    }
    return ""
}