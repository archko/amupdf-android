package cn.archko.pdf.entity

import cn.archko.pdf.core.entity.ReflowBean

/**
 * @author: archko 2024/9/1 :19:49
 */
data class TtsBean(
    val path: String,
    val pc: Int,
    val list: List<ReflowBean>
)