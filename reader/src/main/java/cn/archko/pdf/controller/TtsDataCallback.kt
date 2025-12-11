package cn.archko.pdf.controller

import cn.archko.pdf.core.entity.ReflowBean

/**
 * TTS回调接口，用于传递解码的文本数据
 */
interface TtsDataCallback {
    fun onTtsDataReady(data: List<ReflowBean>)
}
