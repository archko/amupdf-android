package cn.archko.pdf.common

import android.graphics.Bitmap
import androidx.compose.runtime.RememberObserver

/**
 * 单页的pdf解码
 */
class AsyncPageImagePainter internal constructor(
    request: ImageWorker.DecodeParam,
) : AbsAsyncPdfPainter(request), RememberObserver {

    override fun decode(decodeParam: ImageWorker.DecodeParam): Bitmap? {
        return PdfImageDecoder.decode(decodeParam)
    }
}
