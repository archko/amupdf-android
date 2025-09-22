package cn.archko.pdf.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.common.PdfOptionKeys
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.PdfOptionRepository.getAutoScan
import cn.archko.pdf.common.PdfOptionRepository.getAutocrop
import cn.archko.pdf.common.PdfOptionRepository.getColorMode
import cn.archko.pdf.common.PdfOptionRepository.getDecodeBlock
import cn.archko.pdf.common.PdfOptionRepository.getDirsFirst
import cn.archko.pdf.common.PdfOptionRepository.getFullscreen
import cn.archko.pdf.common.PdfOptionRepository.getKeepOn
import cn.archko.pdf.common.PdfOptionRepository.getOrientation
import cn.archko.pdf.common.PdfOptionRepository.getScanFolder
import cn.archko.pdf.common.PdfOptionRepository.getShowExtension
import cn.archko.pdf.common.PdfOptionRepository.setAutoScan
import cn.archko.pdf.common.PdfOptionRepository.setAutocrop
import cn.archko.pdf.common.PdfOptionRepository.setColorMode
import cn.archko.pdf.common.PdfOptionRepository.setDecodeBlock
import cn.archko.pdf.common.PdfOptionRepository.setDirsFirst
import cn.archko.pdf.common.PdfOptionRepository.setFullscreen
import cn.archko.pdf.common.PdfOptionRepository.setImageOcr
import cn.archko.pdf.common.PdfOptionRepository.setKeepOn
import cn.archko.pdf.common.PdfOptionRepository.setOrientation
import cn.archko.pdf.common.PdfOptionRepository.setScanFolder
import cn.archko.pdf.common.PdfOptionRepository.setShowExtension
import cn.archko.pdf.common.PdfOptionRepository.setStyle
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.Logcat.d
import cn.archko.pdf.core.common.ScanEvent
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

        /*prefs = new Prefs(TYPE_LIST, getString(R.string.opts_list_style), getString(R.string.opts_list_style),
                PdfOptionKeys.PREF_STYLE,
                getResources().getStringArray(R.array.opts_list_styles),
                getResources().getStringArray(R.array.opts_list_style_labels),
                PdfOptionRepository.INSTANCE.getStyle());
        prefsList.add(prefs);*/
        prefs = Prefs(
            TYPE_LIST, getString(R.string.opts_color_mode), getString(R.string.opts_color_mode),
            PdfOptionKeys.PREF_COLORMODE,
            resources.getStringArray(R.array.opts_color_modes),
            resources.getStringArray(R.array.opts_color_mode_labels),
            getColorMode()
        )
        prefsList.add(prefs)

        prefs = Prefs(
            TYPE_LIST,
            getString(R.string.opts_decode_block_title),
            getString(R.string.opts_decode_block_title),
            PdfOptionKeys.PREF_DECODE_BLOCK,
            resources.getStringArray(R.array.opts_decode_block),
            resources.getStringArray(R.array.opts_decode_block_label),
            getDecodeBlock()
        )
        ///prefsList.add(prefs)
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
            itemView.setOnClickListener { v: View? -> showListDialog(data, summary) }
        }

        fun showListDialog(data: Prefs, summary: TextView) {
            val builder = AlertDialog.Builder(this@PdfOptionsActivity, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
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
