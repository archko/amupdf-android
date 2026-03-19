package cn.archko.pdf.core.decoder

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.link.Hyperlink
import cn.archko.pdf.core.utils.SmartCropUtils
import com.archko.reader.image.DjvuLoader
import com.archko.reader.pdf.component.IntSize
import com.archko.reader.pdf.component.Size
import java.io.File

/**
 * @author: archko 2025/11/9 :6:26
 */
class DjvuDecoder(val file: File) : ImageDecoder {

    override var pageCount: Int = 0

    // 私有变量存储原始页面尺寸
    override var originalPageSizes: List<Size> = listOf()

    // 对外提供的缩放后页面尺寸
    override var pageSizes: List<Size> = listOf()

    override var imageSize: IntSize = IntSize(0, 0)

    var viewSize: IntSize = IntSize(0, 0)

    override val aPageList: MutableList<APage> = ArrayList()
    private var pageSizeBean: APageSizeLoader.PageSizeBean? = null
    private var cachePage = true

    // override var cacheBean: ReflowCacheBean? = null
    override var filePath: String? = null

    private val linksCache = mutableMapOf<Int, List<Hyperlink>>()
    private var djvuLoader: DjvuLoader? = null

    companion object {
        /**
         * 渲染封面页面，根据高宽比进行特殊处理
         */
        fun renderCoverPage(
            djvuLoader: DjvuLoader,
            outWidth: Int,
            outHeight: Int,
        ): Bitmap? {
            try {
                if (!djvuLoader.isOpened || djvuLoader.djvuInfo?.pages == 0) {
                    return null
                }

                val pageInfo = djvuLoader.getPageInfo(0) ?: return null
                val pageWidth = pageInfo.width
                val pageHeight = pageInfo.height
                val scaleX = outWidth.toFloat() / pageWidth
                val scaleY = outHeight.toFloat() / pageHeight
                val scale = minOf(scaleX, scaleY)

                println("renderDjvuPage:目标尺寸=$outWidth-$outHeight, 原始=${pageWidth}x${pageHeight}, 缩放=$scale")

                val bitmap = djvuLoader.decodeRegionToBitmap(
                    0,
                    0,
                    0,
                    pageWidth,
                    pageHeight,
                    scale,
                )

                return bitmap
            } catch (e: Exception) {
                println("DjvuDecoder.renderPage error: $e")
                return null
            }
        }
    }

    init {
        if (!file.exists()) {
            throw IllegalArgumentException("文档文件不存在: ${file.absolutePath}")
        }

        if (!file.canRead()) {
            throw SecurityException("无法读取文档文件: ${file.absolutePath}")
        }

        filePath = file.absolutePath
        djvuLoader = DjvuLoader()

        // 先打开文件获取页面数
        djvuLoader!!.openDjvu(file.absolutePath)
        val djvuInfo = djvuLoader!!.djvuInfo
        if (djvuInfo != null) {
            pageCount = djvuInfo.pages
        }

        // 先尝试从缓存加载页面尺寸和切边数据
        initPageSizeBean()

        // 如果缓存不存在或不完整，从文档加载页面尺寸
        if (originalPageSizes.isEmpty()) {
            originalPageSizes = prepareSizes()
        }

        // outlineItems = prepareOutlines()
        cacheCoverIfNeeded()
    }

    private fun initPageSizeBean() {
        try {
            val count: Int = pageCount
            val psb: APageSizeLoader.PageSizeBean? =
                APageSizeLoader.loadPageSizeFromFile(count, file.absolutePath)
            println("DjvuDecoder.initPageSizeBean:$psb")

            if (null != psb && psb.list != null && psb.list!!.size == count) {
                // 缓存存在且完整，直接使用
                pageSizeBean = psb
                aPageList.addAll(psb.list as MutableList)

                // 从缓存构建 originalPageSizes，避免重复加载页面
                val list = mutableListOf<Size>()
                var totalHeight = 0
                for (aPage in psb.list!!) {
                    val size = Size(
                        aPage.width.toInt(),
                        aPage.height.toInt(),
                        aPage.index,
                        scale = 1.0f,
                        totalHeight,
                    )
                    totalHeight += size.height
                    list.add(size)
                }
                originalPageSizes = list
                println("DjvuDecoder.initPageSizeBean: 从缓存加载了 ${list.size} 个页面尺寸")
                return
            }

            // 缓存不存在或不完整，需要从文档加载
            pageSizeBean = APageSizeLoader.PageSizeBean()
            pageSizeBean!!.list = aPageList
        } catch (e: Exception) {
            println("DjvuDecoder.initPageSizeBean error: ${e.message}")
            aPageList.clear()
        }
    }

    /**
     * 检查并缓存封面图片
     */
    private fun cacheCoverIfNeeded() {
        val path = file.absolutePath
        try {
            if (null == djvuLoader || null != ImageCache.acquirePage(path)) {
                return
            }
            val bitmap = renderCoverPage(djvuLoader!!, 160, 200)

            //CustomImageFetcher.cacheBitmap(bitmap, path)
        } catch (e: Exception) {
            println("缓存封面失败: ${e.message}")
        }
    }

    override fun size(viewportSize: IntSize): IntSize {
        if ((imageSize == IntSize(0, 0) || viewSize != viewportSize)
            && viewportSize.width > 0 && viewportSize.height > 0
        ) {
            viewSize = viewportSize
            calculateSize(viewportSize)
        }
        return imageSize
    }

    private fun calculateSize(viewportSize: IntSize) {
        if (originalPageSizes.isNotEmpty()) {
            // 文档宽度直接使用viewportSize.width
            val documentWidth = viewportSize.width
            var totalHeight = 0f  // 使用浮点数累积，避免舍入误差

            // 计算缩放后的页面尺寸
            val scaledPageSizes = mutableListOf<Size>()

            for (i in originalPageSizes.indices) {
                val originalPage = originalPageSizes[i]
                // 计算每页的缩放比例，使宽度等于viewportSize.width
                val scale = 1f * documentWidth / originalPage.width
                val scaledWidth = documentWidth
                val scaledHeight = originalPage.height * scale  // 保持浮点数精度

                // 创建缩放后的页面尺寸，yOffset使用当前累积的浮点数值转换为整数
                val scaledPage =
                    Size(scaledWidth, scaledHeight.toInt(), i, scale, totalHeight.toInt())
                scaledPageSizes.add(scaledPage)
                totalHeight += scaledHeight  // 浮点数累积
            }

            // 更新对外提供的页面尺寸
            pageSizes = scaledPageSizes
            imageSize = IntSize(documentWidth, totalHeight.toInt())
            println("DjvuDecoder.caculateSize: documentWidth=$documentWidth, totalHeight=$totalHeight, pageCount=${originalPageSizes.size}")
        }
    }

    /**
     * 获取原始页面尺寸
     */
    fun getOriginalPageSize(index: Int): Size {
        return originalPageSizes[index]
    }

    override fun close() {
        djvuLoader?.close()
        if (cachePage && aPageList.isNotEmpty()) {
            println("DjvuDecoder.close:${aPageList.size}")
            APageSizeLoader.savePageSizeToFile(false, file.absolutePath, aPageList)
        }

        linksCache.clear()
        ImageCache.clear()
    }

    private fun prepareSizes(): List<Size> {
        val list = mutableListOf<Size>()

        if (djvuLoader == null || !djvuLoader!!.isOpened) {
            println("DjvuDecoder: DjvuLoader not opened")
            return list
        }

        var totalHeight = 0
        println("DjVu document has $pageCount pages")

        for (i in 0..pageCount) {
            val pageInfo = djvuLoader!!.getPageInfo(i)
            if (pageInfo != null) {
                val width = pageInfo.width
                val height = pageInfo.height
                val size = Size(
                    width,
                    height,
                    i,
                    scale = 1.0f,
                    totalHeight,
                )
                totalHeight += size.height
                list.add(size)

                // 同时填充 aPageList
                val aPage = APage(i, width.toFloat(), height.toFloat(), 1f)
                aPageList.add(aPage)
            }
        }

        // 保存到缓存
        if (cachePage && aPageList.isNotEmpty()) {
            APageSizeLoader.savePageSizeToFile(false, file.absolutePath, aPageList)
        }

        println("DjvuDecoder.prepareSizes: 从文档加载了 ${list.size} 个页面尺寸")
        return list
    }

    /**
     * 获取页面上的链接
     * @param pageIndex 页面索引
     * @return 链接列表
     */
    override fun getPageLinks(pageIndex: Int): List<Hyperlink> {
        if (linksCache.containsKey(pageIndex)) {
            return linksCache[pageIndex]!!
        }

        if (djvuLoader == null) {
            return emptyList()
        }

        return emptyList()
    }

    private fun decodePageLinks(pageIndex: Int): List<Hyperlink> {
        if (linksCache.containsKey(pageIndex)) {
            return linksCache[pageIndex]!!
        }
        return try {
            val links = djvuLoader!!.getPageLinks(pageIndex)

            if (links == null) {
                linksCache[pageIndex] = emptyList()
                return emptyList()
            }

            val hyperlinks = mutableListOf<Hyperlink>()

            for (link in links) {
                val hyperlink = Hyperlink()
                hyperlink.bbox = Rect(
                    link.x,
                    link.y,
                    link.x + link.width,
                    link.y + link.height
                )

                if (link.page >= 0) {
                    // 页面链接
                    hyperlink.linkType = Hyperlink.LINKTYPE_PAGE
                    hyperlink.page = link.page
                    hyperlink.url = null
                } else if (link.url != null) {
                    // URL链接
                    hyperlink.linkType = Hyperlink.LINKTYPE_URL
                    hyperlink.url = link.url
                    hyperlink.page = -1
                } else {
                    // 无效链接，跳过
                    continue
                }

                hyperlinks.add(hyperlink)
            }

            linksCache[pageIndex] = hyperlinks
            //println("DjvuDecoder.decodePageLinks: page=$pageIndex, links=${hyperlinks.size}")

            hyperlinks
        } catch (e: Exception) {
            println("获取页面链接失败: $e")
            emptyList()
        }
    }

    override fun renderPageRegion(
        region: RectF,
        index: Int,
        scale: Float,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return try {
            val pageWidth = (region.width() / scale).toInt()
            val pageHeight = (region.height() / scale).toInt()
            val patchX = (region.left / scale).toInt()
            val patchY = (region.top / scale).toInt()
            //精度的问题,jni那边不支持小数进位四舍五入,所以这里的高宽要修正一下.
            val bitmap = djvuLoader!!.decodeRegionToBitmap(
                index,
                patchX,
                patchY,
                pageWidth,
                pageHeight,
                scale,
            )

            println("DjvuDecoder.renderPageRegion:index:$index, scale:$scale, patch:$patchX-$patchY-page:$pageWidth-$pageHeight, region:$region")

            bitmap ?: Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            println("renderPageRegion error for file ${file.absolutePath}: $e")
            Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }
    }

    override fun renderPage(
        aPage: APage,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int,
        crop: Boolean
    ): Bitmap {
        try {
            val index = aPage.index
            val originalSize = originalPageSizes[index]

            if (aPage.cropBounds != null && crop) {
                // 有切边信息且启用切边
                val cropBounds = aPage.cropBounds!!

                val scaleX = outWidth.toFloat() / cropBounds.width()
                val scaleY = outHeight.toFloat() / cropBounds.height()
                val scale = minOf(scaleX, scaleY)

                println("DjvuDecoder.renderPage:croped page=$index, $outWidth-$outHeight, 切边后尺寸=${(scale * cropBounds.width()).toInt()}x${(scale * cropBounds.height()).toInt()}, bounds=$cropBounds")

                val bitmap = djvuLoader!!.decodeRegionToBitmap(
                    index,
                    cropBounds.left,
                    cropBounds.top,
                    cropBounds.width(),
                    cropBounds.height(),
                    scale,
                )

                // 在解码缩略图时同时解析链接
                parseLinksIfNeeded(index, false, true)

                return bitmap ?: Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
            } else {
                // 没有切边信息或未启用切边
                val scaleX = outWidth.toFloat() / originalSize.width
                val scaleY = outHeight.toFloat() / originalSize.height
                val scale = minOf(scaleX, scaleY)

                println("DjvuDecoder.renderPage:page=$index, 目标尺寸=$outWidth-$outHeight, 原始=${originalSize.width}x${originalSize.height}, 缩放=$scale")

                val bitmap = djvuLoader!!.decodeRegionToBitmap(
                    index,
                    0,
                    0,
                    originalSize.width,
                    originalSize.height,
                    scale,
                )

                val bitmapResult =
                    bitmap ?: Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)

                // 在解码缩略图时同时解析链接
                parseLinksIfNeeded(index, false, true)

                // 如果启用了切边功能但没有cropBounds，检测并设置
                if (crop && bitmap != null) {
                    val cropBounds = SmartCropUtils.detectSmartCropBounds(bitmap)
                    if (cropBounds != null) {
                        // 将缩略图坐标转换为原始DjVu坐标
                        val ratio = originalSize.width.toFloat() / bitmap.width

                        val leftBound = (cropBounds.left * ratio)
                        val topBound = (cropBounds.top * ratio)
                        val rightBound = (cropBounds.right * ratio)
                        val bottomBound = (cropBounds.bottom * ratio)
                        val djvuCropBounds = Rect(
                            leftBound.toInt(),
                            topBound.toInt(),
                            rightBound.toInt(),
                            bottomBound.toInt()
                        )

                        println("DjvuDecoder.cropBounds:$index, 原始尺寸=${originalSize.width}x${originalSize.height}, 切边区域=($cropBounds), 切边后尺寸=${djvuCropBounds}")

                        if (djvuCropBounds.width() < 0 || djvuCropBounds.height() < 0) {
                            aPage.setCropBounds(
                                Rect(
                                    0,
                                    0,
                                    bitmap.width,
                                    bitmap.height
                                )
                            )
                            return bitmap
                        }
                        aPage.setCropBounds(djvuCropBounds)

                        // 真正对图片进行切边处理
                        val croppedBitmap = cropBitmap(index, bitmap, cropBounds)
                        return croppedBitmap
                    }
                }

                return bitmapResult
            }

        } catch (e: Exception) {
            println("DjvuDecoder.renderPage error: $e")
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }
    }

    /**
     * 在解码缩略图时同时解析链接
     * @param pageIndex 页面索引
     * @param forceParse 是否强制解析（即使已缓存）
     */
    private fun parseLinksIfNeeded(
        pageIndex: Int,
        forceParse: Boolean = false,
        decodeLink: Boolean
    ) {
        if (!decodeLink) {
            return
        }
        if (!forceParse && linksCache.containsKey(pageIndex)) {
            return
        }

        decodePageLinks(pageIndex)
    }

    /**
     * 对图片进行切边处理
     */
    private fun cropBitmap(
        index: Int,
        originalBitmap: Bitmap,
        cropBounds: Rect
    ): Bitmap {
        val cropX = cropBounds.left
        val cropY = cropBounds.top
        val cropWidth = cropBounds.right - cropX
        val cropHeight = cropBounds.bottom - cropY

        // 确保切边区域在图片范围内
        val safeX = cropX.coerceIn(0, originalBitmap.width - 1)
        val safeY = cropY.coerceIn(0, originalBitmap.height - 1)
        val safeWidth = cropWidth.coerceIn(1, originalBitmap.width - safeX)
        val safeHeight = cropHeight.coerceIn(1, originalBitmap.height - safeY)

        // 创建切边后的图片 - 使用Android Bitmap进行切边
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            safeX,
            safeY,
            safeWidth,
            safeHeight
        )

        println("DjvuDecoder.cropBitmap:$index, 原始尺寸=${originalBitmap.width}x${originalBitmap.height}, 切边区域=($safeX,$safeY,$safeWidth,$safeHeight), 切边后尺寸=${croppedBitmap.width}x${croppedBitmap.height}")

        return croppedBitmap
    }

    override fun getStructuredText(index: Int): Any? {
        return djvuLoader
    }

    /**
     * 解析单个页面的文本内容（用于TTS快速启动）
     */
    override fun decodeReflowSinglePage(pageIndex: Int): ReflowBean? {
        if (djvuLoader == null || !djvuLoader!!.isOpened) {
            return null
        }

        if (pageIndex < 0 || pageIndex >= originalPageSizes.size) {
            return null
        }

        return try {
            val text = djvuLoader!!.getPageText(pageIndex)

            if (null != text && text.isNotEmpty() && text.isNotBlank()) {
                val pageText = text.trim()
                if (pageText.length > 10) {
                    ReflowBean(
                        data = pageText,
                        type = ReflowBean.TYPE_STRING,
                        page = pageIndex.toString()
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("TTS: 解码第${pageIndex + 1}页失败: ${e.message}")
            null
        }
    }

    /**
     * 解析所有页面的文本内容（用于TTS后台缓存）
     */
    override fun decodeReflowAllPages(): List<ReflowBean> {
        if (djvuLoader == null || !djvuLoader!!.isOpened) {
            return emptyList()
        }

        val totalPages = originalPageSizes.size
        println("TTS: 开始解析所有页面，共${totalPages}页")
        val allTexts = mutableListOf<ReflowBean>()

        var addedPages = 0
        var skippedPages = 0

        for (currentPage in 0 until totalPages) {
            try {
                val text = djvuLoader!!.getPageText(currentPage)

                if (null != text && text.isNotEmpty() && text.isNotBlank()) {
                    val pageText = text.trim()
                    if (pageText.length > 10) { // 只添加有意义的文本
                        allTexts.add(
                            ReflowBean(
                                data = pageText,
                                type = ReflowBean.TYPE_STRING,
                                page = currentPage.toString()
                            )
                        )
                        addedPages++
                    } else {
                        println("TTS: 第${currentPage + 1}页文本太短: ${pageText.length}")
                        skippedPages++
                    }
                } else {
                    println("TTS: 第${currentPage + 1}页无文本内容")
                    skippedPages++
                }
            } catch (e: Exception) {
                println("TTS: 解码第${currentPage + 1}页失败: ${e.message}")
                skippedPages++
            }
        }

        println("TTS: 解析完成，有效页数=$addedPages，跳过页数=$skippedPages")
        return allTexts
    }
}