package cn.archko.pdf.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Event.Companion.ACTION_FAVORITED
import cn.archko.pdf.common.Event.Companion.ACTION_STOPPED
import cn.archko.pdf.common.Event.Companion.ACTION_UNFAVORITED
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.LengthUtils
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView
import com.jeremyliao.liveeventbus.LiveEventBus
import java.io.File
import java.util.*

/**
 * @description:history list
 * *
 * @author: archko 11-11-17
 */
class HistoryFragment : BrowserFragment() {

    private var curPage = 0
    private lateinit var mListMoreView: ListMoreView
    private var mStyle: Int = STYLE_GRID

    private lateinit var progressDialog: ProgressDialog

    protected lateinit var historyViewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction(ACTION_STOPPED)
        filter.addAction(ACTION_FAVORITED)
        filter.addAction(ACTION_UNFAVORITED)
        LiveEventBus
            .get(Event.ACTION_STOPPED, FileBean::class.java)
            .observe(this, object : Observer<FileBean> {
                override fun onChanged(t: FileBean?) {
                    loadData()
                }
            })
        LiveEventBus
            .get(Event.ACTION_FAVORITED, FileBean::class.java)
            .observe(this, object : Observer<FileBean> {
                override fun onChanged(t: FileBean?) {
                    updateItem(t)
                }
            })
        LiveEventBus
            .get(Event.ACTION_UNFAVORITED, FileBean::class.java)
            .observe(this, object : Observer<FileBean> {
                override fun onChanged(t: FileBean?) {
                    updateItem(t)
                }
            })
        historyViewModel = HistoryViewModel()

        progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Waiting...")
        progressDialog.setMessage("Waiting...")
    }

    override fun updateItem() {
        currentBean = null
    }

    private fun updateItem(fileBean: FileBean?) {
        if (fileBean?.bookProgress != null) {
            for (fb in fileListAdapter.data) {
                if (null != fb.bookProgress && fb.bookProgress!!._id == fileBean.bookProgress!!._id) {
                    fb.bookProgress!!.isFavorited = fileBean.bookProgress!!.isFavorited
                    break
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val options = PreferenceManager.getDefaultSharedPreferences(activity)
        mStyle = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_LIST_STYLE, "0")!!)
        applyStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
        ImageLoader.getInstance().recycle()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_backup -> backup()
            R.id.action_restore -> restore()
            R.id.action_style -> {
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
            }
        }

        return super.onOptionsItemSelected(menuItem)
    }

    private fun backup() {
        progressDialog.show()
        historyViewModel.backupFromDb()
    }

    private fun restore() {
        BackupFragment.showBackupDialog(activity, object : DataListener {
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
        fileListAdapter.addFootView(mListMoreView.getLoadMoreView())

        addObserver()
        return view
    }

    private fun applyStyle() {
        if (mStyle == STYLE_LIST) {
            fileListAdapter.setMode(BookAdapter.TYPE_RENCENT)
            filesListView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            fileListAdapter.notifyDataSetChanged()
        } else {
            fileListAdapter.setMode(BookAdapter.TYPE_GRID)

            filesListView.layoutManager = GridLayoutManager(activity, 3)
            fileListAdapter.notifyDataSetChanged()
        }
    }

    private fun reset() {
        curPage = 0
        currentBean = null
    }

    override fun loadData() {
        reset()
        getHistory()
    }

    private fun addObserver() {
        historyViewModel.uiFileModel.observe(viewLifecycleOwner, { it ->
            updateHistoryBeans(it)
        })

        historyViewModel.uiBackupModel.observe(viewLifecycleOwner, { filepath ->
            kotlin.run {
                progressDialog.dismiss()
                if (!LengthUtils.isEmpty(filepath)) {
                    Logcat.d("", "file:$filepath")
                    Toast.makeText(App.instance, "备份成功:$filepath", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(App.instance, "备份失败", Toast.LENGTH_LONG).show()
                }
            }
        })

        historyViewModel.uiRestorepModel.observe(viewLifecycleOwner, { flag ->
            kotlin.run {
                progressDialog.dismiss()
                if (flag) {
                    Toast.makeText(App.instance, "恢复成功:$flag", Toast.LENGTH_LONG).show()
                    loadData()
                } else {
                    Toast.makeText(App.instance, "恢复失败", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun getHistory() {
        mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)

        historyViewModel.loadFiles(curPage, showExtension)
    }

    private fun updateHistoryBeans(args: Array<Any?>) {
        val totalCount = args[0] as Int
        val entryList = args[1] as ArrayList<FileBean>
        mSwipeRefreshWidget.isRefreshing = false
        fileListAdapter.apply {
            if (entryList.size > 0) {
                if (curPage == 0) {
                    data = entryList
                    //submitList(fileListAdapter!!.data, entryList, fileListAdapter!!, totalCount)
                    notifyDataSetChanged()
                } else {
                    val index = itemCount
                    addData(entryList)
                    notifyItemRangeInserted(index, entryList.size)
                }

                curPage++
            }
        }
        updateLoadingStatus(totalCount)
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(
            String.format(
                "total count:%s, adapter count:%s",
                totalCount,
                fileListAdapter.normalCount
            )
        )
        if (fileListAdapter.normalCount > 0) {
            if (fileListAdapter.normalCount < totalCount) {
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("fileListAdapter!!.normalCount < totalCount")
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("fileListAdapter!!.normalCount <= 0")
            mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            val sp = context!!.getSharedPreferences(PREF_BROWSER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_BROWSER_KEY_FIRST, true)
            if (isFirst) {
                LiveEventBus.get(Event.ACTION_ISFIRST)
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
                                lastVisibleItemPosition >= rowCount - fileListAdapter.headersCount - fileListAdapter.footersCount
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

        @JvmField
        val STYLE_LIST = 0

        @JvmField
        val STYLE_GRID = 1
    }


    /*fun submitList(oldList: List<FileBean>, newList: List<FileBean>, adapter: BookAdapter, totalCount: Int) {
        AppExecutors.instance.diskIO().execute(object : Runnable {
            override fun run() {
                var diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

                    override fun getOldListSize(): Int {
                        // 返回旧数据的长度
                        return if (oldList == null) {
                            0
                        } else {
                            oldList.size
                        }
                    }

                    override fun getNewListSize(): Int {
                        // 返回新数据的长度
                        if (newList == null) {
                            return 0
                        } else {
                            return oldList.size
                        }
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return TextUtils.equals(oldList.get(oldItemPosition).bookProgress.name, newList.get(oldItemPosition).bookProgress.name);
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return oldList.get(oldItemPosition).bookProgress.progress == newList.get(newItemPosition).bookProgress.progress;
                    }
                });
                AppExecutors.instance.mainThread().execute(object : Runnable {
                    override fun run() {
                        adapter.setData(newList);
                        diffResult.dispatchUpdatesTo(adapter)
                        curPage++
                        updateLoadingStatus(totalCount)
                    }
                })
            }
        })
    }*/
}
