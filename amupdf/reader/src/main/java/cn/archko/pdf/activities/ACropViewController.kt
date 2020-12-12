package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.Context
import android.content.res.Configuration
import android.util.SparseArray
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.widgets.APDFView
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration

/**
 * @author: archko 2020/5/15 :12:43
 */
class ACropViewController(
    private var context: Context,
    private var contentView: View,
    private val mControllerLayout: RelativeLayout,
    private var pdfBookmarkManager: PDFBookmarkManager,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: RecyclerView
    private var mMupdfDocument: MupdfDocument? = null
    private lateinit var mPageSizes: SparseArray<APage>
    private var init: Boolean = false

    init {
        initView()
    }

    private fun initView() {
        mRecyclerView = RecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })
        }

    }

    override fun init(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument?, pos: Int) {
        try {
            Logcat.d("init:$this")
            if (null != mupdfDocument) {
                this.mPageSizes = pageSizes
                this.mMupdfDocument = mupdfDocument

                setCropMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$this")
            this.mPageSizes = pageSizes
            this.mMupdfDocument = mupdfDocument

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
            mRecyclerView.scrollToPosition(pos)
        }
    }

    override fun getCurrentPos(): Int {
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(page)
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
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateProgress(index: Int) {
        if (mMupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
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
        pdfBookmarkManager.bookmarkToRestore?.autoCrop = 0

        val position = getCurrentPos()
        val zoomLevel = pdfBookmarkManager.bookmarkToRestore!!.zoomLevel
        if (null != mMupdfDocument) {
            pdfBookmarkManager.saveCurrentPage(
                mPath,
                mMupdfDocument!!.countPages(),
                position,
                zoomLevel,
                -1,
                0
            )
        }
    }

    //===========================================
    override fun showController() {
    }

    private inner class PDFRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var pos: Int = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var pageSize: APage? = null
            if (mPageSizes.size() > pos) {
                pageSize = mPageSizes.get(pos)
                if (pageSize.getTargetWidth() <= 0) {
                    Logcat.d(String.format("create:%s", mRecyclerView.measuredWidth))
                    pageSize.setTargetWidth(parent.width)
                }
            }
            val view = APDFView(context, mMupdfDocument, pageSize!!, true)
            var lp: RecyclerView.LayoutParams? = view.layoutParams as RecyclerView.LayoutParams?
            var width: Int = ViewGroup.LayoutParams.MATCH_PARENT
            var height: Int = ViewGroup.LayoutParams.MATCH_PARENT
            pageSize.let {
                width = it.effectivePagesWidth
                height = it.effectivePagesHeight
            }
            //Logcat.d("create width:" + width + "==>" + mRecyclerView.measuredWidth + "==>" + pageSize!!.targetWidth)
            if (null == lp) {
                lp = RecyclerView.LayoutParams(width, height)
                view.layoutParams = lp
            } else {
                lp.width = width
                lp.height = height
            }
            val holder = PdfHolder(view)
            return holder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            pos = viewHolder.adapterPosition
            val pdfHolder = viewHolder as PdfHolder

            pdfHolder.onBind(position)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            val pdfHolder = holder as PdfHolder?

            pdfHolder?.view?.recycle()
        }

        override fun getItemCount(): Int {
            return mMupdfDocument!!.countPages()
        }

        inner class PdfHolder(internal var view: APDFView) : RecyclerView.ViewHolder(view) {
            fun onBind(position: Int) {
                val pageSize = mPageSizes.get(position)
                //Logcat.d(String.format("bind:position:%s,width:%s,%s", position, pageSize.targetWidth, mRecyclerView.measuredWidth))
                if (pageSize.getTargetWidth() != mRecyclerView.measuredWidth) {
                    pageSize.setTargetWidth(mRecyclerView.measuredWidth)
                }
                if (pageSize.getTargetWidth() <= 0) {
                    return
                }
                view.updatePage(pageSize, 1.0f/*zoomModel!!.zoom*/, true)
            }
        }

    }
}
