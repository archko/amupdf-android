package cn.archko.pdf.entity

import android.graphics.Color
import cn.archko.pdf.core.utils.Utils
import androidx.core.graphics.toColorInt

/**
 * @author: archko 2019/7/12 :19:50
 */
data class StyleBean(
    // Text styling
    var textSize: Float = 18f,  // Slightly larger default for better readability
    var fontWeight: Int = 400,  // Normal font weight
    var letterSpacing: Float = 0.5f,  // Slightly increased letter spacing for readability
    var lineSpacingMult: Float = 1.6f,  // Increased line spacing for better readability
    
    // Colors
    var bgColor: Int = "#FFFDF5".toColorInt(),  // Warm white background
    var fgColor: Int = "#2C2C2C".toColorInt(),  // Soft black for text
    var linkColor: Int = "#1A73E8".toColorInt(),  // Material blue for links
    var highlightColor: Int = "#FFECB3".toColorInt(),  // Soft yellow for highlights
    
    // Layout
    var leftPadding: Int = Utils.dipToPixel(24f),  // Increased padding for better margins
    var topPadding: Int = Utils.dipToPixel(32f),
    var rightPadding: Int = Utils.dipToPixel(24f),
    var bottomPadding: Int = Utils.dipToPixel(32f),
    var paragraphSpacing: Int = Utils.dipToPixel(16f),  // Space between paragraphs
    
    // Reading comfort
    var isDarkMode: Boolean = false,  // Support for dark mode
    var isSepia: Boolean = false,  // Support for sepia mode
    var brightness: Float = 1.0f,  // Screen brightness control
    var textAlignment: Int = 0  // 0: Left, 1: Justified, 2: Center
) {
}