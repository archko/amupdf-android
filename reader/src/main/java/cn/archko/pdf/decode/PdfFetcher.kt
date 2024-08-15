package cn.archko.pdf.decode

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import cn.archko.pdf.core.cache.BitmapPool
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.File

/**
 * @author: archko 2024/8/133 :08:02
 */
class PdfFetcher(
    private val data: PdfFetcherData,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val parcelFileDescriptor =
            ParcelFileDescriptor.open(File(data.path), ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)

        val bitmap = if (pdfRenderer.pageCount > 0)
            renderPage(
                pdfRenderer.openPage(0),
                data.width,
                data.height
            ) else null

        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    private fun renderPage(page: PdfRenderer.Page, width: Int, height: Int): Bitmap {
        val ratio = 1f * width / page.width
        val nHeight = height / ratio
        val bitmap = BitmapPool
            .getInstance()
            .acquire(width, nHeight.toInt(), Bitmap.Config.RGB_565)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
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

data class PdfFetcherData(
    val path: String,
    val width: Int,
    val height: Int,
)