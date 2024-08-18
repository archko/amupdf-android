package cn.archko.pdf.common

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.entity.StyleBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2019-06-19 :12:27
 */
class StyleHelper(
    private var context: ComponentActivity,
) {

    var styleBean: StyleBean? = null
        get() {
            return field
        }

    var fontHelper: FontHelper? = null
        get() {
            if (field == null) {
                fontHelper = FontHelper(context)
            }

            return field
        }

    init {
        loadStyle()
    }

    private fun loadStyle() {
        fontHelper = FontHelper(context)
        fontHelper?.loadFont()
        loadStyleFromSP()
    }

    private fun loadStyleFromSP() {
        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {

                styleBean = StyleBean(
                    PdfOptionRepository.getTextSize(),
                    PdfOptionRepository.getBgColor(),
                    PdfOptionRepository.getFgColor(),
                    PdfOptionRepository.getLineSpacingMult(),
                    PdfOptionRepository.getLeftPadding(),
                    PdfOptionRepository.getTopPadding(),
                    PdfOptionRepository.getRightPadding(),
                    PdfOptionRepository.getBottomPadding()
                )
            }
        }
    }

    fun saveStyleToSP(sBean: StyleBean?) {
        sBean?.run {
            styleBean = sBean
            PdfOptionRepository.setTextSize(sBean.textSize)
            PdfOptionRepository.setBgColor(sBean.bgColor)
            PdfOptionRepository.setFgColor(sBean.fgColor)
            PdfOptionRepository.setLineSpacingMult(sBean.lineSpacingMult)
            PdfOptionRepository.setLeftPadding(sBean.leftPadding)
            PdfOptionRepository.setTopPadding(sBean.topPadding)
            PdfOptionRepository.setRightPadding(sBean.rightPadding)
            PdfOptionRepository.setBottomPadding(sBean.bottomPadding)
        }
    }
}
