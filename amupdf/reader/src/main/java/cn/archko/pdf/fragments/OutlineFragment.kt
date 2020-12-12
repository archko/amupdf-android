package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.tree.TreeAdapter
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.listeners.OutlineListener
import com.artifex.mupdf.viewer.OutlineActivity

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : Fragment() {

    private lateinit var adapter: BaseRecyclerAdapter<OutlineActivity.Item>
    private lateinit var listView: RecyclerView
    private var outline: ArrayList<OutlineActivity.Item>? = null
    private var outlineItems: ArrayList<OutlineItem>? = null
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            currentPage = arguments!!.getInt("POSITION", 0)
            if (arguments!!.getSerializable("OUTLINE") != null) {
                outline = arguments!!.getSerializable("OUTLINE") as ArrayList<OutlineActivity.Item>
            }

            if (arguments!!.getSerializable("out") != null) {
                outlineItems = arguments!!.getSerializable("out") as ArrayList<OutlineItem>
            }
        }

        if (null == outline) {
            outline = ArrayList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outline, container, false)

        listView = view.findViewById(R.id.list)
        listView.layoutManager = LinearLayoutManager(activity)
        listView.itemAnimator = null

        if (null != outlineItems) {
            val treeAdapter = TreeAdapter(activity, outlineItems)
            treeAdapter.setListener(object : OnItemClickListener<Any?> {
                override fun onItemClick(view: View, data: Any?, position: Int) {
                    val ac = activity as OutlineListener
                    ac.onSelectedOutline((data as OutlineItem).page)
                }

                override fun onItemClick2(view: View, data: Any?, position: Int) {}
            })
            listView.adapter = treeAdapter
            return view
        }

        adapter = object : BaseRecyclerAdapter<OutlineActivity.Item>(activity, outline!!) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<OutlineActivity.Item> {
                val itemView = mInflater.inflate(R.layout.item_outline, parent, false)
                return ViewHolder(itemView)
            }
        }
        listView.adapter = adapter
        if (adapter.itemCount > 0) {
            updateSelection(currentPage)
        }
        return view
    }

    open fun updateSelection(currentPage: Int) {
        if (currentPage < 0) {
            return
        }
        this.currentPage = currentPage;
        if (!isResumed) {
            return
        }
        var found = -1
        for (i in outline!!.indices) {
            val item = outline!![i]
            if (found < 0 && item.page >= currentPage) {
                found = i
            }
        }
        if (found >= 0) {
            val finalFound = found
            listView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    listView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    listView.scrollToPosition(finalFound)
                }
            })
        }
    }

    protected fun onListItemClick(item: OutlineActivity.Item) {
        val ac = activity as OutlineListener
        ac.onSelectedOutline(item.page)
    }

    inner class ViewHolder(itemView: View) : BaseViewHolder<OutlineActivity.Item>(itemView) {

        var title: TextView = itemView.findViewById(R.id.title)
        var page: TextView = itemView.findViewById(R.id.page)

        override fun onBind(data: OutlineActivity.Item, position: Int) {
            title.text = data.title
            page.text = (data.page.plus(1)).toString()
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }
}