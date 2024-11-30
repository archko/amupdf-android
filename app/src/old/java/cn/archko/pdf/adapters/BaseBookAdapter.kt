package cn.archko.pdf.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import cn.archko.mupdf.R
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.OnItemClickListener
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.utils.FetcherUtils
import java.util.Locale

/**
 * @author: archko 2024/8/12 :15:43
 */
open class BaseBookAdapter(
    context: Context,
    diffCallback: DiffUtil.ItemCallback<FileBean>,
    private var itemClickListener: OnItemClickListener<FileBean>?
) :
    ListAdapter<FileBean, BaseViewHolder<FileBean>>(diffCallback) {

    protected var mInflater: LayoutInflater

    private var coverWidth = 135
    private var coverHeight = 180

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val screenHeight: Int = Utils.getScreenHeightPixelWithOrientation(context)
        val screenWidth: Int = Utils.getScreenWidthPixelWithOrientation(context)
        var h: Int = (screenHeight - Utils.dipToPixel(12f)) / 3
        var w: Int = (screenWidth - Utils.dipToPixel(12f)) / 3
        if (h < w) {
            w = h
        }
        h = w * 4 / 3
        coverWidth = w
        coverHeight = h
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileBean> {
        val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<FileBean>, position: Int) {
        //Logcat.d("adapter", "bind:$position, holder:$holder")
        holder.onBind(currentList[position], position)
    }

    inner class ViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        private var mName: TextView? = null
        private var mIcon: ImageView? = null
        private var mSize: TextView? = null
        private var mProgressBar: ProgressBar? = null

        init {
            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            mSize = itemView.findViewById(R.id.size)
            mProgressBar = itemView.findViewById(R.id.progressbar)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(
                    itemView,
                    entry,
                    position
                )
            }
            itemView.setOnLongClickListener {
                if (entry.type != FileBean.HOME
                    //&& !entry.isDirectory
                    && !entry.isUpFolder
                ) {
                    itemClickListener?.onItemClick2(itemView, entry, position)
                    return@setOnLongClickListener true
                } else {
                    return@setOnLongClickListener false
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
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_dir_home)
            } else if (entry.type == FileBean.NORMAL && entry.isDirectory && !entry.isUpFolder) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
            } else if (entry.isUpFolder) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
            } else {
                if (bookProgress?.ext != null) {
                    val ext = bookProgress.ext!!.lowercase(Locale.ROOT)

                    AdapterUtils.setIcon(".$ext", mIcon!!)
                }
            }
        }
    }

    inner class SearchViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

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
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(
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
                    itemClickListener?.onItemClick2(itemView, entry, position)
                    return@setOnLongClickListener true
                } else {
                    return@setOnLongClickListener false
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

                AdapterUtils.setIcon(".$ext", mIcon!!)
            }
        }
    }

    inner class GridViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

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
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(
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
                    itemClickListener?.onItemClick2(itemView, entry, position)
                    return@setOnLongClickListener true
                } else {
                    return@setOnLongClickListener false
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

                AdapterUtils.setIcon(".$ext", mIcon!!)

                var lp = mIcon!!.layoutParams
                if (null == lp) {
                    lp = LinearLayout.LayoutParams(coverWidth, coverHeight)
                    mIcon!!.setLayoutParams(lp)
                } else {
                    lp.width = coverWidth
                    lp.height = coverHeight
                }
                entry.file?.absolutePath?.let { FetcherUtils.load(it, mIcon!!.context, mIcon!!) }
            }
        }
    }
}
