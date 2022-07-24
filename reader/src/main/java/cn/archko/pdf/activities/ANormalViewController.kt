package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.util.SparseArray
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.app.ComponentActivity
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.listeners.SimpleGestureListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import org.vudroid.core.AKDecodeService
import org.vudroid.core.DecodeService
import org.vudroid.core.DocumentView
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel
import org.vudroid.core.views.PageViewZoomControls
import org.vudroid.pdfdroid.codec.PdfDocument

/**
 * @author: archko 2020/5/15 :12:43
 */
class ANormalViewController(
    private var context: ComponentActivity,
    private var contentView: View,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?,
    private var optionRepository: PdfOptionRepository
) :
    OutlineListener, AViewController {

    private lateinit var documentView: DocumentView
    private lateinit var frameLayout: FrameLayout
    private var decodeService: DecodeService? = null

    private lateinit var currentPageModel: CurrentPageModel
    private var mPageControls: PageViewZoomControls? = null

    private lateinit var mPageSizes: SparseArray<APage>

    init {
        initView()
    }

    private fun initView() {
        BitmapCache.getInstance().resize(BitmapCache.CAPACITY_FOR_VUDROID)
        initDecodeService()
        val zoomModel = ZoomModel()

        pdfViewModel.bookProgress?.run {
            zoomModel.zoom = this.zoomLevel / 1000
        }
        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel.addEventListener(this)
        documentView =
            DocumentView(context, zoomModel, progressModel, currentPageModel, simpleGestureListener)
        zoomModel.addEventListener(documentView)
        documentView.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        decodeService?.setContainerView(documentView)
        documentView.setDecodeService(decodeService)

        frameLayout = createMainContainer()
        frameLayout.addView(documentView)
        mPageControls = createZoomControls(zoomModel)
        //frameLayout.addView(mPageControls)
        zoomModel.addEventListener(this)

        val lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        mControllerLayout.addView(mPageControls, lp)
    }

    override fun init(pageSizes: SparseArray<APage>, pos: Int) {
        try {
            Logcat.d("init:$this")
            if (null != pdfViewModel.mupdfDocument) {
                this.mPageSizes = pageSizes

                setNormalMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: SparseArray<APage>, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$this")
            this.mPageSizes = pageSizes

            setNormalMode(pos)
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    private fun createZoomControls(zoomModel: ZoomModel): PageViewZoomControls {
        val controls = PageViewZoomControls(context, zoomModel)
        controls.gravity = Gravity.END or Gravity.BOTTOM
        zoomModel.addEventListener(controls)
        return controls
    }

    private fun createMainContainer(): FrameLayout {
        return FrameLayout(context)
    }

    private fun initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService()
        }
    }

    private fun createDecodeService(): DecodeService {
        return AKDecodeService()
    }

    override fun getDocumentView(): View {
        return frameLayout
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        documentView.setOnTouchListener { v, event ->
            val res: Boolean? = gestureDetector?.onTouchEvent(event)
            return@setOnTouchListener res!!
        }
    }

    private fun setNormalMode(pos: Int) {
        val document = PdfDocument()
        document.core = pdfViewModel.mupdfDocument?.document
        (decodeService as AKDecodeService).document = document
        if (pos > 0) {
            documentView.goToPage(
                pos,
                pdfViewModel.bookProgress!!.offsetX,
                pdfViewModel.bookProgress!!.offsetY
            )
        }
        documentView.showDocument()
        mPageControls?.hide()
    }

    override fun getCurrentPos(): Int {
        var position = documentView.getCurrentPage()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun scrollToPosition(page: Int) {
        documentView.goToPage(page)
    }

    override fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            //documentView.scrollPage(-frameLayout.height + margin);
            return true
        } else if (y > bottom) {
            //documentView.scrollPage(frameLayout.height - margin);
            return true
        }
        return false
    }

    override fun onSingleTap() {
        //if (mPageSeekBarControls?.visibility == View.VISIBLE) {
        //    mPageSeekBarControls?.hide()
        //    return
        //}
        mPageControls?.hide()
    }

    override fun onDoubleTap() {
        //if (mMupdfDocument == null) {
        //    return
        //}
        //mPageSeekBarControls?.hide()
        //showOutline()
    }

    override fun onSelectedOutline(index: Int) {
        documentView.goToPage(index - RESULT_FIRST_USER)
        updateProgress(index - RESULT_FIRST_USER)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    private fun updateProgress(index: Int) {
        if (pdfViewModel.mupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
    }

    override fun notifyItemChanged(pos: Int) {
    }

    //--------------------------------------

    override fun onResume() {
        //mPageSeekBarControls?.hide()
        mPageControls?.hide()
    }

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument) {
            pdfViewModel.bookProgress?.run {
                val position = documentView.currentPage
                pdfViewModel.saveBookProgress(
                    mPath,
                    pdfViewModel.countPages(),
                    position,
                    documentView.zoomModel.zoom * 1000f,
                    documentView.scrollX,
                    documentView.scrollY
                )
            }
        }
    }

    override fun onDestroy() {
    }

    //===========================================
    override fun showController() {
        mPageControls?.show()
    }

    private var simpleGestureListener: SimpleGestureListener = object : SimpleGestureListener {
        override fun onSingleTapConfirmed(currentPage: Int) {
            //currentPageChanged(currentPage)
            //gestureDetector?.onTouchEvent()
        }

        override fun onDoubleTapEvent(currentPage: Int) {
            mPageSeekBarControls!!.toggleSeekControls()
            mPageControls!!.toggleZoomControls()
        }
    }

}
