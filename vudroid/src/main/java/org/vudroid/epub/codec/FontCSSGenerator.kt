package org.vudroid.epub.codec

/**
 * @author: archko 2025/9/18 :20:09
 */

import java.io.File

object FontCSSGenerator {

    fun generateFontCSS(fontPath: String?, margin: String): String {
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

            appendLine("* {")
            appendLine("    font-family: '$fontFamily', serif !important;")
            appendLine("}")

            // 忽略mupdf的边距
            appendLine("    @page { margin:$margin $margin !important; }")
            appendLine("    p { margin: 30px !important; padding: 0 !important; }")
            appendLine("    blockquote { margin: 0 !important; padding: 0 !important; }")

            // 强制所有元素的边距和内边距
            appendLine("* {")
            appendLine("    margin: 0 !important;")
            appendLine("    padding: 0 !important;")
            appendLine("}")
        }
    }

    private fun getFontNameFromPath(fontPath: String): String {
        val fileName = File(fontPath).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }
}