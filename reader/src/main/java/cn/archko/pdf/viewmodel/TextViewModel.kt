package cn.archko.pdf.viewmodel

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import cn.archko.pdf.common.TtsHelper
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.TextHelper
import cn.archko.pdf.core.entity.LoadResult
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.entity.TtsBean
import cn.archko.pdf.tts.TTSEngine
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

    fun decodeTextForTts(currentPos: Int, data: List<ReflowBean>?) {
        if (data.isNullOrEmpty() || TextUtils.isEmpty(pdfPath)) {
            return
        }
        val last = TTSEngine.get().getLast()
        val count = countPages()
        Logcat.i(Logcat.TAG, "decodeTextForTts:last:$last, count:$count, currentPos:$currentPos")
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
                val str = data[i].data
                if (str != null && !TextUtils.isEmpty(str.trim())) {
                    TTSEngine.get().speak("$i-$i", str)
                    list.add(ReflowBean(str, page = "$i-$i"))
                }
            }
            TtsHelper.saveToFile(count, pdfPath!!, list)
        } else {
            for (bean in ttsBean.list) {
                TTSEngine.get().speak(bean.page, bean.data)
            }
        }
        Logcat.i(Logcat.TAG, "decodeTextForTts.cos:${System.currentTimeMillis() - start}")
    }
}