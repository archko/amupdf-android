package cn.archko.pdf.common

import android.graphics.Typeface
import androidx.core.app.ComponentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2019-06-19 :12:27
 */
class FontHelper(
    private var context: ComponentActivity,
    private var optionRepository: PdfOptionRepository
) {

    init {
        loadFont()
    }

    var fontBean: FontBean? = null
    var typeface: Typeface? = null
        get() {
            if (field == null) {
                loadFont()
            }

            return field
        }

    fun loadFont() {
        context.lifecycleScope.launch {
            var fontType = PdfOptionRepository.DEFAULT
            var fontName = PdfOptionRepository.SYSTEM_FONT
            withContext(Dispatchers.IO) {
                val data = optionRepository.pdfOptionFlow.first()
                fontType = data.fontType
                fontName = data.fontName
            }
            initFontBean(fontType, fontName)
        }
    }

    private fun initFontBean(fontType: Int, fontName: String?) {
        if (null == fontBean) {
            fontBean = FontBean(fontType, fontName, null)
        }
        fontBean?.fontType = fontType
        fontBean?.fontName = fontName

        if (fontType == PdfOptionRepository.CUSTOM) {
            fontBean?.file = File(FileUtils.getStoragePath(PdfOptionRepository.FONT_DIR + fontName))
            typeface = createFont(fontName)
        } else {
            when (fontType) {
                PdfOptionRepository.DEFAULT -> typeface = Typeface.DEFAULT
                PdfOptionRepository.SANS_SERIF -> typeface = Typeface.SANS_SERIF
                PdfOptionRepository.SERIF -> typeface = Typeface.SERIF
                PdfOptionRepository.MONOSPACE -> typeface = Typeface.MONOSPACE
            }
        }
    }

    fun saveFont(fBean: FontBean) {
        if ((fontBean?.fontType == PdfOptionRepository.CUSTOM && !fBean.fontName.equals(fontBean?.fontName))
            || fBean.fontType != fontBean?.fontType
        ) {
            initFontBean(fBean.fontType, fBean.fontName)
        }

        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                optionRepository.setFontType(fontBean?.fontType!!)
                optionRepository.setFontName(fontBean?.fontName!!)
            }
        }
    }

    fun createFont(fontName: String?): Typeface? {
        val fontPath = FileUtils.getStoragePath(PdfOptionRepository.FONT_DIR + fontName)
        return if (!File(fontPath).exists()) {
            null
        } else Typeface.createFromFile(fontPath)
    }

    fun createFontByPath(fontPath: String): Typeface? {
        return Typeface.createFromFile(fontPath)
    }
}
