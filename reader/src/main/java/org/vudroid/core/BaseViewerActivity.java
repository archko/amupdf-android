package org.vudroid.core;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.DecodingProgressListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import cn.archko.pdf.common.PdfOptionRepository;
import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.common.SensorHelper;
import cn.archko.pdf.core.common.StatusBarHelper;
import cn.archko.pdf.core.listeners.SimpleGestureListener;

public abstract class BaseViewerActivity extends FragmentActivity implements DecodingProgressListener, CurrentPageListener {
    protected DecodeService decodeService;
    protected DocumentView documentView;
    private Toast pageNumberToast;
    private CurrentPageModel currentPageModel;

    //private OutlineDialog outlineDialog;
    private Outline[] outlines = null;
    private boolean addToRecent = true;

    protected ProgressDialog progressDialog;
    protected boolean isDocLoaded = false;
    private SensorHelper sensorHelper;
    //private Recent recent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusBarHelper.INSTANCE.hideSystemUI(this);
        StatusBarHelper.INSTANCE.setImmerseBarAppearance(getWindow(), true);
        sensorHelper = new SensorHelper(this);

        initDecodeService();
        final ZoomModel zoomModel = new ZoomModel();
        final DecodingProgressModel progressModel = new DecodingProgressModel();
        progressModel.addEventListener(this);
        currentPageModel = new CurrentPageModel();
        currentPageModel.addEventListener(this);

        String path = getIntent().getStringExtra("path");
        boolean hasCrop = getIntent().hasExtra("crop");
        boolean crop;
        if (hasCrop) {
            crop = getIntent().getBooleanExtra("crop", true);
        } else {
            crop = PdfOptionRepository.INSTANCE.getAutocrop();
        }

        addToRecent = getIntent().getBooleanExtra("addToRecent", false);
        //recent = ViewerPrefs.instance.getRecent(path);
        /*Recent recent;
        Log.d("TAG", String.format("get recent:%s", recent));
        if (null == recent) {
            recent = new Recent(path);
            recent.crop = crop ? 0 : 1;
        }*/
        int orientation = LinearLayout.VERTICAL;
        /*if (null != recent) {
            orientation = recent.scrollOri;
            if (hasCrop) {
                recent.crop = crop ? 0 : 1;
            } else {
                crop = recent.crop == 0;
            }
        }*/

        /*if (IntentFile.INSTANCE.isImage(path)) {
            zoomModel.setMaxZoom(10);
        }*/
        documentView = new DocumentView(this, zoomModel,
                orientation, 0, 0,
                progressModel, currentPageModel, simpleGestureListener);
        zoomModel.addEventListener(documentView);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);
        //documentView.applyFilter(PdfOptionRepository.INSTANCE.getColorMode());

        final RelativeLayout frameLayout = createMainContainer();
        frameLayout.setBackgroundColor(Color.WHITE);
        frameLayout.addView(documentView);
        //frameLayout.addView(createZoomControls(zoomModel));
        setFullScreen();

        setContentView(frameLayout);
        /*if (null != recent) {
            documentView.goToPage(recent.page);
            zoomModel.setZoom(recent.zoom / 1000f);
        }*/
        loadDocument(path, crop);
    }

    protected void loadDocument(String path, boolean crop) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            CodecDocument document = decodeService.open(path, crop, true);
            AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                progressDialog.dismiss();
                if (null == document) {
                    Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                isDocLoaded = true;
                documentView.showDocument(crop);
            });
        });
    }

    public void decodingProgressChanged(final int currentlyDecoding) {
    }

    public void currentPageChanged(int pageIndex) {
        showPageIndex(pageIndex);
        documentView.goToPage(pageIndex);
    }

    private void showPageIndex(int pageIndex) {
        final String pageText = (pageIndex + 1) + "/" + decodeService.getPageCount();
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
        }
        pageNumberToast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
        pageNumberToast.show();
    }

    private void setWindowTitle() {
        //final String name = getIntent().getData().getLastPathSegment();
        //getWindow().setTitle(name);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
    }

    private void setFullScreen() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /*private PageViewZoomControls createZoomControls(ZoomModel zoomModel) {
        final PageViewZoomControls controls = new PageViewZoomControls(this, zoomModel);
        controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        zoomModel.addEventListener(controls);
        return controls;
    }*/

    private RelativeLayout createMainContainer() {
        return new RelativeLayout(this);
    }

    private void initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService();
        }
    }

    protected abstract DecodeService createDecodeService();

    @Override
    protected void onResume() {
        super.onResume();
        sensorHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.onPause();
        /*if (recent != null && isDocLoaded && addToRecent) {
            int savePage = documentView.getCurrentPage();
            int lastPage = documentView.getLastVisiblePage();
            if (lastPage == decodeService.getPageCount() - 1) {
                savePage = lastPage;
            }
            recent.zoom = documentView.getZoomModel().getZoom() * 1000;
            recent.page = savePage;
            recent.pageCount = decodeService.getPageCount();
            recent.scrollX = documentView.getScrollX();
            recent.scrollY = documentView.getScrollY();
            Log.d("TAG", String.format("add recent:%s", recent));
            ViewerPrefs.instance.addRecent(recent);
        }*/
    }

    @Override
    protected void onDestroy() {
        decodeService.recycle();
        decodeService = null;
        super.onDestroy();
        BitmapCache.getInstance().clear();
        BitmapPool.getInstance().clear();
    }

    private SimpleGestureListener simpleGestureListener = new SimpleGestureListener() {

        @Override
        public void onSingleTapConfirmed(int currentPage) {
            showPageIndex(currentPage);
        }

        @Override
        public void onDoubleTapEvent(int currentPage) {
            onDoubleTap(currentPage);
        }
    };

    public void onDoubleTap(int currentPage) {
        showOutlineDialog();
    }

    protected void showOutlineDialog() {
        if (null == outlines) {
            outlines = decodeService.getOutlines();
        }

        /*if (null == outlineDialog) {
            outlineDialog = new OutlineDialog(this);
        }
        int currPage = documentView.getCurrentPage();

        outlineDialog.initOutlinesIfNeed(
                false,
                outlines,
                decodeService.getPageCount(),
                new OutlineDialog.OutlineListener() {
                    @Override
                    public void selected(int page, boolean dismiss) {
                        documentView.goToPage(page);
                        if (dismiss) {
                            outlineDialog.dismiss();
                        }
                    }

                    @Override
                    public void orientation(int ori) {
                        documentView.setOriention(ori);
                        if (null != recent) {
                            recent.scrollOri = ori;
                        }
                    }

                    @Override
                    public void setCrop(boolean crop) {
                        if (null != recent) {
                            recent.crop = crop ? 0 : 1;
                        }
                    }
                });
        outlineDialog.setCurrPage(currPage);
        outlineDialog.setOrientation(documentView.getOriention());
        outlineDialog.show();*/
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
