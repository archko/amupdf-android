package cn.archko.pdf.controller

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 应用于pdf,epub,mobi,azw3文本重排
 * @author: archko 2020/5/15 :12:43
 */
class ATextReflowViewController(
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
), OutlineListener, AViewController {

    private var mPageSizes = mutableListOf<APage>()
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
        scope.launch {
            val start = SystemClock.uptimeMillis()
            pdfViewModel.loadPdfDoc(context, mPath, null)
            pdfViewModel.pageFlow
                .collectLatest {
                    if (it.state == State.PASS) {
                        showPasswordDialog()
                        return@collectLatest
                    }
                    val cp = pdfViewModel.countPages()
                    Logcat.d(
                        TAG,
                        String.format(
                            "open.cos: %s cp:%s",
                            (SystemClock.uptimeMillis() - start),
                            cp,
                        )
                    )
                    if (cp > 0) {
                        postLoadDoc(cp)
                    } else {
                        context.finish()
                    }
                }
        }
    }

    private fun postLoadDoc(cp: Int) {
        mPageSizes.addAll(pdfViewModel.mPageSizes)
        doLoadDoc()
    }

    override fun doLoadDoc() {
        initReflowMode(docViewModel.getCurrentPage())

        docViewModel.setPageCount(mPageSizes.size)
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
            )
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
        if (pdfViewModel.mupdfDocument != null && pageController?.visibility() == View.VISIBLE) {
            pageController?.updatePageProgress(index)
        }
    }

    override fun decodePageForTts(currentPos: Int) {
        pdfViewModel.decodeTextForTts(currentPos)
    }

    override fun setSpeakingPage(page: Int) {
    }

    override fun selectFont() {
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_TXT
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
