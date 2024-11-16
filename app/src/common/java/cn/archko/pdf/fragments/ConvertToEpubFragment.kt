package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentConvertToEpubBinding
import cn.archko.pdf.adapters.AdapterUtils
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import com.archko.reader.mobi.LibMobi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2024/11/16 :14:34
 */
class ConvertToEpubFragment : DialogFragment(R.layout.fragment_convert_to_epub) {

    private lateinit var binding: FragmentConvertToEpubBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerListAdapter<String>
    private var bookPaths = mutableListOf<String>()

    private val beanItemCallback: DiffUtil.ItemCallback<String> =
        object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(DialogFragment.STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Waiting...")
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
    ): View {
        binding = FragmentConvertToEpubBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.btnAddMobi.setOnClickListener { addMobi() }
        //binding.btnAddAzw.setOnClickListener { addAzw() }

        binding.btnConvert.setOnClickListener { convertToEpub() }

        adapter = object : BaseRecyclerListAdapter<String>(activity, beanItemCallback) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val root = inflater.inflate(R.layout.item_convert_epub, parent, false)
                return ViewHolder(root)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter
    }

    private val pickMobi =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            //如果Uri为null，可能是多选了
            addResult(result)
        }

    private fun addResult(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            val paths = mutableListOf<String>()
            try {
                val oneUri = result.data?.data
                if (oneUri != null) {
                    val parseParams = IntentFile.getPath(requireContext(), oneUri)
                    if (parseParams != null) {
                        val ext = AdapterUtils.getExtensionWithDot(parseParams)
                        if (IntentFile.isMobi(ext) || IntentFile.isDocx(ext)) {
                            paths.add(parseParams)
                        }
                    }
                } else {
                    for (index in 0 until (result.data?.clipData?.itemCount ?: 0)) {
                        val uri = result.data?.clipData?.getItemAt(index)?.uri
                        if (uri != null) {
                            val parseParams = IntentFile.getPath(requireContext(), uri)
                            if (parseParams != null) {
                                val ext = AdapterUtils.getExtensionWithDot(parseParams)
                                if (IntentFile.isMobi(ext) || IntentFile.isDocx(ext)) {
                                    paths.add(parseParams)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handlePickFileResult", e)
            }
            bookPaths.addAll(paths)
            adapter.submitList(bookPaths)
        }
    }

    private val pickAzw =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            addResult(result)
        }

    private fun addMobi() {
        val mimeTypes = arrayOf(
            "application/x-mobipocket-ebook",
            "application/vnd.amazon.ebook",
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*")
        pickMobi.launch(intent)
    }

    private fun addAzw() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("application/vnd.amazon.ebook")
        pickAzw.launch(intent)
    }

    private fun convertToEpub() {
        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                LibMobi.convertToEpubBatch(bookPaths)
            }
            Toast.makeText(activity, R.string.create_epub_convert_success, Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
        }
    }

    inner class ViewHolder(root: View) : BaseViewHolder<String>(root) {

        private var delete: View = root.findViewById(R.id.delete)
        private var tvName: TextView = root.findViewById(R.id.tvName)

        override fun onBind(data: String, position: Int) {
            tvName.text = data
            delete.setOnClickListener { deleteItem(data, position) }
        }
    }

    private fun deleteItem(data: String, position: Int) {
        bookPaths.remove(data)
        adapter.submitList(bookPaths)
        adapter.notifyDataSetChanged()
    }

    companion object {

        const val TAG = "ConvertToEpubFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("converttoepub_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = ConvertToEpubFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "converttoepub_dialog")
        }
    }
}
