package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.view.View
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2019/11/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
class APDFPageView(
    private val mContext: Context,
    private val mupdfDocument: MupdfDocument?,
    private var pageSize: APage,
    crop: Boolean
) : View(mContext) {

    private var mZoom: Float = 0.toFloat()
    private lateinit var pdfPage: APDFPage
    private var showBookmark: Boolean = false
    private val bgpaint = TextPaint()
    private val paint = TextPaint()
    private var fWidth: Float = 40f

    init {
        paint.setColor(Color.MAGENTA)
        paint.setAntiAlias(true)
        paint.setTextSize(Utils.sp2px(20f).toFloat())
        paint.setTextAlign(Paint.Align.CENTER)
        fWidth = paint.measureText("B")

        bgpaint.setColor(Color.LTGRAY)

        mZoom = pageSize.zoom ?: 1f
        initPdfPage(crop)
        updateView()
    }

    private fun initPdfPage(crop: Boolean) {
        pdfPage = APDFPage(this, pageSize, mupdfDocument, crop)
        pdfPage.bounds = RectF(
            0f,
            0f,
            pageSize.cropScaleWidth.toFloat(),
            pageSize.cropScaleHeight.toFloat()
        )
    }

    private fun updateView() {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun recycle() {
        pdfPage.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = pageSize.cropScaleWidth
        var height = pageSize.cropScaleHeight

        val viewWidth = pageSize.getTargetWidth()
        Logcat.d(
            String.format(
                "onMeasure,index:%s, page.crop.w-h:%s-%s, viewWidth:%s, page.w-h:%s-%s, aPage:%s",
                pageSize.index,
                width,
                height,
                viewWidth,
                pageSize.effectivePagesWidth,
                pageSize.effectivePagesHeight,
                pageSize
            )
        )
        if (viewWidth != width && viewWidth > 0) {
            height = (viewWidth.toFloat() / width * height).toInt()
            width = viewWidth
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pdfPage.draw(canvas)
        if (showBookmark) {
            drawBookmark(canvas)
        }
    }

    private fun drawBookmark(canvas: Canvas?) {
        canvas?.drawCircle(40f, 40f, 30f, bgpaint)
        canvas?.drawText("B", 40f, 40f + fWidth / 2, paint)
    }

    fun updatePage(showBookmark: Boolean, pageSize: APage, newZoom: Float, crop: Boolean) {
        this.showBookmark = showBookmark
        mZoom = newZoom
        this.pageSize = pageSize
        this.pageSize.zoom = newZoom
        var isNew = false
        if (pdfPage.bounds.width().toInt() != pageSize.effectivePagesWidth
            || pdfPage.bounds.height().toInt() != pageSize.effectivePagesHeight
        ) {
            pageSize.setCropBounds(null, 1.0f)
            isNew = true
        }
        if (this.pageSize.index != pageSize.index || isNew) {
            this.pageSize = pageSize
            isNew = true
            pageSize.setCropBounds(null, 1.0f)
            pdfPage.bounds = RectF(
                0f,
                0f,
                pageSize.effectivePagesWidth.toFloat(),
                pageSize.effectivePagesHeight.toFloat()
            )
            pdfPage.update(this, pageSize)
        }

        val zoomSize = this.pageSize.zoomPoint
        val xOrigin = (zoomSize.x - this.pageSize.getTargetWidth()) / 2

        Logcat.d(
            String.format(
                "updatePage:index:%s, isNew:%s, width-height:%s-%s, mZoom:%s, bounds:%s, aPage:%s",
                pageSize.index,
                isNew,
                pageSize.cropScaleWidth,
                pageSize.cropScaleHeight,
                mZoom,
                pdfPage.bounds,
                pageSize
            )
        )
        pdfPage.updateVisibility(crop, xOrigin)
    }
}
