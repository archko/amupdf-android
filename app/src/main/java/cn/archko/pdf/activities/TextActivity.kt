package cn.archko.pdf.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.adapters.MuPDFTextAdapter
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/**
 * @author: archko 2022/7/11 :9:49 上午
 */
class TextActivity : AppCompatActivity() {

    private var path: String? = null
    private var mUri: Uri? = null
    var sensorHelper: SensorHelper? = null
    val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    protected val pdfViewModel: PDFViewModel = PDFViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null)
        }
        sensorHelper = SensorHelper(this)
        initIntent()
        loadBookmark()
        
        if (TextUtils.isEmpty(path)) {
            return
        }

        val recyclerView = RecyclerView(this)
        setContentView(recyclerView)

        with(recyclerView) {
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
        }

        val scope = CoroutineScope(Job() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
        recyclerView.adapter =
            MuPDFTextAdapter(this, path!!, StyleHelper(this, preferencesRepository), scope)
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

    companion object {
        fun start(context: Context, path: String) {
            val intent = Intent(context, TextActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }
    }
}