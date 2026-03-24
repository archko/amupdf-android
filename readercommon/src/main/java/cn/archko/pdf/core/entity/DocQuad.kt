package cn.archko.pdf.core.entity

/**
 * MuPDF Quad类的表示（使用Offset简化）
 * @author: archko 2026/3/1
 */
data class DocQuad(
    val ul: Offset,  // 左上角
    val ur: Offset,  // 右上角
    val ll: Offset,  // 左下角
    val lr: Offset   // 右下角
)
