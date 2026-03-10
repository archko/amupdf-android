package cn.archko.pdf.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.base.BaseFragment
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.fragments.OutlineTabFragment.Companion.ARG_CURRENT_PAGE
import org.vudroid.core.codec.OutlineLink

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : BaseFragment() {

    private lateinit var adapter: ARecyclerView.Adapter<ViewHolder>
    var outlineItems: List<OutlineLink>? = null
    var currentPage: Int = 0
    private var recyclerView: ARecyclerView? = null
    private var nodataView: View? = null
    private var pendingPos = -1
    private var found = -1

    companion object {

        fun newInstance(arguments: Bundle?, outlineItems: List<OutlineLink>?): OutlineFragment {
            val fragment = OutlineFragment()
            arguments?.run {
                fragment.outlineItems = outlineItems
                updateArgs(fragment, arguments)
            }
            return fragment
        }

        fun updateArgs(
            fragment: OutlineFragment,
            arguments: Bundle?
        ) {
            arguments?.run {
                fragment.currentPage = arguments.getInt(ARG_CURRENT_PAGE)
                if (fragment.currentPage > 0) {
                    fragment.pendingPos = fragment.currentPage
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outline, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        nodataView = view.findViewById(R.id.nodataView)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        adapter = object : ARecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_outline, parent, false)
                return ViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = outlineItems?.get(position)
                if (item != null) {
                    holder.onBind(item, position)
                }
            }

            override fun getItemCount(): Int = outlineItems?.size ?: 0
        }

        recyclerView?.adapter = adapter

        if (outlineItems.isNullOrEmpty()) {
            recyclerView?.visibility = View.GONE
            nodataView?.visibility = View.VISIBLE
        } else {
            recyclerView?.visibility = View.VISIBLE
            nodataView?.visibility = View.GONE
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (pendingPos >= 0) {
            selection(pendingPos)
            pendingPos = -1
        }
    }

    open fun updateSelection(currentPage: Int) {
        if (currentPage < 0 || null == outlineItems) {
            return
        }
        this.currentPage = currentPage
        if (!isResumed) {
            pendingPos = currentPage
            return
        }
        selection(currentPage)
    }

    private fun selection(currentPage: Int) {
        found = -1
        for (i in outlineItems!!.indices) {
            val item = outlineItems!![i]
            if (found < 0 && item.targetPage >= currentPage) {
                found = i
            }
        }
        Logcat.d(String.format("found:%s, currentPage:%s", found, currentPage))
        if (found >= 0) {
            recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    (recyclerView?.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(found, -10)
                }
            })
        }
    }

    protected fun onListItemClick(item: OutlineLink) {
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                it.onListItemClick(item.targetPage)
            }
        }
    }

    inner class ViewHolder(private val root: View) :
        ARecyclerView.ViewHolder(root) {

        var title: TextView? = null
        var page: TextView? = null

        init {
            title = root.findViewById(R.id.title)
            page = root.findViewById(R.id.page)
        }

        fun onBind(data: OutlineLink, position: Int) {
            if (position == found) {
                root.setBackgroundColor(root.context.resources.getColor(R.color.toc_color_bg))
            } else {
                root.setBackgroundColor(Color.TRANSPARENT)
            }
            val indent = "   ".repeat(data.level)
            title?.text = String.format("%s%s", indent, data.title)
            page?.text = (data.targetPage.plus(1)).toString()
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }
}