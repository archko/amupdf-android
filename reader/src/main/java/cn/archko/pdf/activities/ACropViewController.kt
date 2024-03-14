package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.util.SparseArray
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.core.app.ComponentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APDFPageView
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration

/**
 * @author: archko 2020/5/15 :12:43
 */
class ACropViewController(
    private var context: ComponentActivity,
    private var contentView: View,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?,
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: ARecyclerView
    private lateinit var mPageSizes: SparseArray<APage>

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
    }

    override fun init(pageSizes: SparseArray<APage>, pos: Int) {
        try {
            Logcat.d("init:$this,pos:$pos")
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
            Logcat.d("doLoadDoc:$this")
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
                    }, 100L)
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
        BitmapCache.getInstance().clear()
        mRecyclerView.stopScroll()
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
                autoCrop = 0
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

    private inner class PDFRecyclerAdapter : ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

        var pos: Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ARecyclerView.ViewHolder {
            var pageSize: APage? = null
            if (mPageSizes.size() > pos) {
                pageSize = mPageSizes.get(pos)
                if (pageSize.getTargetWidth() <= 0) {
                    //Logcat.d(String.format("create:%s", mRecyclerView.measuredWidth))
                    pageSize.setTargetWidth(parent.width)
                }
            }
            var width: Int = ViewGroup.LayoutParams.MATCH_PARENT
            var height: Int = ViewGroup.LayoutParams.MATCH_PARENT
            pageSize?.let {
                width = it.effectivePagesWidth
                height = it.effectivePagesHeight
            }

            val view = APDFPageView(context, pdfViewModel.mupdfDocument, pageSize!!, true)
            var lp: ARecyclerView.LayoutParams? = view.layoutParams as ARecyclerView.LayoutParams?

            pageSize?.let {
                Logcat.d(
                    String.format(
                        "create width:%s,measuredWidth:%s,targetWidth:%s",
                        width,
                        mRecyclerView.measuredWidth,
                        pageSize.getTargetWidth()
                    )
                )
            }
            if (null == lp) {
                lp = ARecyclerView.LayoutParams(width, height)
                view.layoutParams = lp
            } else {
                lp.width = width
                lp.height = height
            }
            return PdfHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
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

        inner class PdfHolder(internal var view: APDFPageView) : ARecyclerView.ViewHolder(view) {
            fun onBind(position: Int) {
                val pageSize = mPageSizes.get(position)
                if (pageSize.getTargetWidth() != mRecyclerView.measuredWidth) {
                    pageSize.setTargetWidth(mRecyclerView.measuredWidth)
                }
                Logcat.d(
                    String.format(
                        "bind:position:%s,targetWidth:%s, measuredWidth:%s",
                        position,
                        pageSize.getTargetWidth(),
                        mRecyclerView.measuredWidth
                    )
                )
                if (pageSize.getTargetWidth() <= 0) {
                    return
                }
                view.updatePage(showBookmark(position), pageSize, 1.0f/*zoomModel!!.zoom*/, true)
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
