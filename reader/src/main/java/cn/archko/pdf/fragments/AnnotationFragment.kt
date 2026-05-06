package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.entity.AnnotationPath
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.databinding.FragmentAnnotationBinding
import cn.archko.pdf.databinding.ItemAnnotationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 批注Fragment
 * @author: archko 2026/3/9
 */
class AnnotationFragment(private val annotationManager: AnnotationManager?) :
    Fragment(R.layout.fragment_annotation) {

    private lateinit var binding: FragmentAnnotationBinding
    private lateinit var adapter: AnnotationAdapter

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var annotationsJob: Job? = null

    companion object {
        fun newInstance(annotationManager: AnnotationManager?): AnnotationFragment {
            return AnnotationFragment(annotationManager).apply {
                arguments = Bundle().apply {
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnnotationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = ColorItemDecoration(requireContext())
        binding.recyclerView.addItemDecoration(itemDecoration)
        adapter = AnnotationAdapter(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun onItemClick(pageIndex: Int) {
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                it.onListItemClick(pageIndex)
            }
        }
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
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE

            val annotationList = annotations.entries.map { entry ->
                AnnotationItem(entry.key, entry.value)
            }.sortedBy { it.pageIndex }

            adapter.submitList(annotationList)
        }
    }

    data class AnnotationItem(
        val pageIndex: Int,
        val paths: List<AnnotationPath>

    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AnnotationItem

            if (pageIndex != other.pageIndex) return false
            if (paths != other.paths) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pageIndex
            result = 31 * result + paths.hashCode()
            return result
        }
    }

    inner class AnnotationAdapter(
        context: Context,
    ) : BaseRecyclerListAdapter<AnnotationItem>(
        context,
        object : DiffUtil.ItemCallback<AnnotationItem>() {
            override fun areItemsTheSame(
                oldItem: AnnotationItem,
                newItem: AnnotationItem
            ): Boolean {
                return oldItem.pageIndex == newItem.pageIndex
            }

            override fun areContentsTheSame(
                oldItem: AnnotationItem,
                newItem: AnnotationItem
            ): Boolean {
                return oldItem.paths == newItem.paths
            }
        }) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AnnotationItem> {
            val binding = ItemAnnotationBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
    }

    inner class ViewHolder(
        private val binding: ItemAnnotationBinding
    ) : BaseViewHolder<AnnotationItem>(binding.root) {
        override fun onBind(item: AnnotationItem, position: Int) {
            binding.tvPage.text = context!!.getString(R.string.page_label, item.pageIndex + 1)
            binding.tvCount.text = context!!.getString(R.string.annotations_count, item.paths.size)

            binding.root.setOnClickListener {
                onItemClick(item.pageIndex)
            }

            binding.btnDelete.setOnClickListener {
                annotationManager?.deletePaths(item.pageIndex)
            }
        }
    }
}