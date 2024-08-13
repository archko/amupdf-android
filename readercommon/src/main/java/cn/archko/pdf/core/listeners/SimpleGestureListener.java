package cn.archko.pdf.core.listeners;

import android.view.MotionEvent;

public interface SimpleGestureListener {

    void onSingleTapConfirmed(MotionEvent ev, int currentPage);

    void onDoubleTapEvent(MotionEvent ev, int currentPage);
}
