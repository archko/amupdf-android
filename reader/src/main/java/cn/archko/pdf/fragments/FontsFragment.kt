package cn.archko.pdf.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.common.FontHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.PdfOptionRepository
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.entity.FontBean
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

    private var toolbar: MaterialToolbar? = null
    private var recyclerView: RecyclerView? = null
    private var type: String = type_reflow

    fun setStyleHelper(styleHelper: StyleHelper?) {
        this.mStyleHelper = styleHelper
    }

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)

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
        dialog?.apply {
            window!!.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.dialog_background))
            window!!.decorView?.elevation = 16f // 16dp 的阴影深度，可根据需要调整
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0.5f 
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.85).toInt()
            window?.setLayout(width, height)
        }

        val view = inflater.inflate(R.layout.fragment_font, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        val itemDecoration = ColorItemDecoration(requireContext())
        recyclerView!!.addItemDecoration(itemDecoration)

        dialog?.setTitle("Fonts")

        toolbar?.setNavigationOnClickListener { dismiss() }

        toolbar?.setTitle(R.string.dialog_title_font)
        toolbar?.setSubtitle(R.string.dialog_sub_title_font)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (type == type_reflow) {
                    fontsViewModel.loadFonts().collectLatest { list ->
                        if (list.isNotEmpty()) {
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
                } else {
                    fontsViewModel.loadSdcardFonts().collectLatest { list ->
                        if (list.isNotEmpty()) {
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

    inner class FontHolder(root: View) :
        BaseViewHolder<FontBean>(root) {
        private var title: TextView? = null

        init {
            itemView.minimumHeight = Utils.dipToPixel(48f)
            title = root.findViewById(R.id.title)
        }

        override fun onBind(data: FontBean?, position: Int) {
            title?.text = String.format(
                getString(R.string.dialog_item_title_font),
                data?.fontName
            )
            if (data?.fontType == PdfOptionRepository.CUSTOM) {
                if (null != data.file) {
                    val typeface = FontHelper.createFontByPath(data.file?.absolutePath!!)
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
        const val type_reflow = "type_reflow"
        const val type_select = "type_select"

        fun showFontsDialog(
            activity: FragmentActivity?,
            styleHelper: StyleHelper?,
            type: String = type_reflow,
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
            fragment.type = type

            fragment.setListener(dataListener)
            fragment.setStyleHelper(styleHelper)
            fragment.show(ft!!, "font_dialog")
        }
    }
}
