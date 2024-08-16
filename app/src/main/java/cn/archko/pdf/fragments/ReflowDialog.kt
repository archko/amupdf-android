package cn.archko.pdf.core.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.DialogReflowBinding
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.fragments.PDFEditViewModel
import coil.load
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReflowDialog(
    context: Context,
    val screenWidth: Int,
    val screenHeight: Int,
    val page: Int,
    val pdfEditViewModel: PDFEditViewModel,
    private val pdfPath: String,
    private val reflowListener: ReflowListener
) :
    Dialog(context, R.style.Theme_RangeSlider) {

    interface ReflowListener {

        fun exportRange(start: Int, end: Int, width: Int)
    }

    private var binding: DialogReflowBinding
    private var imageAdapter: ImageAdapter
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window!!.setBackgroundDrawable(ColorDrawable(context.resources.getColor(cn.archko.pdf.R.color.md_theme_background)))
        window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    init {
        //setCancelable(false)
        //setCanceledOnTouchOutside(false)
        count = pdfEditViewModel.countPages()

        binding = DialogReflowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRangeSlider()

        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.btnPreview.setOnClickListener {
            preview()
        }
        binding.btnReflow.setOnClickListener {
            reflowListener.exportRange(
                binding.rangeSlider.values[0].toInt(), binding.rangeSlider.values[1].toInt(),
                binding.resolutionSlider.value.toInt()
            )
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        imageAdapter = ImageAdapter(context)
        binding.recyclerView.adapter = imageAdapter
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun setupRangeSlider() {
        if (screenWidth < 1080) {
            binding.resolutionSlider.valueFrom = screenWidth.toFloat()
        }
        binding.tvStart.text =
            String.format(context.getString(R.string.edit_from_page), "1")
        binding.tvEnd.text =
            String.format(context.getString(R.string.edit_to_page), count.toString())

        binding.rangeSlider.valueFrom = 1f
        binding.rangeSlider.valueTo = count.toFloat()
        val values = mutableListOf<Float>()
        values.add(1f)
        values.add(count.toFloat())
        binding.rangeSlider.values = values
        binding.rangeSlider.setLabelFormatter { value ->
            value.toInt().toString()
        }

        binding.rangeSlider.addOnSliderTouchListener(object :
            RangeSlider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: RangeSlider) {
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: RangeSlider) {
                binding.tvStart.text =
                    String.format(
                        context.getString(R.string.edit_from_page),
                        binding.rangeSlider.values[0]
                    )
                binding.tvEnd.text =
                    String.format(
                        context.getString(R.string.edit_to_page),
                        binding.rangeSlider.values[1]
                    )
            }
        })
    }

    private fun preview() {
        val name = FileUtils.getNameWithoutExt(pdfPath)
        val dir = FileUtils.getStorageDir(name).absolutePath
        val scope = CoroutineScope(Job() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
        scope.launch {
            FileUtils.cleanDir(File(dir))
            val width = binding.resolutionSlider.value.toInt()
            val bitmap = pdfEditViewModel.loadBitmapByPage(width, page)
            if (null != bitmap) {
                val list = mutableListOf<String>()
                val result = pdfEditViewModel.k2pdf(
                    bitmap,
                    screenWidth,
                    screenHeight,
                    270,
                    dir,
                    list
                )
                if (result > 0) {
                    val file = File(dir)
                    val files = file.listFiles()

                    withContext(Dispatchers.Main) {
                        if (files != null) {
                            imageAdapter.files = files
                            imageAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private class ImageAdapter(
        var context: Context,
    ) :
        ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

        var files: Array<File>? = null

        override fun getItemCount(): Int {
            if (null == files) {
                return 0
            }
            return files!!.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ARecyclerView.ViewHolder {
            val view = ImageView(context)
                .apply {
                    layoutParams = ARecyclerView.LayoutParams(
                        ARecyclerView.LayoutParams.MATCH_PARENT,
                        ARecyclerView.LayoutParams.WRAP_CONTENT
                    )
                    adjustViewBounds = true
                }

            return ImageHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
            val pdfHolder = viewHolder as ImageHolder

            pdfHolder.onBind(position)
        }

        inner class ImageHolder(internal var view: ImageView) : ARecyclerView.ViewHolder(view) {

            fun onBind(position: Int) {
                view.load(files?.get(position)?.path)
            }
        }
    }
}
