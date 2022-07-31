package cn.archko.pdf.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import java.text.DecimalFormat

private const val fl = 360f

/**
 * @author: archko 2022/7/30 :08:13
 */
class CakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var ctx: Context? = null
    private var format: DecimalFormat? = null
    private var mList: MutableList<BaseMenu> = ArrayList()
    private var arcPaint: Paint? = null
    private var linePaint: Paint? = null
    private var textPaint: Paint? = null
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var total = 0f
    private var startAngle = 0f
    private var textAngle = 0f
    private var roundAngle = 0f
    private var isAddText = false
    private var lineList: MutableList<Array<PointF>> = ArrayList()
    private var textList: MutableList<PointF> = ArrayList()
    private var H: Handler = Handler(Looper.getMainLooper())

    init {
        init(context)
    }

    private fun init(context: Context) {
        ctx = context
        format = DecimalFormat("##0.00")

        arcPaint = Paint()
        arcPaint!!.isAntiAlias = true
        arcPaint!!.isDither = true
        arcPaint!!.style = Paint.Style.STROKE

        linePaint = Paint()
        linePaint!!.isAntiAlias = true
        linePaint!!.isDither = true
        linePaint!!.style = Paint.Style.STROKE
        linePaint!!.strokeWidth = dip2px(ctx, 1f).toFloat()
        linePaint!!.color = Color.parseColor("#FFFFFF")

        textPaint = Paint()
        textPaint!!.isAntiAlias = true
        textPaint!!.isDither = true
        textPaint!!.style = Paint.Style.FILL
        textPaint!!.color = Color.parseColor("#FFFFFF")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width: Int
        val height: Int
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
            height = heightSpecSize
            width = Math.min(heightSpecSize, Math.min(getScreenSize(ctx)[0], getScreenSize(ctx)[1]))
        } else if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.AT_MOST) {
            width = widthSpecSize
            height = Math.min(widthSpecSize, Math.min(getScreenSize(ctx)[0], getScreenSize(ctx)[1]))
        } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            height = Math.min(getScreenSize(ctx)[0], getScreenSize(ctx)[1])
            width = height
        } else {
            width = widthSpecSize
            height = heightSpecSize
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w * 1f / 2
        centerY = h * 1f / 2
        radius = Math.min(centerX, centerY) * 0.725f
        arcPaint!!.strokeWidth = radius / 3 * 2
        textPaint!!.textSize = radius / 7
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        textList.clear()
        lineList.clear()
        if (mList.isNotEmpty()) {
            val rectF =
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            for (i in mList.indices) {
                arcPaint!!.color = mList[i].color
                canvas.drawArc(
                    rectF,
                    startAngle,
                    mList[i].percent / total * roundAngle,
                    false,
                    arcPaint!!
                )
                if (isAddText) {
                    lineList.add(getLinePointFs(startAngle)) //获取直线 开始坐标 结束坐标
                    textAngle = startAngle + mList[i].percent / total * roundAngle / 2
                    textList.add(getTextPointF(textAngle)) //获取文本文本
                }
                startAngle += mList[i].percent / total * roundAngle
            }
            //绘制间隔空白线
            // drawSpacingLine(canvas, lineList);
            //绘制文字
            //drawText(canvas);
        }
        if (roundAngle < 360f) {
            H.postDelayed({
                roundAngle += 120f
                if (roundAngle == 360f - 120f) {
                    isAddText = true
                }
                postInvalidate()
            }, 100)
        } else {
            // 绘制间隔空白线
            drawSpacingLine(canvas, lineList)
            // 绘制文字
            drawText(canvas)
            H.removeCallbacksAndMessages(null)
        }
    }

    /**
     * 获取文本文本
     *
     * @return
     */
    private fun getTextPointF(angle: Float): PointF {
        val textPointX = (centerX + radius * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val textPointY = (centerY + radius * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        return PointF(textPointX, textPointY)
    }

    /**
     * 获取直线 开始坐标 结束坐标
     */
    private fun getLinePointFs(angle: Float): Array<PointF> {
        val stopX =
            (centerX + (radius + arcPaint!!.strokeWidth / 2) * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val stopY =
            (centerY + (radius + arcPaint!!.strokeWidth / 2) * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        val startX =
            (centerX + (radius - arcPaint!!.strokeWidth / 2) * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val startY =
            (centerY + (radius - arcPaint!!.strokeWidth / 2) * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        val startPoint = PointF(startX, startY)
        val stopPoint = PointF(stopX, stopY)
        return arrayOf(startPoint, stopPoint)
    }

    /**
     * 画间隔线
     *
     * @param canvas
     */
    private fun drawSpacingLine(canvas: Canvas, pointFs: List<Array<PointF>>?) {
        for (fp in pointFs!!) {
            canvas.drawLine(fp[0].x, fp[0].y, fp[1].x, fp[1].y, linePaint!!)
        }
    }

    /**
     * 画文本
     *
     * @param canvas
     */
    private fun drawText(canvas: Canvas) {
        for (i in textList.indices) {
            textPaint!!.textAlign = Paint.Align.CENTER
            val text = mList[i].content
            canvas.drawText(text!!, textList[i].x, textList[i].y, textPaint!!)
            /*val fm = textPaint!!.fontMetrics
            canvas.drawText(
                format!!.format((mList[i].percent * 100 / total).toDouble()) + "%",
                textList[i].x,
                textList[i].y + (fm.descent - fm.ascent),
                textPaint!!
            )*/
        }
    }

    /**
     * 设置间隔线的颜色
     *
     * @param color
     */
    fun setSpacingLineColor(color: Int) {
        linePaint!!.color = color
    }

    /**
     * 设置文本颜色
     *
     * @param color
     */
    fun setTextColor(color: Int) {
        textPaint!!.color = color
    }

    /**
     * 设置开始角度
     *
     * @param startAngle
     */
    fun setStartAngle(startAngle: Float) {
        this.startAngle = startAngle
    }

    /**
     * 设置饼的宽度
     *
     * @param width
     */
    fun setCakeStrokeWidth(width: Int) {
        arcPaint!!.strokeWidth = dip2px(ctx, width.toFloat()).toFloat()
    }

    /**
     * 设置饼的数据
     *
     * @param list
     */
    fun setCakeData(list: MutableList<BaseMenu>?) {
        total = 0f
        if (list == null) {
            return
        }
        for (i in list.indices) {
            total += list[i].percent
        }
        mList.clear()
        mList.addAll(list)
        invalidate()
    }

    companion object {
        /**
         * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
         */
        fun dip2px(context: Context?, dpValue: Float): Int {
            val scale = context!!.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }

        /**
         * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
         */
        fun px2dip(context: Context, pxValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }

        fun getScreenSize(context: Context?): IntArray {
            val wm = context!!.getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager
            val outMetrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(outMetrics)
            return intArrayOf(outMetrics.widthPixels, outMetrics.heightPixels)
        }
    }
}