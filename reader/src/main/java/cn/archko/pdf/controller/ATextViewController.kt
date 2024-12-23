package cn.archko.pdf.controller

import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.adapters.MuPDFTextAdapter
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.viewmodel.TextViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * text,html,json,js.eg.
 * @author: archko 2024/8/15 :12:43
 */
class ATextViewController(
    context: FragmentActivity,
    scope: CoroutineScope,
    mControllerLayout: RelativeLayout,
    docViewModel: DocViewModel,
    mPath: String,
    pageController: IPageController?,
    controllerListener: ControllerListener?,
) : ATextBaseViewController(
    context,
    scope,
    mControllerLayout,
    docViewModel,
    mPath,
    pageController,
    controllerListener
),
    OutlineListener, AViewController {

    private var viewModel = TextViewModel()

    override fun loadDocument() {
        scope.launch {
            viewModel.loadTextDoc(mPath)
            context.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.textFlow.collect {
                    doLoadDoc(it.list)
                }
            }
        }
    }

    private fun doLoadDoc(list: List<ReflowBean>?) {
        initReflowMode(docViewModel.getCurrentPage())
        (mRecyclerView.adapter as MuPDFTextAdapter).data = list

        docViewModel.setPageCount(viewModel.countPages())
        controllerListener?.doLoadedDoc(
            viewModel.countPages(),
            docViewModel.getCurrentPage(),
            listOf()
        )
    }

    override fun initReflowMode(pos: Int) {
        if (null == mRecyclerView.adapter) {
            mRecyclerView.adapter = MuPDFTextAdapter(context, mStyleHelper)
        }

        if (pos > 0) {
            val layoutManager = mRecyclerView.layoutManager
            layoutManager!!.scrollToPosition(pos)
            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d("onGlobalLayout:$this,pos:$pos")
                    layoutManager.scrollToPosition(pos)
                }
            })
        }
    }

    override fun getCount(): Int {
        return mRecyclerView.adapter?.itemCount ?: 0
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

    override fun updateProgress(index: Int) {
        if ((mRecyclerView.adapter?.itemCount
                ?: 0) > 0 && pageController?.visibility() == View.VISIBLE
        ) {
            pageController?.updatePageProgress(index)
        }
    }

    override fun decodePageForTts(currentPos: Int) {
        viewModel.decodeTextForTts(
            getCurrentPos(),
            (mRecyclerView.adapter as MuPDFTextAdapter).data
        )
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_TXT
    }

    //--------------------------------------

    override fun onPause() {
        val count = mRecyclerView.adapter?.itemCount ?: 0
        if (null != docViewModel.bookProgress && count > 0) {
            docViewModel.bookProgress!!.reflow = 1
            var savePos = getCurrentPos() + 1
            val lastPos = getLastPos()
            if (lastPos == count - 1) {
                savePos = lastPos
            }
            docViewModel.saveBookProgress(
                mPath,
                count,
                savePos,
                docViewModel.bookProgress!!.zoomLevel,
                -1,
                0
            )
        }
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFTextAdapter) {
            (mRecyclerView.adapter as MuPDFTextAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
    }

}
