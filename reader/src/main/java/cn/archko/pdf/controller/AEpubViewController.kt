package cn.archko.pdf.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import cn.archko.pdf.R
import cn.archko.pdf.core.App.Companion.instance
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.codec.CodecDocument
import org.vudroid.epub.codec.EpubDocument

/**
 * epub的controller,有字体调整需求.
 * 其它功能与原来的pdf是一样
 * @author: archko 2024/12/11 :20:43
 */
class AEpubViewController(
    context: FragmentActivity,
    scope: CoroutineScope,
    mControllerLayout: RelativeLayout,
    docViewModel: DocViewModel,
    var mPath: String,
    pageController: IPageController?,
    controllerListener: ControllerListener?,
) : ANormalViewController(
    context,
    scope,
    mControllerLayout,
    docViewModel,
    mPath,
    pageController,
    controllerListener
),
    OutlineListener, AViewController {

    private var mStyleControls: View? = null
    private var fontSlider: Slider? = null

    private fun initStyleControls() {
        pageController?.hide()
        if (null == mStyleControls) {
            mStyleControls = LayoutInflater.from(context).inflate(R.layout.epub_style, null, false)
            fontSlider = mStyleControls!!.findViewById(R.id.font_slider)
            fontSlider?.apply {
                valueFrom = 20f
                valueTo = 80f
                value = EpubDocument.getFontSize(mPath)
            }

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }
        fontSlider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                applyFontSize(slider.value)
            }
        })
    }

    private fun applyFontSize(newSize: Float) {
        val def = EpubDocument.getFontSize(mPath)
        if (def != newSize) {
            EpubDocument.setFontSize(mPath, newSize)
            val w = Utils.getScreenWidthPixelWithOrientation(instance)
            val h = Utils.getScreenHeightPixelWithOrientation(instance)
            Logcat.d(String.format("applyFontSize:%s, %s, %s", newSize, w, h))
            decodeService?.resetCrop()
            controllerListener?.reloadDoc()
        }
    }

    override fun doLoadDoc(pageSizeBean: APageSizeLoader.PageSizeBean, document: CodecDocument) {
        super.doLoadDoc(pageSizeBean, document)

        initStyleControls()
    }
}
