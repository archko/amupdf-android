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

        /**
         * 检查点是否在链接区域内
         */
        fun contains(
            hyperlink: Hyperlink,
            x: Float,
            y: Float
        ): Boolean {
            val bbox = hyperlink.bbox ?: return false
            return bbox.contains(Rect(0, x.toInt(), 0, y.toInt()))
        }

        /**
         * 在链接列表中查找包含指定点的链接
         */
        fun findLinkAtPoint(
            links: List<Hyperlink>,
            x: Float,
            y: Float
        ): Hyperlink? {
            for (link in links) {
                if (contains(link, x, y)) {
                    return link
                }
            }
            return null
        }
    }
}