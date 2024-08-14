package cn.archko.pdf.activities

import android.view.MotionEvent

/**
 * @author: archko 2024/8/15 :12:43
 */
interface ControllerListener {
    fun onSingleTapConfirmed(ev: MotionEvent?, currentPage: Int)

    fun onDoubleTapEvent(ev: MotionEvent?, currentPage: Int)

    fun doLoadedDoc(count: Int, pos: Int)
}