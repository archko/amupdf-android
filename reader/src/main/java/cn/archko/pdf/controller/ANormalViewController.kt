package cn.archko.pdf.controller

import android.content.res.Configuration
import android.graphics.Bitmap
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
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.TtsHelper
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.entity.TtsBean
import cn.archko.pdf.core.listeners.SimpleGestureListener
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.tts.TTSEngine
import cn.archko.pdf.viewmodel.DocViewModel
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.DocumentView
import org.vudroid.core.codec.CodecDocument
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel

/**
 * 通用的带切边功能的controller
 * @author: archko 2020/5/15 :12:43
 */
open class ANormalViewController(
    protected var context: FragmentActivity,
    private val scope: CoroutineScope,
    protected val mControllerLayout: RelativeLayout,
    private var docViewModel: DocViewModel,
    private var mPath: String,
    protected var pageController: IPageController?,
    protected var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {
    private var crop: Boolean = true

    protected lateinit var documentView: DocumentView
    private lateinit var frameLayout: FrameLayout
    private var decodeService: DecodeService? = null

    private lateinit var currentPageModel: CurrentPageModel

    private var mPageSizes: List<APage>? = null
    private var scrollOrientation = LinearLayoutManager.VERTICAL
    private var isDocLoaded: Boolean = false
    protected var document: CodecDocument? = null

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
        docViewModel.bookProgress?.run {
            zoomModel.zoom = this.zoomLevel / 1000
            offsetX = this.offsetX
            offsetY = this.offsetY
            scrollOrientation = docViewModel.bookProgress?.scrollOrientation ?: 1
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

    fun loadDocument() {
        instance.diskIO().execute {
            try {
                document = decodeService!!.open(mPath, true)
            } catch (_: Exception) {
            }
            instance.mainThread().execute {
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
        crop = docViewModel.checkCrop()

        gotoPage(docViewModel.getCurrentPage())

        loadDocument()
    }

    open fun doLoadDoc(pageSizeBean: APageSizeLoader.PageSizeBean, document: CodecDocument) {
        Logcat.d("doLoadDoc:${pageSizeBean.crop}, ${pageSizeBean.List!!.size}")
        this.mPageSizes = pageSizeBean.List!!
        if (null == mPageSizes) {
            return
        }

        docViewModel.setPageCount(mPageSizes!!.size)

        controllerListener?.doLoadedDoc(
            mPageSizes!!.size,
            docViewModel.getCurrentPage(),
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
        val codecContext = DecodeServiceBase.openContext(mPath)
        if (null == codecContext) {
            Toast.makeText(context, "open file error", Toast.LENGTH_SHORT).show()
            context.finish()
        }
        return DecodeServiceBase(codecContext)
    }

    override fun getDocumentView(): View {
        return frameLayout
    }

    private fun gotoPage(pos: Int) {
        if (pos > 0) {
            documentView.goToPage(
                pos,
                docViewModel.bookProgress!!.offsetX,
                docViewModel.bookProgress!!.offsetY
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
        if (pageController?.visibility() == View.VISIBLE) {
            pageController?.updatePageProgress(index)
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
        val ttsBean: TtsBean? = TtsHelper.loadFromFile(count, mPath)
        if (ttsBean?.list == null) {
            val start = System.currentTimeMillis()
            val list = mutableListOf<ReflowBean>()
            if (null != document) {
                for (i in 0 until count) {
                    val beans: List<ReflowBean>? = document!!.decodeReflowText(i)
                    if (beans != null) {
                        //这里应该只有一个元素
                        for (j in beans.indices) {
                            list.add(beans[j])
                        }
                    }
                }
            }
            Logcat.i(Logcat.TAG, "decodeTextForTts.cos:${System.currentTimeMillis() - start}")
            TtsHelper.saveToFile(count, mPath, list)
            for (i in currentPos until list.size) {
                TTSEngine.get().speak(list[i])
            }
        } else {
            for (i in currentPos until ttsBean.list.size) {
                TTSEngine.get().speak(ttsBean.list[i])
            }
        }
    }

    /**
     * 重排判断是从这出的.判断是文本重排还是图片重排,依据当前的pdf文档
     * 如果是pdf之外的,直接判断为文本方式重排.
     * 如果是pdf,则从页面内容判断,取第二页,中间两页来判断是否可以取得文本,页数不够则从头开始
     * 如果可以则认为是文本类,如果不可以,则认为是图片类
     */
    override fun reflow(): Int {
        return BookProgress.REFLOW_NO
    }

    //--------------------------------------

    override fun onResume() {
    }

    override fun onPause() {
        if (null != docViewModel.bookProgress && null != mPageSizes && mPageSizes!!.isNotEmpty()) {
            var savePos = getCurrentPos()
            /*val lastPos = getLastPos()
            if (lastPos == documentView.pageCount - 1) {
                savePos = lastPos
            }*/
            docViewModel.saveBookProgress(
                mPath,
                mPageSizes!!.size,
                savePos,
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
