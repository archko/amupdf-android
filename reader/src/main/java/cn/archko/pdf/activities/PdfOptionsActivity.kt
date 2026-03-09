package cn.archko.pdf.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.Logcat.d
import cn.archko.pdf.core.common.PdfOptionKeys
import cn.archko.pdf.core.common.PdfOptionRepository
import cn.archko.pdf.core.common.PdfOptionRepository.getAutoScan
import cn.archko.pdf.core.common.PdfOptionRepository.getAutocrop
import cn.archko.pdf.core.common.PdfOptionRepository.getColorMode
import cn.archko.pdf.core.common.PdfOptionRepository.getDirsFirst
import cn.archko.pdf.core.common.PdfOptionRepository.getFullscreen
import cn.archko.pdf.core.common.PdfOptionRepository.getKeepOn
import cn.archko.pdf.core.common.PdfOptionRepository.getOrientation
import cn.archko.pdf.core.common.PdfOptionRepository.getScanFolder
import cn.archko.pdf.core.common.PdfOptionRepository.getShowExtension
import cn.archko.pdf.core.common.PdfOptionRepository.setAutoScan
import cn.archko.pdf.core.common.PdfOptionRepository.setAutocrop
import cn.archko.pdf.core.common.PdfOptionRepository.setColorMode
import cn.archko.pdf.core.common.PdfOptionRepository.setDecodeBlock
import cn.archko.pdf.core.common.PdfOptionRepository.setDirsFirst
import cn.archko.pdf.core.common.PdfOptionRepository.setFullscreen
import cn.archko.pdf.core.common.PdfOptionRepository.setImageOcr
import cn.archko.pdf.core.common.PdfOptionRepository.setKeepOn
import cn.archko.pdf.core.common.PdfOptionRepository.setOrientation
import cn.archko.pdf.core.common.PdfOptionRepository.setScanFolder
import cn.archko.pdf.core.common.PdfOptionRepository.setShowExtension
import cn.archko.pdf.core.common.PdfOptionRepository.setStyle
import cn.archko.pdf.core.common.ScanEvent
import cn.archko.pdf.core.utils.ColorUtil
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.entity.padding
import com.google.android.material.appbar.MaterialToolbar
import vn.chungha.flowbus.busEvent

/**
 * @author: archko 2018/12/12 :15:43
 */
class PdfOptionsActivity : FragmentActivity() {
    private var adapter: BaseRecyclerAdapter<Prefs>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preferences)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(ColorItemDecoration(this))

        adapter = object : BaseRecyclerAdapter<Prefs>(this) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<Prefs> {
                if (viewType == TYPE_LIST) {
                    val view = mInflater.inflate(R.layout.list_preferences, parent, false)
                    return PrefsListHolder(view)
                } else if (viewType == TYPE_EDIT) {
                    val view = mInflater.inflate(R.layout.list_preferences, parent, false)
                    return PrefsEditHolder(view)
                } else {
                    val view = mInflater.inflate(R.layout.check_preferences, parent, false)
                    return PrefsCheckHolder(view)
                }
            }

            override fun getItemViewType(position: Int): Int {
                val prefs = data[position]!!
                return prefs.type
            }
        }

        initPrefsFromMMkv()
        adapter?.setData(prefsList)
        recyclerView.adapter = adapter

        PdfOptionRepository.setLeftPadding(Utils.dipToPixel(padding))
        PdfOptionRepository.setRightPadding(Utils.dipToPixel(padding))
        PdfOptionRepository.setTopPadding(Utils.dipToPixel(padding))
        PdfOptionRepository.setBottomPadding(Utils.dipToPixel(padding))
    }

    private val prefsList: MutableList<Prefs?> = ArrayList()

    private fun initPrefsFromMMkv() {
        var prefs = Prefs(
            TYPE_LIST,
            getString(R.string.opts_orientation),
            getString(R.string.opts_orientation),
            PdfOptionKeys.PREF_ORIENTATION,
            resources.getStringArray(R.array.opts_orientations),
            resources.getStringArray(R.array.opts_orientation_labels),
            getOrientation()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_fullscreen),
            getString(R.string.opts_fullscreen),
            PdfOptionKeys.PREF_FULLSCREEN,
            getFullscreen()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_autocrop),
            getString(R.string.opts_autocrop_summary),
            PdfOptionKeys.PREF_AUTOCROP,
            getAutocrop()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_keep_on),
            getString(R.string.opts_keep_on),
            PdfOptionKeys.PREF_KEEP_ON,
            getKeepOn()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_dirs_first),
            getString(R.string.opts_dirs_first),
            PdfOptionKeys.PREF_DIRS_FIRST,
            getDirsFirst()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_show_extension),
            getString(R.string.opts_show_extension),
            PdfOptionKeys.PREF_SHOW_EXTENSION,
            getShowExtension()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_CHECK,
            getString(R.string.opts_scan),
            getString(R.string.opts_scan),
            PdfOptionKeys.PREF_AUTO_SCAN,
            getAutoScan()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_EDIT,
            getString(R.string.opts_scan_folder),
            getString(R.string.opts_scan_folder),
            PdfOptionKeys.PREF_SCAN_FOLDER,
            getScanFolder()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_LIST, getString(R.string.opts_list_style), getString(R.string.opts_list_style),
            PdfOptionKeys.PREF_STYLE,
            resources.getStringArray(R.array.opts_list_styles),
            resources.getStringArray(R.array.opts_list_style_labels),
            PdfOptionRepository.getStyle()
        );
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_LIST, getString(R.string.opts_color_mode), getString(R.string.opts_color_mode),
            PdfOptionKeys.PREF_COLORMODE,
            resources.getStringArray(R.array.opts_color_modes),
            resources.getStringArray(R.array.opts_color_mode_labels),
            getColorMode()
        )
        prefsList.add(prefs)

        /*prefs = Prefs(
            TYPE_LIST,
            getString(R.string.opts_decode_block_title),
            getString(R.string.opts_decode_block_title),
            PdfOptionKeys.PREF_DECODE_BLOCK,
            resources.getStringArray(R.array.opts_decode_block),
            resources.getStringArray(R.array.opts_decode_block_label),
            getDecodeBlock()
        )
        prefsList.add(prefs)*/
    }

    private class Prefs {
        var type: Int = TYPE_CHECK
        var labels: Array<String>? = null
        var vals: Array<String>? = null
        var key: String
        var title: String
        var summary: String
        var value: Any? = null
        var index: Int = 0

        constructor(type: Int, title: String, summary: String, key: String, value: Any?) {
            this.type = type
            this.title = title
            this.summary = summary
            this.key = key
            this.value = value
        }

        constructor(
            type: Int,
            title: String,
            summary: String,
            key: String,
            vals: Array<String>,
            labels: Array<String>,
            index: Int
        ) {
            this.type = type
            this.title = title
            this.summary = summary
            this.key = key
            this.vals = vals
            this.labels = labels
            this.index = index
        }

        override fun toString(): String {
            return "Prefs{" +
                    "type=" + type +
                    ", labels=" + labels.contentToString() +
                    ", vals=" + vals.contentToString() +
                    ", key='" + key + '\'' +
                    ", title='" + title + '\'' +
                    ", summary='" + summary + '\'' +
                    ", val=" + value +
                    ", index=" + index +
                    '}'
        }
    }

    private open class AbsPrefsHolder(itemView: View) : BaseViewHolder<Prefs>(itemView) {
        var title: TextView =
            itemView.findViewById(R.id.title)
        var summary: TextView =
            itemView.findViewById(R.id.summary)

        override fun onBind(data: Prefs, position: Int) {
            title.text = data.title
            summary.text = data.summary
        }
    }

    private inner class PrefsListHolder(itemView: View) : AbsPrefsHolder(itemView) {
        override fun onBind(data: Prefs, position: Int) {
            super.onBind(data, position)
            summary.text = data.labels?.get(data.index) ?: ""
            itemView.setOnClickListener { v: View? ->
                if (TextUtils.equals(data.key, PdfOptionKeys.PREF_COLORMODE)) {
                    showColorModeDialog(data, summary)
                } else {
                    showListDialog(data, summary)
                }
            }
        }

        fun showListDialog(data: Prefs, summary: TextView) {
            val builder = AlertDialog.Builder(
                this@PdfOptionsActivity,
                androidx.appcompat.R.style.Theme_AppCompat_Dialog
            )
            builder.setTitle(data.title)
            builder.setSingleChoiceItems(
                data.labels,
                data.index
            ) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                summary.text = data.labels?.get(which) ?: ""
                data.index = which
                setCheckListVal(data.key, data.vals?.get(which) ?: false)
            }
            builder.create().show()
        }

        fun showColorModeDialog(data: Prefs, summary: TextView) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_color_mode, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.color_mode_list)
            val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
            titleView.text = data.title

            recyclerView.layoutManager = LinearLayoutManager(this@PdfOptionsActivity)
            recyclerView.addItemDecoration(ColorItemDecoration(this@PdfOptionsActivity))

            val adapter =
                ColorModeAdapter(data.labels ?: emptyArray(), data.index) { selectedIndex ->
                    summary.text = data.labels?.get(selectedIndex) ?: ""
                    data.index = selectedIndex
                    setCheckListVal(data.key, data.vals?.get(selectedIndex) ?: false)
                }
            recyclerView.adapter = adapter

            val dialog = AlertDialog.Builder(this@PdfOptionsActivity)
                .setView(dialogView)
                .create()

            adapter.setDialog(dialog)
            dialog.show()
        }

        fun setCheckListVal(key: String?, value: Any) {
            if (TextUtils.equals(key, PdfOptionKeys.PREF_ORIENTATION)) {
                setOrientation(value.toString().toInt())
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_COLORMODE)) {
                setColorMode(value.toString().toInt())
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_STYLE)) {
                setStyle(value.toString().toInt())
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_DECODE_BLOCK)) {
                setDecodeBlock(value.toString().toInt())
            }
        }
    }

    private inner class ColorModeAdapter(
        private val labels: Array<String>,
        private var selectedIndex: Int,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ColorModeAdapter.ViewHolder>() {

        private var dialog: AlertDialog? = null

        fun setDialog(dialog: AlertDialog) {
            this.dialog = dialog
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val labelView: TextView = itemView.findViewById(R.id.color_mode_label)
            val previewView: View = itemView.findViewById(R.id.color_preview)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        if (position == labels.size - 1) {
                            // 显示自定义矩阵弹窗
                            showCustomMatrixDialog(itemView.context)
                            return@setOnClickListener
                        }

                        selectedIndex = position
                        notifyDataSetChanged()
                        onItemSelected(position)
                        dialog?.dismiss()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_color_mode, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.labelView.text = labels[position]

            val previewColor = getPreviewColorForMode(position)
            holder.previewView.setBackgroundColor(previewColor)

            holder.itemView.isSelected = position == selectedIndex
        }

        override fun getItemCount(): Int = labels.size

        private fun showCustomMatrixDialog(context: Context) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_custom_matrix, null)
            val previewView = dialogView.findViewById<View>(R.id.preview_view)
            val matrixContainer = dialogView.findViewById<LinearLayout>(R.id.matrix_container)

            // 尝试加载已保存的自定义矩阵，如果没有则使用苹果绿底的矩阵（模式3）
            val savedMatrix = PdfOptionRepository.getCustomColorMatrix()
            val defaultMatrix = savedMatrix ?: (ColorUtil.getColorMode(3) ?: floatArrayOf(
                0.83f, 0.00f, 0.00f, 0.0f, 0.0f,
                0.00f, 0.96f, 0.00f, 0.0f, 0.0f,
                0.00f, 0.00f, 0.84f, 0.0f, 0.0f,
                0.00f, 0.00f, 0.00f, 1.0f, 0.0f
            ))

            val editTexts = mutableListOf<EditText>()

            // 创建4行5列的矩阵输入框
            val rowLabels = arrayOf("R行", "G行", "B行", "A行")
            val columnLabels = arrayOf("R乘数", "G乘数", "B乘数", "A乘数", "偏移量")

            for (row in 0 until 4) {
                val rowLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 8.dpToPx(context)
                    }
                }

                // 添加行标签
                val rowLabel = TextView(context).apply {
                    text = rowLabels[row]
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 1f
                    }
                    textSize = 14f
                    gravity = Gravity.CENTER_VERTICAL
                }
                rowLayout.addView(rowLabel)

                // 添加5个输入框
                for (col in 0 until 5) {
                    val editText = EditText(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1f
                            marginStart = 4.dpToPx(context)
                        }
                        hint = columnLabels[col]
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                        setText(defaultMatrix[row * 5 + col].toString())
                        textSize = 12f
                    }
                    editTexts.add(editText)
                    rowLayout.addView(editText)
                }

                matrixContainer.addView(rowLayout)
            }

            fun updatePreview() {
                try {
                    val matrix = FloatArray(20)
                    for (i in 0 until 20) {
                        matrix[i] = editTexts[i].text.toString().toFloatOrNull() ?: 0f
                    }
                    val previewColor = applyColorMatrix(0xFFFFFFFF.toInt(), matrix)
                    previewView.setBackgroundColor(previewColor)
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }

            updatePreview()

            editTexts.forEach { editText ->
                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        updatePreview()
                    }
                })
            }

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.btn_reset).setOnClickListener {
                val appleGreenMatrix = ColorUtil.getColorMode(3) ?: floatArrayOf(
                    0.83f, 0.00f, 0.00f, 0.0f, 0.0f,
                    0.00f, 0.96f, 0.00f, 0.0f, 0.0f,
                    0.00f, 0.00f, 0.84f, 0.0f, 0.0f,
                    0.00f, 0.00f, 0.00f, 1.0f, 0.0f
                )
                for (i in 0 until 20) {
                    editTexts[i].setText(appleGreenMatrix[i].toString())
                }
                updatePreview()
            }

            dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btn_save).setOnClickListener {
                try {
                    val matrix = FloatArray(20)
                    for (i in 0 until 20) {
                        matrix[i] = editTexts[i].text.toString().toFloatOrNull() ?: 0f
                    }

                    PdfOptionRepository.setCustomColorMatrix(matrix)

                    android.widget.Toast.makeText(
                        context,
                        "自定义矩阵已保存",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // 更新预览颜色
                    selectedIndex = labels.size - 1
                    notifyDataSetChanged()
                    onItemSelected(labels.size - 1)

                    dialog.dismiss()
                    this@ColorModeAdapter.dialog?.dismiss()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "矩阵值无效",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog.show()
        }

        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }

        private fun getPreviewColorForMode(mode: Int): Int {
            val matrix = ColorUtil.getColorMode(mode)

            val baseColor = 0xFFFFFFFF.toInt()

            // 如果没有矩阵（如正常模式或自定义矩阵），返回基础颜色
            if (matrix == null) {
                return when (mode) {
                    0 -> baseColor // Normal - 白色
                    7 -> {
                        // 自定义矩阵模式：如果用户没有设置自定义矩阵，使用模式3（苹果绿底）作为预览
                        val fallbackMatrix = ColorUtil.getColorMode(7)
                        if (fallbackMatrix != null) {
                            applyColorMatrix(baseColor, fallbackMatrix)
                        } else {
                            baseColor
                        }
                    }
                    else -> baseColor
                }
            }

            return applyColorMatrix(baseColor, matrix)
        }

        private fun applyColorMatrix(color: Int, matrix: FloatArray): Int {
            val r = Color.red(color).toFloat()
            val g = Color.green(color).toFloat()
            val b = Color.blue(color).toFloat()
            val a = Color.alpha(color).toFloat()

            // 矩阵是 5x4 布局，每行5个元素，共4行
            // 转换公式：R' = R*m[0] + G*m[1] + B*m[2] + A*m[3] + m[4]
            //           G' = R*m[5] + G*m[6] + B*m[7] + A*m[8] + m[9]
            //           B' = R*m[10] + G*m[11] + B*m[12] + A*m[13] + m[14]
            //           A' = R*m[15] + G*m[16] + B*m[17] + A*m[18] + m[19]

            val newR =
                (r * matrix[0] + g * matrix[1] + b * matrix[2] + a * matrix[3] + matrix[4]).coerceIn(
                    0f,
                    255f
                )
            val newG =
                (r * matrix[5] + g * matrix[6] + b * matrix[7] + a * matrix[8] + matrix[9]).coerceIn(
                    0f,
                    255f
                )
            val newB =
                (r * matrix[10] + g * matrix[11] + b * matrix[12] + a * matrix[13] + matrix[14]).coerceIn(
                    0f,
                    255f
                )
            val newA =
                (r * matrix[15] + g * matrix[16] + b * matrix[17] + a * matrix[18] + matrix[19]).coerceIn(
                    0f,
                    255f
                )

            return Color.argb(newA.toInt(), newR.toInt(), newG.toInt(), newB.toInt())
        }
    }

    private class PrefsCheckHolder(itemView: View) : AbsPrefsHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        override fun onBind(data: Prefs, position: Int) {
            super.onBind(data, position)
            setCheckVal(data.key, data.value as Boolean)
            checkBox.isChecked = data.value as Boolean
            checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                setCheckVal(
                    data.key,
                    isChecked
                )
            }
        }

        fun setCheckVal(key: String?, value: Boolean) {
            if (TextUtils.equals(key, PdfOptionKeys.PREF_OCR)) {
                setImageOcr(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_FULLSCREEN)) {
                setFullscreen(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_AUTOCROP)) {
                setAutocrop(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_KEEP_ON)) {
                setKeepOn(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_DIRS_FIRST)) {
                setDirsFirst(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_SHOW_EXTENSION)) {
                setShowExtension(value)
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_AUTO_SCAN)) {
                setAutoScan(value)
                if (value) {
                    busEvent(ScanEvent(Event.ACTION_SCAN, null))
                } else {
                    busEvent(ScanEvent(Event.ACTION_DONOT_SCAN, null))
                }
            }
        }
    }

    private inner class PrefsEditHolder(itemView: View) : AbsPrefsHolder(itemView) {
        override fun onBind(data: Prefs, position: Int) {
            super.onBind(data, position)
            d(String.format("bind:%s", data))
            title.text = data.title
            summary.text = String.format("%s:%s", data.summary, data.value)
            itemView.setOnClickListener { v: View? -> showEditDialog(data, summary) }
        }

        fun showEditDialog(data: Prefs, summary: TextView?) {
            val builder = AlertDialog.Builder(this@PdfOptionsActivity)
            val editText = EditText(this@PdfOptionsActivity)
            editText.setText(data.value.toString())
            builder.setTitle(data.title)
            builder.setView(editText)
            builder.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                val path = editText.text.toString()
                data.value = path
                setEditVal(data.key, path)
                adapter!!.notifyDataSetChanged()
                dialog.dismiss()
            }
            builder.setNegativeButton(
                "Cancel"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            builder.create().show()
        }

        fun setEditVal(key: String, value: String) {
            d(String.format("key:%s-%s", key, value))
            if (TextUtils.equals(key, PdfOptionKeys.PREF_SCAN_FOLDER)) {
                setScanFolder(value)
            }
        }
    }

    companion object {
        const val TAG: String = "PdfOptionsActivity"

        private const val TYPE_CHECK = 0
        private const val TYPE_LIST = 1
        private const val TYPE_EDIT = 2

        fun start(context: Context) {
            context.startActivity(Intent(context, PdfOptionsActivity::class.java))
        }
    }
}
