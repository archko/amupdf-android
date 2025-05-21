package org.vudroid.core

import android.view.MotionEvent

/**
 * @author: archko 2025/5/15 :12:43
 */
interface DocViewListener {

    fun onSingleTapConfirmed(ev: MotionEvent, currentPage: Int)

    fun onDoubleTap(ev: MotionEvent, currentPage: Int)

    fun setCurrentPage(page: Int)
}