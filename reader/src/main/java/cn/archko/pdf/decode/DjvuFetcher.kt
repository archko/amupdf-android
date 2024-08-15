package cn.archko.pdf.decode

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import org.vudroid.djvudroid.codec.DjvuContext
import org.vudroid.djvudroid.codec.DjvuPage

/**
 * @author: archko 2024/8/133 :08:02
 */
class DjvuFetcher(
    private val data: DjvuFetcherData,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val djvuContext = DjvuContext()
        val djvuDocument = djvuContext.openDocument(data.path)

        val bitmap = if (djvuDocument.pageCount > 0) {
            val djvuPage = djvuDocument.getPage(0)
            renderPage(djvuPage, data.width, data.height)
        } else null

        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    private fun renderPage(page: DjvuPage, width: Int, height: Int): Bitmap {
        val ratio = 1f * width / page.width
        val nHeight = height / ratio
        val bitmap = page.renderBitmap(
            Rect(0, 0, 1, 1),
            width,
            nHeight.toInt(),
            RectF(0f, 0f, 1f, 1f),
            1f
        )
        return bitmap
    }

    class Factory : Fetcher.Factory<PdfFetcherData> {
        override fun create(
            data: PdfFetcherData,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return PdfFetcher(data, options)
        }
    }
}

data class DjvuFetcherData(
    val path: String,
    val width: Int,
    val height: Int,
)