package cn.archko.pdf.core.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.ParseTextMain
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.link.Hyperlink
import cn.archko.pdf.core.utils.FileTypeUtils
import cn.archko.pdf.core.utils.FontCSSGenerator
import cn.archko.pdf.core.utils.SmartCropUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.component.IntSize
import cn.archko.pdf.core.component.Size
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File

/**
 * @author: archko 2025/4/11 :11:26
 */
class PdfDecoder(val file: File) : ImageDecoder {

    private var document: Document? = null
    override var pageCount: Int = 0

    // 私有变量存储原始页面尺寸
    override var originalPageSizes: List<Size> = listOf()

    // 对外提供的缩放后页面尺寸
    override var pageSizes: List<Size> = listOf()

    override var imageSize: IntSize = IntSize(0, 0)

    var viewSize: IntSize = IntSize(0, 0)

    // 密码相关状态
    var needsPassword: Boolean = false
    var isAuthenticated: Boolean = false

    // 页面缓存，最多缓存8页
    private val pageCache = mutableMapOf<Int, Page>()
    private val maxPageCache = 8

    override val aPageList: MutableList<APage>? = ArrayList()
    private var pageSizeBean: APageSizeLoader.PageSizeBean? = null
    private var cachePage = true

    //override var cacheBean: ReflowCacheBean? = null
    override var filePath: String? = null

    // 链接缓存，避免重复解析
    private val linksCache = mutableMapOf<Int, List<Hyperlink>>()

    companion object {
        /**
         * 渲染封面页面，根据高宽比进行特殊处理
         */
        fun renderCoverPage(
            path: String,
            page: Page,
            targetWidth: Int = 160,
            targetHeight: Int = 200
        ): Bitmap? {
            val pWidth = page.bounds.x1 - page.bounds.x0
            val pHeight = page.bounds.y1 - page.bounds.y0

            // 检查是否为极端长宽比的图片（某边大于8000）
            return if (pWidth > 8000 || pHeight > 8000) {
                // 对于极端长宽比，先缩放到目标尺寸之一，再截取
                val scale = if (pWidth > pHeight) {
                    targetWidth.toFloat() / pWidth
                } else {
                    targetHeight.toFloat() / pHeight
                }

                val scaledWidth = (pWidth * scale).toInt()
                val scaledHeight = (pHeight * scale).toInt()

                val cropWidth = maxOf(targetWidth, scaledWidth)
                val cropHeight = maxOf(targetHeight, scaledHeight)
                println("decode.thumb:$path, large.width-height:$cropWidth-$cropHeight")
                val cropBitmap = BitmapPool.getInstance().acquire(cropWidth, cropHeight)
                val cropDev = AndroidDrawDevice(cropBitmap, 0, 0, 0, 0, cropWidth, cropHeight)
                val cropCtm = Matrix()
                cropCtm.scale(scale, scale)
                page.run(cropDev, cropCtm, null)
                cropDev.close()
                cropDev.destroy()
                cropBitmap
            } else if (pWidth > pHeight) {
                // 对于宽大于高的页面，按最大比例缩放后截取
                val scale = maxOf(targetWidth.toFloat() / pWidth, targetHeight.toFloat() / pHeight)

                val scaledWidth = (pWidth * scale).toInt()
                val scaledHeight = (pHeight * scale).toInt()

                // 确保裁剪区域不超过目标尺寸
                val cropWidth = maxOf(targetWidth, scaledWidth)
                val cropHeight = maxOf(targetHeight, scaledHeight)

                println("decode.thumb:$path, wide.width-height:$cropWidth-$cropHeight")
                val cropBitmap = BitmapPool.getInstance().acquire(cropWidth, cropHeight)
                val cropDev = AndroidDrawDevice(cropBitmap, 0, 0, 0, 0, cropWidth, cropHeight)
                val cropCtm = Matrix()
                cropCtm.scale(scale, scale)
                page.run(cropDev, cropCtm, null)
                cropDev.close()
                cropDev.destroy()
                cropBitmap
            } else {
                // 原始逻辑处理其他情况
                val xscale = targetWidth.toFloat() / pWidth
                val yscale = targetHeight.toFloat() / pHeight

                // 使用最大比例以确保填充整个目标区域
                val scale = maxOf(xscale, yscale)

                val scaledWidth = (pWidth * scale).toInt()
                val scaledHeight = (pHeight * scale).toInt()

                // 确保裁剪区域不超过目标尺寸
                val cropWidth = maxOf(targetWidth, scaledWidth)
                val cropHeight = maxOf(targetHeight, scaledHeight)

                println("decode.thumb:$path, width-height:$cropWidth-$cropHeight")
                val cropBitmap = BitmapPool.getInstance().acquire(cropWidth, cropHeight)
                val cropDev = AndroidDrawDevice(cropBitmap, 0, 0, 0, 0, cropWidth, cropHeight)
                val cropCtm = Matrix()
                cropCtm.scale(scale, scale)
                page.run(cropDev, cropCtm, null)
                cropDev.close()
                cropDev.destroy()
                cropBitmap
            }
        }
    }

    init {
        // 检查文件是否存在
        if (!file.exists()) {
            throw IllegalArgumentException("文档文件不存在: ${file.absolutePath}")
        }

        // 检查文件是否可读
        if (!file.canRead()) {
            throw SecurityException("无法读取文档文件: ${file.absolutePath}")
        }

        try {
            filePath = file.absolutePath
            if (FileTypeUtils.isReflowable(file.absolutePath)) {
                val css = FontCSSGenerator.generateFontCSS(FontCSSGenerator.getFontFace(), "10px")
                if (!TextUtils.isEmpty(css)) {
                    println("应用自定义CSS: $css")
                }
                com.artifex.mupdf.fitz.Context.setUserCSS(css)
            }
            document = Document.openDocument(file.absolutePath)
            // 检查是否需要密码
            needsPassword = document?.needsPassword() == true
            if (!needsPassword) {
                isAuthenticated = true // 不需要密码的文档直接设置为已认证
                initializeDocument()
            }
        } catch (e: Exception) {
            throw RuntimeException("无法打开文档: ${file.absolutePath}, 错误: ${e.message}", e)
        }
    }

    /**
     * 使用密码认证文档
     * @param password 密码
     * @return 认证是否成功
     */
    fun authenticatePassword(password: String): Boolean {
        return try {
            val success = document?.authenticatePassword(password) == true
            if (success) {
                isAuthenticated = true
                needsPassword = false
                initializeDocument()
            }
            success
        } catch (e: Exception) {
            println("密码认证失败: ${e.message}")
            false
        }
    }

    /**
     * 初始化文档（在认证成功后调用）
     */
    private fun initializeDocument() {
        document?.let { doc ->
            if (FileTypeUtils.isReflowable(file.absolutePath)) {
                val fontSize = FontCSSGenerator.getDefFontSize()
                val fs = fontSize.toInt().toFloat()
                val w: Float =
                    Utils.getScreenWidthPixelWithOrientation(App.instance as Context).toFloat()
                val h: Float =
                    Utils.getScreenHeightPixelWithOrientation(App.instance as Context).toFloat()
                System.out.printf(
                    "width:%s, height:%s, font:%s->%s, open:%s\n",
                    w,
                    h,
                    fontSize,
                    fs,
                    file.absolutePath
                )
                doc.layout(w, h, fontSize)
            }
            pageCount = doc.countPages()

            // 先尝试从缓存加载页面尺寸和切边数据
            initPageSizeBean()

            // 如果缓存不存在或不完整，从文档加载页面尺寸
            if (originalPageSizes.isEmpty()) {
                originalPageSizes = prepareSizes()
            }

            // outlineItems = prepareOutlines()
            cacheCoverIfNeeded()
        }
    }

    private fun initPageSizeBean() {
        try {
            val count: Int = pageCount
            val psb: APageSizeLoader.PageSizeBean? =
                APageSizeLoader.loadPageSizeFromFile(count, file.absolutePath)
            println("PdfDecoder.initPageSizeBean:$psb")

            if (null != psb && psb.list != null && psb.list!!.size == count) {
                // 缓存存在且完整，直接使用
                pageSizeBean = psb
                aPageList!!.addAll(psb.list as MutableList)

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
                println("PdfDecoder.initPageSizeBean: 从缓存加载了 ${list.size} 个页面尺寸")
                return
            }

            // 缓存不存在或不完整，需要从文档加载
            pageSizeBean = APageSizeLoader.PageSizeBean()
            pageSizeBean!!.list = aPageList
        } catch (e: Exception) {
            println("PdfDecoder.initPageSizeBean error: ${e.message}")
            aPageList!!.clear()
        }
    }

    /**
     * 检查并缓存封面图片
     */
    private fun cacheCoverIfNeeded() {
        val path = file.absolutePath
        try {
            if (null != ImageCache.acquirePage(path)) {
                return
            }
            val page = getPage(0)
            val bitmap = renderCoverPage(path, page)

            //CustomImageFetcher.cacheBitmap(bitmap, path)
        } catch (e: Exception) {
            println("缓存封面失败: ${e.message}")
        }
    }

    override fun size(viewportSize: IntSize): IntSize {
        if ((imageSize == IntSize(0, 0) || viewSize != viewportSize)
            && viewportSize.width > 0 && viewportSize.height > 0
            && document != null && (isAuthenticated || !needsPassword)
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
            var totalHeight = 0

            // 计算缩放后的页面尺寸
            val scaledPageSizes = mutableListOf<Size>()

            for (i in originalPageSizes.indices) {
                val originalPage = originalPageSizes[i]
                // 计算每页的缩放比例，使宽度等于viewportSize.width
                val scale = 1f * documentWidth / originalPage.width
                val scaledWidth = documentWidth
                val scaledHeight = (originalPage.height * scale).toInt()

                // 创建缩放后的页面尺寸
                val scaledPage = Size(scaledWidth, scaledHeight, i, scale, totalHeight)
                scaledPageSizes.add(scaledPage)
                totalHeight += scaledHeight

                //println("PdfDecoder.caculateSize: page $i - original: ${originalPage.width}x${originalPage.height}, scale: $scale, scaled: ${scaledWidth}x${scaledHeight}")
            }

            // 更新对外提供的页面尺寸
            pageSizes = scaledPageSizes

            imageSize = IntSize(documentWidth, totalHeight)
            println("PdfDecoder.caculateSize: documentWidth=$documentWidth, totalHeight=$totalHeight, pageCount=${originalPageSizes.size}")
        }
    }

    /**
     * 获取原始页面尺寸
     */
    fun getOriginalPageSize(index: Int): Size {
        return originalPageSizes[index]
    }

    override fun close() {
        if (cachePage && !aPageList.isNullOrEmpty()) {
            println("PdfDecoder.close:${aPageList.size}")
            APageSizeLoader.savePageSizeToFile(false, file.absolutePath, aPageList)
        }

        // 清理页面缓存
        pageCache.values.forEach { page ->
            try {
                page.destroy()
            } catch (e: Exception) {
                println("Error destroying cached page: $e")
            }
        }
        pageCache.clear()

        // 清理链接缓存
        linksCache.clear()

        document?.destroy()
        document = null

        ImageCache.clear()
        BitmapPool.getInstance().clear()
    }

    private fun prepareSizes(): List<Size> {
        val list = mutableListOf<Size>()
        var totalHeight = 0
        document?.let { doc ->
            for (i in 0 until pageCount) {
                val page = doc.loadPage(i)
                val bounds = page.bounds
                val width = bounds.x1.toInt() - bounds.x0.toInt()
                val height = bounds.y1.toInt() - bounds.y0.toInt()
                val size = Size(
                    width,
                    height,
                    i,
                    scale = 1.0f,
                    totalHeight,
                )
                totalHeight += size.height
                page.destroy()
                list.add(size)

                // 同时填充 aPageList
                val aPage = APage(i, width.toFloat(), height.toFloat(), 1f)
                aPageList!!.add(aPage)
            }

            // 保存到缓存
            if (cachePage && aPageList!!.isNotEmpty()) {
                APageSizeLoader.savePageSizeToFile(false, file.absolutePath, aPageList)
            }
        }
        println("PdfDecoder.prepareSizes: 从文档加载了 ${list.size} 个页面尺寸")
        return list
    }

    /**
     * 获取页面上的链接
     * @param pageIndex 页面索引
     * @return 链接列表
     */
    override fun getPageLinks(pageIndex: Int): List<Hyperlink> {
        // 先检查缓存
        if (linksCache.containsKey(pageIndex)) {
            return linksCache[pageIndex]!!
        }

        if (document == null || (!isAuthenticated && needsPassword)) {
            return emptyList()
        }

        return emptyList()
    }

    private fun decodePageLinks(pageIndex: Int): List<Hyperlink> {
        // 先检查缓存
        if (linksCache.containsKey(pageIndex)) {
            return linksCache[pageIndex]!!
        }

        if (document == null || (!isAuthenticated && needsPassword)) {
            return emptyList()
        }

        return try {
            val page = getPage(pageIndex)
            val links = page.links ?: return emptyList()

            val hyperlinks = mutableListOf<Hyperlink>()

            for (link in links) {
                val hyperlink = Hyperlink()
                hyperlink.bbox = Rect(
                    link.bounds.x0.toInt(),
                    link.bounds.y0.toInt(),
                    link.bounds.x1.toInt(),
                    link.bounds.y1.toInt()
                )

                val location = document!!.resolveLink(link)
                val targetPage = document!!.pageNumberFromLocation(location)

                if (targetPage >= 0) {
                    // 页面链接
                    hyperlink.linkType = Hyperlink.LINKTYPE_PAGE
                    hyperlink.page = targetPage
                    hyperlink.url = null
                } else {
                    // URL链接
                    hyperlink.linkType = Hyperlink.LINKTYPE_URL
                    hyperlink.url = link.uri
                    hyperlink.page = -1
                }

                hyperlinks.add(hyperlink)
            }

            // 缓存结果
            linksCache[pageIndex] = hyperlinks
            println("PdfDecoder.getPageLinks: page=$pageIndex, links=${hyperlinks.size}")

            hyperlinks
        } catch (e: Exception) {
            println("获取页面链接失败: $e")
            emptyList()
        }
    }

    private fun decode(
        index: Int,
        scale: Float,
        bitmap: Bitmap,
        patchX: Int,
        patchY: Int,
        decodeLink: Boolean
    ) {
        val ctm = Matrix(scale)
        val dev = AndroidDrawDevice(bitmap, patchX, patchY, 0, 0, bitmap.width, bitmap.height)

        val page = getPage(index)
        page.run(dev, ctm, null)

        // 在解码缩略图时同时解析链接
        parseLinksIfNeeded(index, false, decodeLink)

        dev.close()
        dev.destroy()
    }

    override fun renderPage(
        aPage: APage,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int,
        crop: Boolean
    ): Bitmap {
        if (document == null || (!isAuthenticated && needsPassword)) {
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }

        try {
            val index = aPage.index
            if (aPage.cropBounds != null && crop) {
                val start = System.currentTimeMillis()
                val cropBounds = aPage.cropBounds!!

                val scaleX = outWidth.toFloat() / cropBounds.width()
                val scaleY = outHeight.toFloat() / cropBounds.height()
                val scale = minOf(scaleX, scaleY)

                val patchX = cropBounds.left * scale
                val patchY = cropBounds.top * scale
                val height = scale * cropBounds.height()
                val bitmap =
                    acquireReusableBitmap((scale * cropBounds.width()).toInt(), height.toInt())

                decode(index, scale, bitmap, patchX.toInt(), patchY.toInt(), true)
                println("PdfDecoder.renderPage:croped page=$index, cos:${System.currentTimeMillis() - start}, $outWidth-$outHeight, 切边后尺寸=${bitmap.width}x${bitmap.height}, patch:$patchX-$patchY, bounds=$cropBounds")
                return bitmap
            } else {
                val start = System.currentTimeMillis()
                val cropBounds = Rect(0, 0, aPage.width.toInt(), aPage.height.toInt())

                val patchX = cropBounds.left
                val patchY = cropBounds.top
                // 根据输出尺寸计算合适的缩放比例
                val originalSize = originalPageSizes[index]
                val scaleX = outWidth.toFloat() / originalSize.width
                val scaleY = outHeight.toFloat() / originalSize.height
                val scale = minOf(scaleX, scaleY)
                val height = scale * aPage.getHeight(crop)
                val bitmap = acquireReusableBitmap(outWidth, height.toInt())

                decode(index, scale, bitmap, patchX, patchY, true)
                println("PdfDecoder.renderPage:page=$index, cos:${System.currentTimeMillis() - start}, 目标尺寸=$outWidth-$outHeight, patch:$patchX-$patchY, bounds=$cropBounds")
                // 如果启用了切边功能但没有cropBounds，检测并设置
                if (crop) {
                    val cropBounds = SmartCropUtils.detectSmartCropBounds(bitmap)
                    if (cropBounds != null) {
                        // 将缩略图坐标转换为原始PDF坐标
                        val originalPage = originalPageSizes[index]
                        val ratio = originalPage.width.toFloat() / outWidth

                        // 使用宽度比例转换左右边界，使用高度比例转换上下边界
                        val leftBound = (cropBounds.left * ratio)
                        val topBound = (cropBounds.top * ratio)
                        val rightBound = (cropBounds.right * ratio)
                        val bottomBound = (cropBounds.bottom * ratio)
                        val pdfCropBounds = Rect(
                            leftBound.toInt(),
                            topBound.toInt(),
                            rightBound.toInt(),
                            bottomBound.toInt()
                        )

                        println("PdfDecoder.cropBounds:$index, 原始尺寸=${originalPage.width}x${originalPage.height}, 切边区域=($cropBounds), 切边后尺寸=${pdfCropBounds}")
                        if (pdfCropBounds.width() < 0 || pdfCropBounds.height() < 0) {
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
                        aPage.setCropBounds(pdfCropBounds)

                        // 真正对图片进行切边处理
                        val croppedBitmap = cropBitmap(index, bitmap, cropBounds)
                        return croppedBitmap
                    }
                }
                return bitmap
            }
        } catch (e: Exception) {
            println("PdfDecoder.renderPage error: $e")
            // 返回一个空的位图，避免崩溃
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
        val cropX = cropBounds.left.toInt()
        val cropY = cropBounds.top.toInt()
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

        println("PdfDecoder.cropBitmap:$index, 原始尺寸=${originalBitmap.width}x${originalBitmap.height}, 切边区域=($safeX,$safeY,$safeWidth,$safeHeight), 切边后尺寸=${croppedBitmap.width}x${croppedBitmap.height}")

        return croppedBitmap
    }

    override fun renderPageRegion(
        region: RectF,
        index: Int,
        scale: Float,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        if (document == null || (!isAuthenticated && needsPassword)) {
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }

        try {
            val patchX = region.left
            val patchY = region.top

            val start = System.currentTimeMillis()
            val bitmap = acquireReusableBitmap(outWidth, outHeight)
            decode(index, scale, bitmap, patchX.toInt(), patchY.toInt(), false)
            println("PdfDecoder.renderPageRegion:index:$index, cos:${System.currentTimeMillis() - start}, scale:$scale, w-h:$outWidth-$outHeight, offset:$patchX-$patchY, bounds=$region")

            /*val file = File(
                Environment.getExternalStorageDirectory(),
                "/Download/$index-${region.left}-${region.top}-${region.right}-${region.bottom}.png"
            )
            println("PdfDecoder.renderPageRegion.path:${file.absolutePath}")
            BitmapUtils.saveBitmapToFile(bitmap, file)*/
            return bitmap
        } catch (e: Exception) {
            println("PdfDecoder.renderPageRegion error: $e")
            // 返回一个空的位图，避免崩溃
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }
    }

    override fun getStructuredText(index: Int): Any? {
        if (document == null || (!isAuthenticated && needsPassword)) {
            return null
        }
        val page = getPage(index)
        return page.toStructuredText()
    }

    /**
     * 解析PDF页面为reflow内容（文本和图片）
     * 注意：此方法必须在主线程调用，MuPDF不支持多线程
     */
    fun decodeReflowItem(pageIndex: Int): List<ReflowBean> {
        val reflowBeans = mutableListOf<ReflowBean>()

        if (document == null || (!isAuthenticated && needsPassword)) {
            return reflowBeans
        }

        try {
            val page = getPage(pageIndex)

            // 提取文本内容
            val result = page.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")
            val text = if (null != result) {
                ParseTextMain.parseAsHtmlList(result, pageIndex)
            } else null

            if (text != null && text.isNotEmpty()) {
                reflowBeans.addAll(text)
            }

            // 注意：这里不调用page.destroy()，因为页面被缓存了
        } catch (e: Exception) {
            println("decodeReflow error for page $pageIndex: $e")
            // 如果解析失败，返回空列表
        }

        return reflowBeans
    }

    /**
     * 解析单个页面的文本内容（用于TTS快速启动）
     */
    override fun decodeReflowSinglePage(pageIndex: Int): ReflowBean? {
        if (document == null || (!isAuthenticated && needsPassword)) {
            return null
        }

        if (pageIndex < 0 || pageIndex >= originalPageSizes.size) {
            return null
        }

        return try {
            val page = getPage(pageIndex)
            val result = page.textAsText("preserve-whitespace,inhibit-spaces")
            val text = if (null != result) {
                ParseTextMain.parseAsText(result)
            } else null

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
        if (document == null || (!isAuthenticated && needsPassword)) {
            return emptyList()
        }

        val totalPages = originalPageSizes.size
        println("TTS: 开始解析所有页面，共${totalPages}页")
        val allTexts = mutableListOf<ReflowBean>()

        var addedPages = 0
        var skippedPages = 0

        for (currentPage in 0 until totalPages) {
            try {
                val page = getPage(currentPage)
                val result = page.textAsText("preserve-whitespace,inhibit-spaces")
                val text = if (null != result) {
                    ParseTextMain.parseAsText(result)
                } else null

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

    /**
     * 获取或创建页面，支持缓存
     */
    private fun getPage(index: Int): Page {
        // 如果缓存已满且当前索引不在缓存中，移除最旧的项
        if (pageCache.size >= maxPageCache && !pageCache.containsKey(index)) {
            val oldestIndex = pageCache.keys.first()
            val oldestPage = pageCache.remove(oldestIndex)
            oldestPage?.destroy()
            //println("Removed page $oldestIndex from cache to make room for page $index")
        }

        return pageCache.getOrPut(index) {
            document!!.loadPage(index)
        }
    }

    /**
     * 优先尝试从ImageCache/BitmapPool复用Bitmap
     */
    private fun acquireReusableBitmap(width: Int, height: Int): Bitmap {
        // 优先从BitmapPool获取，这样可以复用已回收的bitmap
        return BitmapPool.getInstance().acquire(width, height)
    }

    // ========== 切边相关方法 ==========

    /**
     * 在Bitmap上绘制cropBounds矩形
     * @param bitmap 原始图片
     * @param cropBounds 切边区域
     * @return 绘制了cropBounds的Bitmap
     */
    /*private fun drawCropBoundsOnBitmap(
        bitmap: Bitmap,
        cropBounds: Rect,
        index: Int
    ): Bitmap {
        // 创建可变的bitmap副本用于绘制
        val mutableBitmap = bitmap.copy(bitmap.config!!, true)
        val canvas = Canvas(mutableBitmap)

        // 创建画笔
        val paint = Paint().apply {
            color = android.graphics.Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        // 绘制cropBounds矩形
        val rect = RectF(
            cropBounds.left.toFloat(),
            cropBounds.top.toFloat(),
            cropBounds.right.toFloat(),
            cropBounds.bottom.toFloat()
        )
        canvas.drawRect(rect, paint)

        // 在矩形四角绘制小圆圈
        val circlePaint = Paint().apply {
            color = android.graphics.Color.BLUE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val circleRadius = 4f
        canvas.drawCircle(
            cropBounds.left.toFloat(),
            cropBounds.top.toFloat(),
            circleRadius,
            circlePaint
        ) // 左上角
        canvas.drawCircle(
            cropBounds.right.toFloat(),
            cropBounds.top.toFloat(),
            circleRadius,
            circlePaint
        ) // 右上角
        canvas.drawCircle(
            cropBounds.left.toFloat(),
            cropBounds.bottom.toFloat(),
            circleRadius,
            circlePaint
        ) // 左下角
        canvas.drawCircle(
            cropBounds.right.toFloat(),
            cropBounds.bottom.toFloat(),
            circleRadius,
            circlePaint
        ) // 右下角

        println("PdfDecoder.drawCropBoundsOnBitmap: 在图片上绘制了cropBounds矩形，区域=$cropBounds")

        val file = File(
            Environment.getExternalStorageDirectory(),
            "/Download/$index-${bitmap.width}-${bitmap.height}-${cropBounds.left}-${cropBounds.top}-${cropBounds.right}-${cropBounds.bottom}.png"
        )
        println("PdfDecoder.save.path:${file.absolutePath}")
        BitmapUtils.saveBitmapToFile(mutableBitmap, file)

        return mutableBitmap
    }*/
}