package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.base.BaseFragment
import cn.archko.pdf.entity.Bookmark

/**
 * @author: archko 2019/7/11 :17:55
 */
open class BookmarkFragment : BaseFragment() {

    private lateinit var adapter: BaseRecyclerAdapter<Bookmark>
    private var bookmarks = ArrayList<Bookmark>()
    private var page = 0

    private var recyclerView: RecyclerView? = null
    private var add: View? = null
    private var nodataView: View? = null
    var itemListener: ItemListener? = null
        set(value) {
            field = value
        }

    interface ItemListener {
        fun onClick(data: Bookmark, position: Int)
        fun onDelete(data: Bookmark, position: Int)
        fun onAdd(page: Int)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        add = view.findViewById(R.id.add)
        nodataView = view.findViewById(R.id.nodataView)

        recyclerView?.itemAnimator = null
        val itemDecoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
        context?.getDrawable(R.drawable.bg_divider)?.let { itemDecoration.setDrawable(it) }
        recyclerView?.addItemDecoration(
            itemDecoration
        )
        add?.setOnClickListener {
            itemListener?.onAdd(page)
        }

        adapter = object : BaseRecyclerAdapter<Bookmark>(activity, bookmarks) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<Bookmark> {
                val root = inflater.inflate(R.layout.item_bookmark, parent, false)
                return ViewHolder(root)
            }
        }
        recyclerView?.adapter = adapter

        if (bookmarks.size == 0) {
            noBookmark()
        } else {
            nodataView?.visibility = View.GONE
        }


        return view
    }

    private fun noBookmark() {
        nodataView?.visibility = View.VISIBLE
    }

    open fun updateBookmark(page: Int, list: List<Bookmark>) {
        if (!isResumed) {
            return
        }
        this.page = page
        bookmarks.let {
            this.bookmarks.clear()
            this.bookmarks.addAll(list)
            if (this.bookmarks.size > 0) {
                nodataView?.visibility = View.GONE
                adapter.notifyDataSetChanged()
            } else {
                noBookmark()
            }
        }
    }

    inner class ViewHolder(private val root: View) :
        BaseViewHolder<Bookmark>(root) {

        var page: TextView? = null
        var delete: View? = null

        init {
            page = root.findViewById(R.id.page)
            delete = root.findViewById(R.id.delete)
        }

        override fun onBind(data: Bookmark, position: Int) {
            page?.text = (data.page + 1).toString()
            delete?.setOnClickListener { itemListener?.onDelete(data, position) }
            itemView.setOnClickListener { itemListener?.onClick(data, position) }
        }
    }
}