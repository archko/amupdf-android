package cn.archko.pdf.activities

import android.app.ProgressDialog
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author: archko 2020/5/15 :12:43
 */
class ATextReflowViewController(
    context: FragmentActivity,
    scope: CoroutineScope,
    mControllerLayout: RelativeLayout,
    docViewModel: DocViewModel,
    mPath: String,
    pageControls: PageControls?,
    controllerListener: ControllerListener?,
) : ATextBaseViewController(
    context,
    scope,
    mControllerLayout,
    docViewModel,
    mPath,
    pageControls,
    controllerListener
), OutlineListener, AViewController {

    private var mPageSizes = mutableListOf<APage>()
    private var progressDialog: ProgressDialog? = null
    private var pdfViewModel = PDFViewModel()

    override fun showPasswordDialog() {
        /*PasswordDialog.show(this@AMuPDFRecyclerViewActivity,
            object : PasswordDialog.PasswordDialogListener {
                override fun onOK(content: String?) {
                    loadDoc(password = content)
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@AMuPDFRecyclerViewActivity,
                        "error file path:$mPath",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })*/
    }

    override fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        scope.launch {
            val start = SystemClock.uptimeMillis()
            pdfViewModel.loadPdfDoc(context, mPath, null)
            pdfViewModel.pageFlow
                .collectLatest {
                    progressDialog!!.dismiss()
                    if (it.state == State.PASS) {
                        showPasswordDialog()
                        return@collectLatest
                    }
                    val cp = pdfViewModel.countPages()
                    if (cp > 0) {
                        Logcat.d(
                            TAG,
                            "open:" + (SystemClock.uptimeMillis() - start) + " cp:" + cp
                        )

                        postLoadDoc(cp)
                    } else {
                        context.finish()
                    }
                }
        }
    }

    private fun postLoadDoc(cp: Int) {
        val width = mRecyclerView.width
        var start = SystemClock.uptimeMillis()

        scope.launch {
            docViewModel.preparePageSize(width).collectLatest { pageSizeBean ->
                Logcat.d("open3:" + (SystemClock.uptimeMillis() - start))
                mPageSizes.clear()
                var pageSizes: List<APage>? = null
                if (pageSizeBean != null) {
                    pageSizes = pageSizeBean.List
                }
                if (pageSizes.isNullOrEmpty()) {
                    start = SystemClock.uptimeMillis()
                    preparePageSize(cp)
                    Logcat.d("open2:" + (SystemClock.uptimeMillis() - start))
                } else {
                    Logcat.d("open3:pageSizes>0:" + pageSizes.size)
                    mPageSizes.addAll(pageSizes)
                    //checkPageSize(cp)
                }
                doLoadDoc()
            }
        }
    }

    private fun getPageSize(pageNum: Int): APage? {
        val p = pdfViewModel.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(pageNum, w, h, 1.0f/*zoomModel!!.zoom*/)
    }

    private fun preparePageSize(cp: Int) {
        for (i in 0 until cp) {
            val pointF = getPageSize(i)
            if (pointF != null) {
                mPageSizes.add(pointF)
            }
        }
    }

    override fun doLoadDoc() {
        initReflowMode(docViewModel.getCurrentPage())

        controllerListener?.doLoadedDoc(
            pdfViewModel.countPages(),
            docViewModel.getCurrentPage(),
            pdfViewModel.links
        )
    }

    override fun initReflowMode(pos: Int) {
        if (null == mRecyclerView.adapter) {
            mRecyclerView.adapter = MuPDFReflowAdapter(
                context,
                pdfViewModel.mupdfDocument,
                mStyleHelper,
                scope,
                pdfViewModel
            )
        } else {
            (mRecyclerView.adapter as MuPDFReflowAdapter).setScope(scope)
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
        return mPageSizes.size
    }

    override fun updateProgress(index: Int) {
        if (pdfViewModel.mupdfDocument != null && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun decodePageForTts(currentPos: Int) {
        pdfViewModel.decodeTextForTts(currentPos)
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_TXT
    }

    //--------------------------------------

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument && null != docViewModel.bookProgress) {
            docViewModel.bookProgress!!.reflow = 1
            var savePos = getCurrentPos() + 1
            val lastPos = getLastPos()
            if (lastPos == mPageSizes.size - 1) {
                savePos = lastPos
            }
            docViewModel.saveBookProgress(
                mPath,
                mPageSizes.size,
                savePos,
                docViewModel.bookProgress!!.zoomLevel,
                -1,
                0
            )
        }
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
        pdfViewModel.destroy()
    }

    //===========================================

    companion object {

        private const val TAG = "ReflowView"
    }
}
