package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.databinding.FragmentOutlineBinding
import cn.archko.pdf.databinding.ItemOutlineBinding
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.base.BaseFragment

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : BaseFragment<FragmentOutlineBinding>(R.layout.fragment_outline) {

    private lateinit var adapter: BaseRecyclerAdapter<OutlineItem>
    private var outlineItems: ArrayList<OutlineItem>? = null
    private var currentPage: Int = 0

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

    override fun setupView() {
        binding.recyclerView.itemAnimator = null

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
            binding.nodataView.visibility = View.VISIBLE
        } else {
            adapter = object : BaseRecyclerAdapter<OutlineItem>(activity, outlineItems!!) {

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): BaseViewHolder<OutlineItem> {
                    val binding =
                        ItemOutlineBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                    return ViewHolder(binding)
                }
            }
            binding.recyclerView.adapter = adapter
            if (adapter.itemCount > 0) {
                updateSelection(currentPage)
            }
        }
    }

    open fun updateSelection(currentPage: Int) {
        if (currentPage < 0) {
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
            binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    binding.recyclerView.scrollToPosition(finalFound)
                }
            })
        }
    }

    protected fun onListItemClick(item: OutlineItem) {
        val ac = activity as OutlineListener
        ac.onSelectedOutline(item.page)
    }

    inner class ViewHolder(private val binding: ItemOutlineBinding) :
        BaseViewHolder<OutlineItem>(binding.root) {

        override fun onBind(data: OutlineItem, position: Int) {
            binding.title.text = data.title
            binding.page.text = (data.page.plus(1)).toString()
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }
}