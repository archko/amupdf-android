package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.entity.Bookmark

/**
 * @author: archko 2019/7/11 :17:55
 */
open class BookmarkFragment : Fragment() {

    private lateinit var adapter: BaseRecyclerAdapter<Bookmark>
    private lateinit var recyclerView: RecyclerView
    private lateinit var nodataView: TextView
    private lateinit var addView: View
    private var bookmarks = ArrayList<Bookmark>()
    private var page = 0
    var itemListener: ItemListener? = null
        set(value) {
            field = value
        }

    interface ItemListener {
        fun onClick(data: Bookmark, position: Int)
        fun onDelete(data: Bookmark, position: Int)
        fun onAdd(page: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)

        recyclerView = view.findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        val itemDecoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
        context?.getDrawable(R.drawable.bg_divider)?.let { itemDecoration.setDrawable(it) }
        recyclerView.addItemDecoration(
            itemDecoration
        )
        nodataView = view.findViewById(R.id.no_data)
        addView = view.findViewById(R.id.add)
        addView.setOnClickListener {
            itemListener?.onAdd(page)
        }

        adapter = object : BaseRecyclerAdapter<Bookmark>(activity, bookmarks) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<Bookmark> {
                val itemView = mInflater.inflate(R.layout.item_bookmark, parent, false)
                return ViewHolder(itemView)
            }
        }
        recyclerView.adapter = adapter

        if (bookmarks.size == 0) {
            noBookmark()
        } else {
            nodataView.visibility = View.GONE
        }
        return view
    }

    private fun noBookmark() {
        nodataView.visibility = View.VISIBLE
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
                nodataView.visibility = View.GONE
                adapter.notifyDataSetChanged()
            } else {
                noBookmark()
            }
        }
    }

    inner class ViewHolder(itemView: View) : BaseViewHolder<Bookmark>(itemView) {

        var title: TextView = itemView.findViewById(R.id.title)
        var page: TextView = itemView.findViewById(R.id.page)
        var delete: View = itemView.findViewById(R.id.delete)

        override fun onBind(data: Bookmark, position: Int) {
            page.text = (data.page + 1).toString()
            delete.setOnClickListener { itemListener?.onDelete(data, position) }
            itemView.setOnClickListener { itemListener?.onClick(data, position) }
        }
    }
}