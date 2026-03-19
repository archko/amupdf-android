package cn.archko.pdf.core.component

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import cn.archko.pdf.core.entity.APage

/**
 * 解码任务
 * @author: archko 2025/1/10
 */
data class DecodeTask(
    val type: TaskType,
    val pageIndex: Int,
    val key: String,
    val aPage: APage,
    val zoom: Float = 1f,
    val pageSliceBounds: RectF,
    val width: Int,
    val height: Int,
    val crop: Boolean = false,
    val callback: DecodeCallback? = null,
) {
    override fun toString(): String {
        return "DecodeTask(type=$type, pageIndex=$pageIndex, key='$key', zoom=$zoom, pageSliceBounds=$pageSliceBounds, width=$width, height=$height, crop=$crop, aPage=$aPage)"
    }
}

enum class TaskType {
    PAGE,
    NODE,
    CROP
}

/**
 * 解码回调接口
 */
interface DecodeCallback {
    fun onDecodeComplete(bitmap: Bitmap?, isThumb: Boolean, error: Throwable?)
    fun shouldRender(pageNumber: Int, isFullPage: Boolean): Boolean
    fun onFinish(pageNumber: Int)
}

/**
 * 解码器接口
 */
interface Decoder {
    suspend fun decodePage(task: DecodeTask): Bitmap?
    suspend fun decodeNode(task: DecodeTask): Bitmap?
    suspend fun processCrop(task: DecodeTask): CropResult?
    fun generateCropTasks(): List<DecodeTask>
}

/**
 * 切边处理结果
 */
data class CropResult(
    val pageIndex: Int,
    val cropBounds: RectF?
)
