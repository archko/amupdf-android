package cn.archko.pdf.fragments

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 字体列表
 * @author: archko 2019/9/29 :15:58
 */
open class FontsFragment : DialogFragment() {

    lateinit var adapter: BaseRecyclerAdapter<FontBean>
    var mStyleHelper: StyleHelper? = null
    var mDataListener: DataListener? = null
    private lateinit var fontsViewModel: FontsViewModel

    private var layoutSearch: View? = null
    private var toolbar: MaterialToolbar? = null
    private var recyclerView: RecyclerView? = null

    fun setStyleHelper(styleHelper: StyleHelper?) {
        this.mStyleHelper = styleHelper
    }

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Holo_Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Dialog
        }
        setStyle(STYLE_NO_FRAME, themeId)

        fontsViewModel = FontsViewModel()
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_font, container, false)
        layoutSearch = view.findViewById(R.id.layoutSearch)
        recyclerView = view.findViewById(R.id.recyclerView)

        dialog?.setTitle("Fonts")

        layoutSearch?.visibility = View.GONE
        toolbar?.setNavigationOnClickListener { dismiss() }

        toolbar?.setTitle(R.string.dialog_title_font)
        toolbar?.setSubtitle(R.string.dialog_sub_title_font)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fontsViewModel.loadFonts().collectLatest { list ->
                    if (list.size > 0) {
                        adapter.data = list
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this@FontsFragment.activity,
                            R.string.dialog_sub_title_font,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = object : BaseRecyclerAdapter<FontBean>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<FontBean> {
                val root = inflater.inflate(R.layout.item_outline, parent, false)
                return FontHolder(root)
            }
        }
        recyclerView?.adapter = adapter
    }

    inner class FontHolder(private val root: View) :
        BaseViewHolder<FontBean>(root) {
        private var title: TextView? = null

        init {
            itemView.minimumHeight = Utils.dipToPixel(48f)
            title = root.findViewById(R.id.title)
        }

        override fun onBind(data: FontBean?, position: Int) {
            title?.setText(
                String.format(
                    getString(R.string.dialog_item_title_font),
                    data?.fontName
                )
            )
            if (data?.fontType == PdfOptionRepository.CUSTOM) {
                if (null != data.file) {
                    val typeface =
                        mStyleHelper?.fontHelper?.createFontByPath(data.file?.absolutePath!!)
                    title?.setTypeface(typeface)
                }
            } else {
                when (data?.fontType) {
                    PdfOptionRepository.DEFAULT -> title?.setTypeface(Typeface.DEFAULT)
                    PdfOptionRepository.SANS_SERIF -> title?.setTypeface(Typeface.SANS_SERIF)
                    PdfOptionRepository.SERIF -> title?.setTypeface(Typeface.SERIF)
                    PdfOptionRepository.MONOSPACE -> title?.setTypeface(Typeface.MONOSPACE)
                }
            }

            if (fontsViewModel.selectedFontName!! == data?.fontName) {
                itemView.setBackgroundResource(R.color.button_pressed)
            } else {
                itemView.setBackgroundResource(R.color.transparent)
            }
            itemView.setOnClickListener {
                mStyleHelper?.fontHelper?.saveFont(data!!)
                this@FontsFragment.dismiss()
                mDataListener?.onSuccess(data)
            }
        }
    }

    companion object {

        const val TAG = "FontsFragment"

        fun showFontsDialog(
            activity: FragmentActivity?,
            styleHelper: StyleHelper?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("font_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            // Create and show the dialog.
            val fragment = FontsFragment()
            val bundle = Bundle()
            fragment.arguments = bundle

            fragment.setListener(dataListener)
            fragment.setStyleHelper(styleHelper)
            fragment.show(ft!!, "font_dialog")
        }
    }
}
