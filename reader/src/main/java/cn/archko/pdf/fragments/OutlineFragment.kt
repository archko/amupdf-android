package cn.archko.pdf.fragments

import android.graphics.Color
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
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.databinding.FragmentOutlineBinding
import cn.archko.pdf.fragments.OutlineTabFragment.Companion.ARG_CURRENT_PAGE
import org.vudroid.core.codec.OutlineLink

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : Fragment(R.layout.fragment_outline) {

    private lateinit var binding: FragmentOutlineBinding
    private lateinit var adapter: RecyclerView.Adapter<ViewHolder>
    var outlineItems: List<OutlineLink>? = null
    var currentPage: Int = 0
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutlineBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = ColorItemDecoration(requireContext())
        binding.recyclerView.addItemDecoration(itemDecoration)

        adapter = object : RecyclerView.Adapter<ViewHolder>() {
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

        binding.recyclerView.adapter = adapter

        if (outlineItems.isNullOrEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.nodataView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.nodataView.visibility = View.GONE
        }
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
            binding.recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    (binding.recyclerView.layoutManager as LinearLayoutManager)
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
        BaseViewHolder<OutlineLink>(root) {

        var title: TextView? = null
        var page: TextView? = null

        init {
            title = root.findViewById(R.id.title)
            page = root.findViewById(R.id.page)
        }

        override fun onBind(data: OutlineLink, position: Int) {
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