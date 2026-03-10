package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.common.AnnotationManager
import cn.archko.pdf.entity.AnnotationPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 批注Fragment
 * @author: archko 2026/3/9
 */
class AnnotationFragment : Fragment() {

    private var annotationManager: AnnotationManager? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: AnnotationAdapter

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var annotationsJob: Job? = null

    companion object {
        private const val ARG_ANNOTATION_MANAGER = "annotation_manager"

        fun newInstance(annotationManager: AnnotationManager?): AnnotationFragment {
            return AnnotationFragment().apply {
                arguments = Bundle().apply {
                    // 由于AnnotationManager不是Parcelable，我们通过父Fragment传递
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从父Fragment获取AnnotationManager
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                annotationManager = it.annotationManager
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_annotation, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.tv_empty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AnnotationAdapter(requireContext(), emptyList()) { pageIndex ->
            // 点击批注项，跳转到对应页面
            parentFragment?.let {
                if (it is OutlineTabFragment) {
                    it.onAnnotationClick(pageIndex)
                }
            }
        }
        recyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        startObservingAnnotations()
    }

    override fun onPause() {
        super.onPause()
        stopObservingAnnotations()
    }

    private fun startObservingAnnotations() {
        annotationsJob?.cancel()
        annotationsJob = coroutineScope.launch {
            annotationManager?.annotationsFlow?.collectLatest { annotations ->
                updateAnnotations(annotations)
            }
        }
    }

    private fun stopObservingAnnotations() {
        annotationsJob?.cancel()
        annotationsJob = null
    }

    private fun updateAnnotations(annotations: Map<Int, List<AnnotationPath>>) {
        if (annotations.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE

            // 转换数据为列表形式
            val annotationList = annotations.entries.map { entry ->
                AnnotationItem(entry.key, entry.value)
            }.sortedBy { it.pageIndex }

            adapter.updateData(annotationList)
        }
    }

    data class AnnotationItem(
        val pageIndex: Int,
        val paths: List<AnnotationPath>
    )

    class AnnotationAdapter(
        private val context: android.content.Context,
        private var items: List<AnnotationItem>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<AnnotationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPage: android.widget.TextView = view.findViewById(R.id.tv_page)
            val tvCount: android.widget.TextView = view.findViewById(R.id.tv_count)
            val btnDelete: android.widget.ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_annotation, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvPage.text = context.getString(R.string.page_label, item.pageIndex + 1)
            holder.tvCount.text = context.getString(R.string.annotations_count, item.paths.size)

            holder.itemView.setOnClickListener {
                onItemClick(item.pageIndex)
            }

            holder.btnDelete.setOnClickListener {
                // 删除该页面的所有批注
                // 这里需要访问AnnotationManager，可以通过其他方式传递
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<AnnotationItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}