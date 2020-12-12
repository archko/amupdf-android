package cn.archko.pdf.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.*;

import com.artifex.solib.ConfigOptions;
import com.artifex.sonui.editor.DocumentListener;
import com.artifex.sonui.editor.DocumentView;
import com.artifex.sonui.editor.ViewingState;

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

    private View mButtonsView;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private ImageButton mSearchButton;
    private ImageButton mOutlineButton;
    private ViewAnimator mTopBarSwitcher;
    private ImageButton mLinkButton;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private ImageButton mSearchClose;
    private EditText mSearchText;
    protected View mLayoutButton;

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
        mDocView.setDocConfigOptions(new ConfigOptions());

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        //int smax = Math.max(core.countPages()-1,1);
        //mPageSliderRes = ((10 + smax - 1)/smax) * 2;

        // Set the file-name text
        //String docTitle = core.getTitle();
        //if (docTitle != null)
        //    mFilenameView.setText(docTitle);
        //else
        //    mFilenameView.setText(mFileName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                //mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
            }
        });

        // Activate the search-preparing button
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //searchModeOn();
            }
        });

        mSearchClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //searchModeOff();
            }
        });

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        /*mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {}
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {}
        });*/

        //React to Done button on keyboard
        /*mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //search(1);
                }
                return false;
            }
        });*/

        /*mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    //search(1);
                }
                return false;
            }
        });*/

        // Activate search invoking buttons
        /*mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //search(1);
            }
        });

        mLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //setLinkHighlight(!mLinkHighlight);
            }
        });*/

        /*if (core.hasOutline()) {
            mOutlineButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mFlatOutline == null)
                        mFlatOutline = core.getOutline();
                    if (mFlatOutline != null) {
                        Intent intent = new Intent(com.artifex.mupdf.viewer.DocumentActivity.this, OutlineActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
                        bundle.putSerializable("OUTLINE", mFlatOutline);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, OUTLINE_REQUEST);
                    }
                }
            });
        } else {
            mOutlineButton.setVisibility(View.GONE);
        }*/

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        //mDocView.setDisplayedViewIndex(prefs.getInt("page"+mFileKey, 0));

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.DKGRAY);
        layout.addView(mDocView);
        layout.addView(mButtonsView);
        setContentView(layout);
    }

    private void makeButtonsView() {
        mButtonsView = getLayoutInflater().inflate(com.artifex.mupdf.viewer.R.layout.document_activity, null);
        mFilenameView = (TextView) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.docNameText);
        mPageSlider = (SeekBar) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.pageSlider);
        mPageNumberView = (TextView) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.pageNumber);
        mSearchButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.searchButton);
        mOutlineButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.outlineButton);
        mTopBarSwitcher = (ViewAnimator) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.switcher);
        mSearchBack = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.searchBack);
        mSearchFwd = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.searchForward);
        mSearchClose = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.searchClose);
        mSearchText = (EditText) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.searchText);
        mLinkButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.linkButton);
        mLayoutButton = mButtonsView.findViewById(com.artifex.mupdf.viewer.R.id.layoutButton);
        mTopBarSwitcher.setVisibility(View.INVISIBLE);
        mPageNumberView.setVisibility(View.INVISIBLE);

        mPageSlider.setVisibility(View.INVISIBLE);
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
