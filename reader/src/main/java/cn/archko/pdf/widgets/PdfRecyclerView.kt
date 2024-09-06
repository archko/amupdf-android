package cn.archko.pdf.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager

/**
 * @author: archko 2024/9/6 :20:47
 */
class PdfRecyclerView(context: Context) :
    ARecyclerView(context, null) {

    private var mScaleGestureDetector: ScaleGestureDetector
    var gestureDetector: GestureDetector? = null

    private var mScaleFactor: Float = 1f

    private var mMaxWidth: Float = 0f
    private var mMaxHeight: Float = 0f

    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    private var mTouchX: Float = 0f
    private var mTouchY: Float = 0f

    private var mWidth: Float = 0f
    private var mheight: Float = 0f

    private var mIsZoomEnabled: Boolean = true

    private var mMaxZoom: Float = 2f
    private var mMinZoom: Float = 1f

    init {
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun setZoomEnabled(isZoomEnabled: Boolean) {
        this.mIsZoomEnabled = isZoomEnabled
    }

    fun setMaxZoom(maxZoom: Float) {
        this.mMaxZoom = maxZoom
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        mheight = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /*override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val adapter: Adapter<*> = adapter as Adapter<*>
        val lm = (layoutManager as LinearLayoutManager)

        var pendingPos = 0
        Log.d("PDFSDK", "onSizeChanged:$pendingPos")
        if (pendingPos > 0) {
            lm.scrollToPositionWithOffset(pendingPos, 0)
            pendingPos = -1
            return
        }

        val first = lm.findFirstVisibleItemPosition()
        //val last = lm.findLastVisibleItemPosition()
        var offset = 0
        if (first > 0) {
            val child = lm.findViewByPosition(first)
            child?.run {
                val r = Rect()
                child.getLocalVisibleRect(r)
                offset = r.top
            }
        }
        lm.scrollToPositionWithOffset(first, -offset)
        adapter.notifyDataSetChanged()
        //adapter.update(w, h)
        Log.d(
            "",
            "onSizeChanged: $w,$h, old:$oldw,$oldh, pageWidth:$w, first:$first"
        )
    }*/

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        super.onTouchEvent(ev)
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = ev.x
                mLastTouchY = ev.y
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val x = ev.getX(pointerIndex)
                val y = ev.getY(pointerIndex)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                mTouchX += dx
                mTouchY += dy

                if (mTouchX > 0f) {
                    mTouchX = 0f
                } else if (mTouchX < mMaxWidth) {
                    mTouchX = mMaxWidth
                }

                if (mTouchY > 0f) {
                    mTouchY = 0f
                } else if (mTouchY < mMaxHeight) {
                    mTouchY = mMaxHeight
                }

                mLastTouchX = x
                mLastTouchY = y
                invalidate()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val newPointerIndex = if (pointerIndex == 0) 1 else 0
                mLastTouchX = ev.getX(newPointerIndex)
                mLastTouchY = ev.getY(newPointerIndex)
            }
        }
        mScaleGestureDetector.onTouchEvent(ev)
        gestureDetector?.onTouchEvent(ev)

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(mTouchX, mTouchY)
        canvas.scale(mScaleFactor, mScaleFactor)
        canvas.restore()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()

        if (mScaleFactor == mMinZoom) {
            mTouchX = 0f
            mTouchY = 0f
        }

        canvas.translate(mTouchX, mTouchY)
        canvas.scale(mScaleFactor, mScaleFactor)

        super.dispatchDraw(canvas)

        canvas.restore()
        invalidate()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (mIsZoomEnabled) {
                mScaleFactor *= detector.scaleFactor
                mScaleFactor = mMinZoom.coerceAtLeast(mScaleFactor.coerceAtMost(mMaxZoom))
                mMaxWidth = mWidth - mWidth * mScaleFactor
                mMaxHeight = mheight - mheight * mScaleFactor
                //Log.d("PDFSDK", "mScaleFactor:$mScaleFactor")
                invalidate()
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)

            val adapter: Adapter<*> = adapter as Adapter<*>
            val lm = (layoutManager as LinearLayoutManager)
            val first = lm.findFirstVisibleItemPosition()
            val last = lm.findLastVisibleItemPosition()
            adapter.notifyItemRangeChanged(first, last - first)
        }
    }
}