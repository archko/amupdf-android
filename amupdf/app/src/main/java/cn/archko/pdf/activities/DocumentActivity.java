package cn.archko.pdf.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.SOClipboardHandler;
import com.artifex.sonui.editor.DocumentListener;
import com.artifex.sonui.editor.DocumentView;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.archko.pdf.common.PDFBookmarkManager;
import cn.archko.pdf.common.SensorHelper;

/**
 * @author: archko 2020/10/31 :9:49 上午
 */
public class DocumentActivity extends AppCompatActivity {

    DocumentView mDocView;
    SensorHelper sensorHelper;

    String path;
    Uri mUri;

    private PDFBookmarkManager pdfBookmarkManager;

    public static void start(Context context) {
        context.startActivity(new Intent(context, DocumentActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setContentView(R.layout.list_book_search);

        //aa();
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null);
        }

        sensorHelper = new SensorHelper(this);
        createUI();
        initIntent();

        if (!TextUtils.isEmpty(path)) {
            pdfBookmarkManager = new PDFBookmarkManager();
            pdfBookmarkManager.setStartBookmark(path, 0);

            ArDkLib.setClipboardHandler(new SOClipboardHandler() {
                @Override
                public void putPlainTextToClipboard(String text) {

                }

                @Override
                public String getPlainTextFromClipoard() {
                    return null;
                }

                @Override
                public boolean clipboardHasPlaintext() {
                    return false;
                }

                @Override
                public void initClipboardHandler(Activity activity) {

                }
            });

            int page = pdfBookmarkManager.getBookmark();
            mDocView.setDocumentListener(new DocumentListener() {
                @Override
                public void onPageLoaded(int pagesLoaded) {

                }

                @Override
                public void onDocCompleted() {
                    mDocView.goToPage(page);
                }

                @Override
                public void onPasswordRequired() {

                }

                @Override
                public void onViewChanged(float scale, int scrollX, int scrollY, Rect selectionRect) {

                }
            });
            mDocView.start(mUri, page, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.onPause();
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

    @Override
    protected void onResume() {
        super.onResume();
        sensorHelper.onResume();
        mDocView.onResume();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull @NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDocView.onConfigurationChange(newConfig);
    }

    private void createUI() {
        mDocView = new DocumentView(this);
        //setContentView(mDocView);
        ConfigOptions cfg = new ConfigOptions();
        ArDkLib.setAppConfigOptions(cfg);
        mDocView.setDocConfigOptions(cfg);

        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.DKGRAY);
        layout.addView(mDocView);
        setContentView(layout);
    }

    private void initIntent() {
        if (!TextUtils.isEmpty(path)) {
            return;
        }
        Intent intent = getIntent();

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            System.out.println("URI to open is: " + uri);
            if (uri.getScheme().equals("file")) {
                //if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                //	ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                path = uri.getPath();
            } else if (uri.getScheme().equals("content")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                    if (cursor.moveToFirst()) {
                        String p = cursor.getString(0);
                        if (!TextUtils.isEmpty(p)) {
                            uri = Uri.parse(p);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
                path = Uri.decode(uri.getEncodedPath());
            }
            mUri = uri;
        } else {
            if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                path = getIntent().getStringExtra("path");
                mUri = Uri.parse(path);
            }
        }
    }
}
