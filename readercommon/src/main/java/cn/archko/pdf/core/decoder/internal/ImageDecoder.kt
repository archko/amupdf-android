package cn.archko.pdf.core.decoder.internal

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import cn.archko.pdf.core.component.DecodeTask
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.link.Hyperlink
import com.archko.reader.pdf.component.IntSize
import com.archko.reader.pdf.component.Size

/**
 * @author: archko 2025/4/11 :15:51
 */
interface ImageDecoder {

    var pageCount: Int
    var pageSizes: List<Size>
    var originalPageSizes: List<Size>

    // var outlineItems: List<Item>?
    val aPageList: MutableList<APage>?

    // var cacheBean: ReflowCacheBean?
    var filePath: String?

    var imageSize: IntSize

    fun size(viewportSize: IntSize): IntSize

    fun getPageLinks(pageIndex: Int): List<Hyperlink>

    /**
     * 渲染页面区域（带切边参数）
     * @param aPage 页面
     * @param viewSize 视图大小
     * @param outWidth 页面宽度
     * @param outHeight 页面高度
     * @param crop 是否启用切边
     * @return 渲染结果
     */
    fun renderPage(
        aPage: APage,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int,
        crop: Boolean
    ): Bitmap

    fun renderPageRegion(
        region: RectF,
        index: Int,
        scale: Float,
        viewSize: IntSize,
        outWidth: Int,
        outHeight: Int
    ): Bitmap

    fun close()

    fun getStructuredText(index: Int): Any?

    /**
     * 解析单个页面的文本内容（用于TTS快速启动）
     */
    fun decodeReflowSinglePage(pageIndex: Int): ReflowBean?

    /**
     * 解析所有页面的文本内容（用于TTS后台缓存）
     */
    fun decodeReflowAllPages(): List<ReflowBean>
}