package cn.archko.pdf.core.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils

/**
 * @author: archko 2023/7/28 :14:34
 */
class TtsTextFragment : DialogFragment(R.layout.dialog_tts_text) {

    private lateinit var binding: FragmentDialogTtsTextBinding

    private var mDataListener: DataListener? = null
    private lateinit var textAdapter: TextAdapter

    private var job: Job? = null

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

        pdfAdapter = TextAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = pdfAdapter
        binding.recyclerView.setHasFixedSize(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private class TextAdapter(
        var context: Context,
    ) :
        ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

        private val keys: MutableMap<String, String> = HashMap<String, String>()

        override fun getItemCount(): Int {
            if (null == keys) {
                return 0
            }
            return keys!!.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ARecyclerView.ViewHolder {
            val view = TextView(context)
                .apply {
                    layoutParams = ARecyclerView.LayoutParams(
                        ARecyclerView.LayoutParams.MATCH_PARENT,
                        ARecyclerView.LayoutParams.WRAP_CONTENT
                    )
                }

            return TextHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as TextHolder
            pdfHolder.onBind(position)
        }

        inner class TextHolder(internal var view: TextView) : ARecyclerView.ViewHolder(view) {

            fun onBind(position: Int) {
                view.setOnClickListener{
                    mDataListener?.onSuccess(keys.get(position))
                    dismiss()
                }
                view.setText(keys.get(position))
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

            val pdfFragment = PdfReflowFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "TtsTextFragment")
        }
    }
}
