package cn.archko.pdf.core.utils

import cn.archko.pdf.core.App.Companion.instance
import cn.archko.pdf.core.common.Logcat.d
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * @author: archko 2025/9/18 :20:09
 */

object FontCSSGenerator {
    fun getDefFontSize(): Float {
        val fontSize = (8.4f * Utils.getDensityDpi(instance) / 72)
        return fontSize
    }

    fun getFontSize(name: String): Float {
        val mmkv = MMKV.mmkvWithID("epub")
        var fs = mmkv.decodeFloat("font_" + name.hashCode(), getDefFontSize())
        if (fs > 90) {
            fs = 90f
        }
        return fs
    }

    fun setFontSize(name: String, size: Float) {
        val mmkv = MMKV.mmkvWithID("epub")
        d("setFontSize:" + size)
        mmkv.encode("font_" + name.hashCode(), size)
    }

    //"/sdcard/fonts/simsun.ttf"
    fun getFontFace(): String? {
        val mmkv = MMKV.mmkvWithID("epub")
        val fs = mmkv.decodeString("font_face", "")
        return fs
    }

    fun setFontFace(name: String?) {
        val mmkv = MMKV.mmkvWithID("epub")
        d("setFontFace:" + name)
        mmkv.encode("font_face", name)
    }

    fun generateFontCSS(fontPath: String?, margin: String): String {
        val buffer = StringBuilder()

        if (!fontPath.isNullOrEmpty()) {
            val fontFile = File(fontPath)
            if (fontFile.exists()) {
                val fontName = getFontNameFromPath(fontPath)
                buffer.apply {
                    appendLine("@font-face {")
                    appendLine("    font-family: '$fontName' !important;")
                    appendLine("    src: url('file://$fontPath');")
                    appendLine("}")

                    appendLine("* {")
                    appendLine("    font-family: '$fontName', serif !important;")
                    appendLine("}")
                }
            }
        }

        buffer.apply {
            // 忽略mupdf的边距
            appendLine("    @page { margin:$margin $margin !important; }")
            appendLine("    p { margin: 20px !important; padding: 0 !important; }")
            appendLine("    blockquote { margin: 0 !important; padding: 0 !important; }")

            // 强制所有元素的边距和内边距
            appendLine("* {")
            appendLine("    margin: 0 !important;")
            appendLine("    padding: 0 !important;")
            appendLine("}")
        }
        return buffer.toString()
    }

    private fun getFontNameFromPath(fontPath: String): String {
        val fileName = File(fontPath).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.take(dotIndex) else fileName
    }
}