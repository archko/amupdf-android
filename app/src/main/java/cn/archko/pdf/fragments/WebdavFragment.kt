package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.utils.SardineHelper
import com.google.android.material.appbar.MaterialToolbar
import com.tencent.mmkv.MMKV
import com.thegrizzlylabs.sardineandroid.DavResource

/**
 * webdav备份列表
 * @author: archko 2024/8/7 :15:58
 */
open class WebdavFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BaseRecyclerAdapter<DavResource>
    var mDataListener: DataListener? = null
    private lateinit var backupViewModel: BackupViewModel

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(DialogFragment.STYLE_NO_FRAME, themeId)
        backupViewModel = BackupViewModel()
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(cn.archko.pdf.R.layout.item_font, container, false)
        view.findViewById<View>(R.id.layout_search).visibility = View.GONE
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { dismiss() }

        toolbar?.setTitle(R.string.dialog_title_backup)
        toolbar?.setSubtitle(R.string.dialog_sub_title_backup)

        recyclerView = view.findViewById(R.id.files)
        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        backupViewModel.uiDavResourceModel.observe(viewLifecycleOwner) { resources ->
            kotlin.run {
                if (null != resources && resources.isNotEmpty()) {
                    adapter.data = resources
                    adapter.notifyDataSetChanged()
                }
            }
        }

        return view
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        adapter = object : BaseRecyclerAdapter<DavResource>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<DavResource> {
                val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
                return ItemHolder(view)
            }
        }
        recyclerView.adapter = adapter

        loadBackups("amupdf")
    }

    private fun loadBackups(filePath: String) {
        val mmkv = MMKV.mmkvWithID(SardineHelper.KEY_CONFIG)
        val name = mmkv.decodeString(SardineHelper.KEY_NAME)
        val pass = mmkv.decodeString(SardineHelper.KEY_PASS)
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pass)) {
            Toast.makeText(requireActivity(), "Please config webdav first", Toast.LENGTH_SHORT)
                .show()
            return
        }
        backupViewModel.webdavBackupFiles(name!!, pass!!, filePath)
    }

    inner class ItemHolder(itemView: View?) : BaseViewHolder<DavResource>(itemView) {

        private var title: TextView = itemView!!.findViewById(R.id.name)
        private var mIcon: ImageView? = itemView!!.findViewById(R.id.icon)

        init {
            val view: View = itemView!!.findViewById(R.id.progressbar)
            view.visibility = View.GONE
        }

        override fun onBind(data: DavResource, position: Int) {
            title.text = data.displayName
            if (data.isDirectory) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
            } else {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_text)
            }

            itemView.setOnClickListener {
                if (position == 0) {
                    Logcat.d("点击的是当前目录")
                    return@setOnClickListener
                }
                if (data.isDirectory) {
                    loadBackups(data.name)
                    return@setOnClickListener
                }
                this@WebdavFragment.dismiss()
                mDataListener?.onSuccess(data.path)
            }
        }
    }

    companion object {

        const val TAG = "WebdavFragment"

        fun showWebdavDialog(activity: FragmentActivity?, dataListener: DataListener?) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("webdav_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val fragment = WebdavFragment()

            fragment.setListener(dataListener)
            fragment.show(ft!!, "webdav_dialog")
        }
    }
}
