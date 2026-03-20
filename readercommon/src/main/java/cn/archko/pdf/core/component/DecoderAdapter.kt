package com.archko.reader.pdf.component

import android.graphics.Bitmap
import android.graphics.RectF
import cn.archko.pdf.core.component.CropResult
import cn.archko.pdf.core.component.DecodeTask
import cn.archko.pdf.core.component.Decoder
import cn.archko.pdf.core.component.IntSize
import cn.archko.pdf.core.utils.SmartCropUtils
import cn.archko.pdf.core.decoder.internal.ImageDecoder

/**
 * 将现有的ImageDecoder适配到新的Decoder接口
 * @author: archko 2025/1/10
 */
class DecoderAdapter(
    private val imageDecoder: ImageDecoder,
    private val viewSize: IntSize,
    private val isCropEnabled: () -> Boolean
) : Decoder {

    companion object {

        /**
         * 计算缩略图尺寸：根据宽高比选择基准边
         * 对于超大的图片,如果缩略图在最大缩放值10倍的情况下,移动会卡,根本原因是组合次数
         */
        fun calculateThumbnailSize(
            pageWidth: Int,
            pageHeight: Int,
            baseSize: Int = 360
        ): Pair<Int, Int> {
            var size = baseSize
            if (pageWidth > 100_000 || pageHeight > 100_000) {
                size = 20
            } else if (pageWidth > 30_000 || pageHeight > 30_000) {
                size = 30
            } else if (pageWidth > 20_000 || pageHeight > 20_000) {
                size = 40
            } else if (pageWidth > 10_000 || pageHeight > 10_000) {
                size = 80
            }
            val aspectRatio = pageWidth.toFloat() / pageHeight.toFloat()
            return when {
                aspectRatio <= 0.5f -> {
                    // 高度是宽度的2倍以上（竖长条），以宽为基准
                    val width = size
                    val height = (size / aspectRatio).toInt()
                    Pair(width, height)
                }

                aspectRatio >= 2.0f -> {
                    // 宽度是高度的2倍以上（横长条），以高为基准
                    val height = size
                    val width = (size * aspectRatio).toInt()
                    Pair(width, height)
                }

                else -> {
                    // 宽高比在 1:2 到 2:1 之间，以宽为基准
                    val width = size
                    val height = (size / aspectRatio).toInt()
                    Pair(width, height)
                }
            }
        }
    }

    override suspend fun decodePage(task: DecodeTask): Bitmap? {
        return try {
            val aPage = task.aPage
            val (thumbWidth, thumbHeight) = calculateThumbnailSize(
                aPage.width.toInt(),
                aPage.height.toInt()
            )

            //println("decodePage.page:${task.pageIndex}, $thumbWidth-$thumbHeight")
            imageDecoder.renderPage(
                aPage = aPage,
                viewSize = viewSize,
                outWidth = thumbWidth,
                outHeight = thumbHeight,
                crop = task.crop
            )
        } catch (e: Exception) {
            println("PdfDecoderAdapter.decodePage error: ${e.message}")
            null
        }
    }

    override suspend fun decodeNode(task: DecodeTask): Bitmap {
        return imageDecoder.renderPageRegion(
            task.pageSliceBounds,
            task.pageIndex,
            task.zoom,
            IntSize(0, 0),
            task.width,
            task.height
        )
    }

    override suspend fun processCrop(task: DecodeTask): CropResult? {
        return try {
            val aPage = task.aPage

            // 如果已经有切边信息，直接返回
            if (aPage.cropBounds != null) {
                return CropResult(
                    task.pageIndex,
                    RectF(
                        aPage.cropBounds!!.left.toFloat(),
                        aPage.cropBounds!!.top.toFloat(),
                        aPage.cropBounds!!.right.toFloat(),
                        aPage.cropBounds!!.bottom.toFloat()
                    )
                )
            }

            // 渲染缩略图用于切边检测
            val thumbWidth = 300
            val ratio: Float = 1f * aPage.width / thumbWidth
            val thumbHeight = (aPage.height / ratio).toInt()

            val thumbBitmap = imageDecoder.renderPage(
                aPage = aPage,
                viewSize = viewSize,
                outWidth = thumbWidth,
                outHeight = thumbHeight,
                crop = false // 切边检测时不使用已有的切边信息
            )

            // 检测切边区域
            val cropBounds = SmartCropUtils.detectSmartCropBounds(thumbBitmap)

            val finalCropBounds = if (cropBounds != null) {
                // 将缩略图坐标转换为原始PDF坐标
                RectF(
                    cropBounds.left * ratio,
                    cropBounds.top * ratio,
                    cropBounds.right * ratio,
                    cropBounds.bottom * ratio
                )
            } else {
                // 如果检测失败，使用整个页面
                RectF(0f, 0f, aPage.width, aPage.height)
            }

            println("PdfDecoderAdapter.processCrop: page=${task.pageIndex}, cropBounds=$finalCropBounds")

            CropResult(task.pageIndex, finalCropBounds)
        } catch (e: Exception) {
            println("PdfDecoderAdapter.processCrop error: ${e.message}")
            CropResult(task.pageIndex, null)
        }
    }

    override fun generateCropTasks(): List<DecodeTask> {
        val tasks = mutableListOf<DecodeTask>()

        if (!isCropEnabled()) {
            return tasks
        }

        // 为所有没有切边信息的页面生成切边任务
        /*imageDecoder.aPageList?.forEachIndexed { index, aPage ->
            if (aPage.cropBounds == null) {
                val task = DecodeTask(
                    type = TaskType.CROP,
                    pageIndex = index,
                    key = "crop-$index",
                    aPage = aPage,
                    1f,
                    Rect(0f, 0f, 1f, 1f),
                    1,
                    1,
                    crop = true
                )
                tasks.add(task)
            }
        }*/

        println("PdfDecoderAdapter.generateCropTasks: generated ${tasks.size} crop tasks")
        return tasks
    }
}