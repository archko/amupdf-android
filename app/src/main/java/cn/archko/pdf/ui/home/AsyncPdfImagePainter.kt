package cn.archko.pdf.ui.home

import android.graphics.Bitmap
import androidx.compose.runtime.RememberObserver
import cn.archko.pdf.App.Companion.instance
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.utils.BitmapUtils
import cn.archko.pdf.utils.FileUtils
import java.io.File

/**
 * 查看pdf信息时的缩略图
 */
class AsyncPdfImagePainter internal constructor(
    request: ImageWorker.DecodeParam,
) : AbsAsyncPdfPainter(request), RememberObserver {

    override fun decode(decodeParam: ImageWorker.DecodeParam): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val thumb =
                FileUtils.getDiskCacheDir(instance, FileUtils.getRealPath(decodeParam.key))
            bitmap = ImageLoader.decodeFromFile(thumb)
            if (null == bitmap) {
                val file = File(decodeParam.key)
                if (file.exists()) {
                    bitmap = ImageLoader.decodeFromPDF(
                        decodeParam.key,
                        decodeParam.pageNum,
                        decodeParam.zoom,
                        decodeParam.screenWidth
                    )
                    if (bitmap != null) {
                        BitmapUtils.saveBitmapToFile(bitmap, thumb)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bitmap
    }
}
