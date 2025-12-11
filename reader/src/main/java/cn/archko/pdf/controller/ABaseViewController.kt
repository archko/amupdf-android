package cn.archko.pdf.controller

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.core.widgets.ViewerDividerItemDecoration
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * 基于RecyclerView的控制器
 *
 * @author: archko 2024/8/15 :12:43
 */
abstract class ABaseViewController(
    var context: FragmentActivity,
    var scope: CoroutineScope,
    val mControllerLayout: RelativeLayout,
    var docViewModel: DocViewModel,
    var mPath: String,
    var pageController: IPageController?,
    var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {

    protected lateinit var mRecyclerView: ARecyclerView
    protected var mGestureDetector: GestureDetector? = null

    init {
        initView()
    }

    private inner class MySimpleOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            controllerListener?.onSingleTapConfirmed(e, 0)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return controllerListener?.onDoubleTap(e, 0) ?: super.onDoubleTap(e)
        }
    }

    private fun initView() {
        mGestureDetector = GestureDetector(context, MySimpleOnGestureListener())
        mRecyclerView = ARecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = ExtraSpaceLinearLayoutManager(context, LinearLayoutManager.VERTICAL)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(LinearLayoutManager.VERTICAL))
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

        initStyleControls()

        addGesture()
    }

    protected open fun showPasswordDialog() {
    }

    protected open fun loadDocument() {

    }

    override fun init() {
        loadDocument()
    }

    protected open fun doLoadDoc() {
        initReflowMode(docViewModel.getCurrentPage())
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            mGestureDetector?.onTouchEvent(event) == true
        }
    }

    protected open fun initReflowMode(pos: Int) {
    }

    override fun getCurrentBitmap(): Bitmap? {
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

    protected fun getLastPos(): Int {
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

    override fun setOrientation(ori: Int) {
        (mRecyclerView.layoutManager as LinearLayoutManager).orientation = ori
    }

    override fun setCrop(crop: Boolean) {
    }

    override fun getCrop(): Boolean {
        return false
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

    override fun scrollPageHorizontal(x: Int, left: Int, right: Int, margin: Int): Boolean {
        if (x < left) {
            var scrollX = mRecyclerView.scrollX
            scrollX -= mRecyclerView.width
            mRecyclerView.scrollBy(scrollX + margin, 0)
            return true
        } else if (x > right) {
            var scrollX = mRecyclerView.scrollX
            scrollX += mRecyclerView.width
            mRecyclerView.scrollBy(scrollX + margin, 0)
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
        val height = documentView.height
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
        mRecyclerView.smoothScrollToPosition(index)
        updateProgress(index)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mRecyclerView.stopScroll()
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    open fun updateProgress(index: Int) {

    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    override fun setFilter(colorMode: Int) {
    }

    override fun decodePageForTts(currentPos: Int, callback: TtsDataCallback?) {
    }

    //--------------------------------------

    override fun onResume() {
        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    //===========================================
    override fun showController() {
    }

    open fun initStyleControls() {
        pageController?.hide()
    }

    fun updateReflowAdapter() {
        mRecyclerView.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    companion object {

        private const val TAG = "ABaseView"
    }
}
