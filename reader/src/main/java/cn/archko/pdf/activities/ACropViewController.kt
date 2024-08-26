package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APDFView
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope

/**
 * @author: archko 2020/5/15 :12:43
 */
class ACropViewController(
    private var context: FragmentActivity,
    private val scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: DocViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var simpleListener: ControllerListener?,
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

    var defaultWidth = 1080
    var defaultHeight = 1080

    init {
        initView()
    }

    private fun initView() {
        //mRecyclerView = FastScrollRecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        val view = LayoutInflater.from(context)
            .inflate(cn.archko.pdf.R.layout.reader_crop, mControllerLayout, false)
        mRecyclerView = view.findViewById(cn.archko.pdf.R.id.recycler)
        (mRecyclerView.parent as ViewGroup).removeView(mRecyclerView)

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

    override fun init() {
        try {
            Logcat.d("init :$scrollOrientation")
            this.scrollOrientation = scrollOrientation
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    fun doLoadDoc(pageSizes: List<APage>, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$scrollOrientation")
            this.mPageSizes = pageSizes

            setCropMode(pos)
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        mRecyclerView.setOnTouchListener { _, event ->
            //gestureDetector!!.onTouchEvent(event)
            false
        }
    }

    private fun setCropMode(pos: Int) {
        setOrientation(scrollOrientation)
        if (null == pdfAdapter) {
            //pdfAdapter = PDFRecyclerAdapter(context, pdfViewModel, mPageSizes, mRecyclerView)
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
        /*if (pdfViewModel.mupdfDocument != null && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }*/
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

    //--------------------------------------

    override fun onResume() {
        //mPageSeekBarControls?.hide()

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
    }

    //===========================================
    override fun showController() {
    }

}
