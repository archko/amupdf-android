package cn.archko.pdf.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
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
    protected val mContext: Context,
    private val mupdfDocument: MupdfDocument?,
    private var aPage: APage,
    crop: Boolean,
) : ImageView(mContext) {

    private var mZoom: Float = 0.toFloat()
    private val mHandler: Handler = Handler()
    private val bitmapPaint = Paint()
    private val textPaint: Paint = textPaint()
    //private var task: DecodeTask? = null

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
        //task?.let { it.recycle = true }
        setImageBitmap(null)
        //mBitmap?.recycle()
        //mBitmap = null
        //isRecycle = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mwidth = aPage.getCropWidth()
        var mheight = aPage.getCropHeight()

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
                mwidth, mheight, aPage.effectivePagesWidth, aPage.effectivePagesHeight, mZoom, aPage
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (/*mBitmap == null &&*/ drawable == null) {
            canvas.drawText(
                String.format("Page %s", aPage.index + 1), (measuredWidth / 2).toFloat(),
                (measuredHeight / 2).toFloat(), textPaint
            )
        }
        super.onDraw(canvas)
    }

    fun updatePage(pageSize: APage, newZoom: Float, crop: Boolean) {
        //isRecycle = false
        val oldZoom = aPage.scaleZoom
        aPage = pageSize
        aPage.zoom = newZoom

        Logcat.d(
            String.format(
                "updatePage, oldZoom:%s, newScaleZoom:%s,newZoom:%s,",
                oldZoom, aPage.scaleZoom, newZoom
            )
        )

        /*if (null != mBitmap) {
            if (!mBitmap!!.isRecycled) {
                setImageBitmap(mBitmap)
                return
            }
        }

        task?.let { it.recycle = true }
        task = DecodeTask(pageSize, crop)

        decodeBitmap(task!!)*/
        var bmp = BitmapCache.getInstance()
            .getBitmap(ImageDecoder.getCacheKey(aPage.index, crop, aPage.scaleZoom))

        if (null != bmp) {
            setImageBitmap(bmp)
        } else {
            bmp = BitmapCache.getInstance()
                .getBitmap(ImageDecoder.getCacheKey(aPage.index, crop, oldZoom))
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("updatePage xOrigin: %s, oldZoom:%s, newZoom:%s, bmp:%s",
            //            xOrigin, oldZoom, newZoom, bmp));
            //}
            if (null != bmp) {
                setImageBitmap(bmp)
                return
            }
        }

        ImageDecoder.getInstance().loadImage(aPage, crop, 0, this, mupdfDocument?.document) { bitmap ->
            //if (Logcat.loggable) {
            //    Logcat.d(String.format("decode2 relayout bitmap:index:%s, %s:%s imageView->%s:%s",
            //            pageSize.index, bitmap.width, bitmap.height,
            //            getWidth(), getHeight()))
            //}
            setImageBitmap(bitmap)
        }
    }

    //override fun setImageBitmap(bm: Bitmap?) {
    //    super.setImageBitmap(bm)
    //    mBitmap = bm
    //}

    // =================== decode ===================
    //private var mBitmap: Bitmap? = null
    //private var isRecycle = false
    //private var crop: Boolean = false

    /*private fun decodeBitmap(task: DecodeTask) {
        AppExecutors.instance.diskIO().execute(Runnable { doDecode(task) })
    }

    private fun doDecode(task: DecodeTask) {
        val bm: Bitmap? = decode(task)
        if (bm != null && !isRecycle && task.pageSize.index == aPage.index) {
            mHandler.post { setImageBitmap(bm) }
        }
    }

    fun decode(task: DecodeTask): Bitmap? {
        //long start = SystemClock.uptimeMillis();
        val page: Page? = mupdfDocument?.loadPage(aPage.index)

        var leftBound = 0
        var topBound = 0
        val pageSize: APage = aPage
        var pageW = pageSize.zoomPoint.x
        var pageH = pageSize.zoomPoint.y

        val ctm = Matrix(MupdfDocument.ZOOM)
        val bbox = RectI(page?.bounds?.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
        ctm.scale(xscale, yscale)

        if (pageSize.getTargetWidth() > 0) {
            pageW = pageSize.getTargetWidth()
        }

        if (task.crop) {
            //if (pageSize.cropBounds != null) {
            //    leftBound = pageSize.cropBounds?.left?.toInt()!!
            //    topBound = pageSize.cropBounds?.top?.toInt()!!
            //    pageH = pageSize.cropBounds?.height()?.toInt()!!
            //} else {
            val arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
            leftBound = arr[0].toInt()
            topBound = arr[1].toInt()
            pageH = arr[2].toInt()
            val cropScale = arr[3]
            pageSize.setCropHeight(pageH)
            pageSize.setCropWidth(pageW)
            val cropRectf = RectF(
                leftBound.toFloat(), topBound.toFloat(),
                (leftBound + pageW).toFloat(), (topBound + pageH).toFloat()
            );
            pageSize.setCropBounds(cropRectf, cropScale)
            //}
        }

        if (Logcat.loggable) {
            Logcat.d(
                TAG, String.format(
                    "decode bitmap:isRecycle:%s, %s-%s,page:%s-%s, bound(left-top):%s-%s, page:%s",
                    task.recycle, pageW, pageH, pageSize.zoomPoint.x, pageSize.zoomPoint.y,
                    leftBound, topBound, pageSize
                )
            )
        }

        if (task.recycle) {
            Logcat.d(
                TAG, String.format("decode bitmap: ecycle:%s", task)
            )
            return null
        }

        val bitmap = BitmapPool.getInstance().acquire(pageW, pageH)
        //Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

        MupdfDocument.render(page, ctm, bitmap, 0, leftBound, topBound)

        page?.destroy()

        return bitmap
    }

    companion object {
        private val TAG: String = "APDFView"
    }

    class DecodeTask constructor(
        val pageSize: APage,
        val crop: Boolean = false,
        var recycle: Boolean = false

    ) {
        override fun toString(): String {
            return "DecodeTask(pageSize=$pageSize, crop=$crop, recycle=$recycle)"
        }
    }*/
}
