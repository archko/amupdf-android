package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.base.BaseFragment
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.listeners.OutlineListener

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : BaseFragment() {

    private lateinit var adapter: BaseRecyclerAdapter<OutlineItem>
    private var outlineItems: ArrayList<OutlineItem>? = null
    private var currentPage: Int = 0
    private var recyclerView: RecyclerView? = null
    private var nodataView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            currentPage = it.getInt("POSITION", 0)
            if (it.getSerializable("OUTLINE") != null) {
                outlineItems = it.getSerializable("OUTLINE") as ArrayList<OutlineItem>
            }

            //if (it.getSerializable("out") != null) {
            //    outlineItems = it.getSerializable("out") as ArrayList<OutlineItem>
            //}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outline, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView?.itemAnimator = null

        //if (null != outlineItems) {
        //    val treeAdapter = TreeAdapter(activity, outlineItems)
        //    treeAdapter.setListener(object : OnItemClickListener<Any?> {
        //        override fun onItemClick(view: View, data: Any?, position: Int) {
        //            val ac = activity as OutlineListener
        //            ac.onSelectedOutline((data as OutlineItem).page)
        //        }
        //        override fun onItemClick2(view: View, data: Any?, position: Int) {}
        //    })
        //    recyclerView.adapter = treeAdapter
        //    return view
        //}

        if (outlineItems == null) {
            nodataView?.visibility = View.VISIBLE
        } else {
            adapter = object : BaseRecyclerAdapter<OutlineItem>(activity, outlineItems!!) {

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): BaseViewHolder<OutlineItem> {
                    val root = inflater.inflate(R.layout.item_outline, parent, false)
                    return ViewHolder(root)
                }
            }
            recyclerView?.adapter = adapter
            if (adapter.itemCount > 0) {
                updateSelection(currentPage)
            }
        }
        return view
    }

    open fun updateSelection(currentPage: Int) {
        if (currentPage < 0 || null == outlineItems) {
            return
        }
        this.currentPage = currentPage
        if (!isResumed) {
            return
        }
        var found = -1
        for (i in outlineItems!!.indices) {
            val item = outlineItems!![i]
            if (found < 0 && item.page >= currentPage) {
                found = i
            }
        }
        if (found >= 0) {
            val finalFound = found
            recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    recyclerView?.scrollToPosition(finalFound)
                }
            })
        }
    }

    protected fun onListItemClick(item: OutlineItem) {
        val ac = activity as OutlineListener
        ac.onSelectedOutline(item.page)
    }

    inner class ViewHolder(root: View) :
        BaseViewHolder<OutlineItem>(root) {


        var title: TextView? = null
        var page: TextView? = null

        init {
            title = root.findViewById(R.id.title)
            page = root.findViewById(R.id.page)
        }

        override fun onBind(data: OutlineItem, position: Int) {
            title?.text = data.title
            page?.text = (data.page.plus(1)).toString()
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }
}