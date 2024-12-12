package cn.archko.pdf.core.common

/**
 * @author: archko 2019/2/22 :13:49
 */
interface Event {
    companion object {
        const val ACTION_STOPPED = "cn.archko.pdf.STOPPED"
        const val ACTION_FAVORITED = "cn.archko.pdf.FAVORITED"
        const val ACTION_UNFAVORITED = "cn.archko.pdf.UNFAVORITED"
        const val ACTION_ISFIRST = "cn.archko.pdf.ISFIRST"

        const val ACTION_SCAN = "cn.archko.pdf.SCAN"
        const val ACTION_DONOT_SCAN = "cn.archko.pdf.DONTSCAN"
    }
}

data class GlobalEvent(val name: String, val obj: Any?)

data class ScanEvent(val name: String, val obj: Any?)