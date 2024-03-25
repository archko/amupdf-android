package cn.archko.pdf.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.core.App
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.Event.Companion.ACTION_FAVORITED
import cn.archko.pdf.core.common.Event.Companion.ACTION_STOPPED
import cn.archko.pdf.core.common.Event.Companion.ACTION_UNFAVORITED
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.LengthUtils
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import com.jeremyliao.liveeventbus.LiveEventBus
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction(ACTION_STOPPED)
        filter.addAction(ACTION_FAVORITED)
        filter.addAction(ACTION_UNFAVORITED)
        LiveEventBus
            .get(ACTION_STOPPED, String::class.java)
            .observe(this) { onRefresh() }
        LiveEventBus
            .get(ACTION_FAVORITED, FileBean::class.java)
            .observe(this) { t -> updateItem(t) }
        LiveEventBus
            .get(ACTION_UNFAVORITED, FileBean::class.java)
            .observe(this) { t -> updateItem(t) }
        historyViewModel = HistoryViewModel()

        progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
    }

    override fun initAdapter(): BookAdapter {
        return  BookAdapter(activity as Context,beanItemCallback, BookAdapter.TYPE_RENCENT, itemClickListener)
    }

    override fun updateItem() {
        currentBean = null
    }

    private fun updateItem(fileBean: FileBean?) {
        if (fileBean?.bookProgress != null) {
            for (fb in fileListAdapter.currentList) {
                if (null != fb.bookProgress && fb.bookProgress!!._id == fileBean.bookProgress!!._id) {
                    fb.bookProgress!!.isFavorited = fileBean.bookProgress!!.isFavorited
                    break
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //val options = PreferenceManager.getDefaultSharedPreferences(activity)
        //mStyle = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_LIST_STYLE, "0")!!)
        //applyStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_backup -> backup()
            R.id.action_restore -> restore()
            R.id.action_extract -> extractImage()
            R.id.action_create -> createPdf()
            /*R.id.action_style -> {
                if (mStyle == STYLE_LIST) {
                    mStyle = STYLE_GRID
                } else {
                    mStyle = STYLE_LIST
                }
                PreferenceManager.getDefaultSharedPreferences(activity)
                    .edit()
                    .putString(PdfOptionsActivity.PREF_LIST_STYLE, mStyle.toString())
                    .apply()
                applyStyle()
            }*/
        }

        return super.onOptionsItemSelected(menuItem)
    }

    private fun backup() {
        progressDialog.show()
        historyViewModel.backupFromDb()
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
        filesListView.setOnScrollListener(onScrollListener)
        mListMoreView = ListMoreView(filesListView)
        //fileListAdapter.addFootView(mListMoreView.getLoadMoreView())

        addObserver()
        return view
    }

    private fun applyStyle() {
        /*if (mStyle == STYLE_LIST) {
            fileListAdapter.setMode(BookAdapter.TYPE_RENCENT)
            filesListView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            fileListAdapter.notifyDataSetChanged()
        } else {
            fileListAdapter.setMode(BookAdapter.TYPE_GRID)

            filesListView.layoutManager = GridLayoutManager(activity, 3)
            fileListAdapter.notifyDataSetChanged()
        }*/
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
            kotlin.run {
                progressDialog.dismiss()
                if (!LengthUtils.isEmpty(filepath)) {
                    Logcat.d("", "file:$filepath")
                    Toast.makeText(App.instance, "备份成功:$filepath", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(App.instance, "备份失败", Toast.LENGTH_LONG).show()
                }
            }
        }

        historyViewModel.uiRestorepModel.observe(viewLifecycleOwner) { flag ->
            kotlin.run {
                progressDialog.dismiss()
                if (flag) {
                    Toast.makeText(App.instance, "恢复成功:$flag", Toast.LENGTH_LONG).show()
                    getHistory()
                } else {
                    Toast.makeText(App.instance, "恢复失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateHistoryBeans(args: Array<Any>) {
        val totalCount = args[0] as Int
        val entryList = args[1] as ArrayList<FileBean>
        mSwipeRefreshWidget.isRefreshing = false
        fileListAdapter.submitList(entryList)
        fileListAdapter.notifyDataSetChanged()
        updateLoadingStatus(totalCount)
    }

    override fun remove(entry: FileBean) {
        if (entry.type == FileBean.RECENT) {
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "remove")
            historyViewModel.removeRecent(entry.file!!.absolutePath)
        }
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(
            String.format(
                "total count:%s, adapter count:%s",
                totalCount,
                fileListAdapter.currentList.size
            )
        )
        if (fileListAdapter.currentList.size > 0) {
            if (fileListAdapter.currentList.size < totalCount) {
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("fileListAdapter!!.normalCount < totalCount")
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("fileListAdapter!!.normalCount <= 0")
            mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            val sp = requireContext().getSharedPreferences(PREF_BROWSER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_BROWSER_KEY_FIRST, true)
            if (isFirst) {
                LiveEventBus.get<Boolean>(Event.ACTION_ISFIRST)
                    .post(true)
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
                        if (mStyle == STYLE_GRID) {
                            val gridLayoutManager = filesListView.layoutManager as GridLayoutManager
                            val rowCount =
                                fileListAdapter.getItemCount() / gridLayoutManager.spanCount
                            val lastVisibleRowPosition =
                                gridLayoutManager.findLastVisibleItemPosition() / gridLayoutManager.spanCount
                            isReachBottom = lastVisibleRowPosition >= rowCount - 1
                        } else if (mStyle == STYLE_LIST) {
                            val layoutManager: LinearLayoutManager =
                                filesListView.layoutManager as LinearLayoutManager
                            val lastVisibleItemPosition =
                                layoutManager.findLastVisibleItemPosition()
                            val rowCount = fileListAdapter.getItemCount()
                            isReachBottom =
                                lastVisibleItemPosition >= rowCount -1//- fileListAdapter.headersCount - fileListAdapter.footersCount
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
