package cn.archko.pdf.viewmodel

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import cn.archko.pdf.core.common.TtsHelper
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.LoadResult
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.entity.TtsBean
import cn.archko.pdf.tts.TTSEngine
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

    fun decodeTextForTts(currentPos: Int) {
        if (null == mupdfDocument || TextUtils.isEmpty(pdfPath)) {
            return
        }
        val last = TTSEngine.get().getLast()
        val count = countPages()
        Logcat.i(Logcat.TAG, "decodePageForTts:last:$last, count:$count, currentPos:$currentPos")
        if (last == count - 1 && last != 0) {
            return
        }
        if (last > 0) {
            TTSEngine.get().reset()
        }
        val ttsBean: TtsBean? = TtsHelper.loadFromFile(count, pdfPath!!)
        if (ttsBean?.list == null) {
            val start = System.currentTimeMillis()
            val list = mutableListOf<ReflowBean>()
            for (i in currentPos until count) {
                val beans: List<ReflowBean>? = mupdfDocument!!.decodeReflowText(i)
                if (beans != null) {
                    //理论上只有一个数据
                    for (j in beans.indices) {
                        list.add(beans[j])
                    }
                }
            }
            Logcat.i(Logcat.TAG, "decodeTextForTts.cos:${System.currentTimeMillis() - start}")
            TtsHelper.saveToFile(count, pdfPath!!, list)
            for (i in currentPos until list.size) {
                TTSEngine.get().speak(list[i])
            }
        } else {
            for (i in currentPos until ttsBean.list.size) {
                TTSEngine.get().speak(ttsBean.list[i])
            }
        }
    }
}