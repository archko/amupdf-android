package cn.archko.pdf.utils

import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.utils.Utils
import java.util.Locale

/**
 * @author: archko 2021/4/14 :16:25
 */
fun FileBean.getIcon(): Int {
    var drawableId = cn.archko.pdf.R.drawable.ic_book_text
    if (type == FileBean.HOME || type == FileBean.CURRENT) {
        drawableId = cn.archko.pdf.R.drawable.ic_book_dir_home
    } else if (type == FileBean.NORMAL && isDirectory && !isUpFolder) {
        drawableId = cn.archko.pdf.R.drawable.ic_book_folder
    } else if (isUpFolder) {
        drawableId = cn.archko.pdf.R.drawable.ic_book_folder
    } else {
        bookProgress?.let {
            if (null != bookProgress!!.ext) {
                val ext: String = "." + bookProgress!!.ext!!.lowercase(Locale.ROOT)
                if (".pdf".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_pdf
                } else if (".djvu".equals(ext) || ".djv".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_djvu
                } else if (".epub".equals(ext) || ".mobi".equals(ext)
                ) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_epub_zip
                } else if (".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)
                    || ".bmp".equals(ext) || ".svg".equals(ext) || ".gif".equals(ext)
                    || ".jfif".equals(ext) || ".jfif-tbnl".equals(ext)
                    || ".tif".equals(ext) || ".tiff".equals(ext)
                    || ".heic".equals(ext) || ".webp".equals(ext)
                ) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_image
                } else if ("txt".equals(ext) || "log".equals(ext)
                    || "js".equals(ext) || "json".equals(ext)
                    || "html".equals(ext) || "xhtml".equals(ext)
                ) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_text
                } else if (".pptx".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_ppt
                } else if (".doc".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_word
                } else if (".docx".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_word
                } else if (".xls".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_excel
                } else if (".xlsx".equals(ext)) {
                    drawableId = cn.archko.pdf.R.drawable.ic_book_excel
                }
            }
        }
    }

    return drawableId
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