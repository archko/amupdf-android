package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.graphics.Rect
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APDFView
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration

/**
 * @author: archko 2020/5/15 :12:43
 */
class ACropViewController(
    private var context: FragmentActivity,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?,
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: ARecyclerView
    private lateinit var mPageSizes: SparseArray<APage>

    /**
     * 有时需要强制不切边,又不切换到normal的渲染模式,设置这个值
     */
    private var crop: Boolean = true
    private var scrollOrientation = LinearLayoutManager.VERTICAL

    init {
        initView()
    }

    private fun initView() {
        mRecyclerView = ARecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
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

    override fun init(pageSizes: SparseArray<APage>, pos: Int, scrollOrientation: Int) {
        try {
            Logcat.d("init.pos:$pos, :$scrollOrientation")
            this.scrollOrientation = scrollOrientation
            if (null != pdfViewModel.mupdfDocument) {
                this.mPageSizes = pageSizes

                setCropMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: SparseArray<APage>, pos: Int) {
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
        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector!!.onTouchEvent(event)
            false
        }
    }

    private fun setCropMode(pos: Int) {
        setOrientation(scrollOrientation)
        if (null == mRecyclerView.adapter) {
            mRecyclerView.adapter = PDFRecyclerAdapter()
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
                    }, 50L)
                }
            })
        }
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

    override fun getCount(): Int {
        return mPageSizes.size()
    }

    override fun setOrientation(ori: Int) {
        scrollOrientation = ori
        (mRecyclerView.layoutManager as LinearLayoutManager).orientation = (ori)
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun setCrop(crop: Boolean) {
        this.crop = crop
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.run {
            val layoutManager: LinearLayoutManager = this as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(page - 1, 0)
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

    override fun onSingleTap() {
        //if (mPageSeekBarControls?.visibility == View.VISIBLE) {
        //    mPageSeekBarControls?.hide()
        //    return
        //}
    }

    override fun onDoubleTap() {
        //if (mMupdfDocument == null) {
        //    return
        //}
        //mPageSeekBarControls?.hide()
        //showOutline()
    }

    override fun onSelectedOutline(index: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(index - RESULT_FIRST_USER)
        updateProgress(index - RESULT_FIRST_USER)
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
        if (pdfViewModel.mupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    //--------------------------------------

    override fun onResume() {
        //mPageSeekBarControls?.hide()

        mRecyclerView.postDelayed(object : Runnable {
            override fun run() {
                mRecyclerView.adapter?.notifyDataSetChanged()
            }
        }, 250L)
    }

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument) {
            pdfViewModel.bookProgress?.run {
                //autoCrop = 0
                val position = getCurrentPos()
                pdfViewModel.saveBookProgress(
                    mPath,
                    pdfViewModel.countPages(),
                    position + 1,
                    pdfViewModel.bookProgress!!.zoomLevel,
                    -1,
                    0
                )
            }
        }
    }

    override fun onDestroy() {
    }

    //===========================================
    override fun showController() {
    }

    var defaultWidth = 1080
    var defaultHeight = 1080

    private inner class PDFRecyclerAdapter : ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ARecyclerView.ViewHolder {
            val view = APDFView(context)
                .apply {
                    layoutParams = ViewGroup.LayoutParams(defaultWidth, defaultHeight)
                }
            return PdfHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as PdfHolder
            pdfHolder.onBind(position)
        }

        override fun onViewRecycled(holder: ARecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?

            pdfHolder?.view?.recycle()
        }

        override fun getItemCount(): Int {
            return mPageSizes.size()
        }

        inner class PdfHolder(internal var view: APDFView) : ARecyclerView.ViewHolder(view) {
            fun onBind(position: Int) {
                val pageSize = mPageSizes.get(position)
                if (pageSize.getTargetWidth() != defaultWidth) {
                    pageSize.setTargetWidth(defaultWidth)
                }
                Logcat.d(
                    String.format(
                        "bind:position:%s,targetWidth:%s, measuredWidth:%s",
                        position,
                        pageSize.getTargetWidth(),
                        mRecyclerView.measuredWidth
                    )
                )
                if (defaultWidth <= 0) {
                    return
                }
                view.updatePage(pageSize, 1.0f, pdfViewModel.mupdfDocument, crop)
            }

            private fun showBookmark(position: Int): Boolean {
                val bookmarks = pdfViewModel.bookmarks
                if (null != bookmarks) {
                    for (bookmark in bookmarks) {
                        if (position == bookmark.page) {
                            return true
                        }
                    }
                }
                return false
            }
        }

    }
}
