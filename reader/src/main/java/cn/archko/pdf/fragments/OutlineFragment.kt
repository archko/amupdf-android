package cn.archko.pdf.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.OutlineListener
import org.vudroid.core.codec.OutlineLink

/**
 * @author: archko 2019/7/11 :17:55
 */
open class OutlineFragment : DialogFragment() {

    private lateinit var adapter: ARecyclerView.Adapter<ViewHolder>
    var outlineItems: ArrayList<OutlineLink>? = null
    private var currentPage: Int = 0
    private var recyclerView: ARecyclerView? = null
    private var nodataView: View? = null
    private var pendingPos = -1
    private var found = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)

        arguments?.let {
            currentPage = it.getInt("POSITION", 0)
            if (it.getSerializable("OUTLINE") != null) {
                outlineItems = it.getSerializable("OUTLINE") as ArrayList<OutlineLink>
            }

            //if (it.getSerializable("out") != null) {
            //    outlineItems = it.getSerializable("out") as ArrayList<OutlineItem>
            //}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0f
            lp.height =
                ((Utils.getScreenHeightPixelWithOrientation(requireActivity()) * 0.9f).toInt())
            lp.width = (Utils.getScreenWidthPixelWithOrientation(requireActivity()) * 0.8f).toInt()
            window!!.attributes = lp
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

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
            adapter = object : ARecyclerView.Adapter<ViewHolder>() {

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val root = inflater.inflate(R.layout.item_outline, parent, false)
                    return ViewHolder(root)
                }

                override fun getItemCount(): Int {
                    return outlineItems!!.size
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    holder.onBind(outlineItems!![position], position)
                }
            }
            recyclerView?.adapter = adapter
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
        //println(String.format("found:%s, currentPage:%s", found, currentPage))
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
        val ac = activity as OutlineListener
        ac.onSelectedOutline(item.targetPage)
        dismiss()
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
            title?.text = data.title
            page?.text = (data.targetPage.plus(1)).toString()
            itemView.setOnClickListener { onListItemClick(data) }
        }
    }

    fun showDialog(activity: FragmentActivity?) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        show(ft!!, "create_dialog")
    }
}