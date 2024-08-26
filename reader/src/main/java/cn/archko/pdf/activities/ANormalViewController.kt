package cn.archko.pdf.activities

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
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.AppExecutors.Companion.instance
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.listeners.SimpleGestureListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.tts.TTSEngine
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.DocumentView
import org.vudroid.core.codec.CodecDocument
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
    private val scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: DocViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {
    private var crop: Boolean = true

    private lateinit var documentView: DocumentView
    private lateinit var frameLayout: FrameLayout
    private var decodeService: DecodeService? = null

    private lateinit var currentPageModel: CurrentPageModel

    private var mPageSizes: List<APage>? = null
    private var scrollOrientation = LinearLayoutManager.VERTICAL
    protected var progressDialog: ProgressDialog? = null
    protected var isDocLoaded: Boolean = false
    private var document: CodecDocument? = null

    private var simpleGestureListener: SimpleGestureListener = object :
        SimpleGestureListener {
        override fun onSingleTapConfirmed(ev: MotionEvent, currentPage: Int) {
            controllerListener?.onSingleTapConfirmed(ev, currentPage)
        }

        override fun onDoubleTap(ev: MotionEvent, currentPage: Int) {
            controllerListener?.onDoubleTap(ev, currentPage)
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
            scrollOrientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1
        }

        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel.addEventListener(this)
        documentView = DocumentView(
            context,
            zoomModel,
            scrollOrientation,
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

        setFilter(PdfOptionRepository.getColorMode())
    }

    private fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        instance.diskIO().execute {
            try {
                document = decodeService!!.open(mPath, crop, true)
            } catch (e: Exception) {
            }
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
                doLoadDoc(decodeService!!.pageSizeBean, document!!)
                documentView.showDocument(crop)
            }
        }
    }

    override fun init() {
        Logcat.d("init:")
        crop = pdfViewModel.checkCrop()

        gotoPage(pdfViewModel.getCurrentPage())

        loadDocument()
    }

    private fun doLoadDoc(pageSizeBean: APageSizeLoader.PageSizeBean, document: CodecDocument) {
        Logcat.d("doLoadDoc:${pageSizeBean.crop}, ${pageSizeBean.List!!.size}")
        this.mPageSizes = pageSizeBean.List!!
        if (null == mPageSizes) {
            return
        }

        controllerListener?.doLoadedDoc(
            mPageSizes!!.size,
            pdfViewModel.getCurrentPage(),
            document.loadOutline()
        )
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
        return mPageSizes?.size ?: 0
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
        documentView.goToPage(page)
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
        documentView.goToPage(index)
        updateProgress(index)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    private fun updateProgress(index: Int) {
        if (pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        documentView.postInvalidate()
    }

    override fun notifyItemChanged(pos: Int) {
    }

    override fun setFilter(colorMode: Int) {
        documentView.setFilter(colorMode)
    }

    override fun decodePageForTts(currentPos: Int) {
        val last = TTSEngine.get().getLast()
        val count = document!!.pageCount
        Logcat.i(Logcat.TAG, "decodePageForTts:last:$last, count:$count, currentPos:$currentPos")
        if (last == count - 1 && last != 0) {
            return
        }
        if (last > 0) {
            TTSEngine.get().reset()
        }
        val start = System.currentTimeMillis()
        if (null != document) {
            for (i in currentPos until count) {
                val beans: List<ReflowBean>? = document!!.decodeReflowText(i)
                if (beans != null) {
                    for (j in beans.indices) {
                        TTSEngine.get().speak("$i-$j", beans[j].data)
                    }
                }
            }
        }
        Logcat.i(Logcat.TAG, "decodeTextForTts.cos:${System.currentTimeMillis() - start}")
    }

    //--------------------------------------

    override fun onResume() {
        //mPageControls?.hide()
    }

    override fun onPause() {
        if (null != pdfViewModel.bookProgress && null != mPageSizes) {
            val position = documentView.currentPage
            pdfViewModel.saveBookProgress(
                mPath,
                mPageSizes!!.size,
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
