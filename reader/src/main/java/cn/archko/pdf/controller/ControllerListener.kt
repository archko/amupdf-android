package cn.archko.pdf.controller

import android.view.MotionEvent
import org.vudroid.core.codec.OutlineLink

/**
 * @author: archko 2024/8/15 :12:43
 */
interface ControllerListener {
    fun onSingleTapConfirmed(ev: MotionEvent?, currentPage: Int)

    fun onDoubleTap(ev: MotionEvent?, currentPage: Int): Boolean

    fun doLoadedDoc(count: Int, pos: Int, outlineLinks: List<OutlineLink>?)

    fun reloadDoc()
}