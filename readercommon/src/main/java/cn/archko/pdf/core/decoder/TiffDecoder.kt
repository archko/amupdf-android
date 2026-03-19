package cn.archko.pdf.core.decoder

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import cn.archko.pdf.core.cache.ImageCache
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.link.Hyperlink
import com.archko.reader.image.TiffLoader
import com.archko.reader.pdf.component.IntSize
import com.archko.reader.pdf.component.Size
import java.io.File

/**
 * @author: archko 2025/8/9 :6:26
 */
class TiffDecoder(val file: File) : ImageDecoder {

    override var pageCount: Int = 1

    // 私有变量存储原始页面尺寸
    override var originalPageSizes: List<Size> = listOf()

    // 对外提供的缩放后页面尺寸
    override var pageSizes: List<Size> = listOf()

    //override var outlineItems: List<cn.archko.pdf.core.entity.Item>? = emptyList()

    override var imageSize: IntSize = IntSize(0, 0)

    var viewSize: IntSize = IntSize(0, 0)
    override val aPageList: MutableList<APage> = ArrayList()
    private var tiffLoader: TiffLoader? = null

    //override var cacheBean: ReflowCacheBean? = null
    override var filePath: String? = null

    init {
        if (!file.exists()) {
            throw IllegalArgumentException("文档文件不存在: ${file.absolutePath}")
        }

        if (!file.canRead()) {
            throw SecurityException("无法读取文档文件: ${file.absolutePath}")
        }

        tiffLoader = TiffLoader()

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

        tiffLoader!!.openTiff(file.absolutePath)
        val tiffInfo = tiffLoader!!.tiffInfo
        val width = tiffInfo!!.width
        val height = tiffInfo.height

        val size = Size(
            width,
            height,
            0,
            scale = 1.0f,
            0,
        )
        list.add(size)
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
        return try {
            val pageWidth = (region.width() / scale).toInt()
            val pageHeight = (region.height() / scale).toInt()
            val patchX = (region.left / scale).toInt()
            val patchY = (region.top / scale).toInt()
            //精度的问题,jni那边不支持小数进位四舍五入,所以这里的高宽要修正一下.
            val bitmap = tiffLoader!!.decodeRegionToBitmap(
                patchX,
                patchY,
                pageWidth,
                pageHeight,
                scale,
            )

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
            val originalSize = originalPageSizes[aPage.index]

            // 根据输出尺寸计算合适的缩放比例
            // 选择能够完全适应输出尺寸的缩放比例
            val scaleX = outWidth.toFloat() / originalSize.width
            val scaleY = outHeight.toFloat() / originalSize.height
            val scale = minOf(scaleX, scaleY)

            println("TiffDecoder.renderPage: 原始=${originalSize.width}x${originalSize.height}, 输出=${outWidth}x${outHeight}, 缩放=$scale (scaleX=$scaleX, scaleY=$scaleY)")

            val bitmap = tiffLoader!!.decodeRegionToBitmap(
                0,
                0,
                originalSize.width,
                originalSize.height,
                scale,
            )

            return bitmap ?: Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            println("renderPage error: $e")
            return Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565)
        }
    }

    override fun close() {
        tiffLoader?.close()

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