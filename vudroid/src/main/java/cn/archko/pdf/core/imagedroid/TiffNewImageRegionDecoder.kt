package cn.archko.pdf.core.imagedroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.core.graphics.createBitmap
import com.archko.reader.image.TiffInfo
import com.archko.reader.image.TiffLoader
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder

class TiffNewImageRegionDecoder @Keep constructor() : ImageRegionDecoder {
    private var path: String? = null
    private val tiffLoader: TiffLoader?
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var ready = false
    private var tiffInfo: TiffInfo? = null

    init {
        tiffLoader = TiffLoader()
    }

    override fun init(context: Context?, uri: Uri): Point {
        val uriString = uri.toString()
        if (uriString.startsWith(FILE_PREFIX)) {
            path = uriString.substring(FILE_PREFIX.length)
            tiffLoader!!.openTiff(path)
            tiffInfo = tiffLoader.tiffInfo
            width = tiffInfo!!.width
            height = tiffInfo!!.height
            val infoText = String.format(
                "TIFF文件信息: 尺寸: %s x %s, 颜色通道: %s,位深度: %s, 压缩方式: %s, 是否瓦片格式:%s, 页数: %s",
                tiffInfo!!.width,
                tiffInfo!!.height,
                tiffInfo!!.samplesPerPixel,
                tiffInfo!!.bitsPerSample,
                tiffInfo!!.compression,
                tiffInfo!!.isTiled,
                tiffInfo!!.pages
            )
            Log.d("TAG", infoText)
            Log.d("TAG", tiffInfo.toString())
            ready = true
        }

        return Point(width, height)
    }

    override fun isReady(): Boolean {
        return ready
    }

    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap? {
        try {
            Log.d("TAG", String.format("开始测试TIFF加载器.sampleSize:%s, %s", sampleSize, sRect))
            val start = System.currentTimeMillis()

            // 3. 解码一个区域
            //DecodedRegion region = tiffLoader.decodeRegion(sRect.left, sRect.top, sRect.right, sRect.bottom, 1f / sampleSize);
            //Log.d("TAG", String.format("解码区域成功:%s-%s, %sbytes, %sms",
            //        region.width, region.height, region.getDataSize(), System.currentTimeMillis() - start));
            //start = System.currentTimeMillis();
            //val bitmap = tiffLoader!!.decodeRegionToBitmap(sRect.left, sRect.top, sRect.right, sRect.bottom, 1f / sampleSize)
            // 计算缩放后的尺寸
            val scale = 1f / sampleSize
            val scaledWidth = (sRect.width() * scale).toInt()
            val scaledHeight = (sRect.height() * scale).toInt()

            val bitmap = createBitmap(scaledWidth, scaledHeight)
            tiffLoader!!.decodeRegionToBitmapDirect(
                sRect.left,
                sRect.top,
                sRect.width(),
                sRect.height(),
                scale,
                bitmap
            )
            Log.d(
                "TAG",
                String.format(
                    "解码图片成功:%sms, scale:%s, %s-%s, %s-%s", (System.currentTimeMillis() - start),
                    scale,
                    scaledWidth,
                    scaledHeight,
                    bitmap.width,
                    bitmap.height
                )
            )
            return bitmap
        } catch (e: Exception) {
            Log.e("TAG", "测试过程中发生异常", e)
        }
        return null
    }

    override fun recycle() {
        tiffLoader?.close()
    }

    companion object {
        private const val FILE_PREFIX = "file://"
    }
}
