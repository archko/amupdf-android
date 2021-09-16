package cn.archko.pdf.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.FileUtils;
import com.artifex.solib.SOClipboardHandler;
import com.artifex.sonui.editor.DocumentListener;
import com.artifex.sonui.editor.DocumentView;
import com.artifex.sonui.editor.SOPersistentStorage;
import com.artifex.sonui.editor.Utilities;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.archko.pdf.AppExecutors;
import cn.archko.pdf.common.PDFBookmarkManager;
import cn.archko.pdf.common.SensorHelper;

/**
 * @author: archko 2020/10/31 :9:49 上午
 */
public class DocumentActivity extends AppCompatActivity {

    private String path;
    private Uri mUri;

    DocumentView mDocumentView;
    SensorHelper sensorHelper;

    private PDFBookmarkManager pdfBookmarkManager;

    public static void start(Context context) {
        context.startActivity(new Intent(context, DocumentActivity.class));
    }

    static class ClipboardHandler implements SOClipboardHandler {
        private static final String mDebugTag = "ClipboardHandler";
        private static boolean mEnableDebug = false;

        private Activity mActivity;       // The current activity.
        private ClipboardManager mClipboard;      // System clipboard.

        /**
         * This method passes a string, cut or copied from the document, to be
         * stored in the clipboard.
         *
         * @param text The text to be stored in the clipboard.
         */
        @Override
        public void putPlainTextToClipboard(String text) {
            if (mEnableDebug) {
                Log.d(mDebugTag, "putPlainTextToClipboard: '" + text + "'");
            }

            if (text != null) {
                ClipData clip;
                clip = ClipData.newPlainText("text", text);
                mClipboard.setPrimaryClip(clip);
            }
        }

        /**
         * This method returns the contents of the clipboard.
         *
         * @return The text read from the clipboard.
         */
        @Override
        public String getPlainTextFromClipoard() {
            String text = "";

            if (clipboardHasPlaintext()) {
                ClipData clip = mClipboard.getPrimaryClip();
                ClipData.Item item = clip.getItemAt(0);

                text = item.coerceToText(mActivity).toString();
                text = text;

                if (mEnableDebug) {
                    Log.d(mDebugTag, "getPlainTextFromClipoard: '" + text + "'");
                }
            }

            return text;
        }

        /**
         * This method ascertains whether the clipboard has any data.
         *
         * @return True if it has. False otherwise.
         */
        @Override
        public boolean clipboardHasPlaintext() {
            return mClipboard.hasPrimaryClip();
        }

        /**
         * Initialise the class, installing the example system clipboard listener
         * if available.<br><br>
         *
         * @param activity The current activity.
         */
        public void initClipboardHandler(Activity activity) {
            mActivity = activity;

            // Get the system clipboard.
            mClipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }

    private static boolean isSetup = false;

    public static void setupApplicationSpecifics(Context ctx) {
        //  create/register handlers (but only once)
        if (!isSetup) {
            ConfigOptions cfg = new ConfigOptions();
            ArDkLib.setAppConfigOptions(cfg);
            Utilities.setDataLeakHandlers(new DataLeakHandlers());
            //Utilities.setPersistentStorage(new PersistentStorage());
            ArDkLib.setClipboardHandler(new ClipboardHandler());
            //ArDkLib.setSecureFS(new SecureFS());
            FileUtils.init(ctx);
            Utilities.setPersistentStorage(new SOPersistentStorage() {
                @Override
                public Object getStorageObject(Context context, String storeName) {
                    return null;
                }

                @Override
                public void setStringPreference(Object storageObject, String key, String value) {

                }

                @Override
                public String getStringPreference(Object storageObject, String key, String defaultValue) {
                    return null;
                }

                @Override
                public Map<String, ?> getAllStringPreferences(Object storageObject) {
                    return null;
                }

                @Override
                public void removePreference(Object storageObject, String key) {

                }
            });

            isSetup = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null);
        }

        setupApplicationSpecifics(this);
        sensorHelper = new SensorHelper(this);
        initIntent();
        loadBookmark();
        useDefaultUI();
    }

    private void loadBookmark() {
        if (!TextUtils.isEmpty(path)) {
            pdfBookmarkManager = new PDFBookmarkManager();

            AppExecutors.Companion.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    pdfBookmarkManager.setStartBookmark(path, 0);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDocumentView != null) {
            mDocumentView.onPause(new Runnable() {
                @Override
                public void run() {
                    //  called when pausing is complete
                }
            });
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

    @Override
    protected void onResume() {
        super.onResume();
        if (mDocumentView != null) {
            mDocumentView.onResume();
        }
        //sensorHelper.onResume();
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
        if (null != mDocumentView) {
            mDocumentView.onConfigurationChange(newConfig);
        }
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

    private void useDefaultUI() {
        //  set up UI
        setContentView(com.artifex.sonui.editor.R.layout.sodk_editor_doc_view_activity);

        //  find the DocumentView component
        mDocumentView = findViewById(com.artifex.sonui.editor.R.id.doc_view);

        mDocumentView.setDocConfigOptions(ArDkLib.getAppConfigOptions());
        mDocumentView.setDocDataLeakHandler(Utilities.getDataLeakHandlers());

        //  set an optional listener for document events
        int page = pdfBookmarkManager.getBookmark();
        mDocumentView.setDocumentListener(new DocumentListener() {
            @Override
            public void onPageLoaded(int pagesLoaded) {

            }

            @Override
            public void onDocCompleted() {
                mDocumentView.goToPage(page);
            }

            @Override
            public void onPasswordRequired() {

            }

            @Override
            public void onViewChanged(float scale, int scrollX, int scrollY, Rect selectionRect) {
            }
        });

        //  set a listener for when the document view is closed.
        //  typically you'll use it to close your activity.
        mDocumentView.setOnDoneListener(DocumentActivity.super::finish);

        //  get the URI for the document
        //mUri = getIntent().getData();

        //  open it, specifying showUI = true;
        mDocumentView.start(mUri, 0, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDocumentView != null) {
            mDocumentView.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDocumentView != null) {
            mDocumentView.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDocumentView != null) {
            mDocumentView.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}