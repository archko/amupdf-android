package org.vudroid.core;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.events.CurrentPageListener;
import org.vudroid.core.events.DecodingProgressListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import cn.archko.pdf.R;
import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.core.common.SensorHelper;
import cn.archko.pdf.core.common.StatusBarHelper;
import cn.archko.pdf.core.listeners.SimpleGestureListener;
import cn.archko.pdf.core.utils.Utils;

public abstract class BaseViewerActivity extends FragmentActivity implements DecodingProgressListener, CurrentPageListener {
    protected DecodeService decodeService;
    protected DocumentView documentView;
    private CurrentPageModel currentPageModel;

    protected ProgressDialog progressDialog;
    protected boolean isDocLoaded = false;
    private SensorHelper sensorHelper;
    int mMargin;
    protected SeekbarControls seekbarControls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMargin = Utils.dipToPixel(16);

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

        int orientation = LinearLayout.VERTICAL;

        documentView = new DocumentView(this, zoomModel,
                orientation, 0, 0,
                progressModel, currentPageModel, simpleGestureListener);
        zoomModel.addEventListener(documentView);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);

        final RelativeLayout container = createMainContainer();
        container.setBackgroundColor(Color.WHITE);
        container.addView(documentView);
        setFullScreen();

        View view = LayoutInflater.from(this).inflate(R.layout.seekbar, container, false);
        seekbarControls = new SeekbarControls(view, new SeekbarControls.ControlListener() {
            @Override
            public void changeOrientation(int ori) {
                documentView.setOriention(ori);
            }

            @Override
            public void gotoPage(int page) {
                documentView.goToPage(page);
            }
        });
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        container.addView(view, lp);

        setContentView(container);

        loadDocument(path, false);
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
        documentView.goToPage(pageIndex);
    }

    private void setWindowTitle() {
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
        public void onSingleTapConfirmed(MotionEvent ev, int currentPage) {
            int height = documentView.getHeight();
            int top = height / 4;
            int bottom = height * 3 / 4;
            //Log.d(VIEW_LOG_TAG, "height:"+height+" y:"+e.getY()+" mMargin:"+mMargin);

            height = height - mMargin;
            if ((int) ev.getY() < top) {
                documentView.scrollPage(-height);
            } else if ((int) ev.getY() > bottom) {
                documentView.scrollPage(height);
            } else {
                seekbarControls.updatePageProgress(currentPage);
                seekbarControls.toggleControls();
            }
        }

        @Override
        public void onDoubleTap(MotionEvent ev, int currentPage) {
        }
    };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
