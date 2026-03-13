package cn.archko.pdf.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.DrawType
import cn.archko.pdf.widgets.DrawPreviewView
import com.google.android.material.slider.Slider

/**
 * 绘图配置对话框 - 融合颜色、线宽和绘制类型选择
 */
class DrawConfigDialog : DialogFragment() {

    private var initialColor: Int = Color.RED
    private var initialWidth: Float = 4f
    private var initialDrawType: DrawType = DrawType.LINE
    private var configChangeListener: ((Int, Float, DrawType) -> Unit)? = null

    private lateinit var drawPreview: DrawPreviewView
    private lateinit var widthSeekBar: Slider
    private lateinit var drawTypeRadioGroup: RadioGroup
    private lateinit var lineRadioButton: RadioButton
    private lateinit var curveRadioButton: RadioButton
    private lateinit var colorsRecyclerView: RecyclerView
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    // 常用颜色列表
    private val colors = listOf(
        Color.RED,
        Color.parseColor("#FFE91E63"),
        Color.parseColor("#FF9C27B0"), // 红、粉、紫
        Color.parseColor("#FF673AB7"),
        Color.parseColor("#FF3F51B5"),
        Color.BLUE, // 深紫、蓝
        Color.parseColor("#FF03A9F4"),
        Color.parseColor("#FF00BCD4"),
        Color.parseColor("#FF009688"), // 天蓝、青、绿
        Color.parseColor("#FF4CAF50"),
        Color.parseColor("#FF8BC34A"),
        Color.parseColor("#FFCDDC39"), // 浅绿、黄
        Color.parseColor("#FFFFEB3B"),
        Color.parseColor("#FFFFC107"),
        Color.parseColor("#FFFF9800"), // 橙
        Color.BLACK,
        Color.GRAY,
        Color.LTGRAY          // 黑、灰
    )

    private var selectedColor: Int = initialColor
    private var selectedWidth: Float = initialWidth
    private var selectedDrawType: DrawType = initialDrawType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            window!!.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.dialog_background))
            window!!.decorView?.elevation = 16f // 16dp 的阴影深度，可根据需要调整
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0.5f 
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return inflater.inflate(R.layout.dialog_draw_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupColorGrid()
        setupWidthSeekBar()
        setupDrawTypeSelection()
        setupButtons()
        updatePreview()
    }

    private fun initViews(view: View) {
        drawPreview = view.findViewById(R.id.drawPreview)
        widthSeekBar = view.findViewById(R.id.widthSeekBar)
        drawTypeRadioGroup = view.findViewById(R.id.drawTypeRadioGroup)
        lineRadioButton = view.findViewById(R.id.lineRadioButton)
        curveRadioButton = view.findViewById(R.id.curveRadioButton)
        colorsRecyclerView = view.findViewById(R.id.colorsRecyclerView)
        okButton = view.findViewById(R.id.okButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupColorGrid() {
        colorsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)
        val adapter = ColorAdapter() { color ->
            selectedColor = color
            updatePreview()
        }
        adapter.data = colors
        colorsRecyclerView.adapter = adapter
    }

    private fun setupWidthSeekBar() {
        widthSeekBar.valueFrom = 1f
        widthSeekBar.valueTo = 20f
        widthSeekBar.stepSize = 1f
        widthSeekBar.value = selectedWidth

        widthSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                selectedWidth = value
                updatePreview()
            }
        }
    }

    private fun setupDrawTypeSelection() {
        when (selectedDrawType) {
            DrawType.LINE -> lineRadioButton.isChecked = true
            DrawType.CURVE -> curveRadioButton.isChecked = true
            else -> {}
        }

        drawTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDrawType = when (checkedId) {
                R.id.lineRadioButton -> DrawType.LINE
                R.id.curveRadioButton -> DrawType.CURVE
                else -> DrawType.LINE
            }
            updatePreview()
        }
    }

    private fun setupButtons() {
        okButton.setOnClickListener {
            configChangeListener?.invoke(selectedColor, selectedWidth, selectedDrawType)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun updatePreview() {
        // 更新绘图预览
        drawPreview.lineColor = selectedColor
        drawPreview.lineWidth = selectedWidth
        drawPreview.drawType = selectedDrawType
    }

    fun withConfig(color: Int, width: Float, drawType: DrawType): DrawConfigDialog {
        this.initialColor = color
        this.initialWidth = width
        this.initialDrawType = drawType
        this.selectedColor = color
        this.selectedWidth = width
        this.selectedDrawType = drawType
        return this
    }

    fun withListener(listener: (Int, Float, DrawType) -> Unit): DrawConfigDialog {
        this.configChangeListener = listener
        return this
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag ?: "drawConfig")
    }

    private inner class ColorAdapter(
        private val onColorSelected: (Int) -> Unit
    ) : BaseRecyclerAdapter<Int>(context) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<Int> {
            val view = mInflater.inflate(R.layout.item_color_grid, parent, false)
            return ColorViewHolder(view)
        }

        inner class ColorViewHolder(itemView: View) : BaseViewHolder<Int>(itemView) {
            val colorView: View = itemView.findViewById(R.id.colorItemView)
            val colorBackground: View = itemView.findViewById(R.id.colorBackground)

            override fun onBind(color: Int, position: Int) {
                colorView.setBackgroundColor(color)

                val isSelected = color == selectedColor
                val borderDrawable = if (isSelected) {
                    androidx.core.content.ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.color_item_selected_border
                    )
                } else {
                    null
                }
                colorBackground.background = borderDrawable

                colorBackground.setOnClickListener {
                    selectedColor = color
                    notifyDataSetChanged()
                    onColorSelected(color)
                    updatePreview()
                }
            }
        }
    }
}