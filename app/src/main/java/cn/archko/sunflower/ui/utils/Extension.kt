package cn.archko.sunflower.ui.utils

import cn.archko.pdf.entity.FileBean
import cn.archko.sunflower.R
import cn.archko.sunflower.viewmodel.FileViewModel

/**
 * @author: archko 2021/4/14 :16:25
 */
class Extension {

}

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