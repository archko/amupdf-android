package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.databinding.DialogTtsTextBinding

/**
 * @author: archko 2023/7/28 :14:34
 */
class TtsTextFragment : DialogFragment(R.layout.dialog_tts_text) {

    private lateinit var binding: DialogTtsTextBinding
    private var mDataListener: DataListener? = null
    private lateinit var textAdapter: TextAdapter
    var dataList: List<ReflowBean>? = null
    var currentPage: Int = 0

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(DialogFragment.STYLE_NO_TITLE, themeId)
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTtsTextBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        textAdapter = TextAdapter(requireContext())
        textAdapter.keys.clear()
        if (dataList != null && dataList!!.isNotEmpty()) {
            textAdapter.keys.addAll(dataList!!)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = textAdapter
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(ColorItemDecoration(requireContext()))

        // Auto scroll to current page
        if (currentPage > 0 && textAdapter.itemCount > 0) {
            val index = textAdapter.keys.indexOfFirst {
                Utils.parseInt(
                    it.page?.split("-")?.firstOrNull() ?: ""
                ) == currentPage
            }
            if (index >= 0) {
                binding.recyclerView.scrollToPosition(index)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class TextAdapter(
        var context: Context,
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val keys: MutableList<ReflowBean> = mutableListOf()

        override fun getItemCount(): Int {
            return keys.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tts, parent, false)
            return TextHolder(view)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as TextHolder
            pdfHolder.onBind(position)
        }

        inner class TextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val textView: TextView = itemView.findViewById(R.id.text)

            fun onBind(position: Int) {
                textView.setOnClickListener {
                    mDataListener?.onSuccess(position, keys)
                    dismiss()
                }
                textView.text = "Page ${keys[position].page}: ${keys[position].data}"
            }
        }
    }

    companion object {

        const val TAG = "TtsTextFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?,
            dataList: List<ReflowBean>? = null,
            currentPage: Int = 0
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("TtsTextFragment")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = TtsTextFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.dataList = dataList
            pdfFragment.currentPage = currentPage
            pdfFragment.show(ft!!, "TtsTextFragment")
        }
    }
}
