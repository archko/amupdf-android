package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.activities.HomeActivity
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.widgets.ColorItemDecoration
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileFilter

/**
 * @author: archko 2016/2/14 :15:58
 */
open class SearchFragment : DialogFragment() {

    private lateinit var editView: EditText
    private lateinit var filesListView: RecyclerView
    private lateinit var imgClose: ImageView
    private val fileFilter: FileFilter? = null
    protected var fileListAdapter: BookAdapter? = null

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG);
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.list_book_search, container, false)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener({ dismiss() })

        dialog?.setTitle(cn.archko.pdf.R.string.menu_search)
        editView = view.findViewById(R.id.searchEdit)
        imgClose = view.findViewById(R.id.img_close)
        filesListView = view.findViewById(R.id.files)
        filesListView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        filesListView.addItemDecoration(ColorItemDecoration(requireContext()))

        imgClose.setOnClickListener { clear() }

        editView.addTextChangedListener(ATextWatcher())

        return view
    }

    private inner class ATextWatcher : TextWatcher {

        lateinit var string: String

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            string = s.toString()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable) {
            val cs = s.toString()
            if (!TextUtils.isEmpty(cs)) {
                if (cs != string) {
                    search(cs.toLowerCase())
                }
            } else {
                clearList()
            }
        }
    }

    private fun clearList() {
        fileListAdapter?.submitList(listOf())
        fileListAdapter?.notifyDataSetChanged()
    }

    private fun clear() {
        editView.text = null
        clearList()
    }

    private val beanItemCallback: DiffUtil.ItemCallback<FileBean> =
        object : DiffUtil.ItemCallback<FileBean>() {
            override fun areItemsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                return if (null == oldItem.bookProgress) {
                    false
                } else {
                    oldItem.bookProgress!!.equals(newItem.bookProgress)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (null == fileListAdapter) {
            fileListAdapter = BookAdapter(
                activity as Context,
                beanItemCallback,
                BookAdapter.TYPE_SEARCH,
                itemClickListener
            )
            this.fileListAdapter!!.setMode(BookAdapter.TYPE_SEARCH)
        }
        filesListView.adapter = this.fileListAdapter
    }

    private fun search(keyword: String) {
        if (!isResumed) {
            return
        }
        val fileList: ArrayList<FileBean> = ArrayList()

        val home = getHome()
        doSearch(fileList, keyword, File(home))

        fileListAdapter?.submitList(fileList)
        fileListAdapter?.notifyDataSetChanged()
    }

    private fun doSearch(fileList: ArrayList<FileBean>, keyword: String, dir: File) {
        if (dir.isDirectory) {
            val files = dir.listFiles(this.fileFilter)

            if (files != null && files.isNotEmpty()) {
                for (f in files) {
                    if (f.isFile) {
                        if (f.name.toLowerCase().contains(keyword)) {
                            fileList.add(FileBean(FileBean.NORMAL, f, true))
                        }
                    } else {
                        doSearch(fileList, keyword, f)
                    }
                }
            }
        } else {
            if (dir.name.contains(keyword)) {
                fileList.add(FileBean(FileBean.NORMAL, dir, true))
            }
        }
    }

    private fun getHome(): String {
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        var path: String = activity?.getSharedPreferences(HomeActivity.PREF_TAG, 0)!!
            .getString(HomeActivity.PREF_HOME, defaultHome)!!
        if (path.length > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        if (pathFile.exists() && pathFile.isDirectory) {
            return path
        } else {
            return defaultHome
        }
    }

    private val itemClickListener: OnItemClickListener<FileBean> =
        object : OnItemClickListener<FileBean> {
            override fun onItemClick(view: View?, data: FileBean?, position: Int) {
                val clickedEntry =
                    this@SearchFragment.fileListAdapter!!.currentList[position] as FileBean
                val clickedFile = clickedEntry.file

                if (null == clickedFile || !clickedFile.exists()) {
                    return
                }

                PDFViewerHelper.openWithDefaultViewer(clickedFile, activity!!)
            }

            override fun onItemClick2(view: View?, data: FileBean?, position: Int) {
                val entry = this@SearchFragment.fileListAdapter!!.currentList[position] as FileBean
                showFileInfoDialog(entry)
            }
        }

    protected fun showFileInfoDialog(entry: FileBean) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("dialog")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        // Create and show the dialog.
        val fileInfoFragment = FileInfoFragment()
        val bundle = Bundle()
        bundle.putSerializable(FileInfoFragment.FILE_LIST_ENTRY, entry)
        fileInfoFragment.arguments = bundle

        fileInfoFragment.setListener(object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                val fileEntry = args[0] as FileBean
                PDFViewerHelper.openWithDefaultViewer(fileEntry.file!!, activity!!)
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
        fileInfoFragment.show(ft!!, "dialog")
    }

    companion object {

        val TAG = "SearchFragment"
    }
}
