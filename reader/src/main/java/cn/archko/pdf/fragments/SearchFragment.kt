package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.databinding.DialogSearchDocBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.json.JSONException
import org.vudroid.core.codec.CodecDocument
import org.vudroid.core.codec.SearchResult

/**
 * @author: archko 2024/12/11 :17:55
 */
open class SearchFragment : DialogFragment(R.layout.dialog_search_doc) {

    private lateinit var binding: DialogSearchDocBinding
    private var mDataListener: DataListener? = null
    private var textAdapter: TextAdapter? = null
    private var document: CodecDocument? = null


    fun setDocument(document: CodecDocument?) {
        this.document = document
    }

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)
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
        binding = DialogSearchDocBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { dismiss() }

        if (textAdapter == null) {
            textAdapter = TextAdapter(requireContext())
        } else {
            textAdapter!!.notifyDataSetChanged()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = textAdapter

        binding.closeButton.setOnClickListener {
            textAdapter?.searchResults?.clear()
            textAdapter?.notifyDataSetChanged()
        }
        binding.searchButton.setOnClickListener {
            search(binding.keyword.text.toString())
        }
    }

    private fun search(text: String) {
        if (TextUtils.isEmpty(text) || document == null) {
            textAdapter?.searchResults?.clear()
            textAdapter?.notifyDataSetChanged()
            return
        }
        lifecycleScope.launch {
            flow {
                try {
                    val result = document?.search(text, 0)

                    emit(ResponseHandler.Success(result))
                } catch (e: JSONException) {
                    emit(ResponseHandler.Failure())
                    e.printStackTrace()
                } catch (e: Exception) {
                    emit(ResponseHandler.Failure())
                    Logcat.e(Logcat.TAG, e.message)
                }
            }.flowOn(Dispatchers.IO)
                .collectLatest { res ->
                    if (res is ResponseHandler.Success) {
                        textAdapter?.searchResults?.clear()
                        if (res.data != null) {
                            textAdapter?.searchResults?.addAll(res.data!!)
                            textAdapter?.notifyDataSetChanged()
                        }
                    }
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
        var searchResults: MutableList<SearchResult> = ArrayList()

        override fun getItemCount(): Int {
            return searchResults.size
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
                    maxLines = 4
                    val padding = Utils.dipToPixel(8f)
                    setPadding(padding, padding, padding, padding)
                }

            return TextHolder(view)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as TextHolder
            pdfHolder.onBind(position)
        }

        inner class TextHolder(internal var view: TextView) : RecyclerView.ViewHolder(view) {

            fun onBind(position: Int) {
                val searchResult = searchResults[position]
                view.setOnClickListener {
                    mDataListener?.onSuccess(searchResult, searchResults)
                    dismiss()
                }
                view.text = String.format("%s-%s", searchResult.page, searchResult.text)
            }
        }
    }

    fun showDialog(activity: FragmentActivity?) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("SearchFragment")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        show(ft!!, "SearchFragment")
    }
}