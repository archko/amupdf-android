package cn.archko.pdf.widgets

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import cn.archko.mupdf.R
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2018/12/21 :17:23
 */
class ListMoreView : IMoreView {
    private var mHostListView: ViewGroup
    private var mRootView: ViewGroup? = null
    var progressImage: ImageView? = null
        private set
    var loadingView: TextView? = null
        private set
    private var animation: Animation? = null
    var state = IMoreView.STATE_NORMAL
    private var layoutId = R.layout.item_list_more
    private var text = arrayOf("加载更多信息", "正在加载...", "加载失败，点击重试", "没有更多了")
    private var noneMoreDrawable: IntArray? = null
    private var failedDrawable: IntArray? = null


    constructor(listView: ViewGroup) {
        mHostListView = listView
    }

    constructor(mHostListView: ViewGroup, layoutId: Int) {
        this.mHostListView = mHostListView
        this.layoutId = layoutId
    }

    fun setText(text: Array<String>) {
        this.text = text
    }

    fun setTextNormal(textNormal: String) {
        text[0] = textNormal
    }

    fun setTextLoading(textLoading: String) {
        text[1] = textLoading
    }

    fun setTextLoadFailed(textLoadFailed: String) {
        text[2] = textLoadFailed
    }

    fun setTextNoneMore(textNoneMore: String) {
        text[3] = textNoneMore
    }

    fun setLoadingText(loadingText: String?) {
        if (!TextUtils.isEmpty(loadingText)) {
            loadingView!!.text = loadingText
        }
    }

    fun setNoneMoreDrawable(noneMoreDrawable: IntArray?) {
        this.noneMoreDrawable = noneMoreDrawable
    }

    fun setFailedDrawable(failedDrawable: IntArray?) {
        this.failedDrawable = failedDrawable
    }

    fun showMoreView() {
        if (mRootView == null) {
            return
        }
        //mRootView.setVisibility(View.VISIBLE);
        mRootView!!.minimumHeight = Utils.dipToPixel(mRootView!!.context, 80f)
        mRootView!!.layoutParams.height = Utils.dipToPixel(mRootView!!.context, 80f)
    }

    fun hideMoreView() {
        if (mRootView == null) {
            return
        }
        //mRootView.setVisibility(View.GONE);
        mRootView!!.minimumHeight = 0
        mRootView!!.layoutParams.height = 0
        mRootView!!.requestLayout()
    }

    override fun getLoadMoreView(): ViewGroup {
        if (mRootView == null) {
            mRootView = LayoutInflater.from(mHostListView.context)
                .inflate(layoutId, mHostListView, false) as ViewGroup
            loadingView = mRootView!!.findViewById<View>(R.id.txt_more) as TextView
            progressImage = mRootView!!.findViewById<View>(R.id.img_progress) as ImageView
            animation =
                AnimationUtils.loadAnimation(mHostListView.context, R.anim.loading_animation)
            mRootView!!.setOnClickListener { onLoadMore() }
        }
        return mRootView!!
    }

    fun setBackgroundRes(resId: Int) {
        val loadMoreView: ViewGroup? = getLoadMoreView()
        loadMoreView?.setBackgroundResource(resId)
    }

    override fun onLoadMore() {
        // empty
    }

    override fun onLoadingStateChanged(state: Int) {
        this.state = state
        when (state) {
            IMoreView.STATE_NORMAL -> {
                if (progressImage != null) {
                    progressImage!!.visibility = View.GONE
                    progressImage!!.clearAnimation()
                }
                loadingView!!.text = text[0]
            }
            IMoreView.STATE_LOADING -> {
                if (progressImage != null) {
                    progressImage!!.visibility = View.VISIBLE
                    progressImage!!.startAnimation(animation)
                }
                loadingView!!.text = text[1]
            }
            IMoreView.STATE_LOAD_FAIL -> {
                if (progressImage != null) {
                    progressImage!!.visibility = View.GONE
                    progressImage!!.clearAnimation()
                }
                if (failedDrawable != null && failedDrawable!!.size == 4) {
                    loadingView!!.setCompoundDrawablesWithIntrinsicBounds(
                        failedDrawable!![0],
                        failedDrawable!![1], failedDrawable!![2], failedDrawable!![3]
                    )
                }
                loadingView!!.text = text[2]
            }
            IMoreView.STATE_NO_MORE -> {
                if (progressImage != null) {
                    progressImage!!.visibility = View.GONE
                    progressImage!!.clearAnimation()
                }
                if (noneMoreDrawable != null && noneMoreDrawable!!.size == 4) {
                    loadingView!!.setCompoundDrawablesWithIntrinsicBounds(
                        noneMoreDrawable!![0],
                        noneMoreDrawable!![1], noneMoreDrawable!![2], noneMoreDrawable!![3]
                    )
                }
                loadingView!!.text = text[3]
            }
            else -> {
                if (progressImage != null) {
                    progressImage!!.visibility = View.GONE
                    progressImage!!.clearAnimation()
                }
                loadingView!!.text = text[0]
            }
        }
    }
}