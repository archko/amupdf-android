package cn.archko.pdf.viewmodel

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.TextHelper
import cn.archko.pdf.core.common.TtsHelper
import cn.archko.pdf.core.entity.LoadResult
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.entity.TtsBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TextViewModel : ViewModel() {

    var pdfPath: String? = null
    var txtPageCount: Int = 1
    var reflowBeans: List<ReflowBean>? = null

    private val _textFlow = MutableStateFlow<LoadResult<Any, ReflowBean>>(LoadResult(State.INIT))
    val textFlow: StateFlow<LoadResult<Any, ReflowBean>>
        get() = _textFlow

    suspend fun loadTextDoc(path: String) = flow {
        pdfPath = path
        reflowBeans = TextHelper.readString(path)
        txtPageCount = reflowBeans!!.size
        emit(reflowBeans)
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            val state = if (it!!.isNotEmpty()) {
                State.FINISHED
            } else {
                State.ERROR
            }
            _textFlow.value = LoadResult(
                state,
                list = it
            )
        }

    fun countPages(): Int {
        if (!TextUtils.isEmpty(pdfPath)) {
            return txtPageCount
        }
        return 0
    }

    fun decodeTextForTts(currentPos: Int, data: List<ReflowBean>?, callback: (List<ReflowBean>) -> Unit) {
        if (data.isNullOrEmpty() || TextUtils.isEmpty(pdfPath)) {
            callback(emptyList())
            return
        }

        val count = countPages()
        Logcat.i("reflow", "decodeTextForTts: count:$count, currentPos:$currentPos")

        val ttsBean: TtsBean? = TtsHelper.loadFromFile(count, pdfPath!!)
        if (ttsBean?.list == null) {
            val list = mutableListOf<ReflowBean>()
            for (i in currentPos until count) {
                val str = data.getOrNull(i)?.data
                if (str != null && !TextUtils.isEmpty(str.trim())) {
                    list.add(ReflowBean(str, page = "$i-$i"))
                }
            }
            Logcat.i(Logcat.TAG, "decodeTextForTts decoded ${list.size} items")
            TtsHelper.saveToFile(count, pdfPath!!, list)
            callback(list)
        } else {
            val subList = if (currentPos < ttsBean.list.size) {
                ttsBean.list.subList(currentPos, ttsBean.list.size)
            } else {
                emptyList()
            }
            callback(subList)
        }
    }
}
