package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument

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

    init {
        mZoom = pageSize.zoom
        initPdfPage(crop)
        updateView()
    }

    private fun initPdfPage(crop: Boolean) {
        pdfPage = APDFPage(this, pageSize, mupdfDocument, crop)
        pdfPage.setBounds(
            RectF(
                0f,
                0f,
                this.pageSize.cropScaleWidth.toFloat(),
                this.pageSize.cropScaleHeight.toFloat()
            )
        )
    }

    private fun updateView() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
                "onMeasure,width:%s,height:%s, viewWidth:%s, page:%s-%s, mZoom: %s, aPage:%s",
                width,
                height,
                viewWidth,
                pageSize.effectivePagesWidth,
                pageSize.effectivePagesHeight,
                mZoom,
                pageSize
            )
        )
        if (viewWidth != width && viewWidth > 0) {
            width = viewWidth
            height = (viewWidth.toFloat() / width * height).toInt()
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        pdfPage.draw(canvas)
    }

    fun updatePage(pageSize: APage, newZoom: Float, crop: Boolean) {
        var isNew = false
        if (this.pageSize != pageSize) {
            this.pageSize = pageSize
            isNew = true
            pdfPage.setBounds(
                RectF(
                    0f,
                    0f,
                    pageSize.cropScaleWidth.toFloat(),
                    pageSize.cropScaleHeight.toFloat()
                )
            )
            pdfPage.update(this, pageSize)
            requestLayout()
        }
        mZoom = newZoom
        this.pageSize.zoom = newZoom

        val zoomSize = this.pageSize.zoomPoint
        val xOrigin = (zoomSize.x - this.pageSize.getTargetWidth()) / 2

        Logcat.d(
            String.format(
                "updatePage:isNew:%s,width-height:%s-%s, mZoom: %s, aPage:%s",
                isNew, pageSize.cropScaleWidth, pageSize.cropScaleHeight, mZoom, pageSize
            )
        )
        pdfPage.updateVisibility(crop, xOrigin)
    }
}
