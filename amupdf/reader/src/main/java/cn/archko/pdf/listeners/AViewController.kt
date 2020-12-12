package cn.archko.pdf.listeners

import android.content.res.Configuration
import android.util.SparseArray
import android.view.View
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument

/**
 * @author: archko 2020/5/15 :12:43
 */
interface AViewController {

    fun init(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument?, pos: Int)
    fun doLoadDoc(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument, pos: Int)

    fun getDocumentView(): View
    fun onSingleTap()
    fun onDoubleTap()

    fun getCurrentPos(): Int
    fun onSelectedOutline(resultCode: Int)
    fun scrollToPosition(page: Int)

    fun onResume()
    fun onPause()
    fun onConfigurationChanged(newConfig: Configuration)

    fun showController()
    fun notifyDataSetChanged()
}
