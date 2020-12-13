package cn.archko.pdf.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.FileUtils;
import com.artifex.solib.SOClipboardHandler;
import com.artifex.solib.SODocSaveListener;
import com.artifex.sonui.editor.DocumentListener;
import com.artifex.sonui.editor.DocumentView;
import com.artifex.sonui.editor.Utilities;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.archko.mupdf.R;
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
    private float mOriginalScale = -1;

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
        initIntent();
        load();
        useCustomUI();
    }

    private void load() {
        ConfigOptions cfg = new ConfigOptions();
        ArDkLib.setAppConfigOptions(cfg);
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
        }
    }

    private void start() {
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
        mDocumentView.start(mUri, page, false);
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

    private void createUI() {
        mDocumentView = new DocumentView(this);
        //setContentView(mDocView);
        mDocumentView.setDocConfigOptions(ArDkLib.getAppConfigOptions());

        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.DKGRAY);
        layout.addView(mDocumentView);
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

    private void useCustomUI() {
        //  get the document view
        setContentView(R.layout.doc_layout);
        mDocumentView = findViewById(R.id.doc_view);

        mDocumentView.setDocConfigOptions(ArDkLib.getAppConfigOptions());

        mDocumentView.setDocumentListener(new DocumentListener() {
            @Override
            public void onPageLoaded(int pagesLoaded) {
                //  called when another page is loaded from the document.
                if (mOriginalScale == -1)
                    mOriginalScale = mDocumentView.getScaleFactor();
                Log.d("DocumentListener", "onPageLoaded pages= " + mDocumentView.getPageCount());
                updateUI();
            }

            @Override
            public void onDocCompleted() {
                //  called when the document is done loading.
                Log.d("DocumentListener", "onDocCompleted pages= " + mDocumentView.getPageCount());
                updateUI();
            }

            @Override
            public void onPasswordRequired() {
                //  called when a password is required.
                Log.d("DocumentListener", "onPasswordRequired");
                handlePassword();
            }

            @Override
            public void onViewChanged(float scale, int scrollX, int scrollY, Rect selectionRect) {
                //  called when the scale, scroll, or selection in the document changes.
                Log.d("DocumentListener", "onViewportChanged");
            }
        });

        //  set a listener for when the DocumentView is closed.
        //  typically you'll want to close your activity.
        mDocumentView.setOnDoneListener(new DocumentView.OnDoneListener() {
            @Override
            public void done() {
                DocumentActivity.super.finish();
            }
        });

        //  get the URI for the document to open
        //mUri = getIntent().getData();

        //  open it, specifying showUI = false;
        mDocumentView.start(mUri, 0, false);

        setupUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDocumentView != null)
            mDocumentView.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDocumentView != null)
            mDocumentView.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDocumentView != null)
            mDocumentView.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupUI() {
        //  This function does the work of binding buttons in the UI
        //  to the API of DocumentView.

        findViewById(R.id.toggle_pages).setOnClickListener(view -> {
            if (mDocumentView.isPageListVisible()) {
                mDocumentView.hidePageList();
            } else {
                mDocumentView.showPageList();
            }
        });

        findViewById(R.id.toggle_draw).setOnClickListener(view -> {
            if (mDocumentView != null) {
                if (mDocumentView.isDrawModeOn()) {
                    mDocumentView.setDrawModeOff();
                } else {
                    mDocumentView.setDrawModeOn();
                }
            }
        });

        findViewById(R.id.delete_selection).setOnClickListener(view -> {
            mDocumentView.deleteSelection();
        });

        findViewById(R.id.line_color).setOnClickListener(view -> {
            //  this is a sample dialog for choosing a color.
            final Context context = DocumentActivity.this;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Line Color");
            String[] colorNames = {"red", "green", "blue"};
            final int colors[] = {0xffff0000, 0xff00ff00, 0xff0000ff};
            builder.setItems(colorNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //   color chosen, so set it
                    if (mDocumentView != null)
                        mDocumentView.setLineColor(colors[which]);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        findViewById(R.id.line_thickness).setOnClickListener(view -> {
            //  this is a sample dialog for choosing a line thickness
            final Context context = DocumentActivity.this;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Line Thickness");
            String[] sizeNames = {"small", "medium", "large"};
            final float sizes[] = {1, 8, 24};
            builder.setItems(sizeNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //  value chose, now set it.
                    if (mDocumentView != null)
                        mDocumentView.setLineThickness(sizes[which]);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        findViewById(R.id.full_screen).setOnClickListener(view -> {
            //  hide this activity's UI
            findViewById(R.id.ui_layout).setVisibility(View.GONE);
            //  clear the search text
            EditText editText = findViewById(R.id.search_text);
            editText.getText().clear();
            //  put DocumentView in full screen mode
            if (mDocumentView != null) {
                mDocumentView.enterFullScreen(new Runnable() {
                    @Override
                    public void run() {
                        //  restore our UI
                        findViewById(R.id.ui_layout).setVisibility(View.VISIBLE);
                        updateUI();
                    }
                });
            }
        });

        findViewById(R.id.highlight_selection).setOnClickListener(view -> {
            mDocumentView.highlightSelection();
        });

        //  set up a runnable to handle UI updates triggers by DocumentView
        mDocumentView.setOnUpdateUI(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });

        final EditText searchText = findViewById(R.id.search_text);
        Button searchNextButton = findViewById(R.id.search_next);
        Button searchPrevButton = findViewById(R.id.search_previous);

        //  search
        if (mDocumentView.hasSearch()) {
            searchNextButton.setText("->");
            searchNextButton.setOnClickListener(view -> {
                String text = searchText.getText().toString();
                mDocumentView.searchForward(text);
            });

            searchPrevButton.setText("<-");
            searchPrevButton.setOnClickListener(view -> {
                String text = searchText.getText().toString();
                mDocumentView.searchBackward(text);
            });
        } else {
            searchText.setEnabled(false);
            searchNextButton.setEnabled(false);
            searchPrevButton.setEnabled(false);
        }

        findViewById(R.id.save).setOnClickListener(view -> {
            if (mDocumentView != null) {
                mDocumentView.save();
                updateUI();
            }
        });

        findViewById(R.id.save_as).setOnClickListener(view -> {
            if (mDocumentView != null) {
                //  get a new path.  The customer would provide a way to specify a new
                //  name and location.
                File f = new File(mUri.getPath());
                String newPath = f.getParentFile().getPath() + "/testFile.pdf";

                //  check for collision, the SDK does not.
                if (FileUtils.fileExists(newPath)) {
                    showMessage("The file " + newPath + " already exists");
                } else {
                    //  save it
                    mDocumentView.saveTo(newPath, new SODocSaveListener() {
                        @Override
                        public void onComplete(int result, int err) {
                            if (result == SODocSave_Succeeded) {
                                //  success
                                showMessage("The file was saved.");
                            } else {
                                //  error
                                showMessage("There was an error saving the file.");
                            }
                            updateUI();
                        }
                    });
                }
            }
        });

        findViewById(R.id.print).setOnClickListener(view -> {
            mDocumentView.print();
        });

        findViewById(R.id.share).setOnClickListener(view -> {
            if (mDocumentView != null) {
                //  save a copy of the document
                final File file = new File(getFilesDir().toString() + File.separator + new File(mUri.getPath()).getName());
                mDocumentView.saveTo(file.getPath(), new SODocSaveListener() {
                    @Override
                    public void onComplete(int result, int err) {
                        //  share it
                        showMessage("Sharing is application-specific, and should be implemented by the developer.\n\nSee DocEditorActivity.setupUI");
                        file.delete();
                    }
                });
            }
        });

        findViewById(R.id.toggle_note).setOnClickListener(view -> {
            if (mDocumentView != null) {
                boolean noteMode = mDocumentView.isNoteModeOn();
                if (noteMode)
                    mDocumentView.setNoteModeOff();
                else
                    mDocumentView.setNoteModeOn();
            }

        });

        findViewById(R.id.author).setOnClickListener(view -> {
            mDocumentView.author();
        });

        findViewById(R.id.first_page).setOnClickListener(view -> {
            mDocumentView.firstPage();
        });

        findViewById(R.id.last_page).setOnClickListener(view -> {
            mDocumentView.lastPage();
        });

        findViewById(R.id.table_of_contents).setOnClickListener(view -> {
            mDocumentView.tableOfContents();
        });

        findViewById(R.id.redact_mark).setOnClickListener(view -> {
            mDocumentView.redactMarkText();
        });

        findViewById(R.id.redact_mark_area).setOnClickListener(view -> {
            mDocumentView.redactMarkArea();
        });

        findViewById(R.id.redact_remove).setOnClickListener(view -> {
            mDocumentView.redactRemove();
        });

        findViewById(R.id.redact_apply).setOnClickListener(view -> {
            mDocumentView.redactApply();
        });

        final EditText pageNumberText = findViewById(R.id.page_number);
        findViewById(R.id.goto_page).setOnClickListener(view -> {
            if (mDocumentView != null) {
                try {
                    int pageNum = Integer.parseInt(pageNumberText.getText().toString());
                    pageNumberText.getText().clear();
                    mDocumentView.goToPage(pageNum - 1);
                } catch (Exception e) {
                    showMessage("Please enter a valid integer");
                }
            }
        });

        if (mDocumentView != null) {
            mDocumentView.setPageChangeListener(new DocumentView.ChangePageListener() {
                @Override
                public void onPage(int pageNumber) {
                    int page = pageNumber + 1;
                    int count = mDocumentView.getPageCount();
                    String text = String.format("page %d of %d", page, count);
                    TextView view = findViewById(R.id.page_display);
                    view.setText(text);
                    view.measure(0, 0);
                    view.requestLayout();
                }
            });
        }

        //  establish a message handler
        final DocumentActivity thisActivity = this;
        Utilities.setMessageHandler(new Utilities.MessageHandler() {
            @Override
            public void showMessage(String title, String body, String okLabel, final Runnable whenDone) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(thisActivity);
                dialog.setTitle(title);
                dialog.setMessage(body);
                dialog.setCancelable(false);
                dialog.setPositiveButton(okLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (whenDone != null)
                            whenDone.run();
                    }
                });
                dialog.create().show();
            }

            @Override
            public void yesNoMessage(String title, String body, String yesButtonLabel, String noButtonLabel, final Runnable yesRunnable, final Runnable noRunnable) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(thisActivity);
                dialog.setTitle(title);
                dialog.setMessage(body);
                dialog.setCancelable(false);
                dialog.setPositiveButton(yesButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (yesRunnable != null)
                            yesRunnable.run();
                    }
                });
                dialog.setNegativeButton(noButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (noRunnable != null)
                            noRunnable.run();
                    }
                });
                dialog.create().show();
            }
        });

        findViewById(R.id.scale_up).setOnClickListener(view -> scaleBy(0.20f));

        findViewById(R.id.scale_down).setOnClickListener(view -> scaleBy(-0.20f));

        findViewById(R.id.scroll_up).setOnClickListener(view -> scrollBy(-100));

        findViewById(R.id.scroll_down).setOnClickListener(view -> scrollBy(100));

        findViewById(R.id.reset).setOnClickListener(view -> mDocumentView.setScaleAndScroll(mOriginalScale, 0, 0));

        //  Tabs
        findViewById(R.id.file_tab_button).setOnClickListener(v -> onClickTabButton(R.id.file_tab_button, R.id.file_tab));

        //  Tabs
        findViewById(R.id.pages_tab_button).setOnClickListener(v -> onClickTabButton(R.id.pages_tab_button, R.id.pages_tab));

        //  Tabs
        findViewById(R.id.redact_tab_button).setOnClickListener(v -> onClickTabButton(R.id.redact_tab_button, R.id.redact_tab));

        //  Tabs
        findViewById(R.id.annotate_tab_button).setOnClickListener(v -> onClickTabButton(R.id.annotate_tab_button, R.id.annotate_tab));

        //  Tabs
        findViewById(R.id.other_tab_button).setOnClickListener(v -> onClickTabButton(R.id.other_tab_button, R.id.other_tab));

        findViewById(R.id.scale_tab_button).setOnClickListener(v -> onClickTabButton(R.id.scale_tab_button, R.id.scale_tab));

        onClickTabButton(R.id.file_tab_button, R.id.file_tab);
    }

    private void scrollBy(int amount) {
        //  collect the scale and position values
        float scale = mDocumentView.getScaleFactor();
        int sx = mDocumentView.getScrollPositionX();
        int sy = mDocumentView.getScrollPositionY();
        //  add the amount
        sy += amount;
        //  set the new values
        mDocumentView.setScaleAndScroll(scale, sx, sy);
    }

    private void scaleBy(float increment) {
        //  collect the scale and position values
        float scale = mDocumentView.getScaleFactor();
        int sx = mDocumentView.getScrollPositionX();
        int sy = mDocumentView.getScrollPositionY();

        //  new scale factor
        float newScale = scale + increment;

        //  Applications may want to adjust scroll values
        //  in an attempt to maintain focus on a particular point.
        int newSx = sx;
        int newSy = sy;
        mDocumentView.setScaleAndScroll(newScale, newSx, newSy);
    }

    private void onClickTabButton(int buttonId, int tabId) {
        //  a tab button ws clicked.

        //  show the given tab
        findViewById(R.id.file_tab).setVisibility(View.GONE);
        findViewById(R.id.pages_tab).setVisibility(View.GONE);
        findViewById(R.id.redact_tab).setVisibility(View.GONE);
        findViewById(R.id.annotate_tab).setVisibility(View.GONE);
        findViewById(R.id.other_tab).setVisibility(View.GONE);
        findViewById(R.id.scale_tab).setVisibility(View.GONE);
        findViewById(tabId).setVisibility(View.VISIBLE);

        findViewById(R.id.file_tab_button).setSelected(false);
        findViewById(R.id.pages_tab_button).setSelected(false);
        findViewById(R.id.redact_tab_button).setSelected(false);
        findViewById(R.id.annotate_tab_button).setSelected(false);
        findViewById(R.id.other_tab_button).setSelected(false);
        findViewById(R.id.scale_tab_button).setSelected(false);
        findViewById(buttonId).setSelected(true);
    }

    private void setToggleButtonText(Button button, Boolean val, String text1, String text2) {
        if (val) {
            button.setText(text1);
        } else {
            button.setText(text2);
        }
        button.measure(0, 0);
        button.requestLayout();
    }

    private void updateUI() {
        //  this is called every time the UI should be updated.
        //  triggered internally by the selection changing (mostly).

        //  pages button
        Button pagesButton = findViewById(R.id.toggle_pages);
        boolean visible = mDocumentView.isPageListVisible();
        setToggleButtonText(pagesButton, visible, "Hide Pages", "Show Pages");

        //  delete selection button
        findViewById(R.id.delete_selection).setEnabled(mDocumentView.canDeleteSelection());

        //  draw on/off button
        boolean inkDrawMode = mDocumentView.isDrawModeOn();
        Button drawButton = findViewById(R.id.toggle_draw);
        setToggleButtonText(drawButton, inkDrawMode, "Draw Off", "Draw On");

        //  line color and thickness buttons
        findViewById(R.id.line_color).setEnabled(inkDrawMode);
        findViewById(R.id.line_thickness).setEnabled(inkDrawMode);

        //  highlight button
        findViewById(R.id.highlight_selection).setEnabled(mDocumentView.isAlterableTextSelection());

        //  save button
        findViewById(R.id.save).setEnabled(mDocumentView.isDocumentModified());

        //  note on/off button
        boolean noteMode = mDocumentView.isNoteModeOn();
        Button noteButton = findViewById(R.id.toggle_note);
        setToggleButtonText(noteButton, noteMode, "Note Off", "Note On");

        //  TOC button
        findViewById(R.id.table_of_contents).setEnabled(mDocumentView.isTOCEnabled());

        //  page number - see DocumentView.ChangePageListener, above

        //  redaction
        findViewById(R.id.redact_mark).setEnabled(mDocumentView.canMarkTextRedaction());
        findViewById(R.id.redact_remove).setEnabled(mDocumentView.canRemoveRedaction());
        findViewById(R.id.redact_apply).setEnabled(mDocumentView.canApplyRedactions());

        Button markAreaButton = findViewById(R.id.redact_mark_area);
        boolean isMarking = mDocumentView.redactIsMarkingArea();
        setToggleButtonText(markAreaButton, isMarking, "Marking...", "Mark Area");
    }

    private void handlePassword() {
        //  this function presents a dialog to collect the password, and then
        //  calls into DocumentView to provide it.
        //  if the password is wrong, we'll come here again.
        Context context = this;

        AlertDialog.Builder dBuilder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        dBuilder.setTitle("Password:");
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        dBuilder.setView(input);

        // Set up the buttons
        dBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //  provide the password.
                        mDocumentView.providePassword(input.getText().toString());
                    }
                });

        dBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel the operation.
                        dialog.cancel();
                        DocumentActivity.this.finish();
                    }
                });

        dBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Cancel the operation.
                dialog.cancel();
                DocumentActivity.this.finish();
            }
        });

        dBuilder.show();
    }

    public class TOCEntry {
        public int handle;
        public int parentHandle;
        public String label;
        private String url;
        private int page;
        private float x;
        private float y;

        TOCEntry(int handle, int parentHandle, int page, String label, String url, float x, float y) {
            this.handle = handle;
            this.parentHandle = parentHandle;
            this.page = page;
            this.label = label;
            this.url = url;
            this.x = x;
            this.y = y;
        }
    }

    private void showMessage(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                    }
                }).show();
    }
}
