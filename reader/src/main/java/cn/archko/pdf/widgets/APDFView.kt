package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.ImageView
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.ImageDecoder
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
public class APDFView(
    mContext: Context,
) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()
    private val bitmapPaint = Paint()
    private val textPaint: Paint = textPaint()
    private var aPage: APage?=null

    init {
        updateView()
    }

    private fun updateView() {
        scaleType = ImageView.ScaleType.MATRIX
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun textPaint(): Paint {
        val paint = Paint()
        paint.color = Color.BLUE
        paint.isAntiAlias = true
        paint.textSize = Utils.sp2px(30f).toFloat()
        paint.textAlign = Paint.Align.CENTER
        return paint
    }

    fun recycle() {
        setImageBitmap(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (null==aPage){
            return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        var mwidth = aPage!!.getCropWidth()
        var mheight = aPage!!.getCropHeight()

        val d = drawable
        if (null != d) {
            val dwidth = d.intrinsicWidth
            val dheight = d.intrinsicHeight

            if (dwidth > 0 && dheight > 0) {
                mwidth = dwidth
                mheight = dheight
            }
        }

        setMeasuredDimension(mwidth, mheight)
        Logcat.d(
            String.format(
                "onMeasure,width:%s,height:%s, page:%s-%s, mZoom: %s, aPage:%s",
                mwidth, mheight, aPage!!.effectivePagesWidth, aPage!!.effectivePagesHeight, mZoom, aPage
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (/*mBitmap == null &&*/ drawable == null&&null!=aPage) {
            canvas.drawText(
                String.format("Page %s", aPage!!.index + 1), (measuredWidth / 2).toFloat(),
                (measuredHeight / 2).toFloat(), textPaint
            )
        }
        super.onDraw(canvas)
    }

    fun updatePage(pageSize: APage, newZoom: Float, mupdfDocument: MupdfDocument?, crop: Boolean) {
        //isRecycle = false
        val oldZoom = aPage?.scaleZoom?:1f
        aPage = pageSize
        aPage!!.zoom = newZoom

        Logcat.d(
            String.format(
                "updatePage, oldZoom:%s, newScaleZoom:%s,newZoom:%s,",
                oldZoom, aPage!!.scaleZoom, newZoom
            )
        )

        var bmp = BitmapCache.getInstance()
            .getBitmap(ImageDecoder.getCacheKey(aPage!!.index, crop, aPage!!.scaleZoom))

        if (null != bmp) {
            setImageBitmap(bmp)
        } else {
            bmp = BitmapCache.getInstance()
                .getBitmap(ImageDecoder.getCacheKey(aPage!!.index, crop, oldZoom))
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("updatePage xOrigin: %s, oldZoom:%s, newZoom:%s, bmp:%s",
            //            xOrigin, oldZoom, newZoom, bmp));
            //}
            if (null != bmp) {
                setImageBitmap(bmp)
                return
            }
        }

        ImageDecoder.getInstance()
            .loadImage(aPage, crop, 0, this, mupdfDocument?.document) { bitmap ->
                //if (Logcat.loggable) {
                //    Logcat.d(String.format("decode2 relayout bitmap:index:%s, %s:%s imageView->%s:%s",
                //            pageSize.index, bitmap.width, bitmap.height,
                //            getWidth(), getHeight()))
                //}
                setImageBitmap(bitmap)
            }
    }
}
