package cn.archko.pdf.common

import androidx.core.app.ComponentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.entity.StyleBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2019-06-19 :12:27
 */
class StyleHelper(
    private var context: ComponentActivity,
    private var optionRepository: PdfOptionRepository
) {

    var styleBean: StyleBean? = null
        get() {
            return field
        }

    var fontHelper: FontHelper? = null
        get() {
            if (field == null) {
                fontHelper = FontHelper(context, optionRepository)
            }

            return field
        }

    init {
        loadStyle()
    }

    private fun loadStyle() {
        fontHelper = FontHelper(context, optionRepository)
        fontHelper?.loadFont()
        loadStyleFromSP()
    }

    fun loadStyleFromSP() {
        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val data = optionRepository.pdfOptionFlow.first()

                styleBean = StyleBean(
                    data.textSize,
                    data.bgColor,
                    data.fgColor,
                    data.lineSpacingMult,
                    data.leftPadding,
                    data.topPadding,
                    data.rightPadding,
                    data.bottomPadding
                )
            }
        }
    }

    fun saveStyleToSP(sBean: StyleBean?) {
        sBean?.run {
            styleBean = sBean
            context.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    optionRepository.setTextSize(sBean.textSize)
                    optionRepository.setBgColor(sBean.bgColor)
                    optionRepository.setFgColor(sBean.fgColor)
                    optionRepository.setLineSpacingMult(sBean.lineSpacingMult)
                    optionRepository.setLeftPadding(sBean.leftPadding)
                    optionRepository.setTopPadding(sBean.topPadding)
                    optionRepository.setRightPadding(sBean.rightPadding)
                    optionRepository.setBottomPadding(sBean.bottomPadding)
                }
            }
        }
    }
}
