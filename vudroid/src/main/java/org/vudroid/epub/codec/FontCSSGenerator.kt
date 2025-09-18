package org.vudroid.epub.codec

/**
 * @author: archko 2025/9/18 :20:09
 */

import java.io.File

object FontCSSGenerator {

    private const val FONT_DIR = "/sdcard/fonts"
    private val fontNameMapping = mutableMapOf(
        "serif" to "SimSun",
        "sans-serif" to "Microsoft YaHei",
        "monospace" to "SimHei",
        "SimSun" to "SimSun",
        "宋体" to "SimSun",
        "微软雅黑" to "Microsoft YaHei",
        "黑体" to "SimHei"
    )

    fun generateFontCSS(fontPath: String?): String {
        if (fontPath.isNullOrEmpty()) return ""

        val fontFile = File(fontPath)
        if (!fontFile.exists()) return ""

        val fontName = getFontNameFromPath(fontPath)
        val fontFamily = getFontFamily(fontName)

        return buildString {
            appendLine("@font-face {")
            appendLine("    font-family: '$fontFamily';")
            appendLine("    src: url('file://$fontPath');")
            appendLine("}")
            appendLine()
            appendLine("body {")
            appendLine("    font-family: '$fontFamily', serif;")
            appendLine("}")
            appendLine()
            appendLine("p, div, span {")
            appendLine("    font-family: '$fontFamily', serif;")
            appendLine("}")
        }
    }

    fun generateCSSForFontName(fontName: String): String {
        val mappedName = fontNameMapping[fontName] ?: fontName
        val fontPath = "$FONT_DIR/$mappedName.ttf"

        val fontFile = File(fontPath)
        if (!fontFile.exists()) {
            val otfPath = "$FONT_DIR/$mappedName.otf"
            val otfFile = File(otfPath)
            if (!otfFile.exists()) {
                return ""
            }
            return generateFontCSS(otfPath)
        }
        return generateFontCSS(fontPath)
    }

    fun generateCSSForSerifFont(): String = generateCSSForFontName("serif")

    fun generateCSSForSansSerifFont(): String = generateCSSForFontName("sans-serif")

    fun generateCSSForMonospaceFont(): String = generateCSSForFontName("monospace")

    fun generateDefaultFontCSS(): String = buildString {
        val serifCSS = generateCSSForSerifFont()
        val sansCSS = generateCSSForSansSerifFont()
        val monoCSS = generateCSSForMonospaceFont()

        if (serifCSS.isNotEmpty()) {
            appendLine(serifCSS)
        }
        if (sansCSS.isNotEmpty()) {
            appendLine(sansCSS)
        }
        if (monoCSS.isNotEmpty()) {
            appendLine(monoCSS)
        }

        appendLine("body { font-family: 'SimSun', serif; }")
        appendLine("h1, h2, h3, h4, h5, h6 { font-family: 'SimSun', serif; }")
        appendLine("pre, code { font-family: 'SimHei', monospace; }")
    }

    private fun getFontNameFromPath(fontPath: String): String {
        val fileName = File(fontPath).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }

    private fun getFontFamily(fontName: String): String = fontNameMapping[fontName] ?: fontName

    fun isFontAvailable(fontName: String): Boolean {
        val mappedName = fontNameMapping[fontName] ?: fontName
        val ttfPath = "$FONT_DIR/$mappedName.ttf"
        val otfPath = "$FONT_DIR/$mappedName.otf"

        return File(ttfPath).exists() || File(otfPath).exists()
    }

    fun addFontMapping(key: String, value: String) {
        fontNameMapping[key] = value
    }

    fun removeFontMapping(key: String) {
        fontNameMapping.remove(key)
    }

    fun clearFontMappings() {
        fontNameMapping.clear()
    }
}