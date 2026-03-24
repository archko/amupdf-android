package cn.archko.pdf.core.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.AnnotationPath
import cn.archko.pdf.core.entity.DrawType
import cn.archko.pdf.core.entity.Offset
import cn.archko.pdf.core.entity.PathConfig
import cn.archko.pdf.widgets.Flinger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 30f

/**
 */
class DocumentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    private var viewOffset: Offset = Offset(0f, 0f)
        set(value) {
            field = calculateBounds(value, vZoom, pageViewState, orientation)
            invalidate()
        }

    private var vZoom: Float = 1f
        set(value) {
            val clamped = value.coerceIn(1f, MAX_ZOOM)
            if (field != clamped) {
                field = clamped
                if (!isZooming) {
                    pageViewState?.updateViewSize(
                        IntSize(viewWidth, viewHeight),
                        vZoom,
                        orientation
                    )
                }
                invalidate()
            }
        }

    private var orientation: Int = Vertical
        set(value) {
            if (field != value) {
                field = value
                pageViewState?.updateViewSize(IntSize(viewWidth, viewHeight), vZoom, value)
                invalidate()
            }
        }

    private var toPage: Int = -1
    private var isJumping: Boolean = false

    private var pageViewState: PageViewState? = null

    private val viewScope = CoroutineScope(Dispatchers.Main)

    private val gestureDetector: GestureDetector
    private val flinger: Flinger

    private var flingRunnable: FlingRunnable? = null

    private var isZooming = false
    private var zoomBaseDistance = 0f
    private var zoomStartZoom = 1f

    var onPageChanged: ((page: Int) -> Unit)? = null
    var onSaveDocument: ((page: Int, pageCount: Int, zoom: Double, scrollX: Long, scrollY: Long, scrollOri: Long, reflow: Long, crop: Long) -> Unit)? =
        null
    var onCloseDocument: (() -> Unit)? = null

    // 模式控制
    private var selectionMode = false
    private var drawMode = false

    // 选择模式相关状态
    private var isSelecting = false
    private var selectedPage: Page? = null
    private var selectionStartX = 0f
    private var selectionStartY = 0f
    private var selectionEndX = 0f
    private var selectionEndY = 0f

    // 绘制模式相关状态
    private var isDrawing = false
    private var drawingPage: Page? = null
    private val drawingPoints = mutableListOf<PointF>()
    private var drawingPaint: Paint? = null
    private var pathConfig = PathConfig()

    // 回调
    var onTextSelected: ((text: String) -> Unit)? = null

    private var jumpToPage: Int? = null
    private var jumpOffsetY: Float? = null

    private var reflow: Long = 0L
    private var crop: Boolean = false

    //var searchHighlightQuads: Map<Int, List<cn.archko.pdf.core.entity.DocQuad>> = emptyMap()

    private var decoder: ImageDecoder? = null

    private var speakingPageIndex: Int? = null

    private var initialOrientation: Int = Vertical
    private var initialScrollX: Long = 0L
    private var initialScrollY: Long = 0L
    private var initialZoom: Double = 1.0

    init {
        gestureDetector = GestureDetector(context, DocumentGestureListener())
        flinger = Flinger()

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
        canvas.withTranslation(viewOffset.x, viewOffset.y) {
            pvs.drawVisiblePages(this, viewOffset, vZoom)
        }

        // 绘制选择区域和绘制路径
        drawSelectionAndDrawing(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInitialized()) {
            return true
        }

        // 根据当前模式处理触摸事件
        return when {
            selectionMode -> handleSelectionTouch(event)
            drawMode -> handleDrawTouch(event)
            else -> handleViewModeTouch(event)
        }
    }

    private fun isInitialized(): Boolean {
        return pageViewState != null && decoder != null
    }

    private fun handleViewModeTouch(event: MotionEvent): Boolean {
        if (handleMultiTouchZoom(event)) {
            return true
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        return true
    }

    /**
     * 处理多点触控缩放
     */
    private fun handleMultiTouchZoom(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isZooming = true
                    zoomBaseDistance = getZoomDistance(event)
                    zoomStartZoom = vZoom
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isZooming) {
                    isZooming = false
                    zoomBaseDistance = 0f

                    pageViewState?.updateViewSize(
                        IntSize(viewWidth, viewHeight),
                        vZoom,
                        orientation
                    )

                    pageViewState?.updateOffset(viewOffset)
                    postInvalidate()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isZooming && event.pointerCount >= 2 && zoomBaseDistance != 0f) {
                    val currentDistance = getZoomDistance(event)
                    val zoomRatio = currentDistance / zoomBaseDistance
                    val newZoom = (zoomStartZoom * zoomRatio).coerceIn(1f, MAX_ZOOM)

                    // 1. 计算缩放比例
                    // 2. 使用屏幕中心点缩放公式调整位置
                    val oldZoom = vZoom
                    val ratio = newZoom / oldZoom

                    // 使用屏幕中心点缩放，类似于 scrollTo((scrollX + width/2) * ratio - width/2, ...)
                    // 我们的 viewOffset 是负值，scrollX = -viewOffset.x
                    val scrollX = -viewOffset.x
                    val scrollY = -viewOffset.y
                    val newScrollX = (scrollX + viewWidth / 2f) * ratio - viewWidth / 2f
                    val newScrollY = (scrollY + viewHeight / 2f) * ratio - viewHeight / 2f

                    // 先计算新的 offset（暂时不应用边界限制）
                    val newOffset = Offset(-newScrollX, -newScrollY)

                    // 更新缩放（触发布局更新，这会影响 totalWidth/totalHeight）
                    vZoom = newZoom

                    // 应用边界限制（现在 totalWidth/totalHeight 已经是新的缩放值了）
                    viewOffset = calculateBounds(newOffset, vZoom, pageViewState, orientation)

                    pageViewState?.updateVisiblePages(
                        viewOffset,
                        IntSize(viewWidth, viewHeight),
                        vZoom
                    )
                    invalidate()
                    return true
                }
            }
        }

        return false
    }

    /**
     * 计算两点之间的距离
     */
    private fun getZoomDistance(event: MotionEvent): Float {
        return try {
            val dx = event.getX(0) - event.getX(1)
            val dy = event.getY(0) - event.getY(1)
            kotlin.math.sqrt(dx * dx + dy * dy)
        } catch (e: Exception) {
            1f
        }
    }

    // 手势监听器
    inner class DocumentGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            flinger.forceFinished()
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
            viewOffset = Offset(
                viewOffset.x - distanceX,
                viewOffset.y - distanceY
            )
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

    private fun handleClick(e: MotionEvent) {
        val tapPos = Offset(e.x, e.y)

        val pvs = pageViewState ?: return
        val linkHandled = pvs.handleClick(
            tapPos.x - viewOffset.x,
            tapPos.y - viewOffset.y
        )

        if (!linkHandled) {
            handlePageTurn(tapPos)
        }
    }

    private fun handlePageTurn(tapPos: Offset) {
        // 点击屏幕左侧向前翻页,点击右侧向后翻页
        val pvs = pageViewState ?: return
        val tapX = tapPos.x
        val viewWidthThird = viewWidth / 3f

        if (orientation == Vertical) {
            // 纵向模式
            if (tapX < viewWidthThird) {
                // 点击左侧,向上滚动一屏(减小 offset.y，使其更负)
                val scrollAmount = viewHeight.toFloat()
                val contentHeight = pvs.totalHeight * (vZoom / pvs.vZoom)
                val minY = if (contentHeight > viewHeight) viewHeight - contentHeight else 0f
                val newY = (viewOffset.y - scrollAmount).coerceAtLeast(minY)
                viewOffset = Offset(viewOffset.x, newY)
                pvs.updateOffset(viewOffset)
            } else if (tapX > viewWidth * 2 / 3) {
                // 点击右侧,向下滚动一屏(增大 offset.y，使其更接近 0)
                val scrollAmount = viewHeight.toFloat()
                val newY = (viewOffset.y + scrollAmount).coerceAtMost(0f)
                viewOffset = Offset(viewOffset.x, newY)
                pvs.updateOffset(viewOffset)
            }
        } else {
            // 横向模式
            val contentWidth = pvs.totalWidth * (vZoom / pvs.vZoom)
            val minX = if (contentWidth > viewWidth) viewWidth - contentWidth else 0f

            if (tapX < viewWidthThird) {
                // 点击左侧,向右滚动一屏(增大 offset.x，使其更接近 0)
                val scrollAmount = viewWidth.toFloat()
                val newX = (viewOffset.x + scrollAmount).coerceAtMost(0f)
                viewOffset = Offset(newX, viewOffset.y)
                pvs.updateOffset(viewOffset)
            } else if (tapX > viewWidth * 2 / 3) {
                // 点击右侧,向左滚动一屏(减小 offset.x，使其更负)
                val scrollAmount = viewWidth.toFloat()
                val newX = (viewOffset.x - scrollAmount).coerceAtLeast(minX)
                viewOffset = Offset(newX, viewOffset.y)
                pvs.updateOffset(viewOffset)
            }
        }
    }

    private fun performFling(velocityX: Float, velocityY: Float) {
        // 将 viewOffset（文档偏移，负值）转换为 scrollX/Y（滚动偏移，正值）
        val scrollX = (-viewOffset.x).toInt()
        val scrollY = (-viewOffset.y).toInt()

        flingRunnable = FlingRunnable()
        flingRunnable?.fling(
            scrollX, scrollY,
            -velocityX.toInt(),
            -velocityY.toInt(),
            getLeftLimit(), getRightLimit(),
            getTopLimit(), getBottomLimit()
        )
        post(flingRunnable)
    }

    private fun getTopLimit(): Int = 0

    private fun getLeftLimit(): Int = 0

    private fun getBottomLimit(): Int {
        val pvs = pageViewState ?: return 0
        return if (orientation == Horizontal) {
            (viewHeight * vZoom - viewHeight).toInt()
        } else {
            (pvs.totalHeight * vZoom - viewHeight).toInt()
        }
    }

    private fun getRightLimit(): Int {
        val pvs = pageViewState ?: return 0
        return if (orientation == Horizontal) {
            (pvs.totalWidth * vZoom - viewWidth).toInt()
        } else {
            (viewWidth * vZoom - viewWidth).toInt()
        }
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

        pageViewState = PageViewState(
            list,
            decoder,
            annotationManager,
            initialOrientation,
            crop,
            columnCount = columnCount
        )
        pageViewState!!.apply {
            onPageLinkClick = { pageIndex ->
                isJumping = true
                val targetPage = pages.getOrNull(pageIndex)
                if (targetPage != null) {
                    viewOffset = Offset(viewOffset.x, -targetPage.bounds.top)
                    updateOffset(viewOffset)
                }
                isJumping = false
            }

            viewScope.launch {
                renderFlow.collect {
                    invalidate()
                }
            }
        }

        vZoom = initialZoom.toFloat()
        viewOffset = Offset(initialScrollX.toFloat(), initialScrollY.toFloat())
        orientation = initialOrientation

        // 初始化后更新可见页面并触发重绘
        pageViewState?.updateOffset(viewOffset)
        pageViewState?.updateVisiblePages(viewOffset, IntSize(viewWidth, viewHeight), vZoom)
        invalidate()
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

        flinger.forceFinished()
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
                flinger.forceFinished()
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
        flinger.forceFinished()
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
        flinger.forceFinished()
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

    // ============ 模式切换方法 ============

    fun setSelectionMode(enabled: Boolean) {
        if (selectionMode != enabled) {
            selectionMode = enabled
            if (enabled) {
                drawMode = false
            }
            resetGestureState()
            invalidate()
        }
    }

    fun setDrawMode(enabled: Boolean) {
        if (drawMode != enabled) {
            drawMode = enabled
            if (enabled) {
                selectionMode = false
            }
            resetGestureState()
            initDrawingPaint()
            invalidate()
        }
    }

    fun setDrawConfig(config: PathConfig) {
        this.pathConfig = config
        initDrawingPaint()
    }

    private fun resetGestureState() {
        pageViewState?.pages?.forEach { page ->
            page.clearTextSelection()
        }

        isSelecting = false
        isDrawing = false
        selectedPage = null
        drawingPage = null
        drawingPoints.clear()
    }

    private fun initDrawingPaint() {
        drawingPaint = Paint().apply {
            color = pathConfig.color
            strokeWidth = pathConfig.strokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    // ============ 选择模式处理 ============

    private fun handleSelectionTouch(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> return handleSelectionDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> return handleSelectionMove(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return handleSelectionUp()
        }
        return true
    }

    private fun handleSelectionDown(x: Float, y: Float): Boolean {
        val page = getEventPageFromScreen(x, y)
        if (page != null) {
            isSelecting = true
            selectedPage = page
            selectionStartX = x
            selectionStartY = y
            selectionEndX = x
            selectionEndY = y
            invalidate()
            return true
        }
        return false
    }

    private fun handleSelectionMove(x: Float, y: Float): Boolean {
        if (isSelecting && selectedPage != null) {
            selectionEndX = x
            selectionEndY = y
            invalidate()
            return true
        }
        return false
    }

    private fun handleSelectionUp(): Boolean {
        if (isSelecting && selectedPage != null) {
            // TODO: 实现实际的文本选择逻辑
            // 目前只计算选择区域，后续可以添加与decoder交互获取选中文本

            isSelecting = false
            selectedPage = null
            invalidate()
            return true
        }
        return false
    }

    // ============ 绘制模式处理 ============

    private fun handleDrawTouch(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> return handleDrawDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> return handleDrawMove(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return handleDrawUp()
        }
        return true
    }

    private fun handleDrawDown(x: Float, y: Float): Boolean {
        val page = getEventPageFromScreen(x, y)
        if (page != null) {
            isDrawing = true
            drawingPage = page
            drawingPoints.clear()

            // 将屏幕坐标转换为页面相对坐标(0-1)
            val relativePoint = convertScreenToRelativePoint(x, y, page)
            drawingPoints.add(PointF(relativePoint.x, relativePoint.y))

            invalidate()
            return true
        }
        return false
    }

    private fun handleDrawMove(x: Float, y: Float): Boolean {
        if (isDrawing && drawingPage != null) {
            val relativePoint = convertScreenToRelativePoint(x, y, drawingPage!!)
            drawingPoints.add(PointF(relativePoint.x, relativePoint.y))
            invalidate()
            return true
        }
        return false
    }

    private fun handleDrawUp(): Boolean {
        if (isDrawing && drawingPage != null && drawingPoints.size > 1) {
            val annotationManager = pageViewState?.annotationManager
            if (annotationManager != null) {
                try {
                    val offsetList = mutableListOf<Offset>()

                    if (pathConfig.drawType == DrawType.LINE) {
                        // LINE模式: 计算水平或垂直线的终点
                        val startPoint = drawingPoints[0]
                        val endPoint = drawingPoints[drawingPoints.size - 1]

                        val dx = kotlin.math.abs(endPoint.x - startPoint.x)
                        val dy = kotlin.math.abs(endPoint.y - startPoint.y)

                        val finalEndPoint = if (dx > dy) {
                            PointF(endPoint.x, startPoint.y)
                        } else {
                            PointF(startPoint.x, endPoint.y)
                        }

                        offsetList.add(Offset(startPoint.x, startPoint.y))
                        offsetList.add(Offset(finalEndPoint.x, finalEndPoint.y))
                    } else {
                        for (point in drawingPoints) {
                            offsetList.add(Offset(point.x, point.y))
                        }
                    }

                    val annotationPath = AnnotationPath(offsetList, pathConfig)
                    annotationManager.addPath(drawingPage!!.aPage.index, annotationPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 重置绘制状态
            isDrawing = false
            drawingPage = null
            drawingPoints.clear()
            invalidate()
            return true
        }
        return false
    }

    // ============ 辅助方法 ============

    private fun getEventPageFromScreen(screenX: Float, screenY: Float): Page? {
        val pvs = pageViewState ?: return null
        val pages = pvs.pages
        if (pages.isEmpty()) return null

        val offset = viewOffset

        for (page in pages) {
            val rect = page.bounds
            val pageTop = rect.top + offset.y
            val pageBottom = rect.bottom + offset.y
            val pageLeft = rect.left + offset.x
            val pageRight = rect.right + offset.x

            if (screenX >= pageLeft && screenX <= pageRight &&
                screenY >= pageTop && screenY <= pageBottom
            ) {
                return page
            }
        }

        return null
    }

    private fun convertScreenToRelativePoint(screenX: Float, screenY: Float, page: Page): Offset {
        // 转换为页面坐标
        val pageX = screenX - viewOffset.x - page.bounds.left
        val pageY = screenY - viewOffset.y - page.bounds.top

        // 转换为相对坐标(0-1)
        val relativeX = pageX / page.bounds.width()
        val relativeY = pageY / page.bounds.height()

        return Offset(relativeX, relativeY)
    }

    private fun convertRelativeToScreen(relativeX: Float, relativeY: Float, page: Page): PointF {
        val pageX = relativeX * page.bounds.width()
        val pageY = relativeY * page.bounds.height()

        val screenX = pageX + page.bounds.left + viewOffset.x
        val screenY = pageY + page.bounds.top + viewOffset.y

        return PointF(screenX, screenY)
    }

    // ============ 绘制辅助方法 ============

    private fun drawSelectionAndDrawing(canvas: Canvas) {
        if (isSelecting && selectedPage != null) {
            drawSelectionRect(canvas)
        }

        if (isDrawing && drawingPage != null && drawingPoints.size > 1) {
            drawCurrentPath(canvas)
        }
    }

    private fun drawSelectionRect(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.argb(100, 0, 120, 215) // 半透明蓝色
            style = Paint.Style.FILL
        }

        val left = kotlin.math.min(selectionStartX, selectionEndX)
        val top = kotlin.math.min(selectionStartY, selectionEndY)
        val right = kotlin.math.max(selectionStartX, selectionEndX)
        val bottom = kotlin.math.max(selectionStartY, selectionEndY)

        canvas.drawRect(left, top, right, bottom, paint)

        // 绘制边框
        paint.color = Color.rgb(0, 120, 215)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(left, top, right, bottom, paint)
    }

    private fun drawCurrentPath(canvas: Canvas) {
        val paint = drawingPaint ?: return
        val page = drawingPage ?: return

        if (pathConfig.drawType == DrawType.LINE) {
            val startPoint = drawingPoints[0]
            val endPoint = drawingPoints[drawingPoints.size - 1]

            val dx = kotlin.math.abs(endPoint.x - startPoint.x)
            val dy = kotlin.math.abs(endPoint.y - startPoint.y)

            val finalEndPoint = if (dx > dy) {
                PointF(endPoint.x, startPoint.y)
            } else {
                PointF(startPoint.x, endPoint.y)
            }

            val startScreen = convertRelativeToScreen(startPoint.x, startPoint.y, page)
            val endScreen = convertRelativeToScreen(finalEndPoint.x, finalEndPoint.y, page)

            canvas.drawLine(startScreen.x, startScreen.y, endScreen.x, endScreen.y, paint)
        } else if (pathConfig.drawType == DrawType.CURVE) {
            val path = Path()
            val firstScreen = convertRelativeToScreen(drawingPoints[0].x, drawingPoints[0].y, page)
            path.moveTo(firstScreen.x, firstScreen.y)

            for (i in 1 until drawingPoints.size) {
                val screen = convertRelativeToScreen(drawingPoints[i].x, drawingPoints[i].y, page)
                path.lineTo(screen.x, screen.y)
            }

            canvas.drawPath(path, paint)
        }
    }

    private inner class FlingRunnable : Runnable {

        fun cancelFling() {
            flinger.forceFinished()
        }

        fun fling(
            startX: Int, startY: Int, velocityX: Int, velocityY: Int,
            minX: Int, maxX: Int, minY: Int, maxY: Int
        ) {
            flinger.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        }

        fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
            startScroll(startX, startY, dx, dy, 0)
        }

        fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            if (duration == 0) {
                viewOffset = Offset((startX + dx).toFloat(), (startY + dy).toFloat())
                pageViewState?.updateOffset(viewOffset)
                invalidate()
            } else {
                flinger.startScroll(startX, startY, dx, dy)
            }
        }

        override fun run() {
            if (flinger.isFinished) {
                return
            }

            if (flinger.computeScrollOffset()) {
                // flinger.currX/currY 是滚动偏移（正值）
                // 转换为 viewOffset（文档偏移，负值）
                viewOffset = Offset(-flinger.currX.toFloat(), -flinger.currY.toFloat())
                pageViewState?.updateOffset(viewOffset)
                invalidate()

                postOnAnimation(this)
            }
        }
    }
}
