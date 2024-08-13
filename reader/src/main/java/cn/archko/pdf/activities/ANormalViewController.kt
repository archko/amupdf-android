package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.core.common.AppExecutors.Companion.instance
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.listeners.SimpleGestureListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.DocumentView
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel
import org.vudroid.djvudroid.codec.DjvuContext
import org.vudroid.pdfdroid.codec.PdfContext

/**
 * @author: archko 2020/5/15 :12:43
 */
class ANormalViewController(
    private var context: FragmentActivity,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var simpleListener: SimpleGestureListener?,
    private var crop: Boolean,
) :
    OutlineListener, AViewController {

    private lateinit var documentView: DocumentView
    private lateinit var frameLayout: FrameLayout
    private var decodeService: DecodeService? = null

    private lateinit var currentPageModel: CurrentPageModel

    private lateinit var mPageSizes: List<APage>
    private var scrollOrientation = LinearLayoutManager.VERTICAL
    private var pageNumberToast: Toast? = null
    protected var progressDialog: ProgressDialog? = null
    protected var isDocLoaded: Boolean = false

    private var simpleGestureListener: SimpleGestureListener = object :
        SimpleGestureListener {
        override fun onSingleTapConfirmed(ev: MotionEvent, currentPage: Int) {
            simpleListener?.onSingleTapConfirmed(ev, currentPage)
            //showPageToast(currentPage)
        }

        override fun onDoubleTapEvent(ev: MotionEvent, currentPage: Int) {
            //simpleListener?.onDoubleTapEvent(ev, currentPage)
        }
    }

    init {
        initView()
    }

    private fun initView() {
        val zoomModel = ZoomModel()

        var offsetX = 0
        var offsetY = 0
        pdfViewModel.bookProgress?.run {
            zoomModel.zoom = this.zoomLevel / 1000
            offsetX = this.offsetX
            offsetY = this.offsetY
        }
        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel.addEventListener(this)
        documentView = DocumentView(
            context,
            zoomModel,
            DocumentView.VERTICAL,
            offsetX,
            offsetY,
            progressModel,
            currentPageModel,
            simpleGestureListener
        )
        initDecodeService()

        documentView.setDecodeService(decodeService)

        zoomModel.addEventListener(documentView)
        documentView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        decodeService?.setContainerView(documentView)

        frameLayout = createMainContainer()
        frameLayout.addView(documentView)
        zoomModel.addEventListener(this)

        loadDocument()
    }

    private fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        instance.diskIO().execute {
            val document = decodeService!!.open(pdfViewModel.pdfPath, crop, true)
            instance.mainThread().execute {
                progressDialog!!.dismiss()
                if (null == document) {
                    Toast.makeText(
                        context,
                        "Open Failed",
                        Toast.LENGTH_LONG
                    ).show()
                    context.finish()
                    return@execute
                }
                isDocLoaded = true
                documentView.showDocument(crop)
            }
        }
    }

    override fun init(pageSizes: List<APage>, pos: Int, scrollOrientation: Int) {
        try {
            Logcat.d("init.pos:$pos, :$scrollOrientation")
            this.scrollOrientation = scrollOrientation
            this.mPageSizes = pageSizes

            setOrientation(scrollOrientation)
            gotoPage(pos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: List<APage>, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$scrollOrientation")
            this.mPageSizes = pageSizes

            setOrientation(scrollOrientation)
            gotoPage(pos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
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
        if (IntentFile.isDjvu(mPath)) {
            return DecodeServiceBase(DjvuContext())
        }

        return DecodeServiceBase(PdfContext())
    }

    override fun getDocumentView(): View {
        return frameLayout
    }

    private fun gotoPage(pos: Int) {
        if (pos > 0) {
            documentView.goToPage(
                pos,
                pdfViewModel.bookProgress!!.offsetX,
                pdfViewModel.bookProgress!!.offsetY
            )
        }
        //mPageControls?.hide()
    }

    override fun getCurrentBitmap(): Bitmap? {
        return decodeService?.decodeThumb(getCurrentPos())
    }

    override fun getCurrentPos(): Int {
        var position = documentView.getCurrentPage()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun getCount(): Int {
        return mPageSizes.size
    }

    override fun setOrientation(ori: Int) {
        documentView.oriention = ori
    }

    override fun setCrop(crop: Boolean) {
        this.crop = crop
    }

    override fun getCrop(): Boolean {
        return crop
    }

    override fun scrollToPosition(page: Int) {
        documentView.goToPage(page - 1)
    }

    override fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            documentView.scrollPage(-frameLayout.height + margin)
            return true
        } else if (y > bottom) {
            documentView.scrollPage(frameLayout.height - margin)
            return true
        }
        return false
    }

    override fun tryHyperlink(ev: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTap(ev: MotionEvent?, margin: Int): Boolean {
        if (ev == null) {
            return false
        }
        val documentView = getDocumentView()
        val height =
            if (scrollOrientation == LinearLayoutManager.VERTICAL) documentView.height else documentView.width
        val top = height / 4
        val bottom = height * 3 / 4
        if (scrollPage(ev.y.toInt(), top, bottom, margin)) {
            return true
        }
        return false
    }

    override fun onDoubleTap() {
    }

    override fun onSelectedOutline(index: Int) {
        documentView.goToPage(index - RESULT_FIRST_USER)
        updateProgress(index - RESULT_FIRST_USER)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    private fun updateProgress(index: Int) {
        if (/*pdfViewModel.mupdfDocument != null &&*/ pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
    }

    override fun notifyItemChanged(pos: Int) {
    }

    fun showPageToast(currentPage: Int) {
        val pos = currentPage
        val pageText = (pos + 1).toString() + "/" + pdfViewModel.countPages()
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast =
                Toast.makeText(context, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.START, Utils.dipToPixel(15f), 0)
        pageNumberToast!!.show()
    }

    //--------------------------------------

    override fun onResume() {
        //mPageControls?.hide()
    }

    override fun onPause() {
        pdfViewModel.bookProgress?.run {
            val position = documentView.currentPage
            pdfViewModel.saveBookProgress(
                mPath,
                pdfViewModel.countPages(),
                position + 1,
                documentView.zoomModel.zoom * 1000f,
                documentView.scrollX,
                documentView.scrollY
            )
        }
    }

    override fun onDestroy() {
        Logcat.d("normal.onDestroy")
        decodeService?.recycle()
    }

    //===========================================
    override fun showController() {
        //mPageControls?.show()
    }
}
