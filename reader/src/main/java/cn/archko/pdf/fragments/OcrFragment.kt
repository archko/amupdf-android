package cn.archko.pdf.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.pdf.R
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.StreamUtils
import cn.archko.pdf.databinding.FragmentOcrBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tencent.ppocrv5ncnn.BasePolygonResultModel
import com.tencent.ppocrv5ncnn.PPOCRv5Ncnn
import java.io.File
import java.io.IOException

/**
 * OCR Fragment for recognizing text from images
 * Using PPOCRv5Ncnn library
 */
class OcrFragment : DialogFragment() {

    private lateinit var binding: FragmentOcrBinding

    private var bitmap: Bitmap? = null
    private var path: String? = null
    private var name: String = "ocr"

    private var ocrManager: PPOCRv5Ncnn? = null
    private var ocrResultString: String? = null
    private var polygonResults: List<BasePolygonResultModel>? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<View>? = null

    companion object {
        private const val ARG_BITMAP_KEY = "bitmap_key"
        private const val ARG_PATH = "path"

        fun showOcrDialog(activity: FragmentActivity?, key: String?): OcrFragment {
            val fragment = OcrFragment()
            val args = Bundle()
            args.putString(ARG_BITMAP_KEY, key)
            fragment.arguments = args

            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("ocr_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            fragment.show(ft!!, "ocr_dialog")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var themeId = R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

        parseArguments()
        initOcrManager()
    }

    private fun initOcrManager() {
        ocrManager = PPOCRv5Ncnn()
        val ret =
            ocrManager?.loadModel(requireActivity().assets, 0, 2, 0) // default model, size, cpu/gpu
        if (!ret!!) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireActivity(), "OCR initialization failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.back.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        /*binding.btnOcr.setOnClickListener {
            performOcr()
        }*/

        binding.btnSave.setOnClickListener {
            saveResult()
        }

        binding.btnCopy.setOnClickListener {
            copyResult()
        }

        // Initialize BottomSheetBehavior
        val bottomSheet = binding.bottomSheet
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        mBottomSheetBehavior?.peekHeight = 150

        displayImage()
        performOcr()
    }

    private fun parseArguments() {
        arguments?.let { args ->
            val bitmapKey = args.getString(ARG_BITMAP_KEY)
            if (!TextUtils.isEmpty(bitmapKey)) {
                bitmap = BitmapCache.getInstance().getBitmap(bitmapKey!!)
            }
        }
    }

    private fun displayImage() {
        bitmap?.let {
            binding.imageView.setImageBitmap(it)
        }
    }

    private fun performOcr() {
        if (ocrManager == null || bitmap == null) {
            Toast.makeText(requireActivity(), "OCR not ready", Toast.LENGTH_SHORT).show()
            return
        }

        //binding.btnOcr.isEnabled = false
        //binding.btnOcr.text = "Processing..."

        AppExecutors.instance.diskIO().execute {
            ocrResultString = ocrManager?.detectAndRecognize(bitmap)
            polygonResults = parseResult(ocrResultString ?: "")

            requireActivity().runOnUiThread {
                displayResults()
                //binding.btnOcr.isEnabled = true
                //binding.btnOcr.text = getString(R.string.ocr_recognize)
                binding.btnSave.isEnabled = !polygonResults.isNullOrEmpty()
                binding.btnCopy.isEnabled = !polygonResults.isNullOrEmpty()
            }
        }
    }

    private fun displayResults() {
        val results = polygonResults
        if (results.isNullOrEmpty()) {
            binding.textResult.text = "No text detected"
            binding.overlayView.setPolygonListInfo(null, bitmap?.width ?: 0, bitmap?.height ?: 0)
            return
        }

        val textResult = StringBuilder()
        results.forEach { result ->
            textResult.append(result.name).append("\n")
        }

        binding.textResult.text = textResult.toString()
        binding.overlayView.setPolygonListInfo(results, bitmap?.width ?: 0, bitmap?.height ?: 0)

        // Expand bottom sheet when results are available
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun parseResult(result: String): List<BasePolygonResultModel> {
        val modelList = mutableListOf<BasePolygonResultModel>()
        if (result.isEmpty()) return modelList

        val items = result.split(";")
        for (item in items) {
            val parts = item.split(",")
            if (parts.size >= 5) {
                val x = parts[0].toIntOrNull() ?: continue
                val y = parts[1].toIntOrNull() ?: continue
                val w = parts[2].toIntOrNull() ?: continue
                val h = parts[3].toIntOrNull() ?: continue
                val text = parts[4]

                val model = BasePolygonResultModel().apply {
                    setRect(Rect(x, y, x + w, y + h))
                    setName(text)
                    confidence = 1.0f
                }
                modelList.add(model)
            }
        }

        // Group by lines like in MainActivity
        modelList.sortBy { it.getRect(1.0f, Point(0, 0)).top }

        val lines = mutableListOf<MutableList<BasePolygonResultModel>>()
        for (model in modelList) {
            var added = false
            for (line in lines) {
                if (line.isNotEmpty()) {
                    val avgY =
                        line.sumOf { it.getRect(1.0f, Point(0, 0)).centerY().toInt() } / line.size
                    if (Math.abs(model.getRect(1.0f, Point(0, 0)).centerY() - avgY) < 30) {
                        line.add(model)
                        added = true
                        break
                    }
                }
            }
            if (!added) {
                lines.add(mutableListOf(model))
            }
        }

        // Combine each line
        val combinedList = mutableListOf<BasePolygonResultModel>()
        for (line in lines) {
            line.sortBy { it.getRect(1.0f, Point(0, 0)).left }
            val combinedText = line.joinToString(" ") { it.name }
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = 0
            var maxY = 0
            for (model in line) {
                val r = model.getRect(1.0f, Point(0, 0))
                minX = minOf(minX, r.left)
                minY = minOf(minY, r.top)
                maxX = maxOf(maxX, r.right)
                maxY = maxOf(maxY, r.bottom)
            }
            val combinedModel = BasePolygonResultModel().apply {
                setRect(Rect(minX, minY, maxX, maxY))
                setName(combinedText.trim())
                confidence = 1.0f
            }
            combinedList.add(combinedModel)
        }

        return combinedList
    }

    private fun saveResult() {
        val results = polygonResults
        if (results.isNullOrEmpty()) {
            Toast.makeText(requireActivity(), "No OCR result to save", Toast.LENGTH_SHORT).show()
            return
        }

        val textResult = StringBuilder()
        results.forEach { result ->
            textResult.append(result.name).append("\n")
        }

        val dir = FileUtils.getStorageDir("amupdf")
        if (dir != null && dir.exists()) {
            val filePath = dir.absolutePath + File.separator + name + ".txt"
            try {
                StreamUtils.copyStringToFile(textResult.toString(), filePath)
                Toast.makeText(requireActivity(), "Saved to: $filePath", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(requireActivity(), "Save failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun copyResult() {
        val results = polygonResults
        if (results.isNullOrEmpty()) {
            Toast.makeText(requireActivity(), "No OCR result to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val textResult = StringBuilder()
        results.forEach { result ->
            textResult.append(result.name).append("\n")
        }

        val clipboardManager =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("OCR Result", textResult.toString())
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(requireActivity(), "OCR result copied to clipboard", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}