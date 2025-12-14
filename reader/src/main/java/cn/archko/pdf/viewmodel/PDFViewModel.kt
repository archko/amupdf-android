package cn.archko.pdf.viewmodel

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.TtsHelper
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.LoadResult
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.entity.TtsBean
import com.artifex.mupdf.fitz.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.vudroid.core.codec.OutlineLink
import org.vudroid.pdfdroid.codec.PdfDocument

class PDFViewModel : ViewModel() {

    var pdfPath: String? = null

    var mupdfDocument: MupdfDocument? = null
    val mPageSizes = mutableListOf<APage>()
    var isDestroy = false
    val links = mutableListOf<OutlineLink>()

    private val _pageFlow = MutableStateFlow<LoadResult<Any, APage>>(LoadResult(State.INIT))
    val pageFlow: StateFlow<LoadResult<Any, APage>>
        get() = _pageFlow

    suspend fun loadPdfDoc(context: Context, path: String, password: String?) = flow {
        try {
            pdfPath = path
            if (null == mupdfDocument) {
                mupdfDocument = MupdfDocument(context)
            }
            Logcat.d(Logcat.TAG, "loadPdfDoc.password:$password")
            if (IntentFile.isDjvu(path)) {
                emit(listOf())
                return@flow
            }
            mupdfDocument!!.newDocument(path, password)
            mupdfDocument!!.let {
                if (it.getDocument()!!.needsPassword()) {
                    Logcat.d(Logcat.TAG, "needsPassword")
                    if (TextUtils.isEmpty(password)) {
                        emit(null)
                        return@flow
                    }
                    it.getDocument()!!.authenticatePassword(password)
                }
            }

            links.clear()
            PdfDocument.downOutline(
                mupdfDocument!!.getDocument(),
                mupdfDocument!!.loadOutline(),
                links
            )

            val cp = mupdfDocument!!.countPages()
            mPageSizes.clear()
            emit(loadAllPageSize(cp))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(listOf())
        }
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            val state = if (null == it) {
                State.PASS
            } else if (it.isNotEmpty()) {
                State.FINISHED
            } else {
                State.ERROR
            }
            _pageFlow.value = LoadResult(
                state,
                list = it
            )
        }

    private fun loadAllPageSize(cp: Int): List<APage> {
        for (i in 0 until cp) {
            val pointF = loadPageSize(i)
            if (pointF != null) {
                mPageSizes.add(pointF)
            }
        }
        return mPageSizes
    }

    private fun loadPageSize(pageNum: Int): APage? {
        val p = mupdfDocument?.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(pageNum, w, h, 1.0f/*zoomModel!!.zoom*/)
    }

    fun destroy() {
        Logcat.d(Logcat.TAG, "destroy:$mupdfDocument")
        isDestroy = true
        mupdfDocument?.destroy()
        mupdfDocument = null
    }

    fun countPages(): Int {
        val pc = mupdfDocument?.countPages() ?: 0
        return pc
    }

    fun loadPage(pageNum: Int): Page? {
        return mupdfDocument?.loadPage(pageNum)
    }

    fun decodeTextForTts(currentPos: Int, callback: (List<ReflowBean>) -> Unit) {
        if (null == mupdfDocument || TextUtils.isEmpty(pdfPath)) {
            callback(emptyList())
            return
        }
        val count = countPages()
        val fileSize = java.io.File(pdfPath!!).length()
        Logcat.i(Logcat.TAG, "decodePageForTts: count:$count, currentPos:$currentPos")

        val ttsBean: TtsBean? = TtsHelper.loadFromFile(count, pdfPath!!, fileSize)
        if (ttsBean?.list == null) {
            val list = mutableListOf<ReflowBean>()
            for (i in 0 until count) {
                val beans: List<ReflowBean>? = mupdfDocument!!.decodeReflowText(i)
                if (beans != null) {
                    list.addAll(beans)
                }
            }
            Logcat.i(Logcat.TAG, "decodeTextForTts decoded ${list.size} items")
            TtsHelper.saveToFile(count, pdfPath!!, fileSize, list)
            callback(list)
        } else {
            callback(ttsBean.list)
        }
    }
}
