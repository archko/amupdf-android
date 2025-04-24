package cn.archko.pdf.common

import androidx.activity.ComponentActivity
import cn.archko.pdf.entity.StyleBean

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
        styleBean = StyleBean(
            textSize = PdfOptionRepository.getTextSize(),
            fontWeight= 400,
            letterSpacing= 0.5f,
            lineSpacingMult = PdfOptionRepository.getLineSpacingMult(),
            bgColor = PdfOptionRepository.getBgColor(),
            fgColor = PdfOptionRepository.getFgColor(),
            PdfOptionRepository.getLeftPadding(),
            PdfOptionRepository.getTopPadding(),
            PdfOptionRepository.getRightPadding(),
            PdfOptionRepository.getBottomPadding()
        )
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
