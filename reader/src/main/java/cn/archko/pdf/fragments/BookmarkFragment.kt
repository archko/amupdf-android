package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder

/**
 * @author: archko 2019/7/11 :17:55
 */
open class BookmarkFragment : Fragment() {

    private lateinit var adapter: BaseRecyclerAdapter<String>
    private lateinit var recyclerView: RecyclerView
    private lateinit var nodataView: TextView
    private var bookmarks = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outline, container, false)

        recyclerView = view.findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        nodataView = view.findViewById(R.id.no_data)

        adapter = object : BaseRecyclerAdapter<String>(activity, bookmarks!!) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val itemView = mInflater.inflate(R.layout.item_outline, parent, false)
                return ViewHolder(itemView)
            }
        }
        recyclerView.adapter = adapter

        if (bookmarks == null || bookmarks!!.size == 0) {
            noBookmark()
        }
        return view
    }

    private fun noBookmark() {
        nodataView.text = "No bookmark"
        nodataView.visibility = View.VISIBLE
    }

    open fun updateBookmark(bookmark: String?) {
        if (!isResumed) {
            return
        }
        bookmark?.let {
            bookmarks.clear()
            bookmarks.addAll(ArrayList(bookmark.split("_")))
            if (bookmarks!!.size > 0) {
                adapter.notifyDataSetChanged()
            } else {
                noBookmark()
            }
        }
    }

    protected fun onListItemClick(item: String) {
    }

    inner class ViewHolder(itemView: View) : BaseViewHolder<String>(itemView) {

        var title: TextView = itemView.findViewById(R.id.title)
        var page: TextView = itemView.findViewById(R.id.page)

        override fun onBind(data: String, position: Int) {
            title.text = "Page:"
            page.text = data
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }
}