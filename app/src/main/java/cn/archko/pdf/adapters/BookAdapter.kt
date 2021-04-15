package cn.archko.pdf.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import java.util.*

/**
 * @author: archko 2018/12/12 :15:43
 */
class BookAdapter : HeaderAndFooterRecyclerAdapter<FileBean> {

    private var mMode = TYPE_FILE
    private var itemClickListener: OnItemClickListener<FileBean>? = null;
    var screenWidth = 1080

    internal fun setMode(mMode: Int) {
        this.mMode = mMode
    }

    constructor(
        context: Context,
        itemClickListener: OnItemClickListener<FileBean>
    ) : super(context) {
        this.itemClickListener = itemClickListener

        screenWidth = App.instance!!.screenWidth
    }

    constructor(
        context: Context,
        arrayList: List<FileBean>,
        itemClickListener: OnItemClickListener<FileBean>
    ) : super(context, arrayList) {
        this.itemClickListener = itemClickListener
    }

    override fun doGetItemViewType(position: Int): Int {
        return mMode
    }

    override fun doCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileBean> {
        if (viewType == TYPE_FILE) {
            val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
            return ViewHolder(view)
        } else if (viewType == TYPE_RENCENT) {
            val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
            return ViewHolder(view)
        } else if (viewType == TYPE_SEARCH) {
            val view = mInflater.inflate(R.layout.item_book_search, parent, false)
            return SearchViewHolder(view)
        } else if (viewType == TYPE_GRID) {
            val view = mInflater.inflate(R.layout.item_book_grid, parent, false)
            return GridViewHolder(view)
        }
        return BaseViewHolder(parent)
    }

    private inner class ViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        internal var mName: TextView? = null
        internal var mIcon: ImageView? = null
        internal var mSize: TextView? = null
        internal var mProgressBar: ProgressBar? = null

        init {

            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            mSize = itemView.findViewById(R.id.size)
            mProgressBar = itemView.findViewById(R.id.progressbar)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemClickListener?.let {
                itemView.setOnClickListener {
                    itemClickListener!!.onItemClick(
                        itemView,
                        entry,
                        position
                    )
                }
                itemView.setOnLongClickListener {
                    if (entry.type != FileBean.HOME
                        && !entry.isDirectory
                        && !entry.isUpFolder
                    ) {
                        itemClickListener!!.onItemClick2(itemView, entry, position)
                        return@setOnLongClickListener true
                    } else {
                        return@setOnLongClickListener false
                    }
                }
            }
            mName!!.text = entry.label
            val bookProgress = entry.bookProgress
            if (null != bookProgress) {
                if (bookProgress.page > 0) {
                    mProgressBar!!.visibility = View.VISIBLE
                    mProgressBar!!.max = bookProgress.pageCount
                    mProgressBar!!.progress = bookProgress.page
                } else {
                    mProgressBar!!.visibility = View.INVISIBLE
                }
                mSize!!.text = Utils.getFileSize(bookProgress.size)
            } else {
                mProgressBar!!.visibility = View.INVISIBLE
                mSize!!.text = null
            }

            if (entry.type == FileBean.HOME) {
                mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
            } else if (entry.type == FileBean.NORMAL && entry.isDirectory && !entry.isUpFolder) {
                mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
            } else if (entry.isUpFolder) {
                mIcon!!.setImageResource(R.drawable.ic_explorer_fldr)
            } else {
                if (null != bookProgress && null != bookProgress.ext) {
                    val ext = bookProgress.ext!!.toLowerCase(Locale.ROOT)

                    if (ext.contains("pdf")) {
                        mIcon!!.setImageResource(R.drawable.ic_item_book)
                    } else if (ext.contains("epub")) {
                        mIcon!!.setImageResource(R.drawable.ic_item_book)
                    } else {
                        mIcon!!.setImageResource(R.drawable.ic_explorer_any)
                    }
                }
            }
        }
    }

    private inner class SearchViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        internal var mName: TextView? = null
        internal var mIcon: ImageView? = null
        internal var mSize: TextView? = null
        internal var mPath: TextView? = null

        init {

            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            mSize = itemView.findViewById(R.id.size)
            mPath = itemView.findViewById(R.id.fullpath)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemClickListener?.let {
                itemView.setOnClickListener {
                    itemClickListener!!.onItemClick(
                        itemView,
                        entry,
                        position
                    )
                }
                itemView.setOnLongClickListener {
                    if (entry.type != FileBean.HOME
                        && !entry.isDirectory
                        && !entry.isUpFolder
                    ) {
                        itemClickListener!!.onItemClick2(itemView, entry, position)
                        return@setOnLongClickListener true
                    } else {
                        return@setOnLongClickListener false
                    }
                }
            }
            mName!!.text = entry.label
            if (null != entry.bookProgress) {
                mSize!!.text = Utils.getFileSize(entry.bookProgress!!.size)
            }
            if (null != entry.file) {
                mPath!!.text = FileUtils.getDir(entry.file)
            }
            if (null != entry.bookProgress && null != entry.bookProgress!!.ext) {
                val ext = entry.bookProgress!!.ext!!.toLowerCase(Locale.ROOT)

                if (ext.contains("pdf")) {
                    mIcon!!.setImageResource(R.drawable.ic_item_book)
                } else if (ext.contains("epub")) {
                    mIcon!!.setImageResource(R.drawable.ic_item_book)
                } else {
                    mIcon!!.setImageResource(R.drawable.ic_explorer_any)
                }
            }
        }
    }

    private inner class GridViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        internal var mName: TextView? = null
        internal var mIcon: ImageView? = null

        //internal var mSize: TextView? = null
        internal var mProgressBar: ProgressBar? = null

        init {

            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            //mSize = itemView.findViewById(R.id.size)
            mProgressBar = itemView.findViewById(R.id.progressbar)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemClickListener?.let {
                itemView.setOnClickListener {
                    itemClickListener!!.onItemClick(
                        itemView,
                        entry,
                        position
                    )
                }
                itemView.setOnLongClickListener {
                    if (entry.type != FileBean.HOME
                        && !entry.isDirectory
                        && !entry.isUpFolder
                    ) {
                        itemClickListener!!.onItemClick2(itemView, entry, position)
                        return@setOnLongClickListener true
                    } else {
                        return@setOnLongClickListener false
                    }
                }
            }
            mName!!.text = entry.label
            val bookProgress = entry.bookProgress
            if (null != bookProgress) {
                if (bookProgress.page > 0) {
                    mProgressBar!!.visibility = View.VISIBLE
                    mProgressBar!!.max = bookProgress.pageCount
                    mProgressBar!!.progress = bookProgress.page
                } else {
                    mProgressBar!!.visibility = View.INVISIBLE
                }
                //mSize!!.text = Utils.getFileSize(bookProgress.size)
            } else {
                mProgressBar!!.visibility = View.INVISIBLE
                //mSize!!.text = null
            }

            if (null != bookProgress && null != bookProgress.ext) {
                val ext = bookProgress.ext!!.toLowerCase(Locale.ROOT)

                if (ext.contains("pdf")) {
                    mIcon!!.setImageResource(R.drawable.ic_item_book)
                } else if (ext.contains("epub")) {
                    mIcon!!.setImageResource(R.drawable.ic_item_book)
                } else {
                    mIcon!!.setImageResource(R.drawable.ic_explorer_any)
                }

                ImageLoader.getInstance()
                    .loadImage(entry.file?.absolutePath, 0, 1.0f, screenWidth, mIcon!!);
            }
        }
    }

    companion object {

        @JvmField
        val TYPE_FILE = 0

        @JvmField
        val TYPE_RENCENT = 1

        @JvmField
        val TYPE_SEARCH = 2

        @JvmField
        val TYPE_GRID = 3
    }
}
