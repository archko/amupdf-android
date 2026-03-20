package cn.archko.pdf.core.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.RectF
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.link.Hyperlink
import cn.archko.pdf.core.component.IntSize
import cn.archko.pdf.core.component.Size
import java.io.File
import java.io.FileInputStream

/**
 * 图片文件解码器，支持多个图片文件
 * @author: archko 2025/1/20
 */
class ImagesDecoder(private val files: List<File>) : ImageDecoder {

    override var pageCount: Int = files.size

    // 私有变量存储原始页面尺寸
    override var originalPageSizes: List<Size> = listOf()

    // 对外提供的缩放后页面尺寸
    override var pageSizes: List<Size> = listOf()

    // override var outlineItems: List<cn.archko.pdf.core.entity.Item>? = emptyList()

    override var imageSize: IntSize = IntSize(0, 0)

    var viewSize: IntSize = IntSize(0, 0)
    override val aPageList: MutableList<APage> = ArrayList()

    // 缓存BitmapRegionDecoder，避免重复创建，限制数量为10个
    private val regionDecoders = mutableMapOf<Int, BitmapRegionDecoder>()
    private val maxRegionDecoders = 10

    // override var cacheBean: ReflowCacheBean? = null
    override var filePath: String? = null

    init {
        if (files.isEmpty()) {
            throw IllegalArgumentException("图片文件列表不能为空")
        }

        // 检查所有文件是否存在且可读
        files.forEach { file ->
            if (!file.exists()) {
                throw IllegalArgumentException("图片文件不存在: ${file.absolutePath}")
            }
            if (!file.canRead()) {
                throw SecurityException("无法读取图片文件: ${file.absolutePath}")
            }
        }

        // 初始化原始页面尺寸
        originalPageSizes = prepareSizes()
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

    override fun getPageLinks(pageIndex: Int): List<Hyperlink> {
        return emptyList()
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
            }

            // 更新对外提供的页面尺寸
            pageSizes = scaledPageSizes
            imageSize = IntSize(documentWidth, totalHeight)
        }
    }

    /**
     * 获取原始页面尺寸
     */
    fun getOriginalPageSize(index: Int): Size {
        return originalPageSizes[index]
    }

    private fun prepareSizes(): List<Size> {
        val list = mutableListOf<Size>()
        var totalHeight = 0

        for (i in files.indices) {
            val file = files[i]
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(file.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight

            val size = Size(
                width,
                height,
                i,
                scale = 1.0f,
                totalHeight,
            )
            totalHeight += size.height
            list.add(size)
        }
        return list
    }

    override fun renderPageRegion(
        region: RectF,
        index: Int,
        scale: Float,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        if (index >= files.size) {
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }

        val file = files[index]

        return try {
            // 部分区域：使用BitmapRegionDecoder
            val regionDecoder = getRegionDecoder(index)
            if (regionDecoder != null) {
                // 计算偏移量（基于缩放后尺寸）
                val patchX = region.left
                val patchY = region.top

                // 计算原始图片中的区域（需要转换为原始坐标）
                val originalSize = originalPageSizes[index]
                val scaledRegion = android.graphics.Rect(
                    (patchX / scale).toInt().coerceIn(0, originalSize.width),
                    (patchY / scale).toInt().coerceIn(0, originalSize.height),
                    ((patchX + outWidth) / scale).toInt().coerceIn(0, originalSize.width),
                    ((patchY + outHeight) / scale).toInt().coerceIn(0, originalSize.height)
                )

                // 确保区域有效
                if (scaledRegion.width() > 0 && scaledRegion.height() > 0) {
                    // 解码区域
                    val options = BitmapFactory.Options().apply {
                        inSampleSize =
                            calculateInSampleSizeForRegion(scaledRegion, outWidth, outHeight)
                    }

                    val regionBitmap = regionDecoder.decodeRegion(scaledRegion, options)
                    //println("ImagesDecoder.renderPageRegion:原始=${originalSize.width}x${originalSize.height}, 偏移=($patchX,$patchY), 区域=${scaledRegion.width()}x${scaledRegion.height()}, 输出=${outWidth}x${outHeight}, 缩放=$scale, 采样=${options.inSampleSize}, 结果=${regionBitmap.width}x${regionBitmap.height}")
                    regionBitmap
                } else {
                    Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
                }
            } else {
                // 如果无法创建region decoder，返回默认图片
                Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
            }
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
        // 整张图片：使用传入的scale参数，就像PdfDecoder一样
        val originalSize = originalPageSizes[aPage.index]
        val scale = if (aPage.width > 0) {
            outWidth.toFloat() / aPage.getWidth(crop)
        } else {
            1f
        }

        // 使用传入的scale参数计算目标尺寸
        val targetWidth = (originalSize.width * scale).toInt()
        val targetHeight = (originalSize.height * scale).toInt()

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(files[aPage.index], targetWidth, targetHeight)
        }

        println("ImagesDecoder.renderPage: 原始=${originalSize.width}x${originalSize.height}, 输出=${outWidth}x${outHeight}, 目标=$targetWidth-$targetHeight")

        val bitmap = BitmapFactory.decodeFile(files[aPage.index].absolutePath, options)
        if (bitmap != null) {
            //println("ImagesDecoder.renderPage:原始=${originalSize.width}x${originalSize.height}, 输出=${outWidth}x${outHeight}, 缩放=$scale, 目标=${targetWidth}x${targetHeight}, 采样=${options.inSampleSize}, 结果=${bitmap.width}x${bitmap.height}")
            return bitmap
        } else {
            return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        }
    }

    /**
     * 获取或创建BitmapRegionDecoder，限制缓存数量为10个
     */
    private fun getRegionDecoder(index: Int): BitmapRegionDecoder? {
        if (index >= files.size) return null

        // 如果缓存已满且当前索引不在缓存中，移除最旧的项
        if (regionDecoders.size >= maxRegionDecoders && !regionDecoders.containsKey(index)) {
            val oldestIndex = regionDecoders.keys.first()
            val oldestDecoder = regionDecoders.remove(oldestIndex)
            oldestDecoder?.recycle()
            println("Removed region decoder for index $oldestIndex to make room for index $index")
        }

        return regionDecoders.getOrPut(index) {
            try {
                val file = files[index]
                val inputStream = FileInputStream(file)
                BitmapRegionDecoder.newInstance(inputStream, false)
            } catch (e: Exception) {
                println("Failed to create BitmapRegionDecoder for file ${files[index].absolutePath}: $e")
                null
            } ?: throw RuntimeException("Cannot create BitmapRegionDecoder")
        }
    }

    /**
     * 计算采样大小以优化内存使用
     */
    private fun calculateInSampleSize(file: File, reqWidth: Int, reqHeight: Int): Int {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(file.absolutePath, options)

        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 为区域解码计算采样大小
     */
    private fun calculateInSampleSizeForRegion(
        region: android.graphics.Rect,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val regionWidth = region.width()
        val regionHeight = region.height()
        var inSampleSize = 1

        if (regionHeight > reqHeight || regionWidth > reqWidth) {
            val halfHeight = regionHeight / 2
            val halfWidth = regionWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun close() {
        // 关闭所有region decoders
        regionDecoders.values.forEach { decoder ->
            try {
                decoder.recycle()
            } catch (e: Exception) {
                println("Error closing BitmapRegionDecoder: $e")
            }
        }
        regionDecoders.clear()

        ImageCache.clear()
    }

    override fun getStructuredText(index: Int): Any? {
        return null
    }

    override fun decodeReflowSinglePage(pageIndex: Int): ReflowBean? {
        return null
    }

    override fun decodeReflowAllPages(): List<ReflowBean> {
        return emptyList()
    }
} 