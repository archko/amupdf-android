package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.DialogExtractBinding
import com.google.android.material.slider.RangeSlider

class ExtractDialog(
    context: Context,
    val width: Int,
    val page: Int,
    private val count: Int,
    private val extractListener: ExtractListener
) :
    Dialog(context, R.style.Theme_RangeSlider) {

    interface ExtractListener {

        fun export(index: Int, width: Int)
        fun exportRange(start: Int, end: Int, width: Int)
    }

    private var binding: DialogExtractBinding

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding = DialogExtractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRangeSlider()

        binding.toolbar.setNavigationOnClickListener { dismiss() }
        //binding.back.setOnClickListener { dismiss() }

        binding.btnExtract.setOnClickListener {
            val start = binding.extract.rangeSlider.values[0].toInt()
            val end = if (count > 1) {
                binding.extract.rangeSlider.values[1].toInt()
            } else start
            extractListener.exportRange(
                start,
                end,
                binding.extract.resolutionSlider.value.toInt()
            )
        }
        binding.btnExtractSingle.setOnClickListener {
            extractListener.export(
                page,
                binding.extract.resolutionSlider.value.toInt()
            )
        }
    }

    private fun setupRangeSlider() {
        if (width < 1080) {
            binding.extract.resolutionSlider.valueFrom = width.toFloat()
        }

        if (count <= 1) {
            binding.extract.rangeSlider.isEnabled = false
            binding.extract.tvStart.text =
                String.format(context.getString(R.string.edit_from_page), "1")
            binding.extract.tvEnd.text =
                String.format(context.getString(R.string.edit_to_page), "1")
            return
        }

        binding.extract.tvStart.text =
            String.format(context.getString(R.string.edit_from_page), "1")
        binding.extract.tvEnd.text =
            String.format(context.getString(R.string.edit_to_page), count.toString())

        binding.extract.rangeSlider.valueFrom = 1f
        binding.extract.rangeSlider.valueTo = count.toFloat()
        val values = mutableListOf<Float>()
        values.add(1f)
        values.add(count.toFloat())
        binding.extract.rangeSlider.values = values
        binding.extract.rangeSlider.setLabelFormatter { value ->
            value.toInt().toString()
        }

        binding.extract.rangeSlider.addOnSliderTouchListener(object :
            RangeSlider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: RangeSlider) {
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: RangeSlider) {
                binding.extract.tvStart.text =
                    String.format(
                        context.getString(R.string.edit_from_page),
                        binding.extract.rangeSlider.values[0].toString()
                    )
                binding.extract.tvEnd.text =
                    String.format(
                        context.getString(R.string.edit_to_page),
                        binding.extract.rangeSlider.values[1].toString()
                    )
            }
        })
    }

}