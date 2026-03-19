package com.archko.reader.pdf.component

/**
 * @author: archko 2025/1/5 :07:53
 */
public data class Size(
    val width: Int,
    val height: Int,
    val page: Int,
    val scale: Float = 1.0f,
    val offsetHeight: Int = 0,
)

public data class IntSize(
    val width: Int,
    val height: Int,
)