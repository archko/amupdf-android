package cn.archko.pdf.core.entity

/**
 * @author: archko 2024/9/1 :19:49
 */
data class TtsBean(
    val path: String,
    val pc: Int,
    val fileSize: Long,
    val list: List<ReflowBean>
)
