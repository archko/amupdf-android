package cn.archko.pdf.dialogs

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cn.archko.pdf.R

/**
 * 颜色选择对话框
 * 样式：上面是颜色背景，中间是输入框，下面是四个ARGB滑块
 */
class ColorPickerDialog : DialogFragment() {

    private var initialColor: Int = Color.RED
    private var colorChangeListener: ((DialogFragment, Int) -> Unit)? = null

    private lateinit var colorPreview: View
    private lateinit var colorInput: EditText
    private lateinit var applyButton: Button
    private lateinit var alphaSeekBar: SeekBar
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar
    private lateinit var alphaValue: TextView
    private lateinit var redValue: TextView
    private lateinit var greenValue: TextView
    private lateinit var blueValue: TextView
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    private var isUpdatingFromSeekBar = false
    private var isUpdatingFromInput = false

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
        return inflater.inflate(R.layout.dialog_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSeekBars()
        setupInput()
        setupButtons()
        updateColor(initialColor)
    }

    private fun initViews(view: View) {
        colorPreview = view.findViewById(R.id.colorPreview)
        colorInput = view.findViewById(R.id.colorInput)
        applyButton = view.findViewById(R.id.applyButton)
        alphaSeekBar = view.findViewById(R.id.alphaSeekBar)
        redSeekBar = view.findViewById(R.id.redSeekBar)
        greenSeekBar = view.findViewById(R.id.greenSeekBar)
        blueSeekBar = view.findViewById(R.id.blueSeekBar)
        alphaValue = view.findViewById(R.id.alphaValue)
        redValue = view.findViewById(R.id.redValue)
        greenValue = view.findViewById(R.id.greenValue)
        blueValue = view.findViewById(R.id.blueValue)
        okButton = view.findViewById(R.id.okButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupSeekBars() {
        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingFromInput) {
                    isUpdatingFromSeekBar = true
                    updateColorFromSeekBars()
                    isUpdatingFromSeekBar = false
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        alphaSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        redSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        greenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        blueSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
    }

    private fun setupInput() {
        colorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingFromSeekBar && s != null) {
                    isUpdatingFromInput = true
                    try {
                        val colorStr = s.toString().trim()
                        if (colorStr.isNotEmpty()) {
                            val color = parseColor(colorStr)
                            updateColor(color)
                        }
                    } catch (e: Exception) {
                        // 忽略无效的颜色字符串
                    }
                    isUpdatingFromInput = false
                }
            }
        })

        applyButton.setOnClickListener {
            try {
                val colorStr = colorInput.text.toString().trim()
                if (colorStr.isNotEmpty()) {
                    val color = parseColor(colorStr)
                    updateColor(color)
                }
            } catch (e: Exception) {
                // 忽略无效的颜色字符串
            }
        }
    }

    private fun setupButtons() {
        okButton.setOnClickListener {
            colorChangeListener?.invoke(this, getCurrentColor())
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun updateColor(color: Int) {
        colorPreview.setBackgroundColor(color)

        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        alphaSeekBar.progress = alpha
        redSeekBar.progress = red
        greenSeekBar.progress = green
        blueSeekBar.progress = blue

        alphaValue.text = alpha.toString()
        redValue.text = red.toString()
        greenValue.text = green.toString()
        blueValue.text = blue.toString()

        if (!isUpdatingFromInput) {
            colorInput.setText(String.format("#%08X", color))
        }
    }

    private fun updateColorFromSeekBars() {
        val alpha = alphaSeekBar.progress
        val red = redSeekBar.progress
        val green = greenSeekBar.progress
        val blue = blueSeekBar.progress

        val color = Color.argb(alpha, red, green, blue)
        updateColor(color)
    }

    private fun getCurrentColor(): Int {
        val alpha = alphaSeekBar.progress
        val red = redSeekBar.progress
        val green = greenSeekBar.progress
        val blue = blueSeekBar.progress
        return Color.argb(alpha, red, green, blue)
    }

    private fun parseColor(colorStr: String): Int {
        return try {
            when {
                colorStr.startsWith("#") -> {
                    when (colorStr.length) {
                        // #RGB
                        4 -> {
                            val r = colorStr.substring(1, 2).toInt(16) * 17
                            val g = colorStr.substring(2, 3).toInt(16) * 17
                            val b = colorStr.substring(3, 4).toInt(16) * 17
                            Color.rgb(r, g, b)
                        }
                        // #ARGB
                        5 -> {
                            val a = colorStr.substring(1, 2).toInt(16) * 17
                            val r = colorStr.substring(2, 3).toInt(16) * 17
                            val g = colorStr.substring(3, 4).toInt(16) * 17
                            val b = colorStr.substring(4, 5).toInt(16) * 17
                            Color.argb(a, r, g, b)
                        }
                        // #RRGGBB
                        7 -> Color.parseColor(colorStr)
                        // #AARRGGBB
                        9 -> Color.parseColor(colorStr)
                        else -> throw IllegalArgumentException("Invalid color format")
                    }
                }

                colorStr.startsWith("0x") -> {
                    val hexStr = colorStr.substring(2)
                    when (hexStr.length) {
                        6 -> Color.parseColor("#$hexStr")
                        8 -> Color.parseColor("#$hexStr")
                        else -> throw IllegalArgumentException("Invalid color format")
                    }
                }

                else -> {
                    colorStr.toInt()
                }
            }
        } catch (e: Exception) {
            Color.RED
        }
    }

    fun withColor(color: Int): ColorPickerDialog {
        this.initialColor = color
        return this
    }

    fun withListener(listener: (DialogFragment, Int) -> Unit): ColorPickerDialog {
        this.colorChangeListener = listener
        return this
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag ?: "colorPicker")
    }
}
