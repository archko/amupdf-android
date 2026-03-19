package cn.archko.pdf.core.component

import android.graphics.Canvas
import android.graphics.RectF
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.AnnotationPath
import cn.archko.pdf.core.entity.Offset
import cn.archko.pdf.core.entity.PathConfig
import cn.archko.pdf.core.link.Hyperlink
import com.archko.reader.pdf.component.DecodeService
import com.archko.reader.pdf.component.DecoderAdapter
import com.archko.reader.pdf.component.IntSize
import com.archko.reader.pdf.component.TileSpec
import com.archko.reader.pdf.component.Vertical
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author: archko 2025/7/24 :08:21
 */
class PageViewState(
    val list: List<APage>,
    val state: ImageDecoder,
    val annotationManager: AnnotationManager,
    var orientation: Int = Vertical,
    crop: Boolean,
    var columnCount: Int = 1
) {
    var viewOffset: Offset = Offset(0f, 0f)
    var init: Boolean = false
    var totalHeight: Float = 0f
    var totalWidth: Float = 0f

    var viewSize: IntSize = IntSize(0, 0)
    internal var pageToRender: List<Page> = listOf()
    var pages: List<Page> = createPages()
    var vZoom: Float = 1f
    var nodePool: PageNodePool = PageNodePool()

    // 正在朗读的页面索引，null 表示没有朗读
    var speakingPageIndex: Int? = null
        private set

    // var searchHighlightQuads: Map<Int, List<DocQuad>> = emptyMap()
    //    private set
    var currentSearchPageIndex: Int? = null
        private set
    var currentSearchResultIndex: Int = -1
        private set

    private var preloadScreens: Float = 0.8f // 预加载1屏的距离

    private var lastPageKeys: Set<Int> = emptySet()

    private var isShutdown = false

    // 链接处理回调
    var onPageLinkClick: ((pageIndex: Int) -> Unit)? = null
    var onUrlLinkClick: ((url: String) -> Unit)? = null

    // 切边控制
    private var cropEnabled: Boolean = crop

    // 解码服务
    var decodeService: DecodeService? = null
        private set

    // 渲染触发Flow
    private val _renderFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val renderFlow: SharedFlow<Unit> = _renderFlow.asSharedFlow()

    // throttle机制（34ms内最多触发一次）
    private var lastRenderTime = 0L

    // 解码完成回调
    var onDecodeCompleted: (() -> Unit)? = null

    var activeDrawingAnno: Pair<Int, AnnotationPath>? = null

    fun updateDrawing(pageIndex: Int, points: List<Offset>, pathConfig: PathConfig) {
        val anno = AnnotationPath(
            points,
            config = pathConfig
        )

        activeDrawingAnno = pageIndex to anno
    }

    fun finalizeDrawing(pageIndex: Int, points: List<Offset>, pathConfig: PathConfig) {
        val path = AnnotationPath(points, config = pathConfig)
        annotationManager.addPath(pageIndex, path)
        activeDrawingAnno = null
    }

    init {
        initDecodeService()
    }

    // 触发渲染更新（带throttle）
    private fun triggerRenderUpdate() {
        val currentTime = System.currentTimeMillis()
        //println("triggerRenderUpdate:${currentTime - lastRenderTime}")
        if (currentTime - lastRenderTime >= 34) {
            _renderFlow.tryEmit(Unit)
            lastRenderTime = currentTime
        }
    }

    fun notifyDecodeCompleted() {
        triggerRenderUpdate()
    }

    /**
     * 设置切边参数
     * @param enabled 是否启用切边
     */
    fun setCropEnabled(enabled: Boolean) {
        if (cropEnabled != enabled) {
            cropEnabled = enabled
        }
    }

    /**
     * 获取当前切边状态
     * @return 是否启用切边
     */
    fun isCropEnabled(): Boolean = cropEnabled

    fun updateColumnCount(columnCount: Int) {
        val columnCountChanged = this.columnCount != columnCount
        if (columnCountChanged) {
            this.columnCount = columnCount
            println("PageViewState.updateColumnCount: 重新计算页面布局columnCount: $columnCount")
            invalidatePageSizes()
            ImageCache.clearNodes()
        }
    }

    /**
     * 设置预加载距离
     * @param screens 预加载的屏幕数量，默认1.0表示预加载一屏的距离
     */
    fun setPreloadScreens(screens: Float) {
        preloadScreens = screens.coerceAtLeast(0f)
    }

    /**
     * 获取当前预加载距离
     * @return 预加载的屏幕数量
     */
    fun getPreloadScreens(): Float = preloadScreens

    /**
     * 更新正在朗读的页面索引
     * @param pageIndex 页面索引，null 表示没有朗读
     */
    fun updateSpeakingPageIndex(pageIndex: Int?) {
        println("PageViewState: updateSpeakingPageIndex from $speakingPageIndex to $pageIndex")
        speakingPageIndex = pageIndex
    }

    /**
     * 更新搜索高亮状态
     * @param highlightQuads 每个页面的高亮区域映射
     * @param currentPageIndex 当前搜索结果所在的页面索引
     */
    /* fun updateSearchHighlight(
        highlightQuads: Map<Int, List<DocQuad>>,
        currentPageIndex: Int?,
        currentResultIndex: Int = -1
    ) {
        searchHighlightQuads = highlightQuads
        currentSearchPageIndex = currentPageIndex
        currentSearchResultIndex = currentResultIndex
    }*/

    /**
     * 清除搜索高亮
     */
    /* fun clearSearchHighlight() {
        searchHighlightQuads = emptyMap()
        currentSearchPageIndex = null
        currentSearchResultIndex = -1
    }*/

    fun isTileVisible(spec: TileSpec, strictMode: Boolean = false): Boolean {
        val page = pages.getOrNull(spec.page) ?: return false

        // spec.pageWidth/pageHeight 是缩放后的Page尺寸
        // page.bounds 包含了Page在文档中的位置（基于pageViewState.vZoom）
        // 需要计算当前缩放比例下的Page偏移
        val scaleRatio = spec.pageWidth / page.width  // 当前缩放比例 / 基准缩放比例

        // 计算tile在文档中的绝对坐标
        val pixelRect = RectF(
            spec.bounds.left * spec.pageWidth + page.bounds.left * scaleRatio,
            spec.bounds.top * spec.pageHeight + page.bounds.top * scaleRatio,
            spec.bounds.right * spec.pageWidth + page.bounds.left * scaleRatio,
            spec.bounds.bottom * spec.pageHeight + page.bounds.top * scaleRatio
        )

        return if (strictMode) {
            isVisible(viewSize, viewOffset, pixelRect, spec.page)
        } else {
            isVisibleWithPreload(viewSize, viewOffset, pixelRect)
        }
    }

    private fun isVisibleWithPreload(
        viewSize: IntSize,
        offset: Offset,
        bounds: RectF,
    ): Boolean {
        // 获取包含预加载区域的可视区域
        val preloadRect = if (orientation == Vertical) {
            RectF(
                -offset.x,
                -offset.y,
                viewSize.width - offset.x,
                viewSize.height - offset.y + viewSize.height * preloadScreens
            )
        } else {
            RectF(
                -offset.x,
                -offset.y,
                viewSize.width - offset.x + viewSize.width * preloadScreens,
                viewSize.height - offset.y,
            )
        }

        // 检查页面是否与预加载区域相交
        return RectF.intersects(preloadRect, bounds)
    }

    /**
     * 初始化解码服务
     */
    private fun initDecodeService() {
        val decoder = DecoderAdapter(
            imageDecoder = state,
            viewSize = viewSize,
            isCropEnabled = { cropEnabled }
        )
        decodeService = DecodeService(decoder)

        // 如果启用切边，生成切边任务
        if (cropEnabled) {
            decodeService!!.submit {
                val cropTasks = decoder.generateCropTasks()
                if (cropTasks.isNotEmpty()) {
                    decodeService?.submitCropTasks(cropTasks)
                }
            }
        }
    }

    fun shutdown() {
        isShutdown = true
        decodeService?.shutdown()

        pages.forEach {
            it.recycle()
        }
        pages = emptyList()
        pageToRender = emptyList()

        state.close()
        nodePool.clear()

        println("PageViewState.shutdown: 清理完成，调用GC")
        // 建议GC
        System.gc()
    }

    fun isShutdown(): Boolean = isShutdown

    fun invalidatePageSizes() {
        init = false
        if (viewSize.width == 0 || viewSize.height == 0 || list.isEmpty()) {
            println("PageViewState.viewSize高宽为0,或list为空,不计算page: viewSize:$viewSize, totalHeight:$totalHeight, totalWidth:$totalWidth")
            totalHeight = viewSize.height.toFloat()
            totalWidth = viewSize.width.toFloat()
        } else {
            if (columnCount > 1) {
                var currentY = 0f
                // 计算每一列的基础宽度
                val scaledPageWidth = (viewSize.width * vZoom) / columnCount
                val pageCount = list.size

                var i = 0
                while (i < pageCount) {
                    var maxHeightInRow = 0f

                    for (col in 0 until columnCount) {
                        val currentIndex = i + col
                        if (currentIndex >= pageCount) break

                        val aPage = list[currentIndex]
                        val pageObj = pages[currentIndex]

                        // 1. 计算当前页面的缩放比例 (基于列宽)
                        val isCropped = cropEnabled && aPage.hasCrop()
                        val originalWidth = aPage.getWidth(isCropped)
                        val originalHeight = aPage.getHeight(isCropped)

                        val pageScale = scaledPageWidth / originalWidth
                        val scaledPageHeight = originalHeight * pageScale

                        // 2. 计算当前页面的左偏移量 (left offset)
                        val currentX = col * scaledPageWidth

                        // 3. 更新页面布局边界
                        val bounds = RectF(
                            currentX,
                            currentY,
                            currentX + scaledPageWidth,
                            currentY + scaledPageHeight
                        )

                        pageObj.update(scaledPageWidth, scaledPageHeight, bounds)

                        // 4. 记录这一行中最高的页面高度
                        if (scaledPageHeight > maxHeightInRow) {
                            maxHeightInRow = scaledPageHeight
                        }
                    }

                    currentY += maxHeightInRow
                    i += columnCount
                }

                totalWidth = viewSize.width * vZoom
                totalHeight = currentY
            } else {
                if (orientation == Vertical) {
                    var currentY = 0f
                    val scaledPageWidth = viewSize.width * vZoom
                    list.zip(pages).forEach { (aPage, page) ->
                        // 根据是否有切边选择不同的尺寸计算方式
                        val pageScale = if (cropEnabled && aPage.hasCrop()) {
                            // 有切边：使用切边后的尺寸
                            scaledPageWidth / aPage.getWidth(true)
                        } else {
                            // 无切边：使用原始尺寸
                            scaledPageWidth / aPage.getWidth(false)
                        }

                        val scaledPageHeight = if (cropEnabled && aPage.hasCrop()) {
                            aPage.getHeight(true) * pageScale
                        } else {
                            aPage.getHeight(false) * pageScale
                        }

                        val bounds = RectF(
                            0f,
                            currentY,
                            scaledPageWidth,
                            currentY + scaledPageHeight
                        )
                        // 直接用最终宽高初始化Page
                        page.update(scaledPageWidth, scaledPageHeight, bounds)
                        currentY += scaledPageHeight
                        //println("PageViewState.page=${aPage.index}, pageScale:$pageScale, y:$currentY, bounds:$bounds, aPage:$aPage, hasCrop:${aPage.hasCrop()}")
                    }
                    totalHeight = currentY
                    totalWidth = viewSize.width * vZoom
                } else {
                    var currentX = 0f
                    val scaledPageHeight = viewSize.height * vZoom
                    list.zip(pages).forEach { (aPage, page) ->
                        // 根据是否有切边选择不同的尺寸计算方式
                        val pageScale = if (cropEnabled && aPage.hasCrop()) {
                            // 有切边：使用切边后的尺寸
                            scaledPageHeight / aPage.getHeight(true)
                        } else {
                            // 无切边：使用原始尺寸
                            scaledPageHeight / aPage.getHeight(false)
                        }

                        val scaledPageWidth = if (cropEnabled && aPage.hasCrop()) {
                            aPage.getWidth(true) * pageScale
                        } else {
                            aPage.getWidth(false) * pageScale
                        }

                        val bounds = RectF(
                            currentX,
                            0f,
                            currentX + scaledPageWidth,
                            scaledPageHeight
                        )
                        // 直接用最终宽高初始化Page
                        page.update(scaledPageWidth, scaledPageHeight, bounds)
                        currentX += scaledPageWidth
                        //println("PageViewState.pageScale:$pageScale, x:$currentX, bounds:$bounds, aPage:$aPage, hasCrop:${aPage.hasCrop()}")
                    }
                    totalWidth = currentX
                    totalHeight = viewSize.height * vZoom
                    //println("PageViewState: 横向模式 - totalWidth: $totalWidth, totalHeight: $totalHeight, 页面数: ${pages.size}")
                }
            }
            init = true
        }
        println("PageViewState.invalidatePageSizes.viewSize:$viewSize, totalHeight:$totalHeight, totalWidth:$totalWidth, columnCount:$columnCount, zoom:$vZoom, orientation:$orientation, crop:$cropEnabled")
    }

    private fun createPages(): List<Page> {
        val list = list.map { aPage ->
            // 初始化时直接用viewWidth/viewHeight和vZoom计算的宽高
            if (orientation == Vertical) {
                val scaledPageWidth = viewSize.width * 1f
                val pageScale = if (aPage.width == 0f) 1f else scaledPageWidth / aPage.width
                val scaledPageHeight = aPage.height * pageScale
                Page(
                    this,
                    scaledPageWidth,
                    scaledPageHeight,
                    aPage,
                    0f,
                    0f
                )
            } else {
                val scaledPageHeight = viewSize.height * 1f
                val pageScale = if (aPage.height == 0f) 1f else scaledPageHeight / aPage.height
                val scaledPageWidth = aPage.width * pageScale
                Page(
                    this,
                    scaledPageWidth,
                    scaledPageHeight,
                    aPage,
                    0f,
                    0f
                )
            }
        }
        return list
    }

    fun updateViewSize(
        viewSize: IntSize,
        vZoom: Float,
        orientation: Int = this.orientation
    ) {
        val isViewSizeChanged = this.viewSize != viewSize
        val isZoomChanged = this.vZoom != vZoom
        val isOrientationChanged = this.orientation != orientation

        this.viewSize = viewSize
        this.vZoom = vZoom
        if (isOrientationChanged) {
            this.orientation = orientation
            this.vZoom = 1f
        }

        if (isViewSizeChanged || isZoomChanged || isOrientationChanged) {
            println("PageViewState.updateViewSize: 重新计算页面布局orientation: $orientation")
            invalidatePageSizes()
        } else {
            println("PageViewState.viewSize未变化: vZoom:$vZoom, totalHeight:$totalHeight, totalWidth:$totalWidth, orientation: $orientation, viewWidth:$viewSize")
        }
    }

    // 二分查找第一个可见页面
    private fun findVerticalFirstVisible(top: Float, currentVZoom: Float): Int {
        var low = 0
        var high = pages.size - 1
        var result = pages.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            val page = pages[mid]
            // 在缩放过程中，需要考虑当前缩放比例
            val scaleRatio = currentVZoom / this.vZoom
            val currentBottom = page.bounds.bottom * scaleRatio
            if (currentBottom > top) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }

    // 二分查找最后一个可见页面
    private fun findVerticalLastVisible(bottom: Float, currentVZoom: Float): Int {
        var low = 0
        var high = pages.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val page = pages[mid]
            // 在缩放过程中，需要考虑当前缩放比例
            val scaleRatio = currentVZoom / this.vZoom
            val currentTop = page.bounds.top * scaleRatio
            if (currentTop < bottom) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    // 二分查找第一个可见页面
    private fun findHorizontalFirstVisible(left: Float, currentVZoom: Float): Int {
        var low = 0
        var high = pages.size - 1
        var result = pages.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            val page = pages[mid]
            // 在缩放过程中，需要考虑当前缩放比例
            val scaleRatio = currentVZoom / this.vZoom
            val currentRight = page.bounds.right * scaleRatio
            if (currentRight > left) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }

    // 二分查找最后一个可见页面
    private fun findHorizontalLastVisible(right: Float, currentVZoom: Float): Int {
        var low = 0
        var high = pages.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val page = pages[mid]
            // 在缩放过程中，需要考虑当前缩放比例
            val scaleRatio = currentVZoom / this.vZoom
            val currentLeft = page.bounds.left * scaleRatio
            if (currentLeft < right) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun updateVisiblePages(
        offset: Offset,
        viewSize: IntSize,
        currentVZoom: Float = vZoom
    ) {
        // 优化：使用二分查找定位可见页面范围
        if (pages.isEmpty()) {
            pageToRender = emptyList()
            notifyDecodeCompleted()
            return
        }

        // 计算当前可见区域（包含预加载范围）
        val visibleRect = RectF(
            -offset.x,
            -offset.y,
            viewSize.width - offset.x,
            viewSize.height - offset.y
        )

        val scaleRatio = currentVZoom / this.vZoom

        if (columnCount > 1) {
            // 多列布局：需要同时考虑水平和垂直方向的可见性
            val tilesToRenderCopy = pages.filter { page ->
                val scaledBounds = RectF(
                    page.bounds.left * scaleRatio,
                    page.bounds.top * scaleRatio,
                    page.bounds.right * scaleRatio,
                    page.bounds.bottom * scaleRatio
                )
                RectF.intersects(scaledBounds, visibleRect)
            }

            val newPageKeys = tilesToRenderCopy.map { page ->
                page.aPage.index
            }.toSet()
            val toRemove = lastPageKeys - newPageKeys
            toRemove.forEach { key ->
                val page = pages.getOrNull(key) ?: return@forEach
                page.clearVisibleNodes()
            }
            lastPageKeys = newPageKeys

            if (tilesToRenderCopy != pageToRender) {
                pageToRender = tilesToRenderCopy
            }

            // 更新每个可见页面的可见 nodes
            tilesToRenderCopy.forEach { page ->
                page.updateVisibleNodes(visibleRect, scaleRatio)
            }
            println("updateVisiblePages.multiColumn: visible=${tilesToRenderCopy.map { it.aPage.index }}")
        } else if (orientation == Vertical) {
            val visibleTop = -offset.y
            val visibleBottom = viewSize.height - offset.y
            // 预加载区域：向下扩展一屏
            val preloadBottom = visibleBottom + viewSize.height * preloadScreens
            val preloadRect = RectF(
                -offset.x,
                -offset.y,
                viewSize.width - offset.x,
                preloadBottom
            )

            val first = findVerticalFirstVisible(visibleTop, currentVZoom)
            val last = findVerticalLastVisible(preloadBottom, currentVZoom)

            // pageToRender 包含可见页面 + 预加载页面
            val tilesToRenderCopy = if (first <= last && first < pages.size && last >= 0) {
                pages.subList(first, last + 1)
            } else {
                emptyList()
            }
            // 主动移除不再可见的页面图片缓存
            val newPageKeys = tilesToRenderCopy.map { page ->
                page.aPage.index
            }.toSet()
            val toRemove = lastPageKeys - newPageKeys
            toRemove.forEach { key ->
                val page = pages.getOrNull(key) ?: return@forEach
                page.clearVisibleNodes()
            }
            lastPageKeys = newPageKeys

            if (tilesToRenderCopy != pageToRender) {
                pageToRender = tilesToRenderCopy
            }

            // 更新每个可见页面的可见 nodes
            tilesToRenderCopy.forEach { page ->
                page.updateVisibleNodes(preloadRect, scaleRatio)
            }
        } else {
            val visibleLeft = -offset.x
            val visibleRight = viewSize.width - offset.x
            // 预加载区域：向右扩展一屏
            val preloadRight = visibleRight + viewSize.width * preloadScreens
            val preloadRect = RectF(
                -offset.x,
                -offset.y,
                preloadRight,
                viewSize.height - offset.y
            )

            val first = findHorizontalFirstVisible(visibleLeft, currentVZoom)
            val last = findHorizontalLastVisible(preloadRight, currentVZoom)

            // pageToRender 包含可见页面 + 预加载页面
            val tilesToRenderCopy = if (first <= last && first < pages.size && last >= 0) {
                pages.subList(first, last + 1)
            } else {
                emptyList()
            }
            // 主动移除不再可见的页面图片缓存
            val newPageKeys = tilesToRenderCopy.map { page ->
                page.aPage.index
            }.toSet()
            val toRemove = lastPageKeys - newPageKeys
            toRemove.forEach { key ->
                val page = pages.getOrNull(key) ?: return@forEach
                page.clearVisibleNodes()
            }
            lastPageKeys = newPageKeys

            if (tilesToRenderCopy != pageToRender) {
                pageToRender = tilesToRenderCopy
            }

            // 更新每个可见页面的可见 nodes
            tilesToRenderCopy.forEach { page ->
                page.updateVisibleNodes(preloadRect, scaleRatio)
            }
        }
    }

    /**
     * 可见,有可能是预加载
     * 优化：O(1) 快速检查页面是否在可见列表中
     */
    fun isPageInVisibleList(pageIndex: Int): Boolean {
        return lastPageKeys.contains(pageIndex)
    }

    fun updateOffset(newOffset: Offset) {
        if (viewOffset.x != newOffset.x || viewOffset.y != newOffset.y) {
            this.viewOffset = newOffset
            updateVisiblePages(newOffset, viewSize, vZoom)  // 在offset改变时计算可见页面
        }
    }

    fun drawVisiblePages(
        canvas: Canvas,
        offset: Offset,
        vZoom: Float,
    ) {
        pageToRender.forEach { page ->
            page.draw(canvas, offset, vZoom)
        }
    }

    /**
     * 处理点击事件，查找并处理链接
     * @param x 点击的X坐标
     * @param y 点击的Y坐标
     * @return 如果找到链接并处理成功返回true，否则返回false
     */
    fun handleClick(x: Float, y: Float): Boolean {
        /*for (page in pageToRender) {
            val link = page.findLinkAtPoint(x, y)
            if (link != null) {
                return handleLink(link)
            }
        }*/

        return false
    }

    /**
     * 处理链接
     * @param link 找到的链接
     * @return 处理是否成功
     */
    private fun handleLink(link: Hyperlink): Boolean {
        return when (link.linkType) {
            Hyperlink.LINKTYPE_PAGE -> {
                // 页面链接，跳转到指定页面
                if (link.page >= 0 && link.page < pages.size) {
                    onPageLinkClick?.invoke(link.page)
                    true
                } else {
                    false
                }
            }

            Hyperlink.LINKTYPE_URL -> {
                if (!link.url.isNullOrEmpty()) {
                    onUrlLinkClick?.invoke(link.url!!)
                    true
                } else {
                    false
                }
            }

            else -> {
                println("PageViewState.handleLink: 未知链接类型: ${link.linkType}")
                false
            }
        }
    }

    companion object {

        fun isVisible(viewSize: IntSize, offset: Offset, bounds: RectF, page: Int): Boolean {
            // 获取画布的可视区域
            val visibleRect = RectF(
                -offset.x,
                -offset.y,
                viewSize.width - offset.x,
                viewSize.height - offset.y
            )

            // 检查页面是否与可视区域相交
            return RectF.intersects(visibleRect, bounds)
        }
    }
}
