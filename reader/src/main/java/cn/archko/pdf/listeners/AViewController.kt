package cn.archko.pdf.listeners

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import org.vudroid.core.Page

/**
 * @author: archko 2020/5/15 :12:43
 */
interface AViewController {

    fun init()

    fun getDocumentView(): View
    fun onSingleTap(ev: MotionEvent?, margin: Int): Boolean
    fun onDoubleTap()

    fun getCurrentPos(): Int
    fun getCurrentBitmap(): Bitmap?
    fun getCount(): Int
    fun setOrientation(ori: Int)
    fun setCrop(crop: Boolean)
    fun getCrop(): Boolean
    fun onSelectedOutline(index: Int)
    fun scrollToPosition(page: Int)
    fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean
    fun tryHyperlink(ev: MotionEvent): Boolean

    fun onResume()
    fun onPause()
    fun onDestroy()
    fun onConfigurationChanged(newConfig: Configuration)

    fun showController()
    fun notifyDataSetChanged()
    fun notifyItemChanged(pos: Int)
    fun setFilter(colorMode: Int)
    fun decodePageForTts(currentPos: Int)
    fun reflow(): Int
    fun prev(string: String?)
    fun next(string: String?)
    fun clearSearch()
    fun showSearch()
    fun setSpeakingPage(page: Int)
}
