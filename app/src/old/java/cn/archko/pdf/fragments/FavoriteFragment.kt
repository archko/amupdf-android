package cn.archko.pdf.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.viewmodel.FavoriteViewModel
import cn.archko.pdf.widgets.IMoreView
import cn.archko.pdf.widgets.ListMoreView

/**
 * @description:favorite list
 * *
 * @author: archko 2019/9/19 :19:47
 */
class FavoriteFragment : BrowserFragment() {

    private lateinit var mListMoreView: ListMoreView
    protected lateinit var favoriteViewModel: FavoriteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        favoriteViewModel = FavoriteViewModel()
        vn.chungha.flowbus.collectFlowBus<GlobalEvent>(scope = this, isSticky = true) {
            if (TextUtils.equals(
                    Event.ACTION_FAVORITED,
                    it.name
                ) || TextUtils.equals(Event.ACTION_UNFAVORITED, it.name)
            ) {
                Logcat.d(TAG, "FAVORITED:${it.name}")
                onRefresh()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        this.pathTextView.visibility = View.GONE
        recyclerView.addOnScrollListener(onScrollListener)
        mListMoreView = ListMoreView(recyclerView)
        //fileListAdapter.addFootView(mListMoreView.getLoadMoreView())

        addObserver()
        return view
    }

    private fun reset() {
        favoriteViewModel.reset()
    }

    private fun addObserver() {
        favoriteViewModel.uiFileModel.observe(viewLifecycleOwner) { it ->
            updateHistoryBeans(it)
        }
    }

    override fun onRefresh() {
        reset()
        getFavorities()
    }

    private fun getFavorities() {
        mListMoreView.onLoadingStateChanged(IMoreView.STATE_LOADING)

        favoriteViewModel.loadFavorities(favoriteViewModel.curPage, showExtension)
    }

    private fun updateHistoryBeans(args: Array<Any?>) {
        val totalCount = args[0] as Int
        val entryList = args[1] as ArrayList<FileBean>
        mSwipeRefreshWidget.isRefreshing = false
        bookAdapter?.submitList(entryList)
        bookAdapter?.notifyDataSetChanged()
        updateLoadingStatus(totalCount)
    }

    private fun updateLoadingStatus(totalCount: Int) {
        Logcat.d(
            String.format(
                "$this, total count:%s, adapter count:%s",
                totalCount,
                bookAdapter?.currentList?.size
            )
        )
        if (null != bookAdapter && bookAdapter!!.currentList.size > 0) {
            if (bookAdapter!!.currentList.size < totalCount) {
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NORMAL)
            } else {
                Logcat.d("fileListAdapter!!.normalCount < totalCount")
                mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
            }
        } else {
            Logcat.d("fileListAdapter!!.normalCount <= 0")
            mListMoreView.onLoadingStateChanged(IMoreView.STATE_NO_MORE)
        }
    }

    override fun clickItem2(entry: FileBean, view: View) {
        if (!!entry.isDirectory && entry.type != FileBean.HOME) {
            selectedBean = entry
            prepareMenu(view, entry)
            return
        }
        selectedBean = null
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
        getFavorities()
    }

    companion object {

        const val TAG = "FavoriteFragment"
        internal const val PAGE_SIZE = 21
    }

}
