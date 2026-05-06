package cn.archko.pdf.core.link

import android.graphics.Rect

class Hyperlink {
    var linkType = LINKTYPE_PAGE
    var url: String? = null
    var page = 0
    var bbox: Rect? = null
    override fun toString(): String {
        return "Hyperlink{" +
                "linkType=" + linkType +
                ", page=" + page +
                ", bbox=" + bbox +
                ", url='" + url + '\'' +
                '}'
    }

    companion object {

        const val LINKTYPE_PAGE = 0
        const val LINKTYPE_URL = 1
    }
}