package com.archko.reader.pdf.component

/**
 * @author: archko 2026/2/4 :08:17
 */
public const val Vertical: Int = 0
public const val Horizontal: Int = 1
public const val DoublePage: Int = 2

public data class JumpIntent(
    val page: Int = 0,
    val mode: JumpMode = JumpMode.PageNavigation,
    val offsetY: Float? = null  // 精确的Y轴偏移量（相对于页面顶部）
)

public sealed class JumpMode {
    public object PageRestore : JumpMode()     // 书签恢复，使用精确offset
    public object PageNavigation : JumpMode()  // 页面跳转，使用页面顶部或offsetY
}