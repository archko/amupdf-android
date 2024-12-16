package cn.archko.pdf.activities

import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.controller.ControllerListener
import cn.archko.pdf.controller.PdfPageController
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.AppExecutors.Companion.instance
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.listeners.SimpleGestureListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.APDFView
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel

/**
 * @author: archko 2020/5/15 :12:43
 */
class ACropViewController(
    private var context: FragmentActivity,
    private val scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: DocViewModel,
    private var mPath: String,
    private var pageController: PdfPageController?,
    private var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: ARecyclerView
    private lateinit var mPageSizes: List<APage>
    private var pdfAdapter: PDFRecyclerAdapter? = null

    /**
     * 有时需要强制不切边,又不切换到normal的渲染模式,设置这个值
     */
    private var crop: Boolean = true
    private var scrollOrientation = LinearLayoutManager.VERTICAL

    private var decodeService: DocDecodeService? = null
    private lateinit var currentPageModel: CurrentPageModel

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

    var defaultWidth = 1080
    var defaultHeight = 1080

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
        initDecodeService()

        val view = LayoutInflater.from(context)
            .inflate(cn.archko.pdf.R.layout.reader_crop, mControllerLayout, false)
        mRecyclerView = view.findViewById(cn.archko.pdf.R.id.recycler)
        (mRecyclerView.parent as ViewGroup).removeView(mRecyclerView)

        val iView = object : IView {
            override fun getWidth(): Int {
                return mRecyclerView.getWidth()
            }

            override fun getHeight(): Int {
                return mRecyclerView.getHeight()
            }
        }

        decodeService?.setContainerView(iView)

        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = ExtraSpaceLinearLayoutManager(context, LinearLayoutManager.VERTICAL)
            setItemViewCacheSize(0)

            //addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : ARecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: ARecyclerView, newState: Int) {
                    if (newState == ARecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: ARecyclerView, dx: Int, dy: Int) {
                }
            })
        }
        mRecyclerView.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val changed = defaultWidth != mRecyclerView.width
                    defaultWidth = mRecyclerView.width
                    defaultHeight = mRecyclerView.height
                    if (Logcat.loggable) {
                        Logcat.d(
                            "TAG", String.format(
                                "onGlobalLayout : w-h:%s-%s",
                                defaultWidth, defaultHeight
                            )
                        )
                    }
                    if (changed) {
                        mRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            })
    }

    private fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        instance.diskIO().execute {
            try {
                document = decodeService!!.open(mPath, true)
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
                //documentView.showDocument(crop)
            }
        }
    }

    override fun init() {
        Logcat.d("init:")
        crop = pdfViewModel.checkCrop()

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

        gotoPage(pdfViewModel.getCurrentPage())
    }

    private fun initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService()
        }
    }

    private fun createDecodeService(): DocDecodeService {
        val codecContext = DecodeServiceBase.openContext(mPath)
        if (null == codecContext) {
            Toast.makeText(context, "open file error", Toast.LENGTH_SHORT).show()
            context.finish()
        }
        return DocDecodeService(codecContext)
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    private fun gotoPage(pos: Int) {
        setOrientation(scrollOrientation)
        if (null == pdfAdapter) {
            pdfAdapter = PDFRecyclerAdapter(context, decodeService!!, mPageSizes, mRecyclerView)
            mRecyclerView.adapter = pdfAdapter
            pdfAdapter!!.setCrop(crop)
        }

        if (pos > 0) {
            val layoutManager = mRecyclerView.layoutManager

            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d("onGlobalLayout:$this,pos:$pos")
                    mRecyclerView.postDelayed({
                        layoutManager!!.scrollToPosition(pos)
                        mRecyclerView.requestLayout()
                    }, 10L)
                }
            })
        }
    }

    override fun getCurrentBitmap(): Bitmap? {
        //val aPage = mPageSizes[getCurrentPos()]
        //val cacheKey = getCacheKey(aPage!!.index, crop, aPage.scaleZoom)
        //return BitmapCache.getInstance().getBitmap(cacheKey)
        val child =
            (mRecyclerView.layoutManager as LinearLayoutManager).findViewByPosition(getCurrentPos())
        if (child is APDFView) {
            val key = child.getCacheKey()
            if (key != null) {
                return BitmapCache.getInstance().getBitmap(key)
            }
        }
        return null
    }

    override fun getCurrentPos(): Int {
        if (null == mRecyclerView.layoutManager) {
            return 0
        }
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    fun getLastPos(): Int {
        if (null == mRecyclerView.layoutManager) {
            return 0
        }
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun getCount(): Int {
        return mPageSizes.size
    }

    override fun setOrientation(ori: Int) {
        scrollOrientation = ori
        (mRecyclerView.layoutManager as LinearLayoutManager).orientation = (ori)
        pdfAdapter?.setOriention(ori)
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun setCrop(crop: Boolean) {
        this.crop = crop
        if (null != mRecyclerView.adapter) {
            (mRecyclerView.adapter as PDFRecyclerAdapter).setCrop(crop)
        }
    }

    override fun getCrop(): Boolean {
        return crop
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.run {
            val layoutManager: LinearLayoutManager = this as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(page, 0)
        }
    }

    override fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            var scrollY = mRecyclerView.scrollY
            scrollY -= mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY + margin)
            return true
        } else if (y > bottom) {
            var scrollY = mRecyclerView.scrollY
            scrollY += mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY - margin)
            return true
        }
        return false
    }

    override fun tryHyperlink(ev: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTap(e: MotionEvent?, margin: Int): Boolean {
        if (e == null) {
            return false
        }
        if (tryHyperlink(e)) {
            return true
        }
        val documentView = getDocumentView()
        val height =
            if (scrollOrientation == LinearLayoutManager.VERTICAL) documentView.height else documentView.width
        val top = height / 4
        val bottom = height * 3 / 4
        if (scrollPage(e.y.toInt(), top, bottom, margin)) {
            return true
        }

        return false
    }

    override fun onDoubleTap() {

    }

    override fun onSelectedOutline(index: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(index)
        updateProgress(index)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mRecyclerView.stopScroll()
        BitmapCache.getInstance().clear()

        defaultWidth = Utils.dipToPixel(newConfig.screenWidthDp.toFloat())
        defaultHeight = Utils.dipToPixel(newConfig.screenHeightDp.toFloat())
        if (Logcat.loggable) {
            Logcat.d(
                "TAG", String.format(
                    "newConfig:w-h:%s-%s, config:%s-%s, %s",
                    defaultWidth,
                    defaultHeight,
                    newConfig.screenWidthDp,
                    newConfig.screenHeightDp,
                    newConfig.orientation
                )
            )
        }

        if (null != mRecyclerView.adapter) {
            (mRecyclerView.adapter as PDFRecyclerAdapter).defaultWidth = defaultWidth
            (mRecyclerView.adapter as PDFRecyclerAdapter).defaultHeight = defaultHeight
        }

        val lm = (mRecyclerView.layoutManager as LinearLayoutManager)
        var offset = 0
        val first = lm.findFirstVisibleItemPosition()
        if (first > 0) {
            val child = lm.findViewByPosition(first)
            child?.run {
                val r = Rect()
                child.getLocalVisibleRect(r)
                offset = r.top
            }
        }
        lm.scrollToPositionWithOffset(first, -offset)
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateProgress(index: Int) {
        if (pageController?.visibility() == View.VISIBLE) {
            pageController?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    override fun setFilter(colorMode: Int) {
    }

    override fun decodePageForTts(currentPos: Int) {
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_NO
    }

    override fun prev(string: String?) {
    }

    override fun next(string: String?) {
    }

    override fun clearSearch() {
    }

    override fun showSearch() {
    }

    //--------------------------------------

    override fun onResume() {
        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        /*if (null != pdfViewModel.mupdfDocument) {
            var savePos = getCurrentPos() + 1
            val lastPos = getLastPos()
            if (lastPos == mPageSizes.size - 1) {
                savePos = lastPos
            }
            pdfViewModel.bookProgress?.run {
                //autoCrop = 0
                pdfViewModel.saveBookProgress(
                    mPath,
                    pdfViewModel.countPages(),
                    savePos,
                    pdfViewModel.bookProgress!!.zoomLevel,
                    -1,
                    0
                )
            }
        }*/
    }

    override fun onDestroy() {
        Logcat.d("crop.onDestroy")
        decodeService?.recycle()
    }

    //===========================================
    override fun showController() {
    }

}
