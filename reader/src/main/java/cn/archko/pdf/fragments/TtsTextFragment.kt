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
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.tts.TTSEngine

/**
 * @author: archko 2023/7/28 :14:34
 */
class TtsTextFragment : DialogFragment(R.layout.dialog_tts_text) {

    private lateinit var binding: FragmentDialogTtsTextBinding

    private var mDataListener: DataListener? = null
    private lateinit var textAdapter: TextAdapter

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
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
        binding = FragmentDialogTtsTextBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        textAdapter = TextAdapter()
        textAdapter.keys.clear()
        textAdapter.keys.addAll(TTSEngine.get().getTtsContent())

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = pdfAdapter
        binding.recyclerView.setHasFixedSize(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class TextAdapter(
        var context: Context,
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val keys: List<ReflowBean> = mutableListOf<ReflowBean>()

        override fun getItemCount(): Int {
            return keys.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val view = TextView(context)
                .apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                    singleline = true
                }

            return TextHolder(view)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as TextHolder
            pdfHolder.onBind(position)
        }

        inner class TextHolder(internal var view: TextView) : RecyclerView.ViewHolder(view) {

            fun onBind(position: Int) {
                view.setOnClickListener {
                    mDataListener?.onSuccess(keys[position])
                    dismiss()
                }
                view.setText(String.format("%s-%s", keys[position].page, keys[position].data))
            }
        }
    }

    companion object {

        const val TAG = "TtsTextFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("TtsTextFragment")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = TtsTextFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "TtsTextFragment")
        }
    }
}
