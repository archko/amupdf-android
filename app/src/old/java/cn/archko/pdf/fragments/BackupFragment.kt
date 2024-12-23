package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
//import com.umeng.analytics.MobclickAgent
import java.io.File

/**
 * 备份列表
 * @author: archko 2020/1/27 :15:58
 */
open class BackupFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BaseRecyclerAdapter<File>
    var mDataListener: DataListener? = null
    private lateinit var backupViewModel: BackupViewModel

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = android.R.style.Theme_Material_Dialog
        setStyle(STYLE_NO_FRAME, themeId)
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
        toolbar.setNavigationOnClickListener({ dismiss() })

        toolbar?.setTitle(R.string.dialog_title_backup)
        toolbar?.setSubtitle(R.string.dialog_sub_title_backup)

        recyclerView = view.findViewById(R.id.files)
        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        return view
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        adapter = object : BaseRecyclerAdapter<File>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<File> {
                val view = mInflater.inflate(cn.archko.pdf.R.layout.item_outline, parent, false)
                return ItemHolder(view)
            }
        }
        recyclerView.adapter = adapter

        loadBackups()
    }

    private fun loadBackups() {
        lifecycleScope.launch {
            backupViewModel.backupFiles()
                .flowOn(Dispatchers.IO)
                .collectLatest {
                    when (it) {
                        is ResponseHandler.Success -> {
                            if (it.data.isNotEmpty()) {
                                adapter.data = it.data
                                adapter.notifyDataSetChanged()
                            }
                        }

                        else -> {}
                    }
                }
        }
    }

    inner class ItemHolder(itemView: View?) : BaseViewHolder<File>(itemView) {

        private var title: TextView = itemView!!.findViewById(cn.archko.pdf.R.id.title)

        init {
            itemView!!.minimumHeight = Utils.dipToPixel(48f)
        }

        override fun onBind(data: File?, position: Int) {
            title.setText(data?.name)

            itemView.setOnClickListener {
                this@BackupFragment.dismiss()
                mDataListener?.onSuccess(data)
            }
        }
    }

    companion object {

        const val TAG = "BackupFragment"

        fun showBackupDialog(activity: FragmentActivity?, dataListener: DataListener?) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("backup_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            // Create and show the dialog.
            val fragment = BackupFragment()

            fragment.setListener(dataListener)
            fragment.show(ft!!, "backup_dialog")
        }
    }
}
