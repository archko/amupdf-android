package cn.archko.pdf.core.imagedroid

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.core.graphics.createBitmap
import com.archko.reader.image.TiffLoader
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder

class TiffNewImageDecoder @Keep constructor() : ImageDecoder {
    private val tiffLoader: TiffLoader
    private var path: String? = null

    init {
        tiffLoader = TiffLoader()
    }

    override fun decode(context: Context?, uri: Uri): Bitmap? {
        Log.d("TAG", String.format("decode:%s", uri))
        val uriString = uri.toString()
        var bitmap: Bitmap? = null
        if (uriString.startsWith(FILE_PREFIX)) {
            path = uriString.substring(FILE_PREFIX.length)
            tiffLoader.openTiff(path)
            val tiffInfo = tiffLoader.tiffInfo
            val width = tiffInfo.width
            val height = tiffInfo.height
            bitmap = createBitmap(width, height)
            tiffLoader.decodeRegionToBitmapDirect(0, 0, width, height, 1f, bitmap)
            tiffLoader.close()
        }
        return bitmap
    }

    companion object {
        private const val FILE_PREFIX = "file://"
    }
}
