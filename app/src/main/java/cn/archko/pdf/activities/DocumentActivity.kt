package cn.archko.pdf.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.ActivityDocViewBinding
import cn.archko.pdf.AppExecutors.Companion.instance
import cn.archko.pdf.activities.DocumentActivity
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.common.SensorHelper
import com.artifex.solib.ArDkLib
import com.artifex.solib.ConfigOptions
import com.artifex.solib.FileUtils
import com.artifex.solib.SOClipboardHandler
import com.artifex.sonui.editor.DocumentListener
import com.artifex.sonui.editor.NUIView.OnDoneListener
import com.artifex.sonui.editor.Utilities
import com.thuypham.ptithcm.editvideo.base.BaseActivity

/**
 * @author: archko 2020/10/31 :9:49 上午
 */
class DocumentActivity : BaseActivity<ActivityDocViewBinding>(R.layout.activity_doc_view) {
    private var path: String? = null
    private var mUri: Uri? = null
    var sensorHelper: SensorHelper? = null
    private var pdfBookmarkManager: PDFBookmarkManager? = null

    internal class ClipboardHandler : SOClipboardHandler {
        private var mActivity // The current activity.
                : Activity? = null
        private var mClipboard // System clipboard.
                : ClipboardManager? = null

        /**
         * This method passes a string, cut or copied from the document, to be
         * stored in the clipboard.
         *
         * @param text The text to be stored in the clipboard.
         */
        override fun putPlainTextToClipboard(text: String) {
            if (mEnableDebug) {
                Log.d(mDebugTag, "putPlainTextToClipboard: '$text'")
            }
            if (text != null) {
                val clip: ClipData
                clip = ClipData.newPlainText("text", text)
                mClipboard!!.setPrimaryClip(clip)
            }
        }

        /**
         * This method returns the contents of the clipboard.
         *
         * @return The text read from the clipboard.
         */
        override fun getPlainTextFromClipoard(): String {
            var text = ""
            if (clipboardHasPlaintext()) {
                val clip = mClipboard!!.primaryClip
                val item = clip!!.getItemAt(0)
                text = item.coerceToText(mActivity).toString()
                text = text
                if (mEnableDebug) {
                    Log.d(mDebugTag, "getPlainTextFromClipoard: '$text'")
                }
            }
            return text
        }

        /**
         * This method ascertains whether the clipboard has any data.
         *
         * @return True if it has. False otherwise.
         */
        override fun clipboardHasPlaintext(): Boolean {
            return mClipboard!!.hasPrimaryClip()
        }

        /**
         * Initialise the class, installing the example system clipboard listener
         * if available.<br></br><br></br>
         *
         * @param activity The current activity.
         */
        override fun initClipboardHandler(activity: Activity) {
            mActivity = activity

            // Get the system clipboard.
            mClipboard = mActivity!!.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        }

        companion object {
            private const val mDebugTag = "ClipboardHandler"
            private const val mEnableDebug = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null)
        }
        setupApplicationSpecifics(this)
        sensorHelper = SensorHelper(this)
        initIntent()
        loadBookmark()
        useDefaultUI()
    }

    override fun setupView() {

    }

    private fun loadBookmark() {
        if (!TextUtils.isEmpty(path)) {
            pdfBookmarkManager = PDFBookmarkManager()
            instance.diskIO().execute { pdfBookmarkManager!!.setReadProgress(path, 0) }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.documentView.onPause {
            //  called when pausing is complete
        }
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
        binding.documentView.onResume()
        //sensorHelper.onResume();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.documentView.onConfigurationChange(newConfig)
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

    private fun useDefaultUI() {
        //  find the DocumentView component
        binding.documentView.setDocConfigOptions(ArDkLib.getAppConfigOptions())
        binding.documentView.setDocDataLeakHandler(Utilities.getDataLeakHandlers())

        //  set an optional listener for document events
        val page = pdfBookmarkManager!!.readPage
        binding.documentView.setDocumentListener(object : DocumentListener {
            override fun onPageLoaded(pagesLoaded: Int) {}
            override fun onDocCompleted() {
                binding.documentView.goToPage(page)
            }

            override fun onPasswordRequired() {}
            override fun onViewChanged(
                scale: Float,
                scrollX: Int,
                scrollY: Int,
                selectionRect: Rect?
            ) {
            }
        })

        //  set a listener for when the document view is closed.
        //  typically you'll use it to close your activity.
        binding.documentView.setOnDoneListener(OnDoneListener { super@DocumentActivity.finish() })

        //  get the URI for the document
        //mUri = getIntent().getData();

        //  open it, specifying showUI = true;
        binding.documentView.start(mUri, 0, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.documentView.onDestroy()
    }

    override fun onBackPressed() {
        binding.documentView.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        binding.documentView.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DocumentActivity::class.java))
        }

        private var isSetup = false
        fun setupApplicationSpecifics(ctx: Context?) {
            //  create/register handlers (but only once)
            if (!isSetup) {
                val cfg = ConfigOptions()
                ArDkLib.setAppConfigOptions(cfg)
                Utilities.setDataLeakHandlers(DataLeakHandlers())
                Utilities.setPersistentStorage(PersistentStorage())
                ArDkLib.setClipboardHandler(ClipboardHandler())
                //ArDkLib.setSecureFS(new SecureFS());
                FileUtils.init(ctx)
                isSetup = true
            }
        }
    }
}