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
import cn.archko.pdf.common.ImageWorker.DecodeParam
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.DecodeCallback
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
class APDFView(mContext: Context) : ImageView(mContext) {

    private val textPaint: Paint = textPaint()
    private val strokePaint: Paint = strokePaint()
    private var aPage: APage? = null
    private var index: Int = -1
    private var cacheKey: String? = null

    init {
        scaleType = ScaleType.MATRIX
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

    private fun strokePaint(): Paint {
        val strokePaint = Paint()
        strokePaint.setColor(Color.BLACK)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1f
        return strokePaint
    }

    fun recycle() {
        setImageBitmap(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (null == aPage) {
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
                "onMeasure,width:%s,height:%s, page:%s-%s, aPage:%s",
                mwidth,
                mheight,
                aPage!!.effectivePagesWidth,
                aPage!!.effectivePagesHeight,
                aPage
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (drawable == null && null != aPage) {
            canvas.drawText(
                String.format("Page %s", aPage!!.index + 1), (measuredWidth / 2).toFloat(),
                (measuredHeight / 2).toFloat(), textPaint
            )
        }
        super.onDraw(canvas)
        //draw page line,not full width
        canvas.drawLine(
            0f,
            measuredHeight * 1f,
            measuredWidth * 1f / 5,
            measuredHeight * 1f,
            strokePaint
        )
    }

    fun updatePage(pageSize: APage, newZoom: Float, mupdfDocument: MupdfDocument?, crop: Boolean) {
        index = pageSize.index
        val oldZoom = aPage?.scaleZoom ?: 1f
        aPage = pageSize
        aPage!!.zoom = newZoom

        cacheKey = ImageDecoder.getCacheKey(aPage!!.index, crop, aPage!!.scaleZoom)
        Logcat.d(
            String.format(
                "updatePage:%s, key:%s, oldZoom:%s, newScaleZoom:%s,newZoom:%s,",
                aPage!!.index, cacheKey, oldZoom, aPage!!.scaleZoom, newZoom
            )
        )

        val bmp = BitmapCache.getInstance().getBitmap(cacheKey!!)

        if (null != bmp) {
            setImageBitmap(bmp)
            return
        }

        val callback = DecodeCallback { bitmap, param ->
            if (Logcat.loggable) {
                Logcat.d(
                    String.format(
                        "decode callback:index:%s-%s, decode.page:%s, key:%s, param:%s",
                        param.pageNum, index, param.pageNum, cacheKey, param.key
                    )
                )
            }
            if (param.pageNum == index) {
                setImageBitmap(bitmap)
            }
        }
        //aPage 这个如果当参数传递,由于复用机制,后面的页面更新后会把它覆盖,导致解码并不是原来那个
        //这里应该传递页码与key,不是引用
        val decodeParam = DecodeParam(
            cacheKey,
            this,
            crop,
            0,
            aPage!!,
            mupdfDocument?.document,
            callback
        )
        ImageDecoder.getInstance().loadImage(decodeParam)
    }
}
