package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.decode.DecodeParam
import cn.archko.pdf.core.decode.DecodeCallback
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel

/**
 * @author: archko 2018/7/25 :12:43
 */
@SuppressLint("AppCompatCustomView")
class APDFView(
    private var ctx: Context,
    private var pdfViewModel: PDFViewModel,
) : ImageView(ctx) {

    private val textPaint: Paint = textPaint()
    private val strokePaint: Paint = strokePaint()
    private var resultWidth: Int = 1080
    private var resultHeight: Int = 1080
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
        strokePaint.color = Color.BLACK
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1f
        return strokePaint
    }

    fun recycle() {
        setImageBitmap(null)
        index = -1
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (null == aPage) {
            return setMeasuredDimension(resultWidth, resultHeight)
        }
        var mwidth = resultWidth
        var mheight = resultHeight

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

    private fun generateCacheKey(index: Int, w: Int, h: Int, crop: Boolean): String {
        return String.format("%s-%s-%s-%s", index, w, h, crop)
    }

    fun updatePage(
        pageSize: APage?,
        orientation: Int,
        crop: Boolean,
        screenWidth: Int,
        defaultHeight: Int,
    ) {
        aPage = pageSize
        if (aPage == null && pdfViewModel.isDestroy) {
            return
        }
        index = pageSize!!.index

        resultWidth = screenWidth
        resultHeight = defaultHeight
        caculateWidth(orientation, crop)
        //setLayoutSize()
        Log.d(
            "TAG",
            String.format(
                "updatePage.page:%s, size w-h:%s-%s",
                pageSize!!.index,
                resultWidth,
                resultHeight
            )
        )

        val key = generateCacheKey(aPage!!.index, resultWidth, resultHeight, crop)
        cacheKey = key
        val bmp = BitmapCache.getInstance().getBitmap(key)

        if (null != bmp) {
            Log.d("TAG", String.format("hit the cache:%s", aPage?.index))
            updateImage(this, bmp, resultWidth, resultHeight, aPage!!.index)
            return
        }

        //aPage 这个如果当参数传递,由于复用机制,后面的页面更新后会把它覆盖,导致解码并不是原来那个
        //这里应该传递高宽值
        val decodeParam = DecodeParam(
            key,
            this,
            crop,
            0,
            aPage!!,
            pdfViewModel.mupdfDocument,
            callback,
            resultWidth,
            resultHeight
        )
        AppExecutors.instance.diskIO().execute {
            MupdfDocument.decode(aPage!!, decodeParam)
        }
    }

    private val callback = object : DecodeCallback {
        override fun decodeComplete(bitmap: Bitmap?, param: DecodeParam) {
            if (param.pageNum == index) {
                updateImage(this@APDFView, bitmap!!, width, height, param.pageNum)
            } else {
                Log.d(
                    "TAG",
                    String.format(
                        "decode callback:cancel:%s-%s, decode.page:%s, key:%s, param:%s",
                        param.pageNum, index, param.pageNum, cacheKey, param.key
                    )
                )
            }
        }

        override fun shouldRender(index: Int, param: DecodeParam): Boolean {
            return this@APDFView.index == index
        }
    }

    private fun caculateWidth(orientation: Int, crop: Boolean) {
        if (orientation == LinearLayoutManager.VERTICAL) {//垂直方向,以宽为准
            resultHeight = /*if (crop && aPage!!.cropBounds != null) {
                (resultWidth * aPage!!.cropBounds!!.height() / aPage!!.width).toInt()
            } else {*/
                (resultWidth * aPage!!.height / aPage!!.width).toInt()
            //}
        } else {    //水平滚动,以高为准
            resultWidth = /*if (crop && aPage!!.cropBounds != null) {
                (resultHeight * aPage!!.cropBounds!!.width() / aPage!!.height).toInt()
            } else {*/
                (resultHeight * aPage!!.width / aPage!!.height).toInt()
            //}
        }
    }

    private fun updateImage(view: ImageView, bitmap: Bitmap, width: Int, height: Int, index: Int) {
        val bmpWidth = bitmap.width
        val bmpHeight = bitmap.height
        var lp = view.layoutParams as ARecyclerView.LayoutParams?
        if (null == lp) {
            lp = ARecyclerView.LayoutParams(width, height)
            view.layoutParams = lp
        } else {
            //解码的页面与实际显示的高宽不一定一样,有些细微差别,不必重新布局测量
            //而重新测量布局是在旋转屏幕时比较重要
            if (Math.abs(bmpWidth - lp.width) > 5 || Math.abs(bmpHeight - lp.height) > 5) {
                Log.d(
                    "TAG",
                    String.format(
                        "draw:position:%s, view.w:%s-h:%s, bitmap.w::%s-h:%s, lp.w:%s-%s",
                        index,
                        width,
                        height,
                        bmpWidth,
                        bmpHeight,
                        lp.width,
                        lp.height
                    )
                )
                lp.width = bmpWidth
                lp.height = bmpHeight
                view.layoutParams = lp
            }
        }
        view.setImageBitmap(bitmap)
    }

    fun getCacheKey(): String? {
        return cacheKey
    }

    // =================== decode ===================

    companion object {
        private val TAG: String = "APDFView"
    }
}
