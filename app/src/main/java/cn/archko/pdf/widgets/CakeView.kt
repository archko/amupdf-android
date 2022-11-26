package cn.archko.pdf.widgets

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Region
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
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
    private var roundAngle = 360f
    private var isAddText = true
    private var mFontSize = 0f

    //private var mCenterBitMap: Bitmap? = null
    //private var mCenterWidth = 90
    private var lineList: MutableList<Array<PointF>> = ArrayList()
    private var textList: MutableList<PointF> = ArrayList()
    private lateinit var regionList: Array<Region?>
    private var centerRect: RectF? = null
    private var mCanvas: Canvas? = null
    private var H: Handler = Handler(Looper.getMainLooper())
    var viewOnclickListener: ViewOnclickListener? = null
    private var refeshNum = 0
    private var isRefesh = false

    /**
     * 检测按下到抬起时旋转的角度
     */
    private var mTmpAngle = 0f

    /**
     * 检测按下到抬起时使用的时间
     */
    private var mDownTime: Long = 0

    /**
     * 判断是否正在自动滚动
     */
    private var isFling = false

    /**
     * 整个View的旋转角度
     */
    private var rotationAngle = 0f
    private var start = 0f

    /**
     * 获得当前的角度
     */
    private var end = 0f

    /**
     * 每秒最大移动角度
     */
    private val mMax_Speed = 0

    /**
     * 如果移动角度达到该值，则屏蔽点击
     */
    private val mMin_Speed = 0

    /**
     * 判断是否正在自动滚动
     */
    private var isQuickMove = false
    private fun init(context: Context) {
        ctx = context
        format = DecimalFormat("##0.00")
        arcPaint = Paint()
        arcPaint!!.isAntiAlias = true
        arcPaint!!.isDither = true
        arcPaint!!.style = Paint.Style.STROKE
        //        this.arcPaint.setColor(Color.parseColor("#FFFFFF"));
        linePaint = Paint()
        linePaint!!.isAntiAlias = true
        linePaint!!.isDither = true
        linePaint!!.style = Paint.Style.STROKE
        //this.linePaint.setStrokeWidth(dip2px(ctx, 20));
        linePaint!!.color = Color.parseColor("#FFFFFF")
        textPaint = Paint()
        textPaint!!.isAntiAlias = true
        textPaint!!.isDither = true
        textPaint!!.textSize = 36f
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
        Log.e("宽高", "$width,$height")
        setMeasuredDimension(width + dpToPx(20f), height + dpToPx(20f))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = (w / 2).toFloat()
        centerY = (h / 2).toFloat()
        radius = Math.min(centerX, centerY) * 0.725f //0.725f
        arcPaint!!.strokeWidth = radius / 3 * 2
        //textPaint.setTextSize(radius / 10);
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        refeshNum++
        Log.e("onDraw--->>", "重绘次数=$refeshNum")
        textList.clear()
        lineList.clear()
        mCanvas = canvas
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
                /*if (mCenterBitMap != null) {
                    centerRect = RectF(
                        centerX - mCenterWidth / 2,
                        centerY - mCenterWidth / 2,
                        centerX + mCenterWidth / 2,
                        centerY + mCenterWidth / 2
                    )
                    canvas.drawBitmap(mCenterBitMap!!, null, centerRect!!, null)
                }*/
                if (isAddText) {
                    lineList.add(getLinePointFs(startAngle)) //获取直线 开始坐标 结束坐标
                    textAngle = startAngle + mList[i].percent / total * roundAngle / 2
                    textList.add(getTextPointF(textAngle)) //获取文本文本
                }
                startAngle += mList[i].percent / total * roundAngle
            }

//            drawSpacingLine(canvas, lineList);
            // 绘制文字
//            drawText(canvas);
        }
        if (roundAngle < 360f) {
            H.postDelayed({
                roundAngle += 10f
                if (roundAngle == 360f - 10f) {
                    isAddText = true
                }
            }, 0)
        } else {
            // 绘制间隔空白线
            drawSpacingLine(canvas, lineList)
            // 绘制文字
            drawText(canvas)
        }
        if (!isRefesh || isFling) {
            postInvalidate()
        }
        isRefesh = !isRefesh
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
            (centerX + (radius + arcPaint!!.strokeWidth) * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val stopY =
            (centerY + (radius + arcPaint!!.strokeWidth) * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        val startX =
            (centerX + (radius - arcPaint!!.strokeWidth) * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val startY =
            (centerY + (radius - arcPaint!!.strokeWidth) * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        val startPoint = PointF(startX, startY)
        val stopPoint = PointF(stopX, stopY)
        return arrayOf(startPoint, stopPoint)
    }

    /**
     * 画间隔线
     *
     * @param canvas
     */
    private fun drawSpacingLine(canvas: Canvas?, pointFs: List<Array<PointF>>?) {
        canvas?.run {
            for (fp in pointFs!!) {
                this.drawLine(fp[0].x, fp[0].y, fp[1].x, fp[1].y, linePaint!!)
            }
        }
    }

    /**
     * 画文本
     *
     * @param canvas
     */
    private fun drawText(canvas: Canvas) {
        regionList = arrayOfNulls(textList.size)
        for (i in textList.indices) {
            textPaint!!.textAlign = Paint.Align.CENTER
            val text = mList[i].content
            canvas.drawText(text!!, textList[i].x, textList[i].y, textPaint!!)
            //Paint.FontMetrics fm = textPaint.getFontMetrics();
            //canvas.drawText(format.format(mList.get(i).percent * 100 / total) + "%", textList.get(i).x, textList.get(i).y + (fm.descent - fm.ascent), textPaint);
            // 设置绘制图片的区域
            // 设为默认位置
            // todo 计算默认方位有错，自行修改
            /*Rect rect = new Rect((int) (textList.get(i).x - (bitmap.getWidth() / 2)),
                    (int) (textList.get(i).y) - (bitmap.getHeight() * 5 / 4 - dip2px(getContext(), 20)),
                    (int) textList.get(i).x + (bitmap.getWidth() / 2),
                    (int) textList.get(i).y - (bitmap.getHeight() / 4) + dip2px(getContext(), 20));*/

            val re = Region();
            val path = Path();
            path.moveTo(
                (textList.get(i).x - dip2px(getContext(), 35f)),
                textList.get(i).y - dip2px(getContext(), 35f)
            );
            path.lineTo(
                (textList.get(i).x + dip2px(getContext(), 35f)),
                textList.get(i).y - dip2px(getContext(), 35f)
            );
            path.lineTo(
                (textList.get(i).x + dip2px(getContext(), 35f)),
                textList.get(i).y + dip2px(getContext(), 35f)
            );
            path.lineTo(
                (textList.get(i).x - dip2px(getContext(), 35f)),
                textList.get(i).y + dip2px(getContext(), 35f)
            );
            path.close();

            val r = RectF();
            //计算控制点的边界
            path.computeBounds(r, true);
            //设置区域路径和剪辑描述的区域
            re.setPath(
                path, Region(
                    r.left.toInt(), r.top.toInt(), r.right.toInt(),
                    r.bottom.toInt()
                )
            );
            regionList[i] = re;
            //canvas.drawBitmap(bitmap, null, rect, null);
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
     * 设置间隔线的宽度
     *
     * @param width
     */
    fun setSpacingLineWidth(width: Float) {
        linePaint!!.strokeWidth = width
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
     * 设置中心颜色
     *
     * @param color
     */
    fun setCenterColor(color: Int) {
        arcPaint!!.color = color
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
     * 设置标签文字大小
     *
     * @param mFontSize
     */
    fun setFontSize(mFontSize: Float) {
        this.mFontSize = mFontSize
    }

    /**
     * 设置中心图标
     *
     * @param mCenterBitMap
     */
    fun setCenterBitMap(mCenterBitMap: Bitmap?) {
        //this.mCenterBitMap = mCenterBitMap
    }

    /**
     * 设置中心图标宽度
     *
     * @param mCenterWidth
     */
    fun setCenterWidth(mCenterWidth: Int) {
        //this.mCenterWidth = mCenterWidth
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.e(TAG, "ACTION_DOWN$isClick")
                mLastX = x
                mLastY = y
                mDownTime = System.currentTimeMillis()
                mTmpAngle = 0f

                // 如果当前已经在快速滚动
                /*if (isQuickMove) {
                    // 移除快速滚动的回调
                    removeCallbacks(mFlingRunnable)
                    isQuickMove = false
                    return true
                }*/
                isClick = true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.e(TAG, "ACTION_MOVE$isClick")
                isClick = false
                /*start = getAngle(mLastX, mLastY)
                end = getAngle(x, y)
                Log.e(TAG, "$start =start $end ,  =end")

                //  二、三象限，色角度值是付值
                if (getQuadrant(x, y) == 3 || getQuadrant(x, y) == 2) {
                    rotationAngle += start - end
                    mTmpAngle += start - end
                } else {
                    //如果是一、四象限，则直接end-start，角度值都是正值
                    rotationAngle += end - start
                    mTmpAngle += end - start
                }*/

                // 重新布局
//                postInvalidate();
                check
            }
            MotionEvent.ACTION_UP -> {
                Log.e(TAG, "ACTION_UP$isClick")
                // 获取每秒移动的角度
                /*val anglePerSecond = mTmpAngle * 1000 / (System.currentTimeMillis() - mDownTime)
                // 如果达到最大速度
                if (Math.abs(anglePerSecond) > mMax_Speed && !isQuickMove) {
                    // 惯性滚动
                    if (Math.abs(start - end) >= 1) { //放手时大于1度才惯性滚动
                        post(AngleRunnable(anglePerSecond).also { mFlingRunnable = it })
                    }
                    return true
                }

                // 如果当前旋转角度超过minSpeed屏蔽点击
                if (Math.abs(mTmpAngle) > mMin_Speed) {
                    return true
                }*/
                if (!isQuickMove) {
                    Log.e(TAG, "ACTION_UP-点击")
                    var i = 0
                    while (i < regionList.size) {
                        val rect = regionList[i]
                        if (null != rect && rect.contains(x.toInt(), y.toInt())) {
                            if (viewOnclickListener != null) {
                                viewOnclickListener!!.onViewClick(this, i)
                            }
                        }
                        i++
                    }
                    if (centerRect != null) {
                        if (centerRect!!.contains(x.toInt().toFloat(), y.toInt().toFloat())) {
                            if (viewOnclickListener != null) {
                                viewOnclickListener!!.onViewCenterClick()
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    private val check: Unit
        get() {
            rotationAngle %= 360f
            rotation = rotationAngle
        }

    private fun distanceTwoPointF(A: PointF, B: PointF): Double {
        val disX = (Math.abs(A.x) - Math.abs(B.x)).toDouble()
        val disY = (Math.abs(A.y) - Math.abs(B.y)).toDouble()
        return Math.sqrt(disX * disX + disY * disY)
    }

    interface ViewOnclickListener {
        fun onViewClick(v: View?, position: Int)
        fun onViewCenterClick()
    }

    /**
     * 记录上一次的x，y坐标
     */
    private var mLastX = 0f
    private var mLastY = 0f

    /**
     * 移动转盘
     */
    private var isClick = false

    /**
     * 自动滚动的Runnable
     */
    private var mFlingRunnable: AngleRunnable? = null

    /**
     * 当每秒移动角度达到该值时，认为是快速移动
     */
    private val mFlingableValue = FLINGABLE_VALUE

    /**
     * 根据触摸的位置，计算角度
     *
     * @param xTouch
     * @param yTouch
     * @return
     */
    private fun getAngle(xTouch: Float, yTouch: Float): Float {
        val x = xTouch - width / 2.0
        val y = yTouch - height / 2.0
        return (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI).toFloat()
    }

    /**
     * 根据当前位置计算象限
     *
     * @param x
     * @param y
     * @return
     */
    private fun getQuadrant(x: Float, y: Float): Int {
        val tmpX = (x - width / 2).toInt()
        val tmpY = (y - height / 2).toInt()
        return if (tmpX >= 0) {
            if (tmpY >= 0) 4 else 1
        } else {
            if (tmpY >= 0) 3 else 2
        }
    }

    /**
     * 惯性滚动
     */
    private inner class AngleRunnable(private var angelPerSecond: Float) : Runnable {
        override fun run() {
            //小于20停止
            if (Math.abs(angelPerSecond).toInt() < 20) {
                isQuickMove = false
                return
            }
            isQuickMove = true
            // 滚动时候不断修改滚动角度大小
            rotationAngle += angelPerSecond / 30
            //逐渐减小这个值
            angelPerSecond /= 1.0666f
            postDelayed(this, 30)
            // 重新布局
            check
        }
    }

    /**
     * 自动滚动的任务
     *
     * @author zhy
     */
    private inner class AutoFlingRunnable(private var angelPerSecond: Float, flg: Float) :
        Runnable {
        private val flg: Int
        override fun run() {
            // 如果小于20,则停止
            if (Math.abs(angelPerSecond).toInt() < 20) {
                isFling = false
                return
            }
            isFling = true
            // 不断改变mStartAngle，让其滚动，/30为了避免滚动太快
            startAngle += flg * angelPerSecond / 10
            // 逐渐减小这个值
            angelPerSecond /= 1.0666f
            // 绘制间隔空白线
            drawSpacingLine(mCanvas, lineList)
            postDelayed(this, 30)
            // 重新布局
            requestLayout()
        }

        init {
            this.flg = flg.toInt()
        }
    }

    companion object {
        fun dpToPx(dpValue: Float): Int {
            val pxValue = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                Resources.getSystem().displayMetrics
            )
            return pxValue.toInt()
        }

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
            context.display?.getMetrics(outMetrics)
            return intArrayOf(outMetrics.widthPixels, outMetrics.heightPixels)
        }

        /**
         * 当每秒移动角度达到该值时，认为是快速移动
         */
        private const val FLINGABLE_VALUE = 300

        /**
         * 如果移动角度达到该值，则屏蔽点击
         */
        private const val NOCLICK_VALUE = 3
        private const val TAG = "CakeView"
    }

    init {
        init(context)
    }
}