package cn.archko.pdf.entity

import java.io.File

/**
 * @author: archko 2019/7/12 :19:50
 */
class FontBean(var fontType: Int, var fontName: String?, var file: File?) {

    override fun toString(): String {
        return "FontBean(title=$fontName, type=$fontType)"
    }
}