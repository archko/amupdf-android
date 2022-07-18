package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.R
import cn.archko.pdf.adapters.MuPDFTextAdapter
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.databinding.TextStyleBinding
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader


/**
 * @author: archko 2022/7/11 :9:49 上午
 */
class TextActivity : AppCompatActivity() {

    private var path: String? = null
    private var mUri: Uri? = null
    private var sensorHelper: SensorHelper? = null
    private val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    protected val pdfViewModel: PDFViewModel = PDFViewModel()

    private var mStyleControls: View? = null
    private lateinit var binding: TextStyleBinding
    private var mControllerLayout: RelativeLayout? = null
    private var recyclerView: RecyclerView? = null

    private var mStyleHelper: StyleHelper? = null
    private var adapter: MuPDFTextAdapter? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null)
        }
        sensorHelper = SensorHelper(this)
        initIntent()
        loadBookmark()

        if (TextUtils.isEmpty(path)) {
            return
        }

        mStyleHelper = StyleHelper(this, preferencesRepository)

        var margin = window.decorView.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
        val finalMargin = margin
        val gestureDetector = GestureDetector(this, object :
            GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                showReflowConfigMenu()
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val documentView = recyclerView!!
                val top = documentView.height / 4
                val bottom = documentView.height * 3 / 4

                val rs: Boolean = scrollPage(e.y.toInt(), top, bottom, finalMargin)
                return true
            }
        })

        recyclerView = RecyclerView(this)
        mControllerLayout = RelativeLayout(this)
        mControllerLayout!!.addView(recyclerView)
        setContentView(mControllerLayout)

        recyclerView?.run {
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            //setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })

            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }

        val scope = CoroutineScope(Job() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
        adapter = MuPDFTextAdapter(this, mStyleHelper)
        recyclerView?.adapter = adapter

        scope.launch {
            val reflowBeans = withContext(Dispatchers.IO) {
                readString(path!!)
            }
            adapter?.data = reflowBeans
            adapter?.notifyDataSetChanged()
        }
        lifecycleScope.launchWhenCreated {
            //viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow {
                emit(readString(path!!))
            }.collectLatest { reflowBeans ->
                adapter?.data = reflowBeans
                adapter?.notifyDataSetChanged()
            }
            //}
        }
    }

    fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            var scrollY = recyclerView!!.scrollY
            scrollY -= recyclerView!!.height
            recyclerView!!.scrollBy(0, scrollY + margin)
            return true
        } else if (y > bottom) {
            var scrollY = recyclerView!!.scrollY
            scrollY += recyclerView!!.height
            recyclerView!!.scrollBy(0, scrollY - margin)
            return true
        }
        return false
    }

    private fun loadBookmark() {
        lifecycleScope.launch {
            val bookProgress =
                path?.let { pdfViewModel.loadBookProgressByPath(it, preferencesRepository) }
        }
    }

    override fun onPause() {
        super.onPause()
        //sensorHelper.onPause();
        /*mDocView.onPause(() -> {
            pdfBookmarkManager.saveCurrentPage(
                    path,
                    mDocView.getPageCount(),
                    mDocView.getPageNumber(),
                    1,
                    -1,
                    0
            );
        });*/
    }

    override fun onResume() {
        super.onResume()
        //sensorHelper.onResume();
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.clearCacheViews()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun initIntent() {
        if (!TextUtils.isEmpty(path)) {
            return
        }
        val intent = intent
        if (Intent.ACTION_VIEW == intent.action) {
            var uri = intent.data
            println("URI to open is: $uri")
            if (uri!!.scheme == "file") {
                //if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                //	ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                path = uri.path
            } else if (uri.scheme == "content") {
                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(uri, arrayOf("_data"), null, null, null)
                    if (cursor!!.moveToFirst()) {
                        val p = cursor.getString(0)
                        if (!TextUtils.isEmpty(p)) {
                            uri = Uri.parse(p)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    cursor?.close()
                }
                path = Uri.decode(uri!!.encodedPath)
            }
            mUri = uri
        } else {
            if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                path = getIntent().getStringExtra("path")
                mUri = Uri.parse(path)
            }
        }
    }

    private fun showReflowConfigMenu() {
        if (null == mStyleControls) {
            initStyleControls()
        } else {
            if (mStyleControls?.visibility == View.VISIBLE) {
                mStyleControls?.visibility = View.GONE
            } else {
                showStyleFragment()
            }
        }
    }

    private fun showStyleFragment() {
        mStyleControls?.visibility = View.VISIBLE
    }

    private fun initStyleControls() {
        if (null == mStyleControls) {
            binding = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.text_style,
                null,
                false
            )
            mStyleControls = binding.root
            //LayoutInflater.from(context).inflate(R.layout.text_style, null, false)

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout?.addView(mStyleControls, lp)
        }
        mStyleControls?.visibility = View.VISIBLE

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
            binding.fontSizeLabel.text = String.format("%s", progress + START_PROGRESS)
            binding.fontSeekBar.max = 10
            binding.fontSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val index = (progress + START_PROGRESS)
                    binding.fontSizeLabel.text = String.format("%s", index)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    it.styleBean?.textSize = (seekBar?.progress!! + START_PROGRESS).toFloat()
                    it.saveStyleToSP(it.styleBean)
                    updateReflowAdapter()
                }
            })
            binding.fontFaceSelected.text = it.fontHelper?.fontBean?.fontName

            binding.lineSpaceLabel.text = String.format("%s倍", it.styleBean?.lineSpacingMult)
            binding.colorLabel.setBackgroundColor(it.styleBean?.bgColor!!)
            binding.colorLabel.setTextColor(it.styleBean?.fgColor!!)
        }

        binding.fontFaceChange.setOnClickListener {
            FontsFragment.showFontsDialog(
                this as FragmentActivity, mStyleHelper,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        updateReflowAdapter()
                        val fBean = args[0] as FontBean
                        binding.fontFaceSelected.text = fBean.fontName
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }

        binding.linespaceMinus.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! < 0.8f) {
                return@setOnClickListener
            }
            old = old.minus(0.1f)
            applyLineSpace(old)
        }
        binding.linespacePlus.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! > 2.2f) {
                return@setOnClickListener
            }
            old = old?.plus(0.1f)
            applyLineSpace(old)
        }
        binding.bgSetting.setOnClickListener {
            ColorPickerDialog()
                .withColor(mStyleHelper?.styleBean?.bgColor!!)
                .withListener { _, color ->
                    binding.colorLabel.setBackgroundColor(color)
                    mStyleHelper?.styleBean?.bgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                }
                .show(supportFragmentManager, "colorPicker")
        }
        binding.fgSetting.setOnClickListener {
            ColorPickerDialog()
                .withColor(mStyleHelper?.styleBean?.fgColor!!)
                .withListener { _, color ->
                    binding.colorLabel.setTextColor(color)
                    mStyleHelper?.styleBean?.fgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                }
                .show(supportFragmentManager, "colorPicker")
        }
    }

    private fun updateReflowAdapter() {
        recyclerView?.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    private fun applyLineSpace(old: Float?) {
        binding.lineSpaceLabel.text = String.format("%s倍", old)
        mStyleHelper?.styleBean?.lineSpacingMult = old!!
        mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
        updateReflowAdapter()
    }

    companion object {

        private val START_PROGRESS = 15
        fun start(context: Context, path: String) {
            val intent = Intent(context, TextActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }

        private const val READ_LINE = 10
        private const val READ_CHAR_COUNT = 400
        private const val TEMP_LINE = "\n"

        private fun readString(path: String): List<ReflowBean> {
            var bufferedReader: BufferedReader? = null
            val reflowBeans = mutableListOf<ReflowBean>()
            reflowBeans.add(ReflowBean(TEMP_LINE, ReflowBean.TYPE_STRING))
            var lineCount = 0
            val sb = StringBuilder()
            try {
                val fileCharsetName = FileUtils.getFileCharsetName(path)
                val isr = InputStreamReader(FileInputStream(path), fileCharsetName)
                bufferedReader = BufferedReader(isr)
                var temp: String?
                while (bufferedReader.readLine().also { temp = it } != null) {
                    temp = temp?.trimIndent()
                    if (null != temp && temp!!.length > READ_CHAR_COUNT + 40) {
                        //如果一行大于READ_CHAR_COUNT个字符,就应该把这一行按READ_CHAR_COUNT一个字符换行.
                        addLargeLine(temp!!, reflowBeans)
                    } else {
                        if (lineCount < READ_LINE) {
                            sb.append(temp)
                            lineCount++
                        } else {
                            Logcat.d("======================:$sb")
                            reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
                            sb.setLength(0)
                            lineCount = 0
                        }
                    }
                }
                if (sb.isNotEmpty()) {
                    reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
                }
                reflowBeans.add(ReflowBean(TEMP_LINE, ReflowBean.TYPE_STRING))
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                StreamUtils.closeStream(bufferedReader)
            }

            return reflowBeans
        }

        private fun addLargeLine(temp: String, reflowBeans: MutableList<ReflowBean>) {
            val length = temp.length
            var start = 0;
            while (start < length) {
                var end = start + READ_CHAR_COUNT
                if (end > length) {
                    end = length
                }
                val line = temp.subSequence(start, end)
                reflowBeans.add(ReflowBean(line.toString(), ReflowBean.TYPE_STRING))
                start = end
            }
        }
    }
}