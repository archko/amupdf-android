package cn.archko.pdf.controller

import android.view.View
import androidx.fragment.app.FragmentActivity
import cn.archko.pdf.R
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.fragments.FontsFragment.Companion.type_select
import cn.archko.pdf.viewmodel.DocViewModel
import org.vudroid.epub.codec.EpubDocument

/**
 * epub,mobi,azw3
 *
 * @author archko
 */
class EpubPageController(
    view: View,
    docViewModel: DocViewModel?,
    controlListener: PageControllerListener?
) : PdfPageController(view, docViewModel, controlListener) {
    private val reflowLayout: View
    private val fontButton: View = view.findViewById(R.id.fontButton)

    init {
        fontButton.visibility = View.VISIBLE
        fontButton.setOnClickListener {
            FontsFragment.showFontsDialog(
                view.context as FragmentActivity, null,
                type = type_select,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        val fontBean = args[0] as FontBean
                        val absolutePath = fontBean.file?.absolutePath ?: ""
                        EpubDocument.setFontFace(absolutePath)
                        controlListener?.selectFont()
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }

        reflowButton.visibility = View.GONE
        imageButton.visibility = View.GONE
        autoCropButton.visibility = View.VISIBLE
        outlineButton.visibility = View.VISIBLE
        oriButton.visibility = View.VISIBLE
        ttsButton.visibility = View.VISIBLE
        ocrButton.visibility = View.VISIBLE
        previewButton.visibility = View.VISIBLE

        reflowLayout = view.findViewById<View>(R.id.reflow_layout)
        reflowLayout.visibility = View.VISIBLE
    }

    override fun update(count: Int, page: Int, viewMode: ViewMode?) {
        super.update(count, page)
        if (viewMode == ViewMode.REFLOW) {
            reflowLayout.visibility = View.GONE
        }
    }

    override fun show() {
        topLayout.visibility = View.VISIBLE
        bottomLayout.visibility = View.VISIBLE
        reflowLayout.visibility = View.VISIBLE
        updateOrientation()
    }

    override fun hide() {
        topLayout.visibility = View.GONE
        bottomLayout.visibility = View.GONE
        reflowLayout.visibility = View.GONE
    }

}
