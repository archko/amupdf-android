package cn.archko.pdf.entity

import androidx.core.graphics.toColorInt
import cn.archko.pdf.core.utils.Utils

const val padding = 12f

/**
 * @author: archko 2019/7/12 :19:50
 */
data class StyleBean(
    // Text styling
    var textSize: Float = 16f,  // Slightly larger default for better readability
    var fontWeight: Int = 400,  // Normal font weight
    var letterSpacing: Float = 0.5f,  // Slightly increased letter spacing for readability
    var lineSpacingMult: Float = 1.4f,  // Increased line spacing for better readability

    // Colors
    var bgColor: Int = "#FFFDF5".toColorInt(),  // Warm white background
    var fgColor: Int = "#2C2C2C".toColorInt(),  // Soft black for text
    var linkColor: Int = "#1A73E8".toColorInt(),  // Material blue for links
    var highlightColor: Int = "#FFECB3".toColorInt(),  // Soft yellow for highlights

    // Layout
    var leftPadding: Int = Utils.dipToPixel(padding),  // Increased padding for better margins
    var topPadding: Int = Utils.dipToPixel(padding),
    var rightPadding: Int = Utils.dipToPixel(padding),
    var bottomPadding: Int = Utils.dipToPixel(padding),
    var paragraphSpacing: Int = Utils.dipToPixel(padding),  // Space between paragraphs

    // Reading comfort
    var isDarkMode: Boolean = false,  // Support for dark mode
    var isSepia: Boolean = false,  // Support for sepia mode
    var brightness: Float = 1.0f,  // Screen brightness control
    var textAlignment: Int = 0  // 0: Left, 1: Justified, 2: Center
) {
}