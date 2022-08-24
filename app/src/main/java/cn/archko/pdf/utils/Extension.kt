package cn.archko.pdf.utils

import androidx.compose.ui.text.toLowerCase
import cn.archko.mupdf.R
import cn.archko.pdf.entity.FileBean
import java.util.*

/**
 * @author: archko 2021/4/14 :16:25
 */
fun FileBean.getIcon(): Int {
    var iconId = R.drawable.ic_explorer_fldr
    if (type == FileBean.HOME || type == FileBean.CURRENT) {
    } else if (type == FileBean.NORMAL && isDirectory && !isUpFolder) {
    } else if (isUpFolder) {
    } else {
        bookProgress?.let {
            if (null != bookProgress!!.ext) {
                val ext: String = bookProgress!!.ext!!.toLowerCase(Locale.ROOT)

                if (ext.contains("pdf")) {
                    iconId = R.drawable.ic_item_book
                } else if (ext.contains("epub")) {
                    iconId = R.drawable.ic_item_book
                } else if (ext.contains("mobi")) {
                    iconId = R.drawable.ic_item_book
                } else if (ext.contains("txt")) {
                    iconId = R.drawable.ic_item_book
                } else {
                    iconId = R.drawable.ic_explorer_any
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