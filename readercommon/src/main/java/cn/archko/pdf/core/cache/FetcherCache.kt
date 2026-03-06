package cn.archko.pdf.core.cache

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import cn.archko.pdf.core.App
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.FileUtils
import java.io.File

/**
 * @author: archko 2026/3/6 :10:07
 */
class FetcherCache {
    companion object {

        private var cachedBitmap: Bitmap? = null

        fun cacheBitmap(path: String, bitmap: Bitmap?) {
            if (null == bitmap) {
                return
            }
            BitmapCache.getInstance().addBitmap(path, bitmap)
            val dir: File? = FileUtils.getExternalCacheDir(App.instance)
            val cacheDir = File(dir, "image")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val cachePath = String.format("%s/%s", cacheDir.getAbsolutePath(), path.hashCode())
            Logcat.d(String.format("cacheBitmap:%s->%s", path, cachePath))
            BitmapUtils.saveBitmapToFile(bitmap, File(cachePath))
        }

        fun createWhiteBitmap(width: Int, height: Int): Bitmap {
            if (cachedBitmap == null) {
                cachedBitmap = createDefaultBookBitmap(width, height)
            }

            return cachedBitmap!!
        }

        fun createDefaultBookBitmap(width: Int, height: Int): Bitmap {
            val bitmap =
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) // 使用 ARGB_8888 保证渐变平滑
            val canvas = Canvas(bitmap)

            // 1. 绘制背景渐变 (浅灰到白色，模拟自然光)
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFFF5F5F5.toInt(), 0xFFFFFFFF.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2. 绘制书脊阴影折痕 (左侧边缘)
            val spinePaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = (width / 50).toFloat()
            }
            canvas.drawLine(
                spinePaint.strokeWidth,
                0f,
                spinePaint.strokeWidth,
                height.toFloat(),
                spinePaint
            )

            // 3. 准备字母 "B" 的画笔 (带特效)
            val textPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC) // 使用衬线体更有书卷气
                textSize = (width / 2).toFloat()

                // 特效 A: 文字渐变色 (深灰到中灰)
                shader = LinearGradient(
                    0f, height / 3f, 0f, height / 1.5f,
                    intArrayOf(0xFF424242.toInt(), 0xFF9E9E9E.toInt()),
                    null, Shader.TileMode.CLAMP
                )

                // 特效 B: 添加淡淡的投影
                setShadowLayer(10f, 6f, 6f, Color.argb(70, 0, 0, 0))
            }

            // 4. 绘制文字
            val fontMetrics = textPaint.fontMetrics
            val x = (width / 2f)
            val y = (height / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText("B", x, y, textPaint)

            // 5. 可选：在下方画几条虚构的“作者名”横线，增加真实感
            val linePaint = Paint().apply {
                color = Color.parseColor("#EEEEEE")
                strokeWidth = 6f
            }
            val lineY = y + (height / 8f)
            canvas.drawLine(width / 3f, lineY, width * 2 / 3f, lineY, linePaint)

            return bitmap
        }
    }
}