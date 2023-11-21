package cn.archko.pdf.model

import android.graphics.Rect

class Hyperlink {

    companion object {
        const val LINKTYPE_PAGE = 0
        const val LINKTYPE_URL = 1
    }

    var linkType: Int = LINKTYPE_PAGE
    var url: String? = null
    var pageNum = 0
    var bbox: Rect? = null

    override fun toString(): String {
        return "Hyperlink(url=$url, pageNum=$pageNum, bbox=$bbox)"
    }

}