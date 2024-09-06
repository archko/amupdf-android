package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.DateUtils
import cn.archko.pdf.core.utils.FileUtils
import com.google.android.material.appbar.MaterialToolbar
import com.thegrizzlylabs.sardineandroid.DavResource
import kotlinx.coroutines.launch

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
        var themeId = cn.archko.pdf.R.style.AppTheme
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

    private fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            if (toUpLevel()) {
                return true
            }
            dismissAllowingStateLoss()
            return true
        } else {
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(cn.archko.pdf.R.layout.item_font, container, false)
        view.findViewById<View>(cn.archko.pdf.R.id.layout_search).visibility = View.GONE
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { dismiss() }

        toolbar?.setTitle(R.string.dialog_title_backup)
        toolbar?.setSubtitle(R.string.dialog_sub_title_backup)

        recyclerView = view.findViewById(cn.archko.pdf.R.id.files)
        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        backupViewModel.uiDavResourceModel.observe(viewLifecycleOwner) { resources ->
            if (!resources.isNullOrEmpty()) {
                adapter.data = resources
                adapter.notifyDataSetChanged()
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
                val view = mInflater.inflate(R.layout.item_webdav, parent, false)
                return ItemHolder(view)
            }
        }
        recyclerView.adapter = adapter

        dialog?.setOnKeyListener { dialog, keyCode, event ->
            this@WebdavFragment.onKey(
                dialog,
                keyCode,
                event
            )
        }
        /*requireActivity().onBackPressedDispatcher.addCallback(
            this,
            mBackPressedCallback
        )*/

        loadBackups(null)
    }

    private fun loadBackups(filePath: String?) {
        if (backupViewModel.checkAndLoadUser()) {
            backupViewModel.webdavUser?.let {
                val path = if (TextUtils.isEmpty(filePath)) it.path else filePath!!
                lifecycleScope.launch {
                    backupViewModel.webdavBackupFiles(path)
                }
            }
        } else {
            Toast.makeText(requireContext(), "Please config a webdav", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toUpLevel(): Boolean {
        if (adapter.itemCount < 1 || backupViewModel.webdavUser == null) {
            return false
        }
        val davResource = adapter.data[0]
        if (TextUtils.equals(davResource.path, backupViewModel.webdavUser!!.path)) {
            return false
        }
        val upLevel = FileUtils.getDir(davResource.path)
        loadBackups(upLevel)
        return true
    }

    inner class ItemHolder(itemView: View?) : BaseViewHolder<DavResource>(itemView) {

        private var title: TextView = itemView!!.findViewById(R.id.name)
        private var time: TextView = itemView!!.findViewById(R.id.time)
        private var mIcon: ImageView? = itemView!!.findViewById(R.id.icon)

        override fun onBind(data: DavResource, position: Int) {
            title.text = data.displayName
            time.text = DateUtils.formatTime(data.modified, DateUtils.TIME_FORMAT_11)
            if (data.isDirectory) {
                if (position == 0) {
                    mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_dir_home)
                } else {
                    mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
                }
            } else {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_text)
            }

            itemView.setOnClickListener {
                if (position == 0) {
                    Logcat.d("点击的是当前目录,现在向上一级")
                    toUpLevel()
                    return@setOnClickListener
                }
                if (data.isDirectory) {
                    loadBackups(data.href.rawPath)
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
