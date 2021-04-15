package cn.archko.pdf.common

/**
 * @author: archko 2019/2/22 :13:49
 */
interface Event {
    companion object {
        const val ACTION_STOPPED = "cn.archko.pdf.STOPPED"
        const val ACTION_FAVORITED = "cn.archko.pdf.FAVORITED"
        const val ACTION_UNFAVORITED = "cn.archko.pdf.UNFAVORITED"
        const val ACTION_ISFIRST = "cn.archko.pdf.ISFIRST"
    }
}