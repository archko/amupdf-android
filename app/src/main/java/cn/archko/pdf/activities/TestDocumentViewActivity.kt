package cn.archko.pdf.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.component.DocumentView
import cn.archko.pdf.core.component.IntSize
import cn.archko.pdf.core.decoder.PdfDecoder
import cn.archko.pdf.core.decoder.internal.ImageDecoder
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.component.Vertical
import java.io.File

/**
 * 测试DocumentView的Activity
 * 用于验证Android View版本的PDF阅读器功能
 */
class TestDocumentViewActivity : AppCompatActivity() {

    private lateinit var documentView: DocumentView
    private var decoder: ImageDecoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        documentView = DocumentView(this)
        setContentView(documentView)

        // 加载PDF文件
        val pdfPath = "/storage/emulated/0/book/test.pdf"
        val pdfFile = File(pdfPath)

        if (!pdfFile.exists()) {
            Log.e("TestDocumentView", "PDF文件不存在: $pdfPath")
            finish()
            return
        }

        try {
            val pdfDecoder = PdfDecoder(pdfFile)
            decoder = pdfDecoder

            if (pdfDecoder.needsPassword) {
                Log.e("TestDocumentView", "PDF文件需要密码")
                finish()
                return
            }

            val pageSize = pdfDecoder.size(
                IntSize(
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels
                )
            )

            val pages: List<APage> = if (!pdfDecoder.aPageList.isNullOrEmpty()) {
                pdfDecoder.aPageList!!
            } else {
                createPagesFromDecoder(pdfDecoder)
            }

            val annotationManager = AnnotationManager(pdfFile.absolutePath)

            documentView.initialize(
                list = pages,
                decoder = decoder!!,
                initialScrollX = 0L,
                initialScrollY = 0L,
                initialZoom = 1.0,
                initialOrientation = Vertical,
                crop = false,
                columnCount = 1,
                annotationManager = annotationManager,
                speakingPageIndex = null
            )

            documentView.onPageChanged = { pageIndex ->
                Log.d("TestDocumentView", "当前页面: $pageIndex")
            }

            Log.d("TestDocumentView", "PDF加载成功: $pdfPath, 页数: ${pages.size}, 总尺寸: ${pageSize.width}x${pageSize.height}")
        } catch (e: Exception) {
            Log.e("TestDocumentView", "PDF加载失败: ${e.message}", e)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        decoder?.close()
        documentView.cleanup()
    }

    /**
     * 从decoder的originalPageSizes创建APage列表
     */
    private fun createPagesFromDecoder(decoder: PdfDecoder): List<APage> {
        val pages = mutableListOf<APage>()
        for (i in 0 until decoder.originalPageSizes.size) {
            val pageSize = decoder.originalPageSizes[i]
            val page = APage(i, pageSize.width.toFloat(), pageSize.height.toFloat(), 1f)
            pages.add(page)
        }
        return pages
    }
}
