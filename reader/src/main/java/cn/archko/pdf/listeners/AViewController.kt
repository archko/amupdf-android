package cn.archko.pdf.listeners

import android.content.res.Configuration
import android.util.SparseArray
import android.view.View
import cn.archko.pdf.entity.APage

/**
 * @author: archko 2020/5/15 :12:43
 */
interface AViewController {

    fun init(pageSizes: SparseArray<APage>, pos: Int)
    fun doLoadDoc(pageSizes: SparseArray<APage>, pos: Int)

    fun getDocumentView(): View
    fun onSingleTap()
    fun onDoubleTap()

    fun getCurrentPos(): Int
    fun onSelectedOutline(resultCode: Int)
    fun scrollToPosition(page: Int)
    fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean

    fun onResume()
    fun onPause()
    fun onDestroy()
    fun onConfigurationChanged(newConfig: Configuration)

    fun showController()
    fun notifyDataSetChanged()
    fun notifyItemChanged(pos: Int)
}
