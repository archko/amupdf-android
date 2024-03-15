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

        binding.back.setOnClickListener { dismiss() }
        binding.btnExtract.setOnClickListener {
            extractListener.exportRange(
                binding.extract.rangeSlider.values[0].toInt(),
                binding.extract.rangeSlider.values[1].toInt(),
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
        binding.extract.tvStart.text = "从1页"
        binding.extract.tvEnd.text = "到${count}页"

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
                    "从${binding.extract.rangeSlider.values[0].toInt()}页"
                binding.extract.tvEnd.text = "到${binding.extract.rangeSlider.values[1].toInt()}页"
            }
        })
    }

}
