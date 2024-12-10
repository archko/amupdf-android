package cn.archko.pdf.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.adapters.BaseBookAdapter
import cn.archko.pdf.adapters.GridBookAdapter
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.App
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.Event.Companion.ACTION_FAVORITED
import cn.archko.pdf.core.common.Event.Companion.ACTION_STOPPED
import cn.archko.pdf.core.common.Event.Companion.ACTION_UNFAVORITED
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.LengthUtils
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import vn.chungha.flowbus.busEvent
import java.io.File

/**
 * @description:history list
 * *
 * @author: archko 11-11-17
 */
class HistoryFragment : BrowserFragment() {

    private lateinit var mListMoreView: ListMoreView
    private var mStyle: Int = STYLE_LIST

    private lateinit var progressDialog: ProgressDialog

    protected lateinit var historyViewModel: HistoryViewModel
    protected lateinit var backupViewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vn.chungha.flowbus.collectFlowBus<GlobalEvent>(scope = this, isSticky = true) {
            Logcat.d(TAG, "ACTION_STOPPED:${it.obj}")
            if (TextUtils.equals(ACTION_STOPPED, it.name)) {
                updateItem(it.obj as String)
            } else if (TextUtils.equals(ACTION_FAVORITED, it.name)) {
                updateItem(it.obj as FileBean)
            } else if (TextUtils.equals(ACTION_UNFAVORITED, it.name)) {
                updateItem(it.obj as FileBean)
            }
        }
        historyViewModel = HistoryViewModel()
        backupViewModel = BackupViewModel()

        progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
    }

    override fun initAdapter(): BaseBookAdapter {
        mStyle = PdfOptionRepository.getStyle()
        Logcat.d(TAG, "onCreate:$mStyle")

        if (mStyle == STYLE_LIST) {
            return BaseBookAdapter(
                activity as Context,
                beanItemCallback,
                itemClickListener
            )
        } else {
            return GridBookAdapter(
                activity as Context,
                beanItemCallback,
                itemClickListener
            )
        }
    }

    override fun setupUi() {
        if (mStyle == STYLE_LIST) {
            recyclerView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            recyclerView.addItemDecoration(ColorItemDecoration(requireContext()))
        } else {
            recyclerView.layoutManager = GridLayoutManager(activity, 3)
        }
    }

    override fun updateItem() {
        currentBean = null
    }

    private fun updateItem(path: String?) {
        var book: FileBean? = null
        if (!TextUtils.isEmpty(path) && null != bookAdapter) {
            for (fb in bookAdapter!!.currentList) {
                if (null != fb.file && TextUtils.equals(fb.file!!.absolutePath, path)) {
                    book = fb
                    break
                }
            }
        }
        if (book != null) {
            historyViewModel.updateItem(book!!)
        } else {
            onRefresh()
        }
    }

    private fun updateItem(fileBean: FileBean?) {
        if (fileBean?.bookProgress != null && null != bookAdapter) {
            for (fb in bookAdapter!!.currentList) {
                if (null != fb.bookProgress && fb.bookProgress!!._id == fileBean.bookProgress!!._id) {
                    fb.bookProgress!!.isFavorited = fileBean.bookProgress!!.isFavorited
                    break
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val newStyle = PdfOptionRepository.getStyle()
        if (newStyle != mStyle) {
            mStyle = newStyle
            applyStyle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionSelected(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_backup -> backup()
            R.id.action_config_webdav -> configWebdav()
            R.id.action_backup_webdav -> backupToWebdav()
            R.id.action_restore_webdav -> restoreFromWebdav()
            R.id.action_restore -> restore()
            R.id.action_extract -> extractImage()
            R.id.action_create -> createPdf()
            R.id.action_convert_epub -> convertToEpub()
            R.id.action_style -> {
                if (mStyle == STYLE_LIST) {
                    mStyle = STYLE_GRID
                } else {
                    mStyle = STYLE_LIST
                }
                PdfOptionRepository.setStyle(mStyle)
                applyStyle()
            }
        }
    }

    private fun backup() {
        progressDialog.show()
        historyViewModel.backupFromDb()
    }

    private fun configWebdav() {
        WebdavConfigFragment.showCreateDialog(requireActivity())
    }

    private fun backupToWebdav() {
        progressDialog.show()
        lifecycleScope.launch {
            backupViewModel.backupToWebdav().flowOn(Dispatchers.IO).collectLatest {
                progressDialog.dismiss()
                if (it) {
                    Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(App.instance, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun restoreFromWebdav() {
        WebdavFragment.showWebdavDialog(activity, object :
            DataListener {
            override fun onSuccess(vararg args: Any?) {
                val path = args[0] as String
                progressDialog.show()
                lifecycleScope.launch {
                    backupViewModel.restoreFromWebdav(path)
                }
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
    }

    private fun restore() {
        BackupFragment.showBackupDialog(activity, object :
            DataListener {
            override fun onSuccess(vararg args: Any?) {
                val file = args[0] as File
                progressDialog.show()
                historyViewModel.restoreToDb(file)
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        this.pathTextView.visibility = View.GONE
        recyclerView.setOnScrollListener(onScrollListener)
        mListMoreView = ListMoreView(recyclerView)
        //recyclerView.addFootView(mListMoreView.getLoadMoreView())

        addObserver()
        return view
    }

    private fun applyStyle() {
        removeItemDecorations()
        if (mStyle == STYLE_LIST) {
            recyclerView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            recyclerView.addItemDecoration(ColorItemDecoration(requireContext()))
            if (bookAdapter is GridBookAdapter) {
                bookAdapter = BaseBookAdapter(
                    activity as Context,
                    beanItemCallback,
                    itemClickListener
                )
                recyclerView.adapter = bookAdapter
                bookAdapter?.notifyItemInserted(0)
                recyclerView.smoothScrollToPosition(0)
            }
        } else {
            recyclerView.layoutManager = GridLayoutManager(activity, 3)
            if (bookAdapter is BaseBookAdapter) {
                bookAdapter = GridBookAdapter(
                    activity as Context,
                    beanItemCallback,
                    itemClickListener
                )
                recyclerView.adapter = bookAdapter
                bookAdapter?.notifyItemInserted(0)
                recyclerView.smoothScrollToPosition(0)
            }
        }
    }

    private fun removeItemDecorations() {
        for (i in 0 until recyclerView.itemDecorationCount) {
            recyclerView.removeItemDecorationAt(i)
        }
    }

    private fun reset() {
        historyViewModel.reset()
        currentBean = null
    }

    private fun getHistory() {
        mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)
        historyViewModel.loadFiles(historyViewModel.curPage, showExtension)
    }

    override fun onRefresh() {
        reset()
        getHistory()
    }

    private fun addObserver() {
        historyViewModel.uiFileModel.observe(viewLifecycleOwner) { it ->
            updateHistoryBeans(it)
        }

        historyViewModel.uiBackupModel.observe(viewLifecycleOwner) { filepath ->
            progressDialog.dismiss()
            if (!LengthUtils.isEmpty(filepath)) {
                Logcat.d("", "file:$filepath")
                Toast.makeText(App.instance, "备份成功:$filepath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(App.instance, "备份失败", Toast.LENGTH_LONG).show()
            }
        }

        historyViewModel.uiRestoreModel.observe(viewLifecycleOwner) { flag ->
            postRestore(flag)
        }
        backupViewModel.uiRestoreModel.observe(viewLifecycleOwner) { res ->
            when (res) {
                is ResponseHandler.Success<Boolean> -> {
                    postRestore(res.data)
                }

                is ResponseHandler.Failure -> {
                    postRestore(false)
                }

                else -> {
                }
            }
        }
    }

    private fun postRestore(flag: Boolean) {
        progressDialog.dismiss()
        if (flag) {
            Toast.makeText(App.instance, "恢复成功", Toast.LENGTH_LONG).show()
            onRefresh()
        } else {
            Toast.makeText(App.instance, "恢复失败", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateHistoryBeans(args: Array<Any>) {
        val totalCount = args[0] as Int
        val entryList = args[1] as ArrayList<FileBean>
        mSwipeRefreshWidget.isRefreshing = false
        bookAdapter?.submitList(entryList)
        //bookAdapter?.notifyDataSetChanged()
        updateLoadingStatus(totalCount)
    }

    override fun remove(entry: FileBean) {
        if (entry.type == FileBean.RECENT) {
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "remove")
            historyViewModel.removeRecent(entry.file!!.absolutePath)
        }
    }

    override fun removeAndClear(entry: FileBean) {
        historyViewModel.removeRecentAndClearCache(entry.file!!.absolutePath)
    }

    override fun clickItem2(entry: FileBean, view: View) {
        if (!entry.isDirectory && entry.type != FileBean.HOME) {
            selectedBean = entry
            prepareMenu(view, entry)
            return
        }
        selectedBean = null
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(
            String.format(
                "total count:%s, adapter count:%s",
                totalCount,
                bookAdapter?.currentList?.size
            )
        )
        if (null != bookAdapter && bookAdapter!!.currentList.size > 0) {
            if (bookAdapter!!.currentList.size < totalCount) {
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("bookAdapter!!.normalCount < totalCount")
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("bookAdapter!!.normalCount <= 0")
            mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            val sp = requireContext().getSharedPreferences(PREF_BROWSER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_BROWSER_KEY_FIRST, true)
            if (isFirst) {
                busEvent(GlobalEvent(Event.ACTION_ISFIRST, true))
                sp.edit()
                    .putBoolean(PREF_BROWSER_KEY_FIRST, false)
                    .apply()
            }
        }
    }

    private val onScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mListMoreView.state == IMoreView.STATE_NORMAL
                        || mListMoreView.state == IMoreView.STATE_LOAD_FAIL
                    ) {
                        var isReachBottom = false
                        if (null == bookAdapter) {
                            return
                        }
                        val layoutManager = recyclerView.layoutManager
                        if (layoutManager is GridLayoutManager) {
                            Logcat.d("adapter", "layout:$layoutManager")
                            val rowCount = bookAdapter!!.itemCount / layoutManager.spanCount
                            val lastVisibleRowPosition =
                                layoutManager.findLastVisibleItemPosition() / layoutManager.spanCount
                            isReachBottom = lastVisibleRowPosition >= rowCount - 1
                        } else if (layoutManager is LinearLayoutManager) {
                            val lastVisibleItemPosition =
                                layoutManager.findLastVisibleItemPosition()
                            val rowCount = bookAdapter!!.itemCount
                            isReachBottom =
                                lastVisibleItemPosition >= rowCount - 1//- bookAdapter.headersCount - bookAdapter.footersCount
                        }
                        if (isReachBottom) {
                            mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)
                            loadMore()
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            }
        }

    private fun loadMore() {
        Logcat.d("loadMore")
        getHistory()
    }

    companion object {

        const val TAG = "HistoryFragment"
        const val PREF_BROWSER = "pref_browser"
        const val PREF_BROWSER_KEY_FIRST = "pref_browser_key_first"

        const val STYLE_LIST = 0

        const val STYLE_GRID = 1
    }
}
