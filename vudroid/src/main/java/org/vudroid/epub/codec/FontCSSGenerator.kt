package org.vudroid.epub.codec

/**
 * @author: archko 2025/9/18 :20:09
 */

import java.io.File

object FontCSSGenerator {

    fun generateFontCSS(fontPath: String?): String {
        if (fontPath.isNullOrEmpty()) return ""

        val fontFile = File(fontPath)
        if (!fontFile.exists()) return ""

        val fontName = getFontNameFromPath(fontPath)
        val fontFamily = fontName

        return buildString {
            appendLine("@font-face {")
            appendLine("    font-family: '$fontFamily' !important;")
            appendLine("    src: url('file://$fontPath');")
            appendLine("}")
            appendLine()
            appendLine("body {")
            appendLine("    font-family: '$fontFamily', serif !important;")
            appendLine("}")
            appendLine()
            appendLine("p, div, span, h1, h2, h3, h4, h5, h6, calibre, calibre1, calibre2, calibre3, calibre4, calibre5, calibre6, contents, contents1 {")
            appendLine("    font-family: '$fontFamily', serif !important;")
            appendLine("}")
        }
    }

//        appendLine("body { font-family: 'SimSun', serif; }")
//        appendLine("h1, h2, h3, h4, h5, h6 { font-family: 'SimSun', serif; }")
//        appendLine("pre, code { font-family: 'SimHei', monospace; }")

    private fun getFontNameFromPath(fontPath: String): String {
        val fileName = File(fontPath).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }

}