package cn.archko.pdf.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import cn.archko.pdf.core.entity.DrawType

/**
 * 绘图预览View
 * 可以绘制直线或曲线，可以自定义颜色、线宽
 */
class DrawPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()

    var lineColor: Int = Color.RED
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    var lineWidth: Float = 4f
        set(value) {
            field = value
            paint.strokeWidth = value
            invalidate()
        }

    var drawType: DrawType = DrawType.LINE
        set(value) {
            field = value
            invalidate()
        }

    init {
        // 从XML属性中读取自定义属性（如果有的话）
        attrs?.let {
            val a = context.obtainStyledAttributes(it, intArrayOf())
            try {
                // 可以在这里添加自定义属性
            } finally {
                a.recycle()
            }
        }
        paint.color = lineColor
        paint.strokeWidth = lineWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (drawType) {
            DrawType.LINE -> drawLine(canvas)
            DrawType.CURVE -> drawCurve(canvas)
            DrawType.CIRCLE -> drawCircle(canvas)
        }
    }

    private fun drawLine(canvas: Canvas) {
        val drawWidth = width - paddingLeft - paddingRight
        val drawHeight = height - paddingTop - paddingBottom
        val centerY = paddingTop + drawHeight / 2f
        val linePadding = maxOf(lineWidth * 2f, 4f)
        val startX = paddingLeft + linePadding
        val endX = paddingLeft + drawWidth - linePadding

        canvas.drawLine(startX, centerY, endX, centerY, paint)
    }

    private fun drawCurve(canvas: Canvas) {
        val drawWidth = width - paddingLeft - paddingRight
        val drawHeight = height - paddingTop - paddingBottom
        val centerY = paddingTop + drawHeight / 2f
        val linePadding = maxOf(lineWidth * 2f, 4f)
        val startX = paddingLeft + linePadding
        val endX = paddingLeft + drawWidth - linePadding
        val controlY = centerY - drawHeight * 0.3f

        path.reset()
        path.moveTo(startX, centerY)
        path.quadTo((startX + endX) / 2, controlY, endX, centerY)

        canvas.drawPath(path, paint)
    }

    private fun drawCircle(canvas: Canvas) {
        val drawWidth = width - paddingLeft - paddingRight
        val drawHeight = height - paddingTop - paddingBottom
        val centerX = paddingLeft + drawWidth / 2f
        val centerY = paddingTop + drawHeight / 2f
        val linePadding = maxOf(lineWidth * 2f, 4f)
        val radius = minOf(drawWidth, drawHeight) / 2f - linePadding

        val originalStyle = paint.style
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.style = originalStyle
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = 40.dpToPx()

        val width = resolveSizeAndState(
            defaultSize + paddingLeft + paddingRight,
            widthMeasureSpec,
            0
        )

        val height = resolveSizeAndState(
            defaultSize + paddingTop + paddingBottom,
            heightMeasureSpec,
            0
        )

        setMeasuredDimension(width, height)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}