package cn.archko.pdf.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import cn.archko.pdf.R
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.IntentFile
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import org.vudroid.djvudroid.codec.DjvuContext
import org.vudroid.djvudroid.codec.DjvuPage
import java.io.File

/**
 * @author: archko 2024/8/133 :08:02
 */
class PdfFetcher(
    private val data: PdfFetcherData,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        println("PdfFetcher:${data.path}")
        var bitmap = if (IntentFile.isDjvu(data.path)) {
            decodeDjvu()
        } else if (IntentFile.isPdf(data.path)) {
            decodePdfSys()
        } else {
            decodeMuPdf()
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(App.instance!!.resources, R.drawable.ic_book_text)
        }

        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    private fun decodeDjvu(): Bitmap? {
        val djvuContext = DjvuContext()
        val djvuDocument = djvuContext.openDocument(data.path)

        var bitmap: Bitmap? = null
        if (djvuDocument.pageCount > 0) {
            val djvuPage = djvuDocument.getPage(0)
            bitmap = renderDjvuPage(djvuPage, data.width, data.height)
        }

        return bitmap
    }

    private fun renderDjvuPage(page: DjvuPage, width: Int, height: Int): Bitmap {
        val xscale = 1f * width / page.width
        val yscale = 1f * height / page.height
        var w: Int = width
        var h: Int = height
        if (xscale > yscale) {
            h = (page.height * xscale).toInt()
        } else {
            w = (page.width * yscale).toInt()
        }
        val bitmap = page.renderBitmap(
            Rect(0, 0, w, h),
            w,
            h,
            RectF(0f, 0f, 1f, 1f),
            1f
        )
        return bitmap
    }

    private fun decodeMuPdf(): Bitmap? {
        var document: Document = Document.openDocument(data.path)

        val bitmap = if (document.countPages() > 0)
            renderPdfPage(
                document.loadPage(0),
                data.width,
                data.height
            ) else null
        return bitmap
    }

    private fun renderPdfPage(page: Page, width: Int, height: Int): Bitmap {
        val pWidth = page.bounds.x1 - page.bounds.x0
        val pHeight = page.bounds.y1 - page.bounds.y0
        val ctm = Matrix()
        val xscale = 1f * width / pWidth
        val yscale = 1f * height / pHeight
        var w: Int = width
        var h: Int = height
        if (xscale > yscale) {
            h = (pWidth * xscale).toInt()
        } else {
            w = (pHeight * yscale).toInt()
        }

        ctm.scale(xscale, yscale)
        val bitmap = BitmapPool.getInstance().acquire(w, h)
        val dev =
            AndroidDrawDevice(bitmap, 0, 0, 0, 0, bitmap.getWidth(), bitmap.getHeight())
        page.run(dev, ctm, null as Cookie?)
        dev.close()
        dev.destroy()
        return bitmap
    }

    private fun decodePdfSys(): Bitmap? {
        val parcelFileDescriptor =
            ParcelFileDescriptor.open(File(data.path), ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)

        val bitmap = if (pdfRenderer.pageCount > 0)
            renderPdfPageSys(
                pdfRenderer.openPage(0),
                data.width,
                data.height
            ) else null
        return bitmap
    }

    private fun renderPdfPageSys(page: PdfRenderer.Page, width: Int, height: Int): Bitmap {
        val xscale = 1f * width / page.width
        val yscale = 1f * height / page.height
        var w: Int = width
        var h: Int = height
        if (xscale > yscale) {
            h = (page.height * xscale).toInt()
        } else {
            w = (page.width * yscale).toInt()
        }

        val bitmap = BitmapPool
            .getInstance()
            .acquire(w, h, Bitmap.Config.ARGB_8888)
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