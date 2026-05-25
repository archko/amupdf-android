package cn.archko.pdf.core.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import cn.archko.pdf.core.cache.BitmapState
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.component.Page.Companion.MAX_BLOCK
import cn.archko.pdf.core.entity.APage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author: archko 2025/7/24 :08:19
 */
class PageNode(
    private val pageViewState: PageViewState,
    var bounds: RectF,  // 逻辑坐标(0~1)
    var aPage: APage
) {

    private var activeDecodeKey: String? = null

    //不能用bounds.toString(),切边切换,key变化
    var cacheKey: String =
        "${aPage.index}-${bounds.left}-${bounds.top}-${bounds.right}-${bounds.bottom}-${pageViewState.vZoom}-${pageViewState.orientation}-${pageViewState.isCropEnabled()}"

    private var bitmapState: BitmapState? = null
    private var isDecoding = false

    // 缓存像素矩形计算结果
    private var cachedPixelRect: RectF? = null
    private var cachedPageWidth: Float = 0f
    private var cachedPageHeight: Float = 0f
    private var cachedXOffset: Float = 0f
    private var cachedYOffset: Float = 0f

    private val drawRect = RectF()

    fun updateKey() {
        // 只有在 orientation 或 crop 改变时才重新生成字符串
        // 或者使用更快的位运算生成 Long 型 ID
        cacheKey =
            "${aPage.index}-${bounds.left}-${bounds.top}-${bounds.right}-${bounds.bottom}-${pageViewState.vZoom}-${pageViewState.orientation}-${pageViewState.isCropEnabled()}"
    }

    fun update(newBounds: RectF, newAPage: APage) {
        this.bounds = newBounds
        this.aPage = newAPage
        updateKey()
    }

    fun toPixelRect(
        pageWidth: Float,
        pageHeight: Float,
        xOffset: Float,
        yOffset: Float
    ): RectF {
        // bounds是[0,1]的逻辑坐标，乘以pageWidth/pageHeight得到Node在Page中的像素尺寸
        // 然后加上xOffset/yOffset（Page在文档中的偏移）得到Node在文档中的绝对坐标
        // 优化：减少 floor/ceil 调用，直接计算并转换为整数对齐
        val left = (bounds.left * pageWidth + xOffset).toInt().toFloat()
        val top = (bounds.top * pageHeight + yOffset).toInt().toFloat()
        val right = (bounds.right * pageWidth + xOffset).toInt().toFloat()
        val bottom = (bounds.bottom * pageHeight + yOffset).toInt().toFloat()

        return RectF(left, top, right, bottom)
    }

    // 获取缓存的像素矩形，如果参数变化则重新计算
    private fun getCachedPixelRect(
        pageWidth: Float,
        pageHeight: Float,
        xOffset: Float,
        yOffset: Float
    ): RectF {
        if (cachedPixelRect == null ||
            cachedPageWidth != pageWidth ||
            cachedPageHeight != pageHeight ||
            cachedXOffset != xOffset ||
            cachedYOffset != yOffset
        ) {

            // 参数变化，重新计算
            cachedPixelRect = toPixelRect(pageWidth, pageHeight, xOffset, yOffset)
            cachedPageWidth = pageWidth
            cachedPageHeight = pageHeight
            cachedXOffset = xOffset
            cachedYOffset = yOffset
        }

        return cachedPixelRect!!
    }

    fun recycle() {
        activeDecodeKey = null
        bitmapState?.let { ImageCache.releaseNode(it) }
        bitmapState = null
        isDecoding = false

        // 重置缓存
        cachedPixelRect = null
        cachedPageWidth = 0f
        cachedPageHeight = 0f
        cachedXOffset = 0f
        cachedYOffset = 0f
    }

    /**
     * @param pageWidth page的缩放后的宽
     * @param pageHeight page的缩放后的高
     * @param xOffset 缩放后的绝对X偏移（currentBounds.left）
     * @param yOffset 缩放后的绝对Y偏移（currentBounds.top）
     */
    fun draw(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        xOffset: Float,
        yOffset: Float,
    ) {
        if (isDecoding) {
            return
        }
        // 页码合法性判断，防止越界
        if (aPage.index < 0 || aPage.index >= pageViewState.list.size) {
            recycle()
            return
        }
        val pixelRect = getCachedPixelRect(pageWidth, pageHeight, xOffset, yOffset)

        // 1. 首先检查是否在预加载区域内（复用pixelRect坐标，零额外对象分配）
        val isInPreloadArea = pageViewState.isTileVisible(
            pixelRect.left, pixelRect.top, pixelRect.right, pixelRect.bottom,
            strictMode = false
        )
        if (!isInPreloadArea) {
            //println("[PageNode.draw] page=${aPage.index}, recycle")
            recycle()  // 完全超出预加载区域，回收
            return
        }

        // 2. 检查是否在严格可见区域内
        val isStrictlyVisible = pageViewState.isTileVisible(
            pixelRect.left, pixelRect.top, pixelRect.right, pixelRect.bottom,
            strictMode = true
        )

        if (null != bitmapState && bitmapState!!.isRecycled()) {
            bitmapState = null
        }

        //println("[PageNode.draw] page=${aPage.index}, isStrictlyVisible:$isStrictlyVisible, isDecoding:$isDecoding $bitmapState")
        // 3. 只有严格可见才绘制
        if (isStrictlyVisible) {
            bitmapState?.let { state ->
                val bitmap = state.bitmap
                //println("[PageNode.draw] page=${aPage.index}, bounds=$bounds, page.W-H=$pageWidth-$pageHeight, xOffset=$xOffset, yOffset=$yOffset, pixelRect=$pixelRect, bitmapSize=${state.bitmap.width}x${state.bitmap.height}")
                // 确保绘制区域没有间隙，使用向下取整的起始位置和向上取整的尺寸
                val dstLeft = (pixelRect.left).toInt()
                val dstTop = (pixelRect.top).toInt()
                val dstWidth = (pixelRect.width()).toInt() + 1
                val dstHeight = (pixelRect.height()).toInt() + 1

                drawRect.set(
                    dstLeft.toFloat(),
                    dstTop.toFloat(),
                    (dstLeft + dstWidth).toFloat(),
                    (dstTop + dstHeight).toFloat()
                )
                canvas.drawBitmap(bitmap, null, drawRect, null)
            }
        }

        // 4. 无论是否绘制，都尝试解码（预加载区域内）
        //if (bitmapState == null && !isDecoding) {
        //    decode(pageWidth, pageHeight, pageViewState.vZoom)
        //}
    }

    fun decode(pageWidth: Float, pageHeight: Float, vZoom: Float) {
        val currentKey = cacheKey

        //println("[PageNode.decode] page=${aPage.index}, isDecoding:$isDecoding, $activeDecodeKey->$currentKey, bitmapState:$bitmapState")
        if (activeDecodeKey == currentKey || isDecoding || (null != bitmapState && !bitmapState!!.isRecycled())) {
            return
        }

        val cachedState = ImageCache.acquireNode(currentKey)
        if (cachedState != null) {
            //println("[PageNode.decode] page=${aPage.index}, cachedState:$cachedState, $currentKey")
            bitmapState?.let { ImageCache.releaseNode(it) }
            bitmapState = cachedState
            activeDecodeKey = currentKey
            return
        }
        isDecoding = true

        if (!isScopeActive()) {
            println("[PageNode.decode] page=${aPage.index}, isScopeActive")
            return
        }

        activeDecodeKey = currentKey

        val width = aPage.getWidth(pageViewState.isCropEnabled())
        val height = aPage.getHeight(pageViewState.isCropEnabled())
        val scale = pageWidth / width
        val tileSpec = TileSpec(
            aPage.index,
            scale,
            bounds,
            pageWidth.toInt(),
            pageHeight.toInt(),
            pageViewState.viewSize,
            cacheKey,
            null
        )

        val left =
            (if (null != aPage.cropBounds && pageViewState.isCropEnabled()) aPage.cropBounds!!.left.toFloat()
            else 1f) * pageWidth / width
        val top =
            (if (null != aPage.cropBounds && pageViewState.isCropEnabled()) aPage.cropBounds!!.top.toFloat()
            else 1f) * pageHeight / height
        val srcRect = RectF(
            bounds.left * pageWidth + left,
            bounds.top * pageHeight + top,
            bounds.right * pageWidth + left,
            bounds.bottom * pageHeight + top
        )
        //println("[PageNode].decode:$pageWidth-$pageHeight, left:$left, $scale, width:$width, $srcRect, bounds:$bounds, $aPage")
        val outWidth = ((srcRect.right - srcRect.left)).toInt()
        val outHeight = ((srcRect.bottom - srcRect.top)).toInt()

        //外面的计算如果出问题了,会在这里拦截,避免崩溃.目前是正常的
        if (outWidth > MAX_BLOCK * 2 || outHeight > MAX_BLOCK * 2) {
            println("[PageNode.decode]:scaled.w-h:$pageWidth-$pageHeight, page.w-h:$width-$height, out.w-h:$outWidth-$outHeight")
            isDecoding = false
            return
        }

        val decodeTask = DecodeTask(
            type = TaskType.NODE,
            pageIndex = aPage.index,
            key = currentKey,
            aPage = aPage,
            zoom = scale,
            pageSliceBounds = srcRect,
            outWidth,
            outHeight,
            crop = pageViewState.isCropEnabled(),
            callback = object : DecodeCallback {
                override fun onDecodeComplete(
                    bitmap: Bitmap?,
                    isThumb: Boolean,
                    error: Throwable?
                ) {
                    if (bitmap != null && !pageViewState.isShutdown()) {
                        //println("[PageNode.onDecodeComplete] page=${aPage.index}")
                        val newState = ImageCache.putNode(currentKey, bitmap)
                        CoroutineScope(Dispatchers.Main).launch {
                            if (pageViewState.isTileVisible(
                                    tileSpec,
                                    strictMode = false
                                ) && !pageViewState.isShutdown()
                                && activeDecodeKey == currentKey
                            ) {
                                bitmapState?.let { ImageCache.releaseNode(it) }
                                bitmapState = newState
                            } else {
                                ImageCache.releaseNode(newState)
                            }
                        }
                        // 解码完成，触发UI刷新
                        pageViewState.notifyDecodeCompleted()
                    } else {
                        if (error != null) {
                            println("PageNode decode error: ${error.message}")
                        }
                    }
                    isDecoding = false
                }

                override fun shouldRender(pageNumber: Int, isFullPage: Boolean): Boolean {
                    //println("[PageNode.shouldRender] page=$pageNumber, $activeDecodeKey, $currentKey")
                    if (activeDecodeKey != currentKey || pageViewState.isShutdown()) {
                        return false
                    }
                    if (pageViewState.vZoom != vZoom) {
                        return false
                    }

                    // 优化：O(1) 快速检查页面是否在可见列表中
                    if (!pageViewState.isPageInVisibleList(pageNumber)) {
                        //println("[PageNode.shouldRender] page=$pageNumber, !isPageInVisibleList")
                        return false
                    }

                    // 对于节点任务，还需要检查具体的tile是否在预加载区域
                    return pageViewState.isTileVisible(tileSpec, strictMode = false)
                }

                override fun onFinish(pageNumber: Int) {
                    if (activeDecodeKey == currentKey) {
                        isDecoding = false
                    }
                }
            }
        )

        pageViewState.decodeService?.submitTask(decodeTask)
    }

    private fun isScopeActive(): Boolean {
        if (pageViewState.isShutdown()) {
            println("[PageNode.decodeScope] page=PageViewState已关闭")
            isDecoding = false
            return false
        }
        return true
    }
}
