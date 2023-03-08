package cn.archko.pdf.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentCreatePdfBinding
import cn.archko.mupdf.databinding.ItemImageBinding
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.base.BaseDialogFragment
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import coil.load
import com.radaee.util.FileBrowserAdt.SnatchItem
import com.radaee.util.FileBrowserView
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2023/3/8 :14:34
 */
class CreatePdfFragment :
    BaseDialogFragment<FragmentCreatePdfBinding>(R.layout.fragment_create_pdf) {

    protected lateinit var progressDialog: ProgressDialog

    var bookProgress: BookProgress? = null
    var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerAdapter<String>
    var oldPdfPath: String? = null

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Material
        setStyle(DialogFragment.STYLE_NORMAL, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Waiting...")
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG)
    }

    private fun selectPdf() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(layoutInflater.inflate(R.layout.dialog_pick_file, null))
        val dlg = builder.create()
        dlg.setOnShowListener { dialog: DialogInterface? ->
            val fb_view = dlg.findViewById<FileBrowserView>(R.id.fb_view)
            val txt_filter =
                dlg.findViewById<TextView>(R.id.extension_filter)
            txt_filter.text = "*.pdf"
            fb_view.FileInit(
                Environment.getExternalStorageDirectory().path,
                arrayOf(".pdf")
            )
            fb_view.onItemClickListener =
                AdapterView.OnItemClickListener { parent: AdapterView<*>?, _: View?, position: Int, _: Long ->
                    val item = fb_view.getItemAtPosition(position) as SnatchItem
                    if (item.m_item.is_dir) fb_view.FileGotoSubdir(item.m_item._name) else {
                        val fullPath = item.m_item._path
                        oldPdfPath = fullPath
                        binding.oldPdfPath.text = "Old pdf:$fullPath"
                    }
                }
        }
        dlg.show()
    }

    private fun createPdf() {
        val arr = arrayListOf<String>()
        arr.addAll(adapter.data)
        arr.removeLastOrNull()
        var path = oldPdfPath
        if (TextUtils.isEmpty(path)) {
            path = FileUtils.getStorageDir("book").absolutePath + File.separator + "new.pdf"
        }

        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.createPdf(path, arr)
            }
            if (result) {
                Toast.makeText(activity, R.string.create_pdf_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "create pdf error!", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.menu_create)
        binding.back.setOnClickListener { this@CreatePdfFragment.dismiss() }
        binding.btnSelect.setOnClickListener { this@CreatePdfFragment.selectPdf() }
        binding.btnSave.setOnClickListener { this@CreatePdfFragment.createPdf() }
        binding.btnAdd.setOnClickListener { this@CreatePdfFragment.addItem() }

        adapter = object : BaseRecyclerAdapter<String>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val binding =
                    ItemImageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                return ViewHolder(binding)
            }
        }
        binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        binding.recyclerView.adapter = adapter
    }

    inner class ViewHolder(private val binding: ItemImageBinding) :
        BaseViewHolder<String>(binding.root) {

        override fun onBind(data: String, position: Int) {
            binding.delete.setOnClickListener { deleteItem(data, position) }
            binding.ivImage.load(File(data))
        }
    }

    private fun deleteItem(data: String, position: Int) {
        adapter.data.remove(data)
        adapter.notifyDataSetChanged()
    }

    private fun addItem() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(layoutInflater.inflate(R.layout.dialog_pick_file, null))
        val dlg = builder.create()
        dlg.setOnShowListener { dialog: DialogInterface? ->
            val fb_view = dlg.findViewById<FileBrowserView>(R.id.fb_view)
            val txt_filter =
                dlg.findViewById<TextView>(R.id.extension_filter)
            txt_filter.text = "*.jpg,*.jpeg,*.png"
            fb_view.FileInit(
                Environment.getExternalStorageDirectory().path,
                arrayOf(".jpg", ".jpeg", ".png")
            )
            fb_view.onItemClickListener =
                AdapterView.OnItemClickListener { parent: AdapterView<*>?, _: View?, position: Int, _: Long ->
                    val item = fb_view.getItemAtPosition(position) as SnatchItem
                    if (item.m_item.is_dir) fb_view.FileGotoSubdir(item.m_item._name) else {
                        val fullPath = item.m_item._path
                        adapter.data.add(adapter.itemCount, fullPath)
                        adapter.notifyDataSetChanged()
                        //dismiss()
                    }
                }
        }
        dlg.show()
    }

    companion object {

        const val TAG = "CreatePdfFragment"
        const val ITEM_ADD = "ITEM_ADD"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val fileInfoFragment = CreatePdfFragment()
            fileInfoFragment.setListener(dataListener)
            fileInfoFragment.show(ft!!, "create_dialog")
        }
    }
}
