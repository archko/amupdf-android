package cn.archko.pdf.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.entity.DrawType

/**
 * 绘图配置对话框 - 融合颜色、线宽和绘制类型选择
 */
class DrawConfigDialog : DialogFragment() {

    private var initialColor: Int = Color.RED
    private var initialWidth: Float = 4f
    private var initialDrawType: DrawType = DrawType.LINE
    private var configChangeListener: ((Int, Float, DrawType) -> Unit)? = null

    private lateinit var colorPreview: View
    private lateinit var widthSeekBar: SeekBar
    private lateinit var widthValueText: TextView
    private lateinit var linePreview: View
    private lateinit var curvePreview: View
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
        val themeId = android.R.style.Theme_Material_Dialog
        setStyle(STYLE_NO_FRAME, themeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        colorPreview = view.findViewById(R.id.colorPreview)
        widthSeekBar = view.findViewById(R.id.widthSeekBar)
        widthValueText = view.findViewById(R.id.widthValueText)
        linePreview = view.findViewById(R.id.linePreview)
        curvePreview = view.findViewById(R.id.curvePreview)
        drawTypeRadioGroup = view.findViewById(R.id.drawTypeRadioGroup)
        lineRadioButton = view.findViewById(R.id.lineRadioButton)
        curveRadioButton = view.findViewById(R.id.curveRadioButton)
        colorsRecyclerView = view.findViewById(R.id.colorsRecyclerView)
        okButton = view.findViewById(R.id.okButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupColorGrid() {
        colorsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)
        colorsRecyclerView.adapter = ColorAdapter(colors) { color ->
            selectedColor = color
            updatePreview()
        }
    }

    private fun setupWidthSeekBar() {
        widthSeekBar.max = 20 // 1-20px
        widthSeekBar.progress = selectedWidth.toInt()
        widthValueText.text = "${selectedWidth.toInt()} px"

        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    selectedWidth = progress.toFloat().coerceAtLeast(1f)
                    widthValueText.text = "${selectedWidth.toInt()} px"
                    updatePreview()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupDrawTypeSelection() {
        // 设置初始选择
        when (selectedDrawType) {
            DrawType.LINE -> lineRadioButton.isChecked = true
            DrawType.CURVE -> curveRadioButton.isChecked = true
        }

        drawTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDrawType = when (checkedId) {
                R.id.lineRadioButton -> DrawType.LINE
                R.id.curveRadioButton -> DrawType.CURVE
                else -> DrawType.LINE
            }
            updatePreview()
        }

        // 为预览区域添加点击事件
        linePreview.setOnClickListener {
            lineRadioButton.isChecked = true
            selectedDrawType = DrawType.LINE
            updatePreview()
        }

        curvePreview.setOnClickListener {
            curveRadioButton.isChecked = true
            selectedDrawType = DrawType.CURVE
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
        // 更新颜色预览
        colorPreview.setBackgroundColor(selectedColor)

        // 更新线宽和绘制类型预览
        // 这里可以在预览区域绘制线条，但需要自定义View
        // 暂时只更新颜色预览
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
        private val colorList: List<Int>,
        private val onColorSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val colorView: View = itemView.findViewById(R.id.colorItemView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color_grid, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = colorList[position]
            holder.colorView.setBackgroundColor(color)

            // 添加选中状态边框
            val isSelected = color == selectedColor
            val borderDrawable = if (isSelected) {
                androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.color_item_selected_border
                )
            } else {
                androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.color_item_border
                )
            }
            holder.colorView.background = borderDrawable

            holder.colorView.setOnClickListener {
                selectedColor = color
                notifyDataSetChanged()
                onColorSelected(color)
                updatePreview()
            }
        }

        override fun getItemCount(): Int = colorList.size
    }
}