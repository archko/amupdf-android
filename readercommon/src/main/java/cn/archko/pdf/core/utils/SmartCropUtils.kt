package cn.archko.pdf.core.utils

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 智能切边工具类 - 性能优化版本
 * 基于C++算法但采用按需读取像素策略，避免全图像素读取
 * @author: archko 2025/1/20
 */
public object SmartCropUtils {

    /** 扫描步进 */
    private const val V_LINE_SIZE = 6  // 垂直扫描步进
    private const val H_LINE_SIZE = 10 // 水平扫描步进

    /** 白色阈值 - 调整为更激进的切边 */
    private const val WHITE_THRESHOLD = 0.02f

    /** 边缘检测阈值 */
    private const val EDGE_THRESHOLD = 6

    /**
     * 智能检测图片的切边区域 - 按需读取像素版本
     */
    public fun detectSmartCropBounds(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 30 || height < 30) {
            return Rect(0, 0, width, height)
        }

        // 计算扫描范围 - 最多扫描到一半，更激进的切边
        val scanLimitH = width / 2
        val scanLimitV = height / 2
        val vLineMargin = ceil(width * 0.05).toInt()
        val hLineMargin = ceil(height * 0.05).toInt()

        // 按需检测各边界
        val leftBound = getLeftCropBound(bitmap, width, height, scanLimitH, vLineMargin, hLineMargin)
        val topBound = getTopCropBound(bitmap, width, height, scanLimitV, vLineMargin, hLineMargin, leftBound)
        val rightBound = getRightCropBound(bitmap, width, height, scanLimitH, vLineMargin, hLineMargin, topBound)
        val bottomBound =
            getBottomCropBound(bitmap, width, height, scanLimitV, vLineMargin, hLineMargin, leftBound, rightBound)

        // 验证边界有效性，确保不超过一半
        var finalLeft = min(leftBound, 0.5f)  // 最多切到一半
        var finalRight = max(rightBound, 0.5f)
        var finalTop = min(topBound, 0.5f)
        var finalBottom = max(bottomBound, 0.5f)

        if (finalLeft >= finalRight) {
            finalLeft = 0f
            finalRight = 1f
        }

        if (finalTop >= finalBottom) {
            finalTop = 0f
            finalBottom = 1f
        }

        // 转换为像素坐标
        val leftPx = (finalLeft * width).toInt()
        val topPx = (finalTop * height).toInt()
        val rightPx = (finalRight * width).toInt()
        val bottomPx = (finalBottom * height).toInt()

        // 检查是否需要切边
        if (leftPx <= 0 && topPx <= 0 && rightPx >= width && bottomPx >= height) {
            return Rect(0, 0, width, height)
        }

        return Rect(leftPx, topPx, rightPx, bottomPx)
    }

    /**
     * 按需读取并检测矩形区域是否为白色（边缘检测版本）
     */
    private fun isRectWhiteWithEdgeDetection(
        bitmap: Bitmap,
        sx: Int, sy: Int, sw: Int, sh: Int
    ): Boolean {
        // 只读取需要检测的小区域
        val pixels = IntArray(sw * sh)
        bitmap.getPixels(
            pixels,
            0,
            sw,
            sx,
            sy,
            sw,
            sh
        )

        var darkPixelsCount = 0

        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val i = y * sw + x
                val pixel = pixels[i]

                // 边缘检测：计算与前一个像素的亮度差异
                if (x > 0) {
                    val prevPixel = pixels[i - 1]
                    val currentLum = getLuminance(pixel)
                    val prevLum = getLuminance(prevPixel)

                    // 如果亮度差异大于阈值，认为是边缘
                    if (abs(currentLum - prevLum) > EDGE_THRESHOLD) {
                        darkPixelsCount++
                    }
                } else {
                    // 第一个像素，检查是否为暗色
                    val lum = getLuminance(pixel)
                    if (lum < 128) { // 简单的暗色判断
                        darkPixelsCount++
                    }
                }
            }
        }

        val totalPixels = sw * sh
        val limit = floor(totalPixels * WHITE_THRESHOLD).toInt()
        return darkPixelsCount < limit
    }

    /**
     * 计算像素亮度
     */
    private fun getLuminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }    
    
    /*
     *
     * 获取左边界 - 按需读取版本
     */
    private fun getLeftCropBound(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        scanLimit: Int,
        vLineMargin: Int,
        hLineMargin: Int
    ): Float {
        var whiteCount = 0
        var x = 0

        while (x < scanLimit) {
            val isWhite = isRectWhiteWithEdgeDetection(
                bitmap,
                x, hLineMargin,
                V_LINE_SIZE, height - (2 * hLineMargin)
            )

            if (isWhite) {
                whiteCount++
            } else {
                if (whiteCount >= 1) {
                    return max(0, x - V_LINE_SIZE).toFloat() / width
                }
                if (x == 0) {
                    return 0f
                }
                whiteCount = 0
            }
            x += V_LINE_SIZE
        }

        return if (whiteCount > 0) {
            // 如果扫描到边界都是白色，返回扫描到的最远位置，但不超过一半
            min(0.5f, max(0, x - V_LINE_SIZE).toFloat() / width)
        } else {
            0f
        }
    }

    /**
     * 获取上边界 - 按需读取版本
     */
    private fun getTopCropBound(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        scanLimit: Int,
        vLineMargin: Int,
        hLineMargin: Int,
        ignoreZoneLeft: Float
    ): Float {
        var whiteCount = 0
        var y = 0
        val ignore = ceil(ignoreZoneLeft * width).toInt()

        while (y < scanLimit) {
            val isWhite = isRectWhiteWithEdgeDetection(
                bitmap,
                ignore, y,
                width - (vLineMargin + ignore), H_LINE_SIZE
            )

            if (isWhite) {
                whiteCount++
            } else {
                if (whiteCount >= 1) {
                    return max(0, y - H_LINE_SIZE).toFloat() / height
                }
                if (y == 0) {
                    return 0f
                }
                whiteCount = 0
            }
            y += H_LINE_SIZE
        }

        return if (whiteCount > 0) {
            // 如果扫描到边界都是白色，返回扫描到的最远位置，但不超过一半
            min(0.5f, max(0, y - H_LINE_SIZE).toFloat() / height)
        } else {
            0f
        }
    }

    /**
     * 获取右边界 - 按需读取版本
     */
    private fun getRightCropBound(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        scanLimit: Int,
        vLineMargin: Int,
        hLineMargin: Int,
        ignoreZoneTop: Float
    ): Float {
        var whiteCount = 0
        var x = width - V_LINE_SIZE
        val ignore = ceil(ignoreZoneTop * height).toInt()
        val rightLimit = width - scanLimit

        while (x > rightLimit) {
            val isWhite = isRectWhiteWithEdgeDetection(
                bitmap,
                x, ignore,
                V_LINE_SIZE, height - (hLineMargin + ignore)
            )

            if (isWhite) {
                whiteCount++
            } else {
                if (whiteCount >= 1) {
                    return min(width, x + (2 * V_LINE_SIZE)).toFloat() / width
                }
                if (x == width - V_LINE_SIZE) {
                    return 1f
                }
                whiteCount = 0
            }
            x -= V_LINE_SIZE
        }

        return if (whiteCount > 0) {
            // 如果扫描到边界都是白色，返回扫描到的最远位置，但不少于一半
            max(0.5f, min(width, x + (2 * V_LINE_SIZE)).toFloat() / width)
        } else {
            1f
        }
    }

    /**
     * 获取下边界 - 按需读取版本
     */
    private fun getBottomCropBound(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        scanLimit: Int,
        vLineMargin: Int,
        hLineMargin: Int,
        ignoreZoneLeft: Float,
        ignoreZoneRight: Float
    ): Float {
        var whiteCount = 0
        var y = height - H_LINE_SIZE
        val ignoreLeft = ceil(ignoreZoneLeft * width).toInt()
        val ignoreRight = ceil((1 - ignoreZoneRight) * width).toInt()
        val bottomLimit = height - scanLimit

        while (y > bottomLimit) {
            val isWhite = isRectWhiteWithEdgeDetection(
                bitmap,
                ignoreLeft, y,
                width - (ignoreLeft + ignoreRight), H_LINE_SIZE
            )

            if (isWhite) {
                whiteCount++
            } else {
                if (whiteCount >= 1) {
                    return min(height, y + (H_LINE_SIZE * 2)).toFloat() / height
                }
                if (y == height - H_LINE_SIZE) {
                    return 1f
                }
                whiteCount = 0
            }
            y -= H_LINE_SIZE
        }

        return if (whiteCount > 0) {
            // 如果扫描到边界都是白色，返回扫描到的最远位置，但不少于一半
            max(0.5f, min(height, y + (H_LINE_SIZE * 2)).toFloat() / height)
        } else {
            1f
        }
    }
}