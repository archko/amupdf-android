package cn.archko.pdf.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Size
import cn.archko.pdf.R
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.utils.BitmapUtils
import cn.archko.pdf.core.utils.FileUtils
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import org.vudroid.djvudroid.codec.DjvuContext
import org.vudroid.djvudroid.codec.DjvuPage
import java.io.File
import java.nio.ByteBuffer

/**
 * @author: archko 2024/8/133 :08:02
 */
class PdfFetcher(
    private val data: PdfFetcherData,
    private val options: Options
) : Fetcher {

    private fun cacheBitmap(bitmap: Bitmap?) {
        if (null == bitmap) {
            return
        }
        BitmapCache.getInstance().addBitmap(data.path, bitmap)
        val dir = FileUtils.getExternalCacheDir(App.instance)
        val cacheDir = File(dir, "image")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val path = "${cacheDir.absolutePath}/${data.path.hashCode()}"
        val bmp = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            bitmap.config!!
        )
        val buffer = ByteBuffer.allocate(bitmap.getByteCount())
        bitmap.copyPixelsToBuffer(buffer)
        buffer.position(0)
        bmp.copyPixelsFromBuffer(buffer);
        BitmapUtils.saveBitmapToFile(bmp, File(path))
    }

    private fun loadBitmapFromCache(): Bitmap? {
        var bmp = BitmapCache.getInstance().getBitmap(data.path)
        if (null != bmp) {
            return bmp
        }
        val dir = FileUtils.getExternalCacheDir(App.instance)
        val cacheDir = File(dir, "image")
        val key = "${cacheDir.absolutePath}/${data.path.hashCode()}"
        bmp = BitmapFactory.decodeFile(key)
        return bmp
    }

    override suspend fun fetch(): FetchResult {
        var bitmap = loadBitmapFromCache()
        if (bitmap == null) {
            bitmap = if (IntentFile.isDjvu(data.path)) {
                decodeDjvu()
            } else if (IntentFile.isPdf(data.path)) {
                decodePdfSys()
            } else {
                decodeMuPdf()
            }
        }

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(App.instance!!.resources, R.drawable.ic_book_text)
        } else {
            cacheBitmap(bitmap)
        }

        //val drawable = CoverDrawable(bitmap)
        val imageBitmap: BitmapImage = bitmap.asImage()

        return ImageFetchResult(
            image = imageBitmap,
            dataSource = DataSource.DISK,
            isSampled = false
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

    private fun caculateSize(pWidth: Int, pHeight: Int, tWidth: Int, tHeight: Int): Size {
        val xscale = 1f * tWidth / pWidth
        val yscale = 1f * tHeight / pHeight
        var w: Int = tWidth
        var h: Int = tHeight
        if (xscale > yscale) {
            h = (pHeight * xscale).toInt()
        } else {
            w = (pWidth * yscale).toInt()
        }
        return Size(w, h)
    }

    private fun renderDjvuPage(page: DjvuPage, width: Int, height: Int): Bitmap {
        val size = caculateSize(page.width, page.height, width, height)
        val bitmap = page.renderBitmap(
            Rect(0, 0, size.width, size.height),
            size.width, size.height,
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
        val size = caculateSize(pWidth.toInt(), pHeight.toInt(), width, height)

        ctm.scale(xscale, yscale)
        val bitmap = BitmapPool.getInstance().acquire(size.width, size.height)
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
        val size = caculateSize(page.width, page.height, width, height)

        val bitmap = BitmapPool
            .getInstance()
            .acquire(size.width, size.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    class Factory : Fetcher.Factory<PdfFetcherData> {
        override fun create(
            data: PdfFetcherData,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return PdfFetcher(data, options)
        }
    }

    /*private fun paint(image: Bitmap): Bitmap {
        val left = 15
        val top = 10
        val width = 135 + left
        val height = 180 + top

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        bmp.eraseColor(Color.TRANSPARENT)

        val c = Canvas(bmp)

        val cornerBmp: Bitmap = BitmapFactory.decodeResource(App.instance!!.resources, R.drawable.components_thumbnail_corner)
        val leftBmp: Bitmap = BitmapFactory.decodeResource(App.instance!!.resources, R.drawable.components_thumbnail_left)
        val topBmp: Bitmap = BitmapFactory.decodeResource(App.instance!!.resources, R.drawable.components_thumbnail_top)

        c.drawBitmap(cornerBmp, null, Rect(0, 0, left, top), null)
        c.drawBitmap(topBmp, null, Rect(left, 0, width, top), null)
        c.drawBitmap(leftBmp, null, Rect(0, top, left, height), null)
        val imgWidth = image.width
        val imgHeight = image.height
        Logcat.d("image:$imgWidth, $imgHeight")
        val right = imgWidth
        var bottom = (135f * imgHeight / imgWidth).toInt()
        if (bottom > imgHeight) {
            bottom = imgHeight
        }

        c.drawBitmap(image, Rect(0, 0, right, bottom), Rect(left, top, width, height), null)

        return bmp
    }*/
}