package cn.archko.pdf.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentCreatePdfBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import coil3.load
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog
import java.io.File
import java.util.Collections

/**
 * @author: archko 2023/3/8 :14:34
 */
class PdfCreationFragment : DialogFragment(R.layout.fragment_create_pdf) {

    private lateinit var binding: FragmentCreatePdfBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerAdapter<File>
    private var oldPdfPath: String? = null
    private var txtPath: String? = null

    private var type: Int = TYPE_IMAGE
    private var bgColor = Color.WHITE

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Waiting...")
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val path = IntentFile.getPath(
                    requireActivity(),
                    result.data?.data
                )
                oldPdfPath = path
            }
        }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/pdf")
        pickPdf.launch(intent)
    }

    private fun createPdfFromImage() {
        val arr = arrayListOf<String>()
        adapter.data.forEach { arr.add(it.absolutePath) }
        var name = binding.pdfPath.editableText.toString()
        if (TextUtils.isEmpty(name)) {
            name = "new.pdf"
        }
        if (!name.endsWith(".pdf")) {
            name = "$name.pdf"
        }
        val path = FileUtils.getStorageDir("book").absolutePath + File.separator + name

        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.createPdfFromImages(
                    path,
                    arr
                )
            }
            if (result) {
                Toast.makeText(activity, R.string.edit_create_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.edit_create_error, Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    private fun createPdfFromTxt() {
        if (TextUtils.isEmpty(txtPath)) {
            Toast.makeText(activity, R.string.edit_create_select_file, Toast.LENGTH_SHORT).show()
            return
        }
        var name: String? = binding.pdfName.editableText.toString()
        if (TextUtils.isEmpty(name)) {
            name = "new.pdf"
        }
        var path = FileUtils.getStorageDir("book").absolutePath + File.separator + name

        val fontSize = binding.fontSlider.value
        val padding = Utils.dipToPixel(binding.paddingSlider.value)
        val lineSpace = binding.lineSlider.value

        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.createPdfUseSystemFromTxt(
                    activity,
                    binding.layoutTmp,
                    fontSize,
                    padding,
                    lineSpace,
                    bgColor,
                    txtPath,
                    path
                )
            }
            if (result) {
                Toast.makeText(activity, R.string.edit_create_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.edit_create_error, Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatePdfBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun updateUi() {
        if (type == TYPE_IMAGE) {
            binding.layoutImage.visibility = View.VISIBLE
            binding.layoutTxt.visibility = View.GONE
        } else {
            binding.layoutImage.visibility = View.GONE
            binding.layoutTxt.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        //binding.btnSelect.setOnClickListener { selectPdf() }
        binding.btnCreateFromImage.setOnClickListener { createPdfFromImage() }
        binding.btnAddImage.setOnClickListener { addImageItem() }

        binding.btnTxt.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                type = TYPE_TXT
                updateUi()
            }
        }
        binding.btnImage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                type = TYPE_IMAGE
                updateUi()
            }
        }

        binding.btnAddTxt.setOnClickListener { addTxtItem() }
        binding.btnCreateFromTxt.setOnClickListener { createPdfFromTxt() }

        adapter = object : BaseRecyclerAdapter<File>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<File> {
                val root = inflater.inflate(R.layout.item_image, parent, false)
                return ViewHolder(root)
            }
        }
        binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(MyItemTouchHelperCallBack())
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        updateUi()

        binding.fontSlider.value = 16f
        binding.paddingSlider.value = 20f
        binding.lineSlider.value = 1.2f

        binding.fontSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            binding.txtPreview.textSize = value
        })

        binding.paddingSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            val padding = Utils.dipToPixel(value)
            binding.txtPreview.setPadding(padding, padding, padding, padding)
        })

        binding.lineSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            binding.txtPreview.setLineSpacing(0f, value)
        })

        binding.txtPreview.setOnClickListener {
            ColorPickerDialog()
                .withColor(bgColor)
                .withListener { _, color ->
                    bgColor = color
                    binding.txtPreview.setBackgroundColor(color)
                }
                .show(requireActivity().supportFragmentManager, "colorPicker")
        }
    }

    inner class ViewHolder(root: View) : BaseViewHolder<File>(root) {

        var delete: View? = null
        var ivImage: ImageView? = null

        init {
            delete = root.findViewById(R.id.delete)
            ivImage = root.findViewById(R.id.ivImage)
        }

        override fun onBind(data: File, position: Int) {
            delete?.setOnClickListener { deleteItem(data, position) }
            ivImage?.load(data)
        }
    }

    private fun deleteItem(data: File, position: Int) {
        adapter.data.remove(data)
        adapter.notifyDataSetChanged()
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            //如果Uri为null，可能是多选了
            if (result?.resultCode == Activity.RESULT_OK) {
                val files = mutableListOf<File>()
                try {
                    val oneUri = result.data?.data
                    if (oneUri != null) {
                        val parseParams = IntentFile.getPath(requireContext(), oneUri)
                        if (parseParams != null) {
                            files.add(File(parseParams))
                        }
                    } else {
                        for (index in 0 until (result.data?.clipData?.itemCount ?: 0)) {
                            val uri = result.data?.clipData?.getItemAt(index)?.uri
                            if (uri != null) {
                                val parseParams = IntentFile.getPath(requireContext(), uri)
                                if (parseParams != null) {
                                    files.add(File(parseParams))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "handlePickFileResult", e)
                }
                // 按修改时间倒序排序
                files.sortByDescending { it.lastModified() }
                adapter.data.addAll(adapter.itemCount, files)
                adapter.notifyDataSetChanged()
            }
        }

    private fun addImageItem() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*")
        pickImage.launch(intent)
    }

    private val pickTxt =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val path = result.data?.data?.let { IntentFile.getPath(requireContext(), it) }
                txtPath = path
                binding.txtPath.text = txtPath
            }
        }

    private fun addTxtItem() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("text/*")
        pickTxt.launch(intent)
    }

    private fun setType(type: Int) {
        this.type = type
    }

    inner class MyItemTouchHelperCallBack : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            // 长按拖动，不可删除,可换位使用
            val dragFlags =
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            return makeMovementFlags(dragFlags, 0)
        }

        // 拖拽 排序item时调用
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val form = viewHolder.absoluteAdapterPosition
            val to = target.absoluteAdapterPosition

            var isMove = false

            if (adapter.data != null) {
                Collections.swap(adapter.data, form, to)
                adapter.notifyItemMoved(form, to)
                isMove = true
            }
            return isMove
        }

        // 轻拖滑动出recyclerview后调用（可做删除item）
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }
    }

    companion object {

        const val TAG = "CreatePdfFragment"
        const val TYPE_TXT = 0
        const val TYPE_IMAGE = 1

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = PdfCreationFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "create_dialog")
        }
    }
}