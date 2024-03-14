package org.vudroid.core

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.activities.PdfOptionsActivity.Companion.start
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.listeners.SimpleGestureListener
import cn.archko.pdf.presenter.PageViewPresenter
import cn.archko.pdf.utils.StatusBarHelper
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vudroid.core.events.CurrentPageListener
import org.vudroid.core.events.DecodingProgressListener
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
import org.vudroid.core.models.ZoomModel
import org.vudroid.core.views.PageViewZoomControls

abstract class BaseViewerActivity : FragmentActivity(), DecodingProgressListener,
    CurrentPageListener {
    //private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";
    var decodeService: DecodeService? = null
        private set
    var documentView: DocumentView? = null
        private set

    //private ViewerPreferences viewerPreferences;
    private var pageNumberToast: Toast? = null
    private var currentPageModel: CurrentPageModel? = null
    var pageControls: PageViewZoomControls? = null

    //private CurrentPageModel mPageModel;
    var pageSeekBarControls: APageSeekBarControls? = null
    var sensorHelper: SensorHelper? = null
    val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    protected val pdfViewModel: PDFViewModel = PDFViewModel()

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setImmerseBarAppearance(window, true)

        BitmapCache.getInstance().resize(BitmapCache.CAPACITY_FOR_VUDROID)
        initDecodeService()
        val zoomModel = ZoomModel()
        sensorHelper = SensorHelper(this)
        val uri = intent.data
        val absolutePath = Uri.decode(uri!!.encodedPath)
        lifecycleScope.launch {
            val bookProgress =
                pdfViewModel.loadBookProgressByPath(absolutePath, preferencesRepository)
            zoomModel.zoom = bookProgress!!.zoomLevel / 1000
        }
        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel!!.addEventListener(this)
        documentView =
            DocumentView(this, zoomModel, progressModel, currentPageModel, simpleGestureListener)
        zoomModel.addEventListener(documentView)
        documentView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        decodeService!!.setContainerView(documentView)
        documentView!!.setDecodeService(decodeService)
        decodeService!!.open(absolutePath)
        val frameLayout = createMainContainer()
        frameLayout.addView(documentView)
        pageControls = createZoomControls(zoomModel)
        frameLayout.addView(pageControls)
        setContentView(frameLayout)

        /*final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        documentView.goToPage(sharedPreferences.getInt(getIntent().getData().toString(), 0));*/
        val currentPage = pdfViewModel.getCurrentPage(
            decodeService!!.pageCount
        )
        if (0 < currentPage && null != pdfViewModel.bookProgress) {
            documentView!!.goToPage(
                currentPage,
                pdfViewModel.bookProgress!!.offsetX,
                pdfViewModel.bookProgress!!.offsetY
            )
        }
        documentView!!.showDocument()

        //viewerPreferences.addRecent(getIntent().getData());

        /*mPageModel=new CurrentPageModel();
        mPageModel.addEventListener(new CurrentPageListener() {
            @Override
            public void currentPageChanged(int pageIndex) {
                Log.d(TAG, "currentPageChanged:"+pageIndex);
                if (documentView.getCurrentPage()!=pageIndex) {
                    documentView.goToPage(pageIndex);
                }
            }
        });
        documentView.setPageModel(mPageModel);
        mPageSeekBarControls=createSeekControls(mPageModel);*/pageSeekBarControls =
            APageSeekBarControls(this, object : PageViewPresenter {
                override fun getPageCount(): Int {
                    return decodeService!!.pageCount
                }

                override fun getCurrentPageIndex(): Int {
                    return documentView!!.currentPage
                }

                override fun goToPageIndex(page: Int) {
                    documentView!!.goToPage(page)
                }

                override fun showOutline() {
                    openOutline()
                }

                override fun back() {
                    finish()
                }

                override fun getTitle(): String {
                    val uri = intent.data
                    return Uri.decode(uri!!.encodedPath)
                }

                override fun reflow() {}
                override fun autoCrop() {}
                override fun showBookmark() {}
            })
        frameLayout.addView(pageSeekBarControls)
        pageSeekBarControls!!.hide()
        pageSeekBarControls!!.showReflow(true)
        pageSeekBarControls!!.updateTitle(absolutePath)
    }

    override fun decodingProgressChanged(currentlyDecoding: Int) {
        runOnUiThread {
            //getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, currentlyDecoding == 0 ? 10000 : currentlyDecoding);
        }
    }

    override fun currentPageChanged(pageIndex: Int) {
        val pageText = (pageIndex + 1).toString() + "/" + decodeService!!.pageCount
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.LEFT, 30, 0)
        pageNumberToast!!.show()
        //saveCurrentPage();
    }

    private fun setWindowTitle() {
        val name = intent.data!!.lastPathSegment
        window.setTitle(name)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setWindowTitle()
    }

    private fun setFullScreen() {
        /*if (viewerPreferences.isFullScreen())
        {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else
        {
            getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }*/
    }

    private fun createZoomControls(zoomModel: ZoomModel): PageViewZoomControls {
        val controls = PageViewZoomControls(this, zoomModel)
        controls.gravity = Gravity.RIGHT or Gravity.BOTTOM
        zoomModel.addEventListener(controls)
        return controls
    }

    /*private PageSeekBarControls createSeekControls(CurrentPageModel pageModel)
    {
        final PageSeekBarControls controls = new APageSeekBarControls(this, pageModel);
        //controls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        pageModel.addEventListener(controls);
        return controls;
    }*/
    private fun createMainContainer(): FrameLayout {
        return FrameLayout(this)
    }

    private fun initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService()
        }
    }

    protected abstract fun createDecodeService(): DecodeService?
    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        decodeService!!.recycle()
        decodeService = null
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_EXIT, 0, "Exit")
        menu.add(1, MENU_GOTO, 0, getString(R.string.menu_goto_page))
        menu.add(2, MENU_OUTLINE, 0, getString(R.string.opts_table_of_contents))
        menu.add(3, MENU_OPTIONS, 0, getString(R.string.options))
        /*final MenuItem menuItem = menu.add(0, MENU_FULL_SCREEN, 0, "Full screen").setCheckable(true).setChecked(viewerPreferences.isFullScreen());
        setFullScreenMenuItemText(menuItem);*/return true
    }

    private fun setFullScreenMenuItemText(menuItem: MenuItem) {
        menuItem.title = "Full screen " + if (menuItem.isChecked) "on" else "off"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_EXIT -> {
                finish()
                return true
            }

            MENU_GOTO -> {
                //showDialog(DIALOG_GOTO);
                //mPageModel.setCurrentPage(currentPageModel.getCurrentPageIndex());
                //mPageModel.setPageCount(decodeService.getPageCount());
                pageSeekBarControls!!.fade()
                return true
            }

            MENU_FULL_SCREEN -> {
                item.isChecked = !item.isChecked
                setFullScreenMenuItemText(item)
                //viewerPreferences.setFullScreen(item.isChecked());
                finish()
                startActivity(intent)
                return true
            }

            MENU_OUTLINE -> {
                openOutline()
                return true
            }

            MENU_OPTIONS -> start(this)
        }
        return false
    }

    open fun openOutline() {}

    //--------------------------------------
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()
        sensorHelper!!.onResume()

        lifecycleScope.launch() {
            var keepOn = false
            var fullscreen = true
            var verticalScrollLock = true
            withContext(Dispatchers.IO) {
                val data = preferencesRepository.pdfOptionFlow.first()
                keepOn = data.keepOn
                fullscreen = data.fullscreen
                verticalScrollLock = data.verticalScrollLock
            }
            if (keepOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            /*setZoomLayout(options);
            pagesView.setZoomLayout(zoomLayout);*/
            //documentView!!.setVerticalScrollLock(verticalScrollLock)

            /*int zoomAnimNumber = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_ZOOM_ANIMATION, "2"));

            if (zoomAnimNumber == PdfOptionsActivity.ZOOM_BUTTONS_DISABLED)
                zoomAnim = null;
            else
                zoomAnim = AnimationUtils.loadAnimation(this,
                    zoomAnimations[zoomAnimNumber]);
            int pageNumberAnimNumber = Integer.parseInt(options.getString(PdfOptionsActivity.PREF_PAGE_ANIMATION, "3"));

            if (pageNumberAnimNumber == PdfOptionsActivity.PAGE_NUMBER_DISABLED)
                pageNumberAnim = null;
            else
                pageNumberAnim = AnimationUtils.loadAnimation(this,
                    pageNumberAnimations[pageNumberAnimNumber]);*/
            if (fullscreen) {
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
        pageControls!!.hide()
        var height = documentView!!.height
        height = if (height <= 0) {
            ViewConfiguration().scaledTouchSlop * 2
        } else {
            (height * 0.03).toInt()
        }
        documentView!!.setScrollMargin(height)
        documentView!!.setDecodePage(1 /*options.getBoolean(PdfOptionsActivity.PREF_RENDER_AHEAD, true) ? 1 : 0*/)
    }

    override fun onPause() {
        super.onPause()
        val uri = intent.data
        val filePath = Uri.decode(uri!!.encodedPath)
        pdfViewModel.saveBookProgress(
            filePath,
            decodeService!!.pageCount,
            documentView!!.currentPage,
            documentView!!.getZoomModel().zoom * 1000f,
            documentView!!.scrollX,
            documentView!!.scrollY
        )
        sensorHelper!!.onPause()
    }

    //--------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OUTLINE_REQUEST -> {
                if (resultCode >= 0) documentView!!.goToPage(resultCode)
                pageSeekBarControls!!.hide()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    var simpleGestureListener: SimpleGestureListener = object : SimpleGestureListener {
        override fun onSingleTapConfirmed(currentPage: Int) {
            currentPageChanged(currentPage)
        }

        override fun onDoubleTapEvent(currentPage: Int) {
            pageSeekBarControls!!.toggleSeekControls()
            pageControls!!.toggleZoomControls()
        }
    }

    protected fun currentPage(): Int {
        return documentView!!.currentPage
    }

    companion object {
        private const val MENU_EXIT = 0
        private const val MENU_GOTO = 1
        private const val MENU_FULL_SCREEN = 2
        private const val MENU_OPTIONS = 3
        private const val MENU_OUTLINE = 4
        private const val DIALOG_GOTO = 0
        private const val TAG = "BaseViewer"

        private const val OUTLINE_REQUEST: Int = 0
    }
}