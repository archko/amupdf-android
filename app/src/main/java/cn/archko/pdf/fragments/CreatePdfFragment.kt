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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import coil.load
import com.radaee.util.FileBrowserAdt.SnatchItem
import com.radaee.util.FileBrowserView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2023/3/8 :14:34
 */
class CreatePdfFragment : DialogFragment() {

    protected lateinit var progressDialog: ProgressDialog

    var bookProgress: BookProgress? = null
    var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerAdapter<String>
    var oldPdfPath: String? = null

    private var oldPdfPathView: TextView? = null
    private var back: View? = null
    private var btnSelect: View? = null
    private var btnSave: View? = null
    private var btnAdd: View? = null
    private var recyclerView: RecyclerView? = null

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
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    private fun selectPdf() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(layoutInflater.inflate(com.radaee.viewlib.R.layout.dialog_pick_file, null))
        val dlg = builder.create()
        dlg.setOnShowListener { dialog: DialogInterface? ->
            val fb_view = dlg.findViewById<FileBrowserView>(com.radaee.viewlib.R.id.fb_view)
            val txt_filter =
                dlg.findViewById<TextView>(com.radaee.viewlib.R.id.extension_filter)
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
                        oldPdfPathView?.text = "Old pdf:$fullPath"
                    }
                }
        }
        dlg.show()
    }

    private fun createPdf() {
        val arr = arrayListOf<String>()
        arr.addAll(adapter.data)
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

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_pdf, container, false)
        oldPdfPathView = view.findViewById(R.id.oldPdfPath)
        back = view.findViewById(R.id.back)
        btnSelect = view.findViewById(R.id.btnSelect)
        btnSave = view.findViewById(R.id.btnSave)
        btnAdd = view.findViewById(R.id.btnAdd)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.menu_create)
        back?.setOnClickListener { this@CreatePdfFragment.dismiss() }
        btnSelect?.setOnClickListener { this@CreatePdfFragment.selectPdf() }
        btnSave?.setOnClickListener { this@CreatePdfFragment.createPdf() }
        btnAdd?.setOnClickListener { this@CreatePdfFragment.addItem() }

        adapter = object : BaseRecyclerAdapter<String>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val root = inflater.inflate(R.layout.item_image, parent, false)
                return ViewHolder(root)
            }
        }
        recyclerView?.layoutManager = GridLayoutManager(activity, 2)
        recyclerView?.adapter = adapter
    }

    inner class ViewHolder(root: View) : BaseViewHolder<String>(root) {

        var delete: View? = null
        var ivImage: ImageView? = null

        init {
            delete = root.findViewById(R.id.delete)
            ivImage = root.findViewById(R.id.ivImage)
        }

        override fun onBind(data: String, position: Int) {
            delete?.setOnClickListener { deleteItem(data, position) }
            ivImage?.load(File(data))
        }
    }

    private fun deleteItem(data: String, position: Int) {
        adapter.data.remove(data)
        adapter.notifyDataSetChanged()
    }

    private fun addItem() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(layoutInflater.inflate(com.radaee.viewlib.R.layout.dialog_pick_file, null))
        val dlg = builder.create()
        dlg.setOnShowListener { dialog: DialogInterface? ->
            val fb_view = dlg.findViewById<FileBrowserView>(com.radaee.viewlib.R.id.fb_view)
            val txt_filter =
                dlg.findViewById<TextView>(com.radaee.viewlib.R.id.extension_filter)
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
