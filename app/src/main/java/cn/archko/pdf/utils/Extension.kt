package cn.archko.pdf.utils

import cn.archko.mupdf.R
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.viewmodel.FileViewModel

/**
 * @author: archko 2021/4/14 :16:25
 */
fun FileBean.getIcon(viewModel: FileViewModel): Int {
    var iconId = R.drawable.ic_explorer_fldr
    if (isDirectory) {
    } else {
        if (label?.endsWith("pdf") == true) {
            iconId = R.drawable.ic_item_book
        } else {
            if (viewModel.isHome(label!!)) {
                iconId = R.drawable.ic_explorer_fldr
            } else {
                iconId = R.drawable.ic_explorer_any
            }
        }
    }
    return iconId
}