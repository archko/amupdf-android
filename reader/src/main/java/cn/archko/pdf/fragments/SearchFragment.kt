package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.widgets.ColorItemDecoration
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
    private var keyword: String? = null

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
        binding.recyclerView.addItemDecoration(ColorItemDecoration(activity))

        binding.closeButton.setOnClickListener {
            keyword = null
            textAdapter?.data?.clear()
            textAdapter?.notifyDataSetChanged()
        }
        binding.searchButton.setOnClickListener {
            search(binding.keyword.text.toString())
        }
        binding.keyword.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(
                v: TextView?,
                actionId: Int,
                event: KeyEvent?
            ): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    val keyword = binding.keyword.getText().toString()
                    search(keyword)
                    return true
                }
                return false
            }
        })
    }

    private fun search(text: String) {
        if (TextUtils.isEmpty(text) || document == null) {
            textAdapter?.data?.clear()
            textAdapter?.notifyDataSetChanged()
            return
        }
        keyword = text
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
                        if (res.data != null) {
                            textAdapter?.data = (res.data!!)
                            textAdapter?.notifyDataSetChanged()
                            if (res.data!!.isEmpty()) {
                                Toast.makeText(requireContext(), "No Results", Toast.LENGTH_SHORT)
                                    .show()
                            }
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
        BaseRecyclerAdapter<SearchResult>(context) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<SearchResult> {
            val view =
                mInflater.inflate(R.layout.item_search_doc, parent, false)
            return TextHolder(view)
        }

        inner class TextHolder(internal var view: View) : BaseViewHolder<SearchResult>(view) {

            private val page = view.findViewById<AppCompatTextView>(R.id.page)
            private val content = view.findViewById<AppCompatTextView>(R.id.content)

            override fun onBind(searchResult: SearchResult?, position: Int) {
                view.setOnClickListener {
                    mDataListener?.onSuccess(searchResult, data)
                    dismiss()
                }
                page.text = String.format("%s", searchResult?.page?.plus(1))

                // 高亮关键词
                val text = searchResult?.text
                val spannableString = SpannableString(text)
                val keyword = this@SearchFragment.keyword

                if (!keyword.isNullOrEmpty() && !text.isNullOrEmpty()) {
                    val startIndex = text.indexOf(keyword, 0, ignoreCase = true)
                    if (startIndex != -1) {
                        val endIndex = startIndex + keyword.length
                        val foregroundColorSpan = ForegroundColorSpan(
                            ContextCompat.getColor(
                                context,
                                R.color.red
                            )
                        ) // 定义你想要的高亮颜色
                        spannableString.setSpan(
                            foregroundColorSpan,
                            startIndex,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                content.text = spannableString
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