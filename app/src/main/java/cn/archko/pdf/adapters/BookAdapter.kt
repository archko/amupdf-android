package cn.archko.pdf.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import java.util.Locale

/**
 * @author: archko 2018/12/12 :15:43
 */
class BookAdapter(context: Context, itemClickListener: OnItemClickListener<FileBean>) :
    HeaderAndFooterRecyclerAdapter<FileBean>(context) {

    private var mMode = TYPE_FILE
    private var itemClickListener: OnItemClickListener<FileBean>? = itemClickListener
    var screenWidth = 1080

    internal fun setMode(mMode: Int) {
        this.mMode = mMode
    }

    init {
        screenWidth = App.instance!!.screenWidth
    }

    override fun doGetItemViewType(position: Int): Int {
        return mMode
    }

    override fun doCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileBean> {
        when (viewType) {
            TYPE_FILE -> {
                val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
                return ViewHolder(view)
            }
            TYPE_RENCENT -> {
                val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
                return ViewHolder(view)
            }
            TYPE_SEARCH -> {
                val view = mInflater.inflate(R.layout.item_book_search, parent, false)
                return SearchViewHolder(view)
            }
            TYPE_GRID -> {
                val view = mInflater.inflate(R.layout.item_book_grid, parent, false)
                return GridViewHolder(view)
            }
            else -> return BaseViewHolder(parent)
        }
    }

    private inner class ViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        var mName: TextView? = null
        var mIcon: ImageView? = null
        var mSize: TextView? = null
        var mProgressBar: ProgressBar? = null

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
                if (bookProgress?.ext != null) {
                    val ext = bookProgress.ext!!.lowercase(Locale.ROOT)

                    AdapterUtils.setIcon(ext, mIcon)
                }
            }
        }
    }

    private inner class SearchViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        var mName: TextView? = null
        var mIcon: ImageView? = null
        var mSize: TextView? = null
        var mPath: TextView? = null

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
                val ext = entry.bookProgress!!.ext!!.lowercase(Locale.ROOT)

                AdapterUtils.setIcon(ext, mIcon)
            }
        }
    }

    private inner class GridViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        var mName: TextView? = null
        var mIcon: ImageView? = null

        // var mSize: TextView? = null
        var mProgressBar: ProgressBar? = null

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

            if (bookProgress?.ext != null) {
                val ext = bookProgress.ext!!.lowercase(Locale.ROOT)

                AdapterUtils.setIcon(ext, mIcon)

                //ImageLoader.getInstance()
                //    .loadImage(entry.file?.absolutePath, 0, 1.0f, screenWidth, mIcon!!)
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
