package cn.archko.pdf.activities

import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.adapters.MuPDFTextAdapter
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @author: archko 2024/8/15 :12:43
 */
class ATextViewController(
    private var context: FragmentActivity,
    private var scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var docViewModel: DocViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var controllerListener: ControllerListener?,
) : ATextBaseViewController(
    context,
    scope,
    mControllerLayout,
    docViewModel,
    mPath,
    pageControls,
    controllerListener
),
    OutlineListener, AViewController {

    override fun loadDocument() {
        scope.launch {
            pdfViewModel.loadTextDoc(mPath)
            context.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pdfViewModel.textFlow.collect {
                    doLoadDoc(it.list)
                }
            }
        }
    }

    private fun doLoadDoc(list: List<ReflowBean>?) {
        initReflowMode(docViewModel.getCurrentPage())
        (mRecyclerView.adapter as MuPDFTextAdapter).data = list

        controllerListener?.doLoadedDoc(
            pdfViewModel.countPages(),
            docViewModel.getCurrentPage(),
            pdfViewModel.links
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

    override fun setOrientation(ori: Int) {
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
                ?: 0) > 0 && pageControls?.visibility() == View.VISIBLE
        ) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun decodePageForTts(currentPos: Int) {
    }

    //--------------------------------------

    override fun onPause() {
        val count = mRecyclerView.adapter?.itemCount ?: 0
        if (null != pdfViewModel.mupdfDocument && null != docViewModel.bookProgress && count > 0) {
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
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFTextAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
        pdfViewModel.destroy()
    }

}
