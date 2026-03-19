package cn.archko.pdf.core.component

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.entity.Offset
import com.archko.reader.pdf.component.Horizontal
import com.archko.reader.pdf.component.IntSize
import com.archko.reader.pdf.component.Vertical
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

private const val MAX_ZOOM = 30f

/**
 * 文档视图 - Android View 版本
 * 用于显示和交互 PDF 文档
 * 
 * 核心功能：
 * - 缩放
 * - 惯性滑动
 * - 页面布局
 */
class DocumentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 核心状态
    var viewWidth: Int = 0
    var viewHeight: Int = 0
    
    var viewOffset: Offset = Offset(0f, 0f)
        set(value) {
            field = value
            invalidate()
        }
    
    var vZoom: Float = 1f
        set(value) {
            val clamped = value.coerceIn(1f, MAX_ZOOM)
            if (field != clamped) {
                field = clamped
                pageViewState?.updateViewSize(IntSize(viewWidth, viewHeight), vZoom, orientation)
                invalidate()
            }
        }
    
    var orientation: Int = Vertical
        set(value) {
            if (field != value) {
                field = value
                pageViewState?.updateViewSize(IntSize(viewWidth, viewHeight), vZoom, value)
                invalidate()
            }
        }
    
    var toPage: Int = -1
    var isJumping: Boolean = false
    
    // PageViewState
    var pageViewState: PageViewState? = null
        set(value) {
            field = value
            if (value != null) {
                value.onPageLinkClick = { pageIndex ->
                    isJumping = true
                    val targetPage = value.pages.getOrNull(pageIndex)
                    if (targetPage != null) {
                        viewOffset = Offset(viewOffset.x, -targetPage.bounds.top)
                        value.updateOffset(viewOffset)
                    }
                    isJumping = false
                }
            }
        }
    
    // 协程作用域
    private val viewScope = CoroutineScope(Dispatchers.Main)
    

    
    // 手势检测器
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private val scroller: OverScroller

    // Fling 动画相关
    private var flingRunnable: FlingRunnable? = null

    // 回调
    var onPageChanged: ((page: Int) -> Unit)? = null
    var onSaveDocument: ((page: Int, pageCount: Int, zoom: Double, scrollX: Long, scrollY: Long, scrollOri: Long, reflow: Long, crop: Long) -> Unit)? = null
    var onCloseDocument: (() -> Unit)? = null
    
    // 跳转相关
    var jumpToPage: Int? = null
    var jumpOffsetY: Float? = null
    
    // 重新流动/切边相关
    var reflow: Long = 0L
    var crop: Boolean = false
    
    // 搜索相关
    //var searchHighlightQuads: Map<Int, List<cn.archko.pdf.core.entity.DocQuad>> = emptyMap()
    
    // 解码器相关
    var decoder: ImageDecoder? = null
    
    // 朗读相关
    var speakingPageIndex: Int? = null
    
    // 初始化相关
    var initialOrientation: Int = Vertical
    var initialScrollX: Long = 0L
    var initialScrollY: Long = 0L
    var initialZoom: Double = 1.0
    
    init {
        gestureDetector = GestureDetector(context, DocumentGestureListener())
        scaleGestureDetector = ScaleGestureDetector(context, DocumentScaleListener())
        scroller = OverScroller(context)
        
        // 启用硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        
        pageViewState?.updateViewSize(IntSize(w, h), vZoom, orientation)
        pageViewState?.updateVisiblePages(viewOffset, IntSize(w, h), vZoom)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pvs = pageViewState ?: return

        // 保存画布状态
        canvas.save()

        // 应用平移
        canvas.translate(viewOffset.x, viewOffset.y)

        // 绘制可见页面
        pvs.drawVisiblePages(canvas, viewOffset, vZoom)

        // 恢复画布状态
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        
        if (event.pointerCount > 1) {
            // 缩放手势由 ScaleGestureDetector 处理
            return true
        }
        
        return gestureDetector.onTouchEvent(event)
    }
    
    // 手势监听器
    inner class DocumentGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            flingRunnable?.cancelFling()
            flingRunnable = null
            return true
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleClick(e)
            return true
        }
        
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // distanceX/distanceY 是正数表示手指向右/下滑动
            // 但文档应该向相反方向移动，所以减去 distance
            val newOffsetX = viewOffset.x - distanceX
            val newOffsetY = viewOffset.y - distanceY
            val newOffset = Offset(newOffsetX, newOffsetY)

            viewOffset = calculateBounds(newOffset, vZoom, pageViewState, orientation)
            pageViewState?.updateOffset(viewOffset)
            return true
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            performFling(velocityX, velocityY)
            return true
        }
    }
    
    // 缩放手势监听器
    inner class DocumentScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var scaleFactor = 1.0f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            val newZoom = (vZoom * scaleFactor).coerceIn(1f, MAX_ZOOM)
            val zoomFactor = newZoom / vZoom

            // 计算居中偏移
            val centerOffsetX = if (orientation == Horizontal &&
                (pageViewState?.totalWidth ?: 0f) < viewWidth) {
                (viewWidth - (pageViewState?.totalWidth ?: 0f)) / 2
            } else 0f

            val centerOffsetY = if (orientation == Vertical &&
                (pageViewState?.totalHeight ?: 0f) < viewHeight) {
                (viewHeight - (pageViewState?.totalHeight ?: 0f)) / 2
            } else 0f

            // 计算缩放中心点
            val contentCenterX = detector.focusX - viewOffset.x - centerOffsetX
            val contentCenterY = detector.focusY - viewOffset.y - centerOffsetY

            // 计算新的偏移量
            val newOffsetX = detector.focusX - contentCenterX * zoomFactor - centerOffsetX
            val newOffsetY = detector.focusY - contentCenterY * zoomFactor - centerOffsetY

            vZoom = newZoom
            val targetOffset = Offset(newOffsetX, newOffsetY)
            viewOffset = calculateBounds(targetOffset, vZoom, pageViewState, orientation)

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleFactor = 1.0f
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            // 缩放结束后，确保偏移量在边界内
            pageViewState?.updateOffset(viewOffset)
            invalidate()
        }
    }
    
    private fun handleClick(e: MotionEvent) {
        val tapPos = Offset(e.x, e.y)
        
        val pvs = pageViewState ?: return
        val linkHandled = pvs.handleClick(
            tapPos.x - viewOffset.x,
            tapPos.y - viewOffset.y
        )
        
        if (!linkHandled) {
            // 处理翻页
            handlePageTurn(tapPos)
        }
    }
    
    private fun handlePageTurn(tapPos: Offset) {
        // TODO: 实现点击翻页逻辑
        // 点击屏幕左侧向前翻页,点击右侧向后翻页
        val pvs = pageViewState ?: return
        val tapX = tapPos.x
        val viewWidthThird = viewWidth / 3f
        
        if (orientation == Vertical) {
            // 纵向模式
            if (tapX < viewWidthThird) {
                // 点击左上角,向上滚动一屏
                val scrollAmount = viewHeight.toFloat()
                val newY = (viewOffset.y + scrollAmount).coerceAtMost(0f)
                viewOffset = Offset(viewOffset.x, newY)
                pvs.updateOffset(viewOffset)
            } else if (tapX > viewWidth * 2 / 3) {
                // 点击右上角,向下滚动一屏
                val scrollAmount = viewHeight.toFloat()
                val contentHeight = pvs.totalHeight * (vZoom / pvs.vZoom)
                val minY = if (contentHeight > viewHeight) viewHeight - contentHeight else 0f
                val newY = (viewOffset.y - scrollAmount).coerceAtLeast(minY)
                viewOffset = Offset(viewOffset.x, newY)
                pvs.updateOffset(viewOffset)
            }
        } else {
            // 横向模式
            if (tapX < viewWidthThird) {
                // 点击左侧,向右滚动一屏
                val scrollAmount = viewWidth.toFloat()
                val newX = (viewOffset.x + scrollAmount).coerceAtMost(0f)
                viewOffset = Offset(newX, viewOffset.y)
                pvs.updateOffset(viewOffset)
            } else if (tapX > viewWidth * 2 / 3) {
                // 点击右侧,向左滚动一屏
                val scrollAmount = viewWidth.toFloat()
                val contentWidth = pvs.totalWidth * (vZoom / pvs.vZoom)
                val minX = if (contentWidth > viewWidth) viewWidth - contentWidth else 0f
                val newX = (viewOffset.x - scrollAmount).coerceAtLeast(minX)
                viewOffset = Offset(newX, viewOffset.y)
                pvs.updateOffset(viewOffset)
            }
        }
    }
    
    private fun performFling(velocityX: Float, velocityY: Float) {
        val pvs = pageViewState ?: return

        // 限制最大速度
        val maxVelocity = 15000f
        val clampedVX = velocityX.coerceIn(-maxVelocity, maxVelocity)
        val clampedVY = velocityY.coerceIn(-maxVelocity, maxVelocity)

        val minX: Int
        val minY: Int
        val maxX: Int
        val maxY: Int

        // contentWidth/contentHeight 是缩放后文档的实际尺寸
        val contentWidth = if (orientation == Vertical) {
            viewWidth * vZoom
        } else {
            pvs.totalWidth * vZoom
        }
        val contentHeight = if (orientation == Vertical) {
            pvs.totalHeight * vZoom
        } else {
            viewHeight * vZoom
        }

        // 当内容小于视图时，不限制滚动范围（固定在顶部/左侧）
        // 当内容大于视图时，offset 从 -(content - view) 限制到 0
        minX = if (contentWidth > viewWidth) -(contentWidth - viewWidth).toInt() else 0
        minY = if (contentHeight > viewHeight) -(contentHeight - viewHeight).toInt() else 0
        maxX = 0
        maxY = 0

        // 使用 FlingRunnable 处理 fling 动画
        flingRunnable = FlingRunnable()
        flingRunnable?.fling(
            viewOffset.x.toInt(),
            viewOffset.y.toInt(),
            clampedVX.toInt(),
            clampedVY.toInt(),
            minX, maxX, minY, maxY
        )
        post(flingRunnable)
    }
    
    private fun calculateBounds(
        targetOffset: Offset,
        currentZoom: Float,
        pvs: PageViewState?,
        ori: Int
    ): Offset {
        val pageViewState = pvs ?: return targetOffset

        // 由于 vZoom 的 setter 会实时更新 pageViewState.vZoom 和 totalWidth/totalHeight
        // 所以这里 pageViewState.totalWidth/totalHeight 已经是基于 currentZoom 计算的了
        // 但为了保险起见，还是用 scaleRatio 来调整（应该接近 1）
        val scaleRatio = currentZoom / pageViewState.vZoom

        val contentWidth = if (ori == Vertical) {
            viewWidth * currentZoom
        } else {
            pageViewState.totalWidth * scaleRatio
        }
        val contentHeight = if (ori == Vertical) {
            pageViewState.totalHeight * scaleRatio
        } else {
            viewHeight * currentZoom
        }

        // offset 是负值表示向右/下偏移文档
        // 当内容大于视图时，offset 应该在 [-(content - view), 0] 之间
        // 当内容小于视图时，offset 固定为 0
        val minX = if (contentWidth > viewWidth) -(contentWidth - viewWidth) else 0f
        val minY = if (contentHeight > viewHeight) -(contentHeight - viewHeight) else 0f

        return Offset(
            targetOffset.x.coerceIn(minX, 0f),
            targetOffset.y.coerceIn(minY, 0f)
        )
    }
    
    // 公共方法
    
    fun initialize(
        list: List<APage>,
        decoder: ImageDecoder,
        initialScrollX: Long,
        initialScrollY: Long,
        initialZoom: Double,
        initialOrientation: Int,
        crop: Boolean,
        columnCount: Int,
        annotationManager: AnnotationManager,
        speakingPageIndex: Int?
    ) {
        this.decoder = decoder
        this.initialScrollX = initialScrollX
        this.initialScrollY = initialScrollY
        this.initialZoom = initialZoom
        this.initialOrientation = initialOrientation
        this.crop = crop
        this.speakingPageIndex = speakingPageIndex
        
        // 创建 PageViewState
        pageViewState = PageViewState(
            list,
            decoder,
            annotationManager,
            initialOrientation,
            crop,
            columnCount = columnCount
        )
        
        // 设置初始缩放和偏移
        vZoom = initialZoom.toFloat()
        viewOffset = Offset(initialScrollX.toFloat(), initialScrollY.toFloat())
        orientation = initialOrientation
    }
    
    fun jumpToPage(page: Int, offsetY: Float? = null) {
        jumpToPage = page
        jumpOffsetY = offsetY
        
        performJump()
    }
    
    fun restorePageState(page: Int, scrollX: Long, scrollY: Long) {
        jumpToPage = page
        initialScrollX = scrollX
        initialScrollY = scrollY
        
        performJump()
    }
    
    private fun performJump() {
        val pvs = pageViewState ?: return
        
        if (jumpToPage == null) return
        
        isJumping = true
        toPage = jumpToPage!!
        
        val page = pvs.pages.getOrNull(toPage)
        if (page != null) {
            if (orientation == Vertical) {
                val targetY = if (jumpOffsetY != null) {
                    page.bounds.top + jumpOffsetY!!
                } else {
                    page.bounds.top
                }
                
                val clampedTargetY = targetY.coerceIn(
                    0f, (pvs.totalHeight - viewHeight).coerceAtLeast(0f)
                )
                val clampedY = -clampedTargetY
                viewOffset = Offset(viewOffset.x, clampedY)
            } else {
                val clampedTargetX = page.bounds.left.coerceIn(
                    0f, (pvs.totalWidth - viewWidth).coerceAtLeast(0f)
                )
                val clampedX = -clampedTargetX
                viewOffset = Offset(clampedX, viewOffset.y)
            }
        }
        
        scroller.forceFinished(true)
        pvs.updateOffset(viewOffset)
        isJumping = false
    }
    
    fun updateOrientation(newOrientation: Int) {
        if (orientation != newOrientation && pageViewState != null) {
            isJumping = true
            val currentPage = jumpToPage ?: 0
            
            orientation = newOrientation
            viewOffset = Offset(0f, 0f)
            vZoom = 1f
            pageViewState!!.updateViewSize(IntSize(viewWidth, viewHeight), vZoom, orientation)
            
            val page = pageViewState!!.pages.getOrNull(currentPage)
            if (page != null) {
                if (orientation == Vertical) {
                    val clampedTargetY = page.bounds.top.coerceIn(
                        0f, (pageViewState!!.totalHeight - viewHeight).coerceAtLeast(0f)
                    )
                    val clampedY = -clampedTargetY
                    viewOffset = Offset(viewOffset.x, clampedY)
                } else {
                    val clampedTargetX = page.bounds.left.coerceIn(
                        0f, (pageViewState!!.totalWidth - viewWidth).coerceAtLeast(0f)
                    )
                    val clampedX = -clampedTargetX
                    viewOffset = Offset(clampedX, viewOffset.y)
                }
                scroller.forceFinished(true)
                pageViewState!!.updateOffset(viewOffset)
            }
            isJumping = false
        }
    }
    
    fun updateCropEnabled(enabled: Boolean) {
        val pvs = pageViewState ?: return
        
        if (pvs.isCropEnabled() != enabled) {
            pvs.setCropEnabled(enabled)
            pvs.pages.forEach { it.recycle() }
            ImageCache.clear()
            pvs.invalidatePageSizes()
            pvs.updateOffset(viewOffset)
            invalidate()
        }
    }
    
    /*fun updateSearchHighlight(quads: Map<Int, List<cn.archko.pdf.core.entity.DocQuad>>) {
        // pageViewState?.updateSearchHighlight(quads, null)
        invalidate()
    }*/
    
    fun updateSpeakingPageIndex(pageIndex: Int?) {
        scroller.forceFinished(true)
        pageViewState?.updateSpeakingPageIndex(pageIndex)
        invalidate()
    }
    
    fun updateColumnCount(count: Int) {
        pageViewState?.updateColumnCount(count)
        pageViewState?.updateVisiblePages(viewOffset, IntSize(viewWidth, viewHeight), vZoom)
        invalidate()
    }
    
    fun saveDocumentState() {
        val pvs = pageViewState ?: return
        val pages = pvs.pages
        
        var currentPage = 0
        if (pages.isNotEmpty()) {
            val offsetY = viewOffset.y
            val offsetX = viewOffset.x
            val firstVisible = pages.indexOfFirst { page ->
                if (orientation == Vertical) {
                    val top = -offsetY
                    val bottom = top + viewHeight
                    page.bounds.bottom > top && page.bounds.top < bottom
                } else {
                    val left = -offsetX
                    val right = left + viewWidth
                    page.bounds.right > left && page.bounds.left < right
                }
            }
            if (firstVisible != -1) {
                currentPage = firstVisible
            }
        }
        
        val pageCount = pages.size
        val zoom = vZoom.toDouble()
        
        if (pages.isNotEmpty()) {
            onSaveDocument?.invoke(
                currentPage,
                pageCount,
                zoom,
                viewOffset.x.toLong(),
                viewOffset.y.toLong(),
                orientation.toLong(),
                reflow,
                if (pvs.isCropEnabled()) 0L else 1L
            )
        }
    }

    fun cleanup() {
        scroller.forceFinished(true)
        flingRunnable?.cancelFling()
        flingRunnable = null
        saveDocumentState()
        onCloseDocument?.invoke()
        pageViewState?.shutdown()
        viewScope.cancel()
        ImageCache.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanup()
    }

    /**
     * Fling 动画 Runnable
     * 参考 vudroid 的实现，使用 Runnable 而不是在 onDraw 中处理动画
     */
    private inner class FlingRunnable : Runnable {
        private var isFinished = true

        fun cancelFling() {
            isFinished = true
            scroller.forceFinished(true)
        }

        fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int,
                  minX: Int, maxX: Int, minY: Int, maxY: Int) {
            isFinished = false
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        }

        override fun run() {
            if (isFinished || scroller.isFinished) {
                isFinished = true
                return
            }

            if (scroller.computeScrollOffset()) {
                val newX = scroller.currX.toFloat()
                val newY = scroller.currY.toFloat()

                val pvs = pageViewState ?: return
                val newOffset = Offset(newX, newY)
                viewOffset = calculateBounds(newOffset, vZoom, pvs, orientation)

                // 更新页面可见性
                pvs.updateOffset(viewOffset)

                // 触发重绘
                invalidate()

                // 使用 postOnAnimation 实现流畅动画
                postOnAnimation(this)
            } else {
                isFinished = true
            }
        }
    }
}
