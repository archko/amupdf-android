package com.artifex.sonui.editor;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import androidx.core.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.artifex.solib.ArDkBitmap;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkSelectionLimits;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.FileUtils;
import com.artifex.solib.SOClipboardHandler;
import com.artifex.solib.SODoc;
import com.artifex.solib.SODocSaveListener;
import com.artifex.solib.SOSearchListener;
import com.artifex.solib.ArDkUtils;

import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.ClassNotFoundException;
import java.lang.ExceptionInInitializerError;
import java.lang.LinkageError;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.IllegalArgumentException;
import java.util.Map;

public class NUIDocView
        extends FrameLayout
        implements TabHost.OnTabChangeListener, DocViewHost, View.OnClickListener
{
    private final String mDebugTag = "NUIDocView";

    protected Activity activity() {return (Activity)getContext();}
    private boolean mStarted = false;
    protected boolean mShowUI = true;

    protected SODocSession mSession;
    private Boolean mEndSessionSilent;
    private SOFileState mFileState;
    private SOFileDatabase mFileDatabase;
    private ProgressDialog mProgressDialog;
    private boolean mProgressStarted = false;
    protected int mPageCount;
    private DocView mDocView;
    private DocListPagesView mDocPageListView;
    private String mCustomDocdata;
    private String mDocUserPath;
    protected PageAdapter mAdapter;
    private SOSearchListener mSearchListener = null;

    private SODataLeakHandlers mDataLeakHandlers;
    protected ToolbarButton mSaveButton;
    protected ToolbarButton mSaveAsButton;
    protected ToolbarButton mSavePdfButton;
    protected ToolbarButton mPrintButton;
    private ToolbarButton mShareButton;
    protected ToolbarButton mOpenInButton;
    protected ToolbarButton mOpenPdfInButton;
    protected ToolbarButton mProtectButton;

    private ToolbarButton mCustomSaveButton;

    protected Button mUndoButton;
    protected Button mRedoButton;

    private Button mSearchButton;
    private Button mFullscreenButton;
    private ImageView mSearchClear;
    private SOEditText mSearchText;
    private ToolbarButton mSearchNextButton;
    private ToolbarButton mSearchPreviousButton;
    private SOTextView mFooterText;
    private View mFooterLead;

    protected ToolbarButton mCopyButton2; // copy button in file menu,
    // this is enabled when editing is disabled.

    protected ToolbarButton mInsertImageButton;
    protected ToolbarButton mInsertPhotoButton;

    private SOTextView mFooter;
    private int mVersionTapCount = 0;
    private long mVersionLastTapTime = 0;
    private static final int VERSION_TAP_INTERVAL = 500;  //  msec

    //  back button
    protected ImageButton mBackButton;

    //  page buttons
    private ToolbarButton mFirstPageButton;
    private ToolbarButton mLastPageButton;
    private ToolbarButton mReflowButton;

    //  start page, get and set
    private int mStartPage=-1;
    protected void setStartPage(int page) {mStartPage = page;}
    protected int getStartPage() {return mStartPage;}

    //  viewing state
    protected ViewingState mViewingState;

    //  Factor by which our rendering buffer is greater than the screen in both width and height
    public final static int OVERSIZE_PERCENT = 20;
    public static int OVERSIZE_MARGIN;

    //  bitmaps for rendering.  We get these from the library, and then hand them to the DocViews.
    private ArDkBitmap[] bitmaps = {null,null};

    protected boolean mIsSession = false;
    private boolean mIsTemplate = false;
    protected SOFileState mState = null;
    protected Uri mStartUri = null;
    private NUIView.OnDoneListener mOnDoneListener = null;
    protected ConfigOptions mDocCfgOptions = null;
    private String mForeignData = null;

    // The last active keyboard type.
    private int mPrevKeyboard = -1;

    // indicates the IME is currently composing text
    private boolean mIsComposing = false;

    //  track if we're active via pause/resume
    private boolean mIsActivityActive = false;
    protected boolean isActivityActive() {return mIsActivityActive;}

    //  current page number
    protected int mCurrentPageNum = 0;


    protected ListPopupWindow   mListPopupWindow;
    protected ArrayList<String> mAllTabHostTabs   = new ArrayList<String>();
    protected Map<String, View> tabMap            = new HashMap<String, View>();

    //  tab host
    protected TabHost tabHost=null;

    protected class TabData
    {
        public String name;
        public int contentId;
        public int layoutId;
        public int visibility;
        public TabData(String name, int contentId, int layoutId, int visibility)
        {
            this.name = name;
            this.contentId = contentId;
            this.layoutId = layoutId;
            this.visibility = visibility;
        }
    }

    //  data describing the tabs to be displayed.
    protected NUIDocView.TabData[] mTabs = null;

    //  the library that's in charge of this document
    private ArDkLib mDocumentLib;

    //  a list of files to delete when the document is closed
    private ArrayList<String> mDeleteOnClose = new ArrayList<>();
    public void addDeleteOnClose(String path) {mDeleteOnClose.add(path);}

    //  the input view for this document
    private InputView mInputView = null;
    public InputView getInputView(){return mInputView;}

    //  the currently active instance of us.
    private static NUIDocView mCurrentNUIDocView=null;
    public static NUIDocView currentNUIDocView() {return mCurrentNUIDocView;}

    private View mDecorView = null;

    //  indicates whether the back button or back arrow can be used.
    //  after the document has been opened, this gets set to true.
    private boolean mCanGoBack = false;

    //  cough up the view's current session
    public SODocSession getSession() {return mSession;}

    private void setupBitmaps()
    {
        //  create bitmaps
        createBitmaps();
        setBitmapsInViews();
    }

    private void createBitmaps()
    {
        //  get current screen size
        Point displaySize = Utilities.getRealScreenSize(activity());

        //  make two bitmaps.
        //  make them large enough for both screen orientations, so we don't have to
        //  change them when the orientation changes.
        //  This relates to the fact that we can't use Bitmap.reconfigure() in ArDkBitmap due
        //  to our minimum API.

        int screenSize = Math.max(displaySize.x, displaySize.y);
        int bitmapSize = screenSize*(100+OVERSIZE_PERCENT)/100;
        OVERSIZE_MARGIN = (bitmapSize-screenSize)/2;

        for (int i=0; i<bitmaps.length; i++)
            bitmaps[i] = mDocumentLib.createBitmap(bitmapSize, bitmapSize);
    }

    public NUIDocView(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    // called right after this class is instantiated.
    public void setDocSpecifics(ConfigOptions cfgOpts, SODataLeakHandlers leakHandlers)
    {
        mDocCfgOptions    = cfgOpts;
        mDataLeakHandlers = leakHandlers;
    }

    private void checkKeyboardVisibility()
    {
        // Save the current keybaord height value.
        int lastKeyboardHeight = keyboardHeight;

        //  get decor view height
        final int decorViewHeight = mDecorView.getHeight();

        //  assume soft keyboards take up at least 15 percent of the decor view height
        final int minKeyboardHeight = decorViewHeight * 15 / 100;

        //  difference between decor view and visible frame is the
        //  soft keyboard height
        final Rect windowVisibleDisplayFrame = new Rect();
        mDecorView.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame);
        keyboardHeight = decorViewHeight - windowVisibleDisplayFrame.bottom;

        //  adjust keyboard height by the height of the soft nav bar.
        //  This became necessary after the switch to AppCompatActivity
        //  Without this adjustment, we judge the keyboard to be larger than it is,
        //  resulting in a blank space above it.

        Resources resources = getContext().getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if ((id>0 && resources.getBoolean(id)) || Utilities.isEmulator())
        {
            int nbh = 0;
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                nbh =  resources.getDimensionPixelSize(resourceId);
            }
            keyboardHeight -= nbh;
        }

        if (keyboardHeight >= minKeyboardHeight)
        {
            //  keyboard is showing
            if (lastKeyboardHeight != keyboardHeight)
                onShowKeyboard(true);
        }
        else
        {
            //  keyboard is not showing
            keyboardHeight = 0;
            if (keyboardShown)
                onShowKeyboard(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        //  fix https://bugs.ghostscript.com/show_bug.cgi?id=702943
        if (!mFinished)
            checkKeyboardVisibility();

        //  call the super
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initialize(Context context)
    {
        // Top-level window decor view.
        mDecorView = ((Activity)getContext()).getWindow().getDecorView();

        // Initialise the utilities class
        FileUtils.init(context);

        // Store the initial keyboard type.
        mPrevKeyboard = context.getResources().getConfiguration().keyboard;
    }

    private int keyboardHeight = 0;
    public int getKeyboardHeight() {return keyboardHeight;}

    protected int getLayoutId()
    {
        return 0;
    }

    //  this function recursively enables or disables our view and its children,
    //  except for the soft Back button.
    //  it's called when the document fails to load.
    private void setViewAndChildrenEnabled(View view, boolean enabled)
    {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup)
        {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++)
            {
                View child = viewGroup.getChildAt(i);
                //  leave the soft Back button alone
                if (child != mBackButton)
                    setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    protected void disableUI()
    {
        setViewAndChildrenEnabled(this, false);
    }

    private void startUI()
    {
        mStarted = false;

        //  inflate the UI
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewGroup view = (ViewGroup) inflater.inflate(getLayoutId(), null);

        final ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                NUIDocView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!mStarted)
                {
                    enforceInitialShowUI(view);
                    afterFirstLayoutComplete();

                    mStarted = true;
                }
            }
        });

        addView(view);
    }

    protected void enforceInitialShowUI(View view)
    {
        boolean bShow = mDocCfgOptions.showUI();
        View tabhost = view.findViewById(R.id.tabhost);
        if (tabhost != null)
            tabhost.setVisibility(bShow? View.VISIBLE:View.GONE);
        view.findViewById(R.id.footer).setVisibility(bShow?View.VISIBLE:View.GONE);

        //  hide the UI if we were told to when we were started
        if (!mShowUI)
        {
            if (tabhost != null)
                tabhost.setVisibility(View.GONE);
            view.findViewById(R.id.header).setVisibility(View.GONE);
            view.findViewById(R.id.footer).setVisibility(View.GONE);
        }

        mFullscreen = !bShow;
    }

    private void initClipboardHandler()
    {
        String errorBase =
            "initClipboardHandler() experienced unexpected exception [%s]";

        try
        {
            //  find a registered handler
            SOClipboardHandler handler = ArDkLib.getClipboardHandler();
            if (handler==null)
                throw new ClassNotFoundException();

            handler.initClipboardHandler(activity());
        }
        catch (ExceptionInInitializerError e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                             "ExceptionInInitializerError"));
        }
        catch (LinkageError e)
        {
            Log.e(mDebugTag, String.format(errorBase, "LinkageError"));
        }
        catch (SecurityException e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                                           "SecurityException"));
        }
        catch (ClassNotFoundException e)
        {
            Log.i(mDebugTag, "initClipboardHandler implementation unavailable");
        }
    }

    /*
     * Instantiate an instance of the application data leakage handler class
     * if available.
     */
    private void setDataLeakHandlers()
    {
        String errorBase =
            "setDataLeakHandlers() experienced unexpected exception [%s]";

        try
        {
            //  find a registered instance
            if (mDataLeakHandlers==null)
                throw new ClassNotFoundException();

            mDataLeakHandlers.initDataLeakHandlers(activity(), mDocCfgOptions);
        }
        catch (ExceptionInInitializerError e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                             "ExceptionInInitializerError"));
        }
        catch (LinkageError e)
        {
            Log.e(mDebugTag, String.format(errorBase, "LinkageError"));
        }
        catch (SecurityException e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                                           "SecurityException"));
        }
        catch (ClassNotFoundException e)
        {
            Log.i(mDebugTag, "DataLeakHandlers implementation unavailable");
        }
        catch (IOException e)
        {
            Log.i(mDebugTag, "DataLeakHandlers IOException");
        }
    }

    //  get the user path for the doc used in this view
    public String getDocFileExtension()
    {
        String ext;

        if (mState != null)
            ext = FileUtils.getExtension(mState.getUserPath());
        else if (mSession != null)
            ext = FileUtils.getExtension(mSession.getUserPath());
        else
            ext = FileUtils.getFileTypeExtension(getContext(), mStartUri);

        return ext;
    }

    //  start given a path
    public void start(final Uri uri, boolean template, ViewingState viewingState, String customDocData, NUIView.OnDoneListener listener, boolean showUI)
    {
        mIsTemplate = template;
        mStartUri = uri;
        mCustomDocdata = customDocData;
        mOnDoneListener = listener;
        String ext = FileUtils.getFileTypeExtension(getContext(), uri);
        mDocumentLib = ArDkUtils.getLibraryForPath(activity(), "filename."+ext);
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);

        mShowUI = showUI;
        startUI();
        setDataLeakHandlers();
        initClipboardHandler();
    }

    //  start given a session
    public void start(SODocSession session, ViewingState viewingState, String foreignData, NUIView.OnDoneListener listener)
    {
        mIsSession = true;
        mSession = session;
        mIsTemplate = false;
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);
        mOnDoneListener = listener;
        mForeignData = foreignData;
        mDocumentLib = ArDkUtils.getLibraryForPath(activity(), session.getUserPath());

        startUI();
        setDataLeakHandlers();
        initClipboardHandler();
    }

    //  start given a saved auto-open state
    public void start(SOFileState state, ViewingState viewingState, NUIView.OnDoneListener listener)
    {
        mIsSession = false;
        mIsTemplate = state.isTemplate();
        mState = state;
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);
        mOnDoneListener = listener;
        mDocumentLib = ArDkUtils.getLibraryForPath(activity(), state.getOpenedPath());

        startUI();
        setDataLeakHandlers();
        initClipboardHandler();
    }

    protected boolean hasSearch() {return true;}
    protected boolean hasUndo() {return true;}
    protected boolean hasRedo() {return true;}

    protected boolean canSelect() {return true;}
    protected boolean hasReflow() {return false;}

//    //  edit toolbar object
//    protected NUIEditToolbar mNuiEditToolbar;

    protected void afterFirstLayoutComplete()
    {
        mFinished = false;

        /*
         * this is done here, and in ExplorerActivity to support docs being
         * sent to us.
         */
        if (mDocCfgOptions.usePersistentFileState())
        {
            SOFileDatabase.init(activity());
        }

        createEditButtons();
        createEditButtons2();
        createReviewButtons();
        createPagesButtons();
        createInsertButtons();

        mBackButton           = (ImageButton) createToolbarButton(R.id.back_button);
        mUndoButton           = (Button)    createToolbarButton(R.id.undo_button);
        mRedoButton           = (Button)    createToolbarButton(R.id.redo_button);
        mSearchButton         = (Button)    createToolbarButton(R.id.search_button);
        mFullscreenButton     = (Button)    createToolbarButton(R.id.fullscreen_button);
        mSearchNextButton     = (ToolbarButton) createToolbarButton(R.id.search_next);
        mSearchPreviousButton = (ToolbarButton) createToolbarButton(R.id.search_previous);

        if (!hasSearch() && mSearchButton!=null)
            mSearchButton.setVisibility(View.GONE);
        if (!hasUndo() && mUndoButton!=null)
            mUndoButton.setVisibility(View.GONE);
        if (!hasRedo() && mRedoButton!=null)
            mRedoButton.setVisibility(View.GONE);

        if (!mDocCfgOptions.isFullscreenEnabled() && mFullscreenButton!=null)
            mFullscreenButton.setVisibility(View.GONE);

        // hide Redo/Undo buttons in read-only mode.
        if (! mDocCfgOptions.isEditingEnabled())
        {
            if (mUndoButton!=null)
                mUndoButton.setVisibility(View.GONE);
            if (mRedoButton!=null)
                mRedoButton.setVisibility(View.GONE);
        }

        showSearchSelected(false);
        mSearchText = (SOEditText) findViewById(R.id.search_text_input);

        mFooterText = (SOTextView)findViewById(R.id.footer_page_text);
        mFooterLead = findViewById(R.id.footer_lead);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onSearchNext(null);
                    return true;
                }
                return false;
            }
        });

        //  fix 700402
        mSearchText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode==KeyEvent.KEYCODE_DEL) {
                    //  Swallow KEYCODE_DEL if the cursor is sitting at the beginning
                    if (mSearchText.getSelectionStart()==0 && mSearchText.getSelectionEnd()==0)
                        return true;
                }
                return false;
            }
        });

        //  start with the search buttons disabled
        mSearchNextButton.setEnabled(false);
        mSearchPreviousButton.setEnabled(false);

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                //  whenever the search text changes, reset the search start
                setSearchStart();

                //  enable the search buttons if there is text in the field.
                mSearchNextButton.setEnabled(s.toString().length()>0);
                mSearchPreviousButton.setEnabled(s.toString().length()>0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //  fix 700195, 700215
        //  We use an ActionMode.Callback() to manage which menu items appear
        //  in the edit field's native UI, depending on certain ConfigOptions.

        ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

                //  a list of items to be removed
                ArrayList<Integer> itemsToRemove = new ArrayList<>();

                int numItems = menu.size();
                for (int i=0; i<numItems; i++)
                {
                    // get the item and its ID
                    MenuItem item = menu.getItem(i);
                    int itemId = item.getItemId();

                    //  default is to remove it
                    boolean remove = true;

                    //  keep share if enabled
                    if (mDocCfgOptions.isShareEnabled() && itemId==android.R.id.shareText)
                        remove = false;

                    //  keep cut and copy if enabled
                    if (mDocCfgOptions.isExtClipboardOutEnabled() && (itemId==android.R.id.cut || itemId==android.R.id.copy))
                        remove = false;

                    //  keep paste if enabled
                    if (mDocCfgOptions.isExtClipboardInEnabled() && itemId==android.R.id.paste)
                        remove = false;

                    //  always keep select all
                    if (item.getItemId()==android.R.id.selectAll)
                        remove = false;

                    if (remove)
                        itemsToRemove.add(itemId);
                }

                //  now remove the items marked for removal
                for (Integer x : itemsToRemove) {
                    menu.removeItem(x);
                }

                return true;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }
        };

        mSearchText.setCustomSelectionActionModeCallback(actionModeCallback);
        mSearchText.setCustomInsertionActionModeCallback(actionModeCallback);

        //  clear the search text when the clear button is tapped.
        mSearchClear = (ImageView) findViewById(R.id.search_text_clear);
        mSearchClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ignore button presses while we are finishing up.
                if (mFinished)
                {
                    return;
                }

                mSearchText.setText("");
            }
        });

        mSaveButton      = (ToolbarButton) createToolbarButton(R.id.save_button);
        mSaveAsButton    = (ToolbarButton) createToolbarButton(R.id.save_as_button);
        mSavePdfButton   = (ToolbarButton) createToolbarButton(R.id.save_pdf_button);
        mPrintButton     = (ToolbarButton) createToolbarButton(R.id.print_button);
        mShareButton     = (ToolbarButton) createToolbarButton(R.id.share_button);
        mOpenInButton    = (ToolbarButton) createToolbarButton(R.id.open_in_button);
        mOpenPdfInButton = (ToolbarButton) createToolbarButton(R.id.open_pdf_in_button);
        mProtectButton   = (ToolbarButton) createToolbarButton(R.id.protect_button);
        mCopyButton2     = (ToolbarButton) createToolbarButton(R.id.copy_button2);

        /*
         * Initialise the custom save button only if the required resource is
         * available.
         */
        int customSaveId =
            getContext().getResources().getIdentifier(
                                                   "custom_save_button",
                                                   "id",
                                                   getContext().getPackageName());

        if (customSaveId != 0)
        {
            mCustomSaveButton = (ToolbarButton) createToolbarButton(customSaveId);
        }

        //  set up tabs
        setupTabs();

        //Continue setup
        onDeviceSizeChange();

        // Hide/Show buttons as per the current configration.
        setConfigurableButtons();

        // fix for 699283
        fixFileToolbar(R.id.file_toolbar);

        //  make the adapter and view
        mAdapter = createAdapter();
        mDocView = createMainView(activity());
        mDocView.setHost(this);
        mDocView.setAdapter(mAdapter);
        mDocView.setDocSpecifics(mDocCfgOptions, mDataLeakHandlers);

        if (usePagesView())
        {
            mDocPageListView = new DocListPagesView(activity());
            mDocPageListView.setHost(this);
            mDocPageListView.setAdapter(mAdapter);
            mDocPageListView.setMainView(mDocView);
            mDocPageListView.setBorderColor(mDocView.getBorderColor());
            mDocPageListView.setDocSpecifics(mDocCfgOptions, mDataLeakHandlers);
        }

        //  add the view to the layout
        //  and set up its drag handles
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.doc_inner_container);
        layout.addView(mDocView,0);
        mDocView.setup(layout);

        if (usePagesView())
        {
            //  pages container
            RelativeLayout layout2 = (RelativeLayout) findViewById(R.id.pages_container);
            layout2.addView(mDocPageListView);
            mDocPageListView.setup(layout2);
            mDocPageListView.setCanManipulatePages(canCanManipulatePages());
        }

        //  footer
        mFooter = (SOTextView) findViewById(R.id.footer_text);

        //  check if we need to modify tabhost background colour
        LinearLayout headertop = findViewById(R.id.header_top);
        if (headertop != null)
            headertop.setBackgroundColor( getTabUnselectedColor() );

        View headertopspacer = findViewById(R.id.header_top_spacer);
        if (headertopspacer != null)
            headertopspacer.setBackgroundColor( getTabUnselectedColor() );

        findViewById(R.id.footer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Ignore button presses while we are finishing up.
                if (mFinished)
                {
                    return;
                }

                long now = System.currentTimeMillis();
                if (now-mVersionLastTapTime > VERSION_TAP_INTERVAL)
                    mVersionTapCount=1;
                else
                    mVersionTapCount++;

                if (mVersionTapCount==5)
                {
                    //  four bits of info
                    String date     = "";
                    String issue    = "";
                    String version  = "";
                    String customer = "";

                    //  get three of them from the library
                    String []verInfo = ArDkUtils.getLibVersionInfo(getContext());
                    if (verInfo != null)
                    {
                        date = verInfo[0];
                        issue = verInfo[1];
                        //  ignoring verInfo[2]
                        customer = verInfo[3];
                    }

                    //  get the fourth one from the package
                    ApplicationInfo info = getContext().getApplicationInfo();
                    PackageInfo pInfo = null;
                    try {
                        pInfo = getContext().getPackageManager().getPackageInfo(info.packageName, 0);
                        version = pInfo.versionName;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    //  show the message
                    Utilities.showMessage((Activity)getContext(), getContext().getString(R.string.sodk_editor_version_title),
                            String.format(getContext().getString(R.string.sodk_editor_version_format), date, issue, version, customer));

                    mVersionTapCount = 0;
                }
                mVersionLastTapTime = now;
            }
        });

        if (mDocCfgOptions.usePersistentFileState())
        {
            //  create a file database
            mFileDatabase = SOFileDatabase.getDatabase();
        }

        //  final copy of us
        final Activity thisActivity = activity();
        final NUIDocView ndv = this;

        //  watch for a possible orientation change
        final ViewTreeObserver observer3 = getViewTreeObserver();
        observer3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                //  this layout listener stays active forever, so don't use it
                //  after we've finished.
                //  mDocView.finished() is added to not access a destroyed document in the
                //  subsequent onPossibleOrientationChange() call.
                if (mFinished || mDocView.finished()) {
                    observer3.removeOnGlobalLayoutListener(this);
                    return;
                }

                onPossibleOrientationChange();
            }
        });

        //  wait for the initial layout before loading the doc
        ViewTreeObserver observer = mDocView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDocView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                onNewTabShown(mCurrentTab);

                setupBitmaps();

                if (mIsSession)
                {
                    //  if the session was created earlier,
                    //  a load error may have happened before we arrive here.
                    //  so we should disable the UI.
                    if (mSession.hasLoadError())
                        disableUI();

                    mDocUserPath = mSession.getUserPath();

                    if (! mDocCfgOptions.usePersistentFileState())
                    {
                        // We do not expect this entry point when use of the
                        // file state database is disabled.
                        throw new UnsupportedOperationException();
                    }

                    //  make a state for this file and open it.
                    //  This will cause an internal copy to be created
                    mFileState = mFileDatabase.stateForPath(mDocUserPath, mIsTemplate);

                    mFileState.setForeignData(mForeignData);

                    mSession.setFileState(mFileState);
                    mFileState.openFile(mIsTemplate);

                    //  when opened from preview, assume we're starting with no changes.
                    mFileState.setHasChanges(false);

                    setFooterText(mFileState.getUserPath());

                    //  give the open doc to the adapter and views
                    mDocView.setDoc(mSession.getDoc());
                    if (usePagesView())
                        mDocPageListView.setDoc(mSession.getDoc());
                    mAdapter.setDoc(mSession.getDoc());

                    //  if we have an optional document listener,
                    //  set a password Runnable in the session.
                    if (mDocumentListener!=null) {
                        mSession.setPasswordHandler(new Runnable() {
                            @Override
                            public void run() {
                                mDocumentListener.onPasswordRequired();
                            }
                        });
                    }

                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
                        @Override
                        public void onPageLoad(int pageNum) {
                            //  next page has loaded.
                            endProgress();
                            onPageLoaded(pageNum);
                        }

                        @Override
                        public void onDocComplete() {
                            //  all pages have loaded
                            endProgress();
                            onDocCompleted();
                            setPageNumberText();
                        }

                        @Override
                        public void onError(int error, int errorNum) {
                            if (mSession.isOpen()) {
                                disableUI();
                                endProgress();
                                //  There was an error, show a message
                                //  but don't show 'aborted error message' if doc loading was cancelled by user.
                                if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
                                    String message = Utilities.getOpenErrorDescription(getContext(), error);
                                    Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_error), message);
                                }
                            }
                        }

                        @Override
                        public void onCancel()
                        {
                            //  loading was cancelled
                            endProgress();
                        }

                        @Override
                        public void onSelectionChanged(final int startPage, final int endPage)
                        {
                            onSelectionMonitor(startPage, endPage);
                        }

                        @Override
                        public void onLayoutCompleted()
                        {
                            startProgress();
                            onLayoutChanged();
                        }
                    });

                    if (usePagesView())
                    {
                        int pagelist_width_percentage = getResources().getInteger(R.integer.sodk_editor_pagelist_width_percentage);
                        float scale = ((float) pagelist_width_percentage) / 100f;
                        mDocPageListView.setScale(scale);
                    }

                }
                else if (mState!=null)
                {
                    if (! mDocCfgOptions.usePersistentFileState())
                    {
                        // We do not expect this entry point when use of the
                        // file state database is disabled.
                        throw new UnsupportedOperationException();
                    }

                    //  this is the auto-open case

                    //  get the path from the state
                    mDocUserPath = mState.getOpenedPath();

                    //  TODO: where to get this path for cloud files?
                    setFooterText(mState.getUserPath());

                    //  open the given state (don't make a new one)
                    mFileState = mState;
                    mFileState.openFile(mIsTemplate);

                    //  create a session
                    mSession = new SODocSession(thisActivity, mDocumentLib);
                    mSession.setFileState(mFileState);

                    //  if we have an optional document listener,
                    //  set a password Runnable in the session.
                    if (mDocumentListener!=null) {
                        mSession.setPasswordHandler(new Runnable() {
                            @Override
                            public void run() {
                                mDocumentListener.onPasswordRequired();
                            }
                        });
                    }

                    //  establish file loading listener
                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
                        @Override
                        public void onPageLoad(int pageNum) {
                            if(mFinished)
                                return;
                            //  next page has loaded.
                            endProgress();
                            onPageLoaded(pageNum);
                        }

                        @Override
                        public void onDocComplete() {
                            if(mFinished)
                                return;
                            //  all pages have loaded
                            endProgress();
                            onDocCompleted();
                        }

                        @Override
                        public void onError(int error, int errorNum) {
                            if(mFinished)
                                return;
                            disableUI();
                            endProgress();
                            //  There was an error, show a message
                            //  but don't show 'aborted error message' if doc loading was cancelled by user.
                            if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
                                String message = Utilities.getOpenErrorDescription(getContext(), error);
                                Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_error), message);
                            }
                        }

                        @Override
                        public void onCancel()
                        {
                            //  loading was cancelled
                            endProgress();
                        }

                        @Override
                        public void onSelectionChanged(final int startPage, final int endPage)
                        {
                            onSelectionMonitor(startPage, endPage);
                        }

                        @Override
                        public void onLayoutCompleted()
                        {
                            startProgress();
                            onLayoutChanged();
                        }
                    });

                    //  ... and we open the state's internal file.
                    mSession.open(mFileState.getInternalPath(), mDocCfgOptions);

                    //  give the open doc to the adapter and views
                    mDocView.setDoc(mSession.getDoc());
                    if (usePagesView())
                        mDocPageListView.setDoc(mSession.getDoc());
                    mAdapter.setDoc(mSession.getDoc());

                    if (usePagesView())
                        mDocPageListView.setScale(0.20f);
                }
                else
                {
                    //  export the content to a temp file
                    Uri    uri    = mStartUri;
                    String scheme = uri.getScheme();

                    if (scheme != null &&
                        scheme.equalsIgnoreCase(ContentResolver.SCHEME_CONTENT))
                    {
                        //  make a tmp file from the content
                        String path =
                            FileUtils.exportContentUri(getContext(), uri);

                        if (path.equals("---fileOpen"))
                        {
                            //  special case indicating that the file could not be opened.
                            Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_content_error),
                                    getContext().getString(R.string.sodk_editor_error_opening_from_other_app));
                            return;
                        }

                        if (path.startsWith("---"))
                        {
                            /*
                             * if the path starts with "---", that
                             * indicates that we should display
                             * an exception message.
                             */
                            String message = getResources().getString(R.string.sodk_editor_cant_create_temp_file);
                            Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_content_error),
                                    getContext().getString(R.string.sodk_editor_error_opening_from_other_app) + ": \n\n" + message);

                            return;
                        }

                        mDocUserPath = path;

                        if (mIsTemplate)
                            addDeleteOnClose(path);
                    }
                    else
                    {
                        mDocUserPath = uri.getPath();

                        if (mDocUserPath == null)
                        {
                            Utilities.showMessage(
                                thisActivity,
                                getContext().getString(R.string.sodk_editor_invalid_file_name),
                                getContext().getString(R.string.sodk_editor_error_opening_from_other_app));

                            Log.e(mDebugTag,
                                  " Uri has no path: " + uri.toString());
                            return;
                        }
                    }

                    setFooterText(mDocUserPath);

                    if (mDocCfgOptions.usePersistentFileState())
                    {
                        /*
                         * make a state for this file and open it.
                         * This will cause an internal copy to be created
                         */
                        mFileState = mFileDatabase.stateForPath(mDocUserPath,
                                                                mIsTemplate);
                    }
                    else
                    {
                        /*
                         * Use a dummy file state class that overrides operations
                         * we want to avoid.
                         */
                        mFileState = new SOFileStateDummy(mDocUserPath);
                    }
                    mFileState.openFile(mIsTemplate);

                    //  we are the target of a Open In or Share of some sort.
                    //  assume we have no changes.
                    mFileState.setHasChanges(false);

                    //  ... and we open that file instead.
                    mSession = new SODocSession(thisActivity, mDocumentLib);
                    mSession.setFileState(mFileState);

                    //  if we have an optional document listener,
                    //  set a password Runnable in the session.
                    if (mDocumentListener!=null) {
                        mSession.setPasswordHandler(new Runnable() {
                            @Override
                            public void run() {
                                mDocumentListener.onPasswordRequired();
                            }
                        });
                    }

                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
                        @Override
                        public void onPageLoad(int pageNum) {
                            if(mFinished)
                                return;
                            //  next page has loaded.
                            endProgress();
                            onPageLoaded(pageNum);
                        }

                        @Override
                        public void onDocComplete() {
                            if(mFinished)
                                return;
                            //  all pages have loaded
                            endProgress();
                            onDocCompleted();
                        }

                        @Override
                        public void onError(int error, int errorNum) {
                            if(mFinished)
                                return;
                            disableUI();
                            endProgress();
                            //  There was an error, show a message
                            //  but don't show 'aborted error message' if doc loading was cancelled by user.
                            if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
                                String message = Utilities.getOpenErrorDescription(getContext(), error);
                                Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_error), message);
                            }
                        }

                        @Override
                        public void onCancel()
                        {
                            //  loading was cancelled
                            disableUI();
                            endProgress();
                        }

                        @Override
                        public void onSelectionChanged(final int startPage, final int endPage)
                        {
                            onSelectionMonitor(startPage, endPage);
                        }

                        @Override
                        public void onLayoutCompleted()
                        {
                            startProgress();
                            onLayoutChanged();
                        }
                    });
                    mSession.open(mFileState.getInternalPath(), mDocCfgOptions);

                    //  give the open doc to the adapter and views
                    mDocView.setDoc(mSession.getDoc());
                    if (usePagesView())
                        mDocPageListView.setDoc(mSession.getDoc());
                    mAdapter.setDoc(mSession.getDoc());

                    if (usePagesView())
                        mDocPageListView.setScale(0.20f);
                }

                //  create an InputView and add it to the layout.
                //  this view will get focus when the doc is tapped
                createInputView();

                //  now we allow the back button and back arrow
                mCanGoBack = true;
            }
        });

        if (Utilities.isPhoneDevice(activity()))
            scaleHeader();

        //  tell the main document view about the viewing state
        getDocView().setViewingState(mViewingState);
    }

    protected void createInputView()
    {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.doc_inner_container);
        mInputView = new InputView(getContext(), mSession.getDoc(), this);
        layout.addView(mInputView);
    }

    protected void resetInputView()
    {
        if (mInputView != null)
            mInputView.resetEditable();
    }

    protected void updateInputView()
    {
        if (mInputView != null)
            mInputView.updateEditable();
    }

    protected void focusInputView()
    {
        //  don't focus if editing is disabled
        if (!mDocCfgOptions.isEditingEnabled())
        {
            defocusInputView();
            return;
        }

        if (mInputView != null)
            mInputView.setFocus();
    }

    protected void defocusInputView()
    {
        if (mInputView != null)
            mInputView.clearFocus();
    }

    protected boolean inputViewHasFocus()
    {
        if (mInputView != null)
            return mInputView.hasFocus();
        return false;
    }

    private static final int ORIENTATION_PORTAIT = 1;
    private static final int ORIENTATION_LANDSCAPE = 2;
    private int mLastOrientation = 0;

    //  a way to trigger an orientation change
    private boolean mForceOrientationChange = false;
    public void triggerOrientationChange() {mForceOrientationChange=true;}

    private void onPossibleOrientationChange()
    {
        //  get current orientation
        Point p = Utilities.getRealScreenSize(activity());
        int orientation = ORIENTATION_PORTAIT;
        if (p.x>p.y)
            orientation = ORIENTATION_LANDSCAPE;

        //  see if it's changed, or if we've been triggered
        if ((mForceOrientationChange) || (orientation != mLastOrientation && mLastOrientation!=0))
            onOrientationChange();
        mForceOrientationChange = false;

        mLastOrientation = orientation;
    }

    protected void onOrientationChange()
    {
        mDocView.onOrientationChange();

        if (usePagesView())
            mDocPageListView.onOrientationChange();

        if (!isFullScreen())
            showUI(!keyboardShown);

        onDeviceSizeChange();
    }

    public void onConfigurationChange(Configuration newConfig)
    {
        /*
         * Clear any notion of compositing text the IME may have if the
         * keyboard type has changed..
         */
        if (newConfig!=null && newConfig.keyboard!=mPrevKeyboard)
        {
            resetInputView();
            mPrevKeyboard = newConfig.keyboard;
        }

        //  config was changed, probably orientation.
        //  call the doc view
        if (mDocView!=null)
            mDocView.onConfigurationChange();

        //  ,,, and the page list
        if (usePagesView() && mDocPageListView!=null)
            mDocPageListView.onConfigurationChange();
    }

    protected DocView createMainView(Activity activity)
    {
        return new DocView(activity);
    }

    protected boolean usePagesView() {return true;}

    protected void createInsertButtons()
    {
        mInsertImageButton =
            (ToolbarButton) createToolbarButton(R.id.insert_image_button);
        mInsertPhotoButton =
            (ToolbarButton) createToolbarButton(R.id.insert_photo_button);

    }

    protected void createEditButtons2()
    {
    }

    protected void createEditButtons()
    {
    }

    protected void createPagesButtons()
    {
        mFirstPageButton = (ToolbarButton) createToolbarButton(R.id.first_page_button);
        mLastPageButton  = (ToolbarButton) createToolbarButton(R.id.last_page_button);
        mReflowButton    = (ToolbarButton) createToolbarButton(R.id.reflow_button);

        //  begin with this disabled.  We'll enable it when a page has been loaded. (bug 700699)
        if (mReflowButton!=null)
            mReflowButton.setEnabled(false);

        ToolbarButton.setAllSameSize(new ToolbarButton[]{mFirstPageButton, mLastPageButton, mReflowButton});
    }

    protected void createReviewButtons()
    {
    }

    //  called from the main DocView whenever the pages list needs to be updated.
    public void setCurrentPage(int pageNumber)
    {
        if (usePagesView())
        {
            //  set the new current page and scroll there.
            mDocPageListView.setCurrentPage(pageNumber);
            mDocPageListView.scrollToPage(pageNumber, false);
        }

        //  keep track of the "current" page number
        mCurrentPageNum = pageNumber;

        //  and change the text in the lower right.
        setPageNumberText();

        //  save the current page number in the state
        mSession.getFileState().setPageNumber(mCurrentPageNum);
    }

    protected void setPageNumberText()
    {
        //  do this in a handler; we may be called during onLayout.
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                mFooterText.setText(getPageNumberText());

                //  making the leading space the same size will
                //  effectively center the name.
                mFooterText.measure(0,0);
                mFooterLead.getLayoutParams().width = mFooterText.getMeasuredWidth();
                mFooterLead.getLayoutParams().height = mFooterText.getMeasuredHeight();

                //  send  notification
                doUpdateCustomUI();
                if (mChangePageListener!=null)
                    mChangePageListener.onPage(mCurrentPageNum);
            }
        });
    }

    protected String getPageNumberText()
    {
        return String.format(getContext().getString(R.string.sodk_editor_page_d_of_d), mCurrentPageNum+1, getPageCount());
    }

    protected PageAdapter createAdapter()
    {
        return new PageAdapter(getContext(), this, PageAdapter.DOC_KIND);
    }

    protected void onDocCompleted()
    {
        //  not if we're finished
        if (mFinished)
            return;

        //  we may be called when pages have been added or removed
        //  so update the page count and re-layout
        mPageCount = mSession.getDoc().getNumPages();
        mAdapter.setCount(mPageCount);
        layoutNow();

        //  use the optional document listener
        if (mDocumentListener!=null)
            mDocumentListener.onDocCompleted();

        if (ArDkUtils.isTrackChangesEnabled(activity()))
        {
            // Each time we open a document, set the author for change tracking
            Object store =
                    Utilities.getPreferencesObject(activity(), Utilities.generalStore);

            String author =
                    Utilities.getStringPreference(
                            store,
                            "DocAuthKey",
                            Utilities.getApplicationName(activity()));

            mSession.getDoc().setAuthor(author);
        }
    }

    protected int getPageCount() {return mPageCount;}

    protected void setPageCount(int newCount)
    {
        mPageCount = newCount;
        mAdapter.setCount(newCount);
        requestLayouts();
    }

    private void requestLayouts()
    {
        mDocView.requestLayout();

        if (usePagesView() && isPageListVisible())
            mDocPageListView.requestLayout();
    }

    public void layoutNow()
    {
        //  cause the main and page list views to re-layout
        if (mDocView!=null)
            mDocView.layoutNow();
        if (mDocPageListView!=null && usePagesView() && isPageListVisible())
            mDocPageListView.layoutNow();
    }

    public void selectionupdated()
    {
        onSelectionChanged();
    }

    protected void layoutAfterPageLoad()
    {
        layoutNow();
    }

    protected void onPageLoaded(int pagesLoaded)
    {
        //  to see if this is the first call
        boolean first = (mPageCount==0);

        mPageCount = pagesLoaded;
        if (first)
        {
            setupSearch();
            updateUIAppearance();

            //  now enable the reflow button (bug 700699)
            if (mReflowButton!=null)
                mReflowButton.setEnabled(true);
        }

        int oldCount = mAdapter.getCount();
        boolean pagesChanged = (mPageCount != oldCount);
        if (mPageCount<oldCount)
        {
            //  if the number of pages goes down, then remove all the DocPageViews.
            //  The correct number of them will be re-inserted at the next layout.
            //  this fixes 698220 - Two identical editing lines appear on two pages
            mDocView.removeAllViewsInLayout();
            if (usePagesView())
                mDocPageListView.removeAllViewsInLayout();
        }
        mAdapter.setCount(mPageCount);

        if (pagesChanged)
        {
            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);
                    if (mFinished)
                        return;

                    if (mDocView.getReflowMode())
                        onReflowScale();
                    else
                        mDocView.scrollSelectionIntoView();
                }
            });
            layoutAfterPageLoad();

            //  use the optional document listener
            if (mDocumentListener!=null)
                mDocumentListener.onPageLoaded(mPageCount);
        }
        else
            requestLayouts();

        handleStartPage();

        //  update the page count text in the lower right no more that once/sec.
        if (!mIsWaiting)
        {
            mIsWaiting = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPageNumberText();
                    mIsWaiting = false;
                }
            }, 1000);
        }
    }

    private boolean mIsWaiting = false;

    private void reconfigurePrintButton()
    {
        if (mSession==null || mPrintButton==null)
            return;

        //  if the doc can't be printed, remove the print button.
        boolean canPrint = mSession.getDoc().canPrint();
        if (canPrint && (mDocCfgOptions.isPrintingEnabled() || mDocCfgOptions.isSecurePrintingEnabled()))
            mPrintButton.setVisibility(View.VISIBLE);
        else
            mPrintButton.setVisibility(View.GONE);
    }

    protected void showPagesTab(String id, int labelId)
    {
        //  This function sets the current tab to the given id
        //  without invoking onTabChanged.

        //  stop listening
        tabHost.setOnTabChangedListener(null);

        tabHost.setCurrentTabByTag(id);
        setTabColors(id);
        setSingleTabTitle(getContext().getString(labelId));
        mCurrentTab = id;
        measureTabs();

        //  start listening again
        tabHost.setOnTabChangedListener(this);
    }

    protected void showPagesTab()
    {
        showPagesTab("PAGES", R.string.sodk_editor_tab_pages);
    }

    protected void handleStartPage()
    {
        //  if we've been passed the start page, go there
        int start = getStartPage();
        if (start>=0 && getPageCount()>start)
        {
            setStartPage(-1);  //  but just once

            mDocView.setStartPage(start);
            mCurrentPageNum = start;
            setPageNumberText();

            if (mViewingState !=null)
            {
                //  show the page list and tab if requested
                if (mViewingState.pageListVisible) {
                    //  show the page list at the starting page
                    showPages(start);
                    showPagesTab();
                }

                //  give the doc view the viewing state
                //  these will get used in DocView.handleStartPage().
                getDocView().setScale(mViewingState.scale);
                getDocView().forceLayout();
            }

            // update print button state on a page loaded.
            reconfigurePrintButton();
        }
    }

    protected void onDeviceSizeChange()
    {
        View bba = findViewById(R.id.back_button_after);

        if (Utilities.isPhoneDevice(activity()))
        {
            scaleHeader();

            if (mSearchButton!=null)
                mSearchButton.setVisibility(View.GONE);

            if (tabHost!=null)
            {
                //  hide the main tabs
                hideMainTabs();

                //  show the single tab
                getSingleTabView().setVisibility(View.VISIBLE);
            }

            int space = (int) getContext().getResources().getDimension(R.dimen.sodk_editor_after_back_button_phone);
            bba.getLayoutParams().width = Utilities.convertDpToPixel(space);
        }
        else
        {
            if (hasSearch() && mSearchButton!=null)
                mSearchButton.setVisibility(View.VISIBLE);

            if (tabHost!=null)
            {
                //  show the main tabs
                showMainTabs();

                //  hide the single tab
                getSingleTabView().setVisibility(View.GONE);
            }

            int space = (int) getContext().getResources().getDimension(R.dimen.sodk_editor_after_back_button);
            bba.getLayoutParams().width = Utilities.convertDpToPixel(space);
        }
    }

    protected void scaleHeader()
    {
        float factor = 0.65f;

        //  do all of the toolbars
        scaleToolbar(R.id.annotate_toolbar, factor);
        scaleToolbar(R.id.doc_pages_toolbar, factor);
        scaleToolbar(R.id.edit_toolbar, factor);
        scaleToolbar(R.id.excel_edit_toolbar, factor);
        scaleToolbar(R.id.file_toolbar, factor);
        scaleToolbar(R.id.format_toolbar, factor);
        scaleToolbar(R.id.formulas_toolbar, factor);
        scaleToolbar(R.id.insert_toolbar, factor);
        scaleToolbar(R.id.pdf_pages_toolbar, factor);
        scaleToolbar(R.id.ppt_format_toolbar, factor);
        scaleToolbar(R.id.ppt_insert_toolbar, factor);
        scaleToolbar(R.id.ppt_slides_toolbar, factor);
        scaleToolbar(R.id.review_toolbar, factor);
        scaleSearchToolbar(factor);

        //  now the tab area
        scaleTabArea(factor);

        //  find the single tab and set the text size to be smaller.
        int count = tabHost.getTabWidget().getTabCount();
        View tabview = tabHost.getTabWidget().getChildTabViewAt(count-1);
        SOTextView tv = (SOTextView) tabview.findViewById(R.id.tabText);
        int size = getContext().getResources().getInteger(R.integer.sodk_editor_single_tab_text_size);
        tv.setTextSize(size);

        //  scale the back button
        mBackButton.setScaleX(factor);
        mBackButton.setScaleY(factor);

        //  scale the undo button
        mUndoButton.setScaleX(factor);
        mUndoButton.setScaleY(factor);

        //  scale the redo button
        mRedoButton.setScaleX(factor);
        mRedoButton.setScaleY(factor);

        //  scale the search button
        mSearchButton.setScaleX(factor);
        mSearchButton.setScaleY(factor);
    }

    protected void scaleTabArea(float factor)
    {
        //  find the layout
        LinearLayout content = (LinearLayout)findViewById(R.id.header_top);
        if (content == null)
            return;

        //  get the intended height
        int childWidthMeasureSpec  = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        content.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        int h = content.getMeasuredHeight();

        //  scale it.
        content.getLayoutParams().height = (int)((float)h * factor);

        content.requestLayout();
        content.invalidate();
    }

    protected void fixFileToolbar(int id)
    {
        //  find the toolbar
        LinearLayout toolbar = (LinearLayout)findViewById(id);
        if (toolbar == null)
            return;

        // remove divider if there's no buttons on the right.
        View    lastdivider   = null;
        boolean buttonOnRight = false;
        boolean hasButtons    = false;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof ToolbarButton) {
                //  button
                if (child.getVisibility() == View.VISIBLE)
                {
                    buttonOnRight = true;
                    hasButtons = true;
                }
            }
            else {
                //  divider
                if (!hasButtons)
                {
                    // it doesn't look good if toolbar starts with a divider.
                    // hide the current divider, as no button is on the left.
                    child.setVisibility(View.GONE);
                }
                else
                {
                    // hide the last divider if no button is on the right.
                    if (!buttonOnRight)
                    {
                        if (lastdivider!=null)
                            lastdivider.setVisibility(View.GONE);
                    }
                }

                lastdivider  = child;
                buttonOnRight = false;
            }
        }

        // hide the last divider, if no button is following.
        if (lastdivider != null && buttonOnRight == false)
            lastdivider.setVisibility(View.GONE);

        //  if no visible buttons at all,
        //  hide the toolbar and its parent (fix 699322)
        if (!hasButtons) {
            toolbar.setVisibility(View.GONE);
            ViewParent parent = toolbar.getParent();
            ((View)parent).setVisibility(View.GONE);
        }
    }

    protected void scaleSearchToolbar(float factor)
    {
        LinearLayout toolbar = (LinearLayout)findViewById(R.id.search_toolbar);
        if (toolbar == null)
            return;

        //  scale select children
        scaleChild(toolbar, R.id.search_icon, factor);
        scaleChild(toolbar, R.id.search_text_clear, factor);
        scaleChild(toolbar, R.id.search_next, factor);
        scaleChild(toolbar, R.id.search_previous, factor);

        //  choose a wrapper style
        LinearLayout wrapper = (LinearLayout)toolbar.findViewById(R.id.search_input_wrapper);
        Drawable drawable;
        if (Utilities.isPhoneDevice(getContext()))
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.sodk_editor_search_input_wrapper_phone);
        else
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.sodk_editor_search_input_wrapper);
        wrapper.setBackground(drawable);

        //  some magic numbers
        float textSize = 20f;
        float wrapperFactor = 0.85f;
        int negPadding = -15;  //  pixels

        //  scale the text font size
        mSearchText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize*factor);

        //  scale wrapper
        wrapper.measure(0, 0);
        int h1 = wrapper.getMeasuredHeight();
        wrapper.getLayoutParams().height = (int) ((float) h1 * wrapperFactor);

        toolbar.setPadding(0, negPadding, 0, negPadding);
    }

    private void scaleChild(View parent, int id, float factor)
    {
        View child = parent.findViewById(id);

        child.setScaleX(factor);
        child.setScaleY(factor);
    }

    protected void scaleToolbar(int id, float factor)
    {
        //  find the layout
        LinearLayout layout = (LinearLayout)findViewById(id);
        if (layout == null)
            return;

        //  get the intended height
        int childWidthMeasureSpec  = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        layout.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        int h = layout.getMeasuredHeight();
        int w = layout.getMeasuredWidth();

        //  scale it
        layout.setScaleX(factor);
        layout.setScaleY(factor);
        layout.setPivotX(0);
        layout.setPivotY(0);
        layout.getLayoutParams().height = (int)((float)h * factor*2);
        layout.getLayoutParams().width = (int)((float)w * factor);
        layout.requestLayout();
        layout.invalidate();

        //  add this little bit to the new height.  It helps the horizontal scrollbar to not
        //  obscure the icon titles.
        int extraHeight = Utilities.convertDpToPixel(3);

        int newHeight = (int) ((float) h * factor);
        int newWidth = (int) ((float) w * factor);

        //  also scale the parent scrollview
        ViewParent parent = layout.getParent();
        if (parent instanceof HorizontalScrollView)
        {
            HorizontalScrollView hsv = (HorizontalScrollView)parent;
            hsv.getLayoutParams().height = extraHeight + newHeight;
            hsv.getLayoutParams().width = newWidth;
            hsv.requestLayout();
            hsv.invalidate();
        }

        //  adjust some of the children
        int count = layout.getChildCount();
        for (int i=0;i<count;i++)
        {
            View child = layout.getChildAt(i);

            //  for toolbar buttons, remove padding.
            if (child instanceof ToolbarButton) {
                child.setPadding(0, 0, 0, 0);
            }

            //  for dividers change the gravity, size and margins
            String tag = (String)child.getTag();
            if (tag!=null && tag.equals("toolbar_divider")) {
                child.getLayoutParams().height = newHeight;
                ((LinearLayout.LayoutParams)child.getLayoutParams()).gravity = Gravity.TOP;
                int topmargin = Utilities.convertDpToPixel(7);
                int sidemargin = Utilities.convertDpToPixel(3);
                ((LinearLayout.LayoutParams)child.getLayoutParams()).setMargins(sidemargin, topmargin, sidemargin, 0);
            }
        }
    }

    protected NUIDocView.TabData[] getTabData()
    {
        if (mTabs == null)
        {
            mTabs = new NUIDocView.TabData[5];

            if (mDocCfgOptions.isEditingEnabled())
            {
                mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),   R.id.fileTab,   R.layout.sodk_editor_tab_left, View.VISIBLE);
                mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_edit),   R.id.editTab,   R.layout.sodk_editor_tab, View.VISIBLE);
                mTabs[2] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_insert), R.id.insertTab, R.layout.sodk_editor_tab, View.VISIBLE);
                mTabs[3] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),  R.id.pagesTab,  R.layout.sodk_editor_tab, View.VISIBLE);

                //  this tab is visible, but will be "blocked" by IAP until the Pro package is purchased.
                //  Clicking on the tab's buttons will redirect the user to upgrade.
                mTabs[4] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_review), R.id.reviewTab, R.layout.sodk_editor_tab_right, View.VISIBLE);
            }
            else
            {
                mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),   R.id.fileTab,   R.layout.sodk_editor_tab_left, View.VISIBLE);
                mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_edit),   R.id.editTab,   R.layout.sodk_editor_tab, View.GONE);
                mTabs[2] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_insert), R.id.insertTab, R.layout.sodk_editor_tab, View.GONE);
                mTabs[3] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),  R.id.pagesTab,  R.layout.sodk_editor_tab_right, View.VISIBLE);
                mTabs[4] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_review), R.id.reviewTab, R.layout.sodk_editor_tab_right, View.GONE);
            }
        }

        //  if track changes is not enabled, and there's no feature tracker,
        //  hide the Review tab.
        if (!mDocCfgOptions.isTrackChangesFeatureEnabled() && mDocCfgOptions.featureTracker==null)
        {
            mTabs[4].visibility = View.GONE;
            mTabs[3].layoutId = R.layout.sodk_editor_tab_right;
        }

        return mTabs;
    }

    protected int getInitialTab()
    {
        return 0;
    }

    protected void setupTabs()
    {
        tabHost = (TabHost)findViewById(R.id.tabhost);
        tabHost.setup();

        final NUIDocView.TabData tabData[] = getTabData();

        //  first tab is and stays hidden.
        //  when the search tab is selected, we programmatically "select" this hidden tab
        //  which results in NO tabs appearing selected in this tab host.
        setupTab (tabHost, getContext().getString(R.string.sodk_editor_tab_hidden),   R.id.hiddenTab  , R.layout.sodk_editor_tab, View.VISIBLE);
        tabHost.getTabWidget().getChildTabViewAt(0).setVisibility(View.GONE);

        //  these tabs are shown.
        int i;
        for (i=0; i<tabData.length; i++)
        {
            NUIDocView.TabData tab = tabData[i];
            setupTab (tabHost, tab.name, tab.contentId, tab.layoutId, tab.visibility);
        }

        //  another hidden tab.  Must be the last one
        setupTab (tabHost, getContext().getString(R.string.sodk_editor_tab_single),   R.id.hiddenTab  , R.layout.sodk_editor_tab_single, View.VISIBLE);

        //  initial tab
        final int initialTab = getInitialTab();
        setTab(tabData[initialTab].name);
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                setTabColors(tabData[initialTab].name);
            }
        });
        setSingleTabTitle(tabData[initialTab].name);

        measureTabs();

        tabHost.setOnTabChangedListener(this);

        //  set a click handler for each tab in the tabhost.
        //  we'll use it to catch wind of the tab change
        //  before it actually happens.
        TabWidget tw = tabHost.getTabWidget();
        int ntabs = tw.getTabCount();
        for (int j=0; j<ntabs; j++) {
            View tabView = tw.getChildAt(j);
            tabView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_UP) {
                        //  tab will be changing, so call a function
                        //  to handle it.
                        onPreTabChange();
                    }
                    return false;
                }
            });
        }
    }

    protected void measureTabs()
    {
        //  get tab host and widget
        TabHost tabHost = findViewById(R.id.tabhost);
        TabWidget tabWidget = tabHost.getTabWidget();

        int totalw = 0;

        if (Utilities.isPhoneDevice(activity()))
        {
            int count = tabHost.getTabWidget().getTabCount();
            View tabView = tabHost.getTabWidget().getChildTabViewAt(count-1);
            tabView.measure(0, 0);
            totalw += tabView.getMeasuredWidth();
        }
        else
        {
            NUIDocView.TabData tabData[] = getTabData();
            for (int i=0; i<tabData.length; i++)
            {
                //  find the tab view
                View tabView = tabWidget.getChildAt(i+1);
                if (tabView == null)
                    continue;

                //  calculate the width based on the text width, accounting for padding
                SOTextView textView = tabView.findViewById(R.id.tabText);
                if (textView == null)
                    continue;
                textView.measure(0, 0);
                int w = textView.getMeasuredWidth();
                w += tabView.getPaddingLeft() + tabView.getPaddingRight();

                //  set the tab view width
                tabView.measure(0, 0);
                int h = tabView.getMeasuredHeight();
                tabView.setLayoutParams(new
                        LinearLayout.LayoutParams(w,h));

                totalw += (w);
            }
        }

        //  set the width of the TabWidget and it's parent.
        tabWidget.setLayoutParams(new FrameLayout.LayoutParams(totalw, -2));
        HorizontalScrollView hsv = (HorizontalScrollView)tabWidget.getParent();
        hsv.getLayoutParams().width = totalw;
    }

    //  this function is called when a tab is about to be changed.
    private void onPreTabChange()
    {
        //  hide the keyboard on any tab change.
        Utilities.hideKeyboard(getContext());
    }

    protected void setSingleTabTitle(String title)
    {
        if (title.equalsIgnoreCase(getContext().getString(R.string.sodk_editor_tab_hidden)))
            return;

        //  it's always the last one
        int count = tabHost.getTabWidget().getTabCount();
        View tabview = tabHost.getTabWidget().getChildTabViewAt(count-1);
        SOTextView tv = (SOTextView) tabview.findViewById(R.id.tabText);
        tv.setText(title);
    }

    protected String mCurrentTab = "";
    protected void setTab(String id)
    {
        mCurrentTab = id;
        final TabHost tabHost = (TabHost)findViewById(R.id.tabhost);
        tabHost.setCurrentTabByTag(mCurrentTab);
        setSingleTabTitle(id);

        if (Utilities.isPhoneDevice(activity()))
            scaleHeader();
    }
    protected String getCurrentTab() {return mCurrentTab;}

    protected boolean isRedactionMode() {return false;}

    protected void setupTab(TabHost tabHost, String text, int viewId, int tabId, int visibility)
    {
        /*
         * Don't add disabled main tabs.
         *
         * If we ignore hidden tabs in single tab mode the tab width is
         * adversely affected.
         */
        if ((! Utilities.isPhoneDevice(activity())) && visibility == View.GONE)
        {
            findViewById(viewId).setVisibility(View.GONE);
            return;
        }

        View tabview = LayoutInflater.from(tabHost.getContext()).inflate(tabId, null);
        SOTextView tv = (SOTextView) tabview.findViewById(R.id.tabText);
        tv.setText(text);

        tabMap.put(text, tabview);

        TabHost.TabSpec tab = tabHost.newTabSpec(text);
        tab.setIndicator(tabview);
        tab.setContent(viewId);
        tabHost.addTab(tab);

        // Keep a list of the identifiers of added tabs.
        mAllTabHostTabs.add(text);
    }

    private void startProgress() {

        //  this is only used once.
        if (mProgressStarted)
            return;
        mProgressStarted = true;

        //  display a progress dialog
        mProgressDialog = new ProgressDialog(getContext(),
                                    R.style.sodk_editor_alert_dialog_style );
        mProgressDialog.setMessage(getContext().getString(R.string.sodk_editor_loading_please_wait));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);

        //  make sure that the progress dialog does not gain focus, so loading
        //  can be aborted using the back button. (Alan S.)
        Window window = mProgressDialog.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mProgressDialog.show();
    }

    private void endProgress() {

        //  remove the progress dialog
        if (mProgressDialog != null) {
            try {
                mProgressDialog.dismiss();
            } catch (IllegalArgumentException e ) {
                /*
                 * It is possible, most notably when quickly toggling
                 * between day/night mode, for the dialog to be
                 * disconnected from the window.
                 *
                 * We catch that exception here.
                 */
            } finally {
                mProgressDialog = null;
            }
        }
        mProgressStarted = true;
    }

    public void onUndoButton(final View v) { doUndo();}

    public void doUndo()
    {
        SODoc soldoc = (SODoc)mSession.getDoc();
        int currentEdit = soldoc.getCurrentEdit();
        if (currentEdit > 0)
        {
            //  clear any selection
            getDoc().clearSelection();

            currentEdit--;
            soldoc.setCurrentEdit(currentEdit);

            // Update the input view with the current docment content.
            updateInputView();
        }
    }

    public void doSelectAll()
    {
        //  cause an initial selection
        getDocView().selectTopLeft();

        //  then select all
        mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_DocStart);
        mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionDocEnd);
    }

    public void doArrowKey(KeyEvent event)
    {
        boolean shift = event.isShiftPressed();
        boolean cmd   = event.isAltPressed();

        switch (event.getKeyCode())
        {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_Up);
                if ( shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionLineUp);
                if (!shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_DocStart);
                if ( shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionDocStart);
                onTyping();

                /*
                 * Let the input view know that the selection has been
                 * modified.
                 */
                updateInputView();

                return;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_Down);
                if ( shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionLineDown);
                if (!shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_DocEnd);
                if ( shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionDocEnd);
                onTyping();

                /*
                 * Let the input view know that the selection has been
                 * modified.
                 */
                updateInputView();

                return;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_Left);
                if ( shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionCharBack);
                if (!shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_LineStart);
                if ( shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionLineStart);
                onTyping();

                /*
                 * Let the input view know that the selection has been
                 * modified.
                 */
                updateInputView();

                return;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_Right);
                if ( shift && !cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionCharForward);
                if (!shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_LineEnd);
                if ( shift &&  cmd) mSession.getDoc().processKeyCommand(ArDkDoc.SmartOfficeCmd_SelectionLineEnd);
                onTyping();

                /*
                 * Let the input view know that the selection has been
                 * modified.
                 */
                updateInputView();

                return;
        }
    }

    public DocView getDocView() {return mDocView;}

    public DocListPagesView getDocListPagesView() {return mDocPageListView;}

    public ArDkDoc getDoc() {
        if (mSession==null)
            return null;
        return mSession.getDoc();
    }

    public boolean isPageListVisible()
    {
        RelativeLayout pages = (RelativeLayout) findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis == View.VISIBLE)
                return true;
        }

        return false;
    }

    protected void showPages()
    {
        showPages(-1);
    }

    protected void showPages(final int pageNumber)
    {
        RelativeLayout pages = (RelativeLayout) findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis != View.VISIBLE)
            {
                pages.setVisibility(View.VISIBLE);
                mDocPageListView.setVisibility(View.VISIBLE);
                mDocView.onShowPages();
            }

            final ViewTreeObserver observer = mDocView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);

                    //  starting page will either be from the main DocView (most visible)
                    //  or given as a parameter
                    int page;
                    if (pageNumber==-1)
                        page = mDocView.getMostVisiblePage();
                    else
                        page = pageNumber;

                    mDocPageListView.setCurrentPage(page);
                    mDocPageListView.scrollToPage(page, false);
                    mDocPageListView.fitToColumns();
                    mDocPageListView.layoutNow();

                    //  send update UI notification
                    doUpdateCustomUI();
                }
            });
        }
    }

    protected void hidePages()
    {
        RelativeLayout pages = (RelativeLayout) findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis != View.GONE) {
                mDocView.onHidePages();
                pages.setVisibility(View.GONE);

                //  send update UI notification
                doUpdateCustomUI();
            }
        }
    }

    public void onRedoButton(final View v)
    {
        doRedo();
    }

    public void doRedo()
    {
        SODoc soldoc = (SODoc)mSession.getDoc();
        int currentEdit = soldoc.getCurrentEdit();
        int numEdits = soldoc.getNumEdits();
        if (currentEdit < numEdits) {

            //  clear any selection
            getDoc().clearSelection();

            currentEdit++;
            soldoc.setCurrentEdit(currentEdit);

            // Update the input view with the current docment content.
            updateInputView();
        }
    }

    //  true while a search is underway
    //  only used on the main thread.
    private boolean mIsSearching = false;

    private void setupSearch()
    {
        //  set up the search listener
        if (mSearchListener==null)
        {
            mSearchListener = new SOSearchListener() {
                @Override
                public void progressing(int page)
                {
                }

                @Override
                public void found(int page, RectF box) {
                    hideSearchProgress();
                    getDocView().onFoundText(page, box);

                    //  onFoundText() may initiate a scroll,
                    //  so wait for that to finish and then declare that
                    //  searching is done.
                    getDocView().waitForRest(new Runnable() {
                        @Override
                        public void run() {
                            mIsSearching = false;
                        }
                    });
                }

                @Override
                public void notFound()
                {
                    mIsSearching = false;
                    hideSearchProgress();
                    Utilities.yesNoMessage((Activity) getContext(),
                            getResources().getString(R.string.sodk_editor_no_more_found), getResources().getString(R.string.sodk_editor_keep_searching),
                            getResources().getString(R.string.sodk_editor_str_continue), getResources().getString(R.string.sodk_editor_stop),
                            new Runnable() {
                                @Override
                                public void run() {
                                    Handler handler = new Handler();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mIsSearching = true;
                                            doSearch();
                                        }
                                    });
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    mIsSearching = false;
                                }
                            }
                    );
                }

                @Override
                public boolean startOfDocument() {
                    doSearch();
                    return true;
                }

                @Override
                public boolean endOfDocument() {
                    doSearch();
                    return true;
                }

                @Override
                public void error() {
                    hideSearchProgress();
                    mIsSearching = false;
                }

                @Override
                public void cancelled() {
                    hideSearchProgress();
                    mIsSearching = false;
                }
            };
            mSession.getDoc().setSearchListener(mSearchListener);
        }

        mSession.getDoc().setSearchMatchCase(false);
    }

    protected void onSearch()
    {
        //  "deselect" all the visible tabs by selecting the hidden (first) one
        setTab(getContext().getString(R.string.sodk_editor_tab_hidden));

        //  hide all the other tabs
        if (Utilities.isPhoneDevice(getContext()))
            hideMainTabs();

        findViewById(R.id.searchTab).setVisibility(View.VISIBLE);
        //  show search as selected
        showSearchSelected(true);

        mSearchText.getText().clear();
        mSearchText.requestFocus();
        Utilities.showKeyboard(getContext());
    }

    public void setSearchStart()
    {
    }

    public void onSearchButton(final View v)
    {
        onSearch();
    }

    private void hideMainTabs()
    {
        int count = tabHost.getTabWidget().getTabCount();
        for (int i=1; i<count-1; i++)
            tabHost.getTabWidget().getChildAt(i).setVisibility(View.GONE);
    }

    private void showMainTabs()
    {
        int count = tabHost.getTabWidget().getTabCount();
        for (int i=1; i<count-1; i++)
            tabHost.getTabWidget().getChildAt(i).setVisibility(View.VISIBLE);
    }

    //  this is called after a cut is done, used by NUIDocViewXls
    public void afterCut() {}

    //  this is called after a pqste is done, used by NUIDocViewXls
    public void afterPaste() {}

    protected void doCut()
    {
    }
    protected void doCopy()
    {
    }
    protected void doPaste()
    {
    }
    protected void doBold()
    {
    }
    protected void doItalic()
    {
    }
    protected void doUnderline()
    {
    }

    protected int getTargetPageNumber()
    {
        DocPageView dpv1 = getDocView().findPageContainingSelection();
        if (dpv1 != null)
            return dpv1.getPageNumber();

        //  get the document view's rect
        Rect r = new Rect();
        getDocView().getGlobalVisibleRect(r);

        //  find the page (including margins) under the center of the view
        DocPageView dpv = getDocView().findPageViewContainingPoint((r.left+r.right)/2, (r.top+r.bottom)/2, true);

        //  get the page number
        int pageNum = 0;
        if (dpv!=null)
            pageNum = dpv.getPageNumber();

        return pageNum;
    }

    protected boolean mFinished = false;

    public void prefinish()
    {
        //  notional fix for https://bugs.ghostscript.com/show_bug.cgi?id=699651
        if (mFinished)
            return;

        //  tell ourselves we're closing.
        mFinished  = true;

        //  remove the search listener (bug 699357)
        //  check for null - notional fix for 699821
        if (mSession!=null && mSession.getDoc()!=null)
            mSession.getDoc().setSearchListener(null);
        mSearchListener = null;

        //  remove loading progress
        endProgress();

        //  tell the file database we're closed.
        if (null != mFileState) {
            closeFile();
        }

        //  hide the keyboard
        Utilities.hideKeyboard(getContext());

        //  tell doc views to finish
        //  they will in turn tell their pages to do the same.
        if (mDocView!=null)
        {
            mDocView.finish();
            mDocView = null;
        }

        if (usePagesView())
        {
            if (mDocPageListView !=null)
            {
                mDocPageListView.finish();
                mDocPageListView = null;
            }
        }

        //  stop any loading
        if (mSession!=null)
            mSession.abort();

        //  destroy the doc's tracked SOPage objects
        ArDkDoc theDoc = getDoc();
        if (theDoc!=null)
            theDoc.destroyPages();

        //  get rid of the page adapter
        if (mAdapter!=null)
            mAdapter.setDoc(null);
        mAdapter = null;

        if (mEndSessionSilent != null)
        {
            endDocSession(mEndSessionSilent.booleanValue());
            mEndSessionSilent = null;
        }

        //  destroy the session in the background
        if (mSession!=null)
        {
            final ProgressDialog progress =
                new ProgressDialog(getContext(),
                                   R.style.sodk_editor_alert_dialog_style);
            progress.setMessage(getContext().getString(R.string.sodk_editor_wait));
            progress.setCancelable(false);
            progress.setIndeterminate(true);
            Window window = progress.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            progress.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mSession!=null)
                                mSession.destroy();
                            progress.dismiss();

                            //  Call the OnDoneListener
                            if (mOnDoneListener != null)
                                mOnDoneListener.done();
                        }
                    });
                }
            });
            progress.show();
        }
        else
        {
            if (mOnDoneListener != null)
                mOnDoneListener.done();
        }

        //  attempt to reclaim memory.
        if (mDocumentLib!=null)
            mDocumentLib.reclaimMemory();
    }

    public void endDocSession(boolean silent)
    {
        /*
         * Finish the doc and pageList views to abort any
         * active page renders.
         *
         * This allows endSession to complete in a timely
         * manner.
         */
        if (mDocView!=null)
        {
            mDocView.finish();
        }

        if (usePagesView())
        {
            if (mDocPageListView !=null)
            {
                mDocPageListView.finish();
            }
        }

        if (mSession!=null)
            mSession.endSession(silent);

        endProgress();
    }

    protected boolean shouldConfigureSaveAsPDFButton()
    {
        return true;
    }

    public void setConfigurableButtons()
    {
        ArrayList<ToolbarButton> buttons = new ArrayList<ToolbarButton>();

        //  The save button is always shown, but it will be disabled
        //  when editing is disabled - see updateSaveUIAppearance()
        if (null!=mSaveButton) {
            if (mDocCfgOptions.isSaveEnabled()) {
                mSaveButton.setVisibility(View.VISIBLE);
                buttons.add(mSaveButton);
            } else {
                mSaveButton.setVisibility(View.GONE);
            }
        }

        if (null!=mCustomSaveButton) {
            if (mDocCfgOptions.isCustomSaveEnabled()) {
                mCustomSaveButton.setVisibility(View.VISIBLE);
                buttons.add(mCustomSaveButton);
            } else {
                mCustomSaveButton.setVisibility(View.GONE);
            }
        }

        // Check if 'saveAs' option is allowed for this session.
        if (null!=mSaveAsButton) {
            if (mDocCfgOptions.isSaveAsEnabled()) {
                mSaveAsButton.setVisibility(View.VISIBLE);
                buttons.add(mSaveAsButton);
            } else {
                mSaveAsButton.setVisibility(View.GONE);
            }
        }

        // Check if 'savePdf' option is allowed for this session.
        // The File toolbar is shared with several document types, but not all will be
        // showing it. Those that won't can override shouldConfigureSaveAsPDFButton()
        if (shouldConfigureSaveAsPDFButton())
        {
            if (null!=mSavePdfButton) {
                if (mDocCfgOptions.isSaveAsPdfEnabled()) {
                    mSavePdfButton.setVisibility(View.VISIBLE);
                    buttons.add(mSavePdfButton);
                } else {
                    mSavePdfButton.setVisibility(View.GONE);
                }
            }
        }

        // Check if 'share' option is allowed for this session.
        if (null!=mShareButton) {
            if (mDocCfgOptions.isShareEnabled()) {
                mShareButton.setVisibility(View.VISIBLE);
                buttons.add(mShareButton);
            } else {
                mShareButton.setVisibility(View.GONE);
            }
        }

        // Check if 'openIn' option is allowed for this session.
        if (null!=mOpenInButton) {
            if (mDocCfgOptions.isOpenInEnabled()) {
                mOpenInButton.setVisibility(View.VISIBLE);
                buttons.add(mOpenInButton);
            } else {
                mOpenInButton.setVisibility(View.GONE);
            }
        }

        // Check if 'openPdfIn' option is allowed for this session.
        if (null!=mOpenPdfInButton) {
            if (mDocCfgOptions.isOpenPdfInEnabled()) {
                mOpenPdfInButton.setVisibility(View.VISIBLE);
                buttons.add(mOpenPdfInButton);
            } else {
                mOpenPdfInButton.setVisibility(View.GONE);
            }
        }

        // Check if 'print' option is allowed for this session.
        if (null!=mPrintButton) {
            if (mDocCfgOptions.isPrintingEnabled() ||
                mDocCfgOptions.isSecurePrintingEnabled()) {
                mPrintButton.setVisibility(View.VISIBLE);
                buttons.add(mPrintButton);
            } else {
                mPrintButton.setVisibility(View.GONE);
            }
        }

        if (mProtectButton!=null)
        {
            buttons.add(mProtectButton);

            //  for now
            mProtectButton.setVisibility(View.GONE);
        }

        // enable 'copy' button in file menu,
        // if editing is disabled and doc type is not pdf
        // NOTE: this copy button is shown even when the external clipboard
        // isn't available, as it can be used to paste into this app's search box.
        // See: https://bugs.ghostscript.com/show_bug.cgi?id=702459
        if (null!=mCopyButton2) {
            if (!mDocCfgOptions.isEditingEnabled() &&
                    !getDocFileExtension().equalsIgnoreCase("pdf")) {
                mCopyButton2.setVisibility(View.VISIBLE);
                buttons.add(mCopyButton2);
            } else {
                mCopyButton2.setVisibility(View.GONE);
            }
        }

        ToolbarButton.setAllSameSize(
            buttons.toArray(new ToolbarButton[buttons.size()]));

        // hide Redo/Undo buttons in read-only mode.
        if (! mDocCfgOptions.isEditingEnabled())
        {
            if (mUndoButton!=null)
                mUndoButton.setVisibility(View.GONE);
            if (mRedoButton!=null)
                mRedoButton.setVisibility(View.GONE);
        }

        // Insert Tab Buttons
        buttons = new ArrayList<ToolbarButton>();

        // Check if 'Image Insert' option is allowed for this session.
        if (null!=mInsertImageButton)
        {
            if (mDocCfgOptions.isImageInsertEnabled() && mDocCfgOptions.isEditingEnabled())
            {
                mInsertImageButton.setVisibility(View.VISIBLE);
                buttons.add(mInsertImageButton);
            }
            else
            {
                mInsertImageButton.setVisibility(View.GONE);
            }
        }

        // Check if 'Photo Insert' option is allowed for this session.
        if (null!=mInsertPhotoButton)
        {
            if (mDocCfgOptions.isPhotoInsertEnabled() && mDocCfgOptions.isEditingEnabled())
            {
                mInsertPhotoButton.setVisibility(View.VISIBLE);
                buttons.add(mInsertPhotoButton);
            }
            else
            {
                mInsertPhotoButton.setVisibility(View.GONE);
            }
        }

        if (buttons.size() > 0)
        {
            ToolbarButton.setAllSameSize(
                buttons.toArray(new ToolbarButton[buttons.size()]));
        }

        // Show/hide the insert tab.
        setInsertTabVisibility();
    }

    private void recycleBitmaps()
    {
        //  tell doc and pagelist views to dereference
        if (mDocView != null)
            mDocView.releaseBitmaps();
        if (usePagesView() && (mDocPageListView != null))
            mDocPageListView.releaseBitmaps();

        for (int i=0; i<bitmaps.length; i++) {
            if (bitmaps[i]!=null) {
                bitmaps[i].getBitmap().recycle();
                bitmaps[i] = null;
            }
        }
    }

    public void onDestroy()
    {
        recycleBitmaps();

        //  delete temp files
        if (mDeleteOnClose!=null) {
            for (int i = 0; i < mDeleteOnClose.size(); i++) {
                FileUtils.deleteFile(mDeleteOnClose.get(i));
            }
            mDeleteOnClose.clear();
        }

        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            mDataLeakHandlers.finaliseDataLeakHandlers();
        }
        else
        {
            //  the editor no longer has this capability built-in.

            //  bug 699357
            //  don't throw UnsupportedOperationException here
            //  onDestroy() may be called by android.app.Activity.performDestroy()
            //  which won't handle it, and crash.

//            throw new UnsupportedOperationException();
        }
    }

    public void onBackPressed()
    {
        goBack();
    }

    protected void prepareToGoBack()
    {
    }

    protected void preSaveQuestion(Runnable yesRunnable, Runnable noRunnable)
    {
        //  derived classes can implement a yes/no question
        if (yesRunnable != null)
            yesRunnable.run();
    }

    protected void goBack()
    {
        if (!mCanGoBack) {
            return;
        }

        prepareToGoBack();

        if (documentHasBeenModified()) {

            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    int saveId = R.string.sodk_editor_save;

                    /*
                     * If we will be offering a custom save customise the button
                     * text appropriately.
                     */
                    if (mCustomDocdata != null)
                    {
                        // Obtain and set the resource id for the required text.
                        int customSaveId =
                                getContext().getResources().getIdentifier(
                                        "secure_save_upper",
                                        "string",
                                        getContext().getPackageName());

                        if (customSaveId != 0)
                        {
                            saveId = customSaveId;
                        }
                    }

                    new AlertDialog.Builder(activity(), R.style.sodk_editor_alert_dialog_style)
                            .setTitle(R.string.sodk_editor_document_has_been_modified)
                            .setMessage(R.string.sodk_editor_would_you_like_to_save_your_changes)
                            .setCancelable(false)
                            .setPositiveButton(saveId, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();

                                    preSaveQuestion(new Runnable() {
                                        @Override
                                        public void run() {
                                            //  yes
                                            // this requires a custom save.
                                            if (mCustomDocdata != null)
                                            {
                                                onCustomSaveButton(null);
                                            }
                                            else if (mIsTemplate)
                                            {
                                                //  this is a template
                                                doSaveAs(true, null);
                                            }
                                            else
                                            {
                                                //  Not a template so save the document
                                                //  call closeFile() after the save is done, so postSaveHandler()
                                                //  can access the internal file.
                                                mSession.getDoc().saveTo(mFileState.getInternalPath(), new SODocSaveListener() {
                                                    @Override
                                                    public void onComplete(final int result, final int err) {
                                                        if (result == SODocSave_Succeeded) {
                                                            mFileState.saveFile();

                                                            if (mDataLeakHandlers!=null) {
                                                                mDataLeakHandlers.postSaveHandler(new SOSaveAsComplete()
                                                                {
                                                                    @Override
                                                                    public boolean onFilenameSelected( String path )
                                                                    {
                                                                        return true;
                                                                    }

                                                                    @Override
                                                                    public void onComplete(int result, String path)
                                                                    {
                                                                        //  if the postSaveHandler did not succeed (result!=0)
                                                                        //  leave the document open.
                                                                        //  This is a partial fix for 702405; it allows the user to
                                                                        //  do Save As to another location without losing changes.
                                                                        if (result==0) {
                                                                            closeFile();
                                                                            prefinish();
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                            else
                                                            {
                                                                closeFile();
                                                                prefinish();
                                                            }

                                                        } else {
                                                            closeFile();
                                                            String message = String.format(activity().getString(com.artifex.sonui.editor.R.string.sodk_editor_error_saving_document_code), err);
                                                            Utilities.showMessage(activity(), activity().getString(com.artifex.sonui.editor.R.string.sodk_editor_error), message);
                                                        }
                                                    }
                                                });
                                            }

                                        }
                                    }, new Runnable() {
                                        @Override
                                        public void run() {
                                            //  no
                                        }
                                    });

                                }
                            })
                            .setNegativeButton(R.string.sodk_editor_discard, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    closeFile();
                                    mEndSessionSilent = new Boolean(false);
                                    prefinish();
                                }
                            })
                            .setNeutralButton(R.string.sodk_editor_continue_editing, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            });
        } else {
            mEndSessionSilent = new Boolean(false);
            prefinish();
        }
    }

    private void saveState()
    {
        DocView docView = getDocView();
        if (docView != null)
        {
            int pageNumber = mCurrentPageNum;
            float scale = docView.getScale();
            int scrollX = docView.getScrollX();
            int scrollY = docView.getScrollY();
            boolean pageListVisible = usePagesView() && docView.pagesShowing();

            //  set the values in the file state, for apps that are using
            //  persistent file state.
            if (mSession != null)
            {
                SOFileState state = mSession.getFileState();
                if ( state != null )
                {
                    state.setPageNumber( pageNumber );
                    state.setScale( scale );
                    state.setScrollX( scrollX );
                    state.setScrollY( scrollY );
                    state.setPageListVisible( pageListVisible );
                }
            }

            //  set the values in the ViewingState, for apps that are NOT using
            //  persistent file state.
            if ( mViewingState != null )
            {
                mViewingState.pageNumber = pageNumber;
                mViewingState.scale = scale;
                mViewingState.scrollX = scrollX;
                mViewingState.scrollY = scrollY;
                mViewingState.pageListVisible = pageListVisible;
            }
        }
    }

    private void closeFile()
    {
        saveState();
        mFileState.closeFile();
    }

    public void doSaveAs(final boolean goingBack, final SOSaveAsComplete completionHandler)
    {
        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            preSaveQuestion(new Runnable() {
                @Override
                public void run() {
                    //  yes
                    try
                    {
                        //  fix 698388 - Files in document template has two names
                        String path = mFileState.getUserPath();
                        if (path == null)
                            path = mFileState.getOpenedPath();
                        File file = new File(path);

                        mDataLeakHandlers.saveAsHandler(file.getName(),
                                mSession.getDoc(),
                                new SOSaveAsComplete()
                                {
                                    @Override
                                    public boolean onFilenameSelected( String path )
                                    {
                                        if (completionHandler != null)
                                            return completionHandler.onFilenameSelected( path );
                                        return true;
                                    }

                                    @Override
                                    public void onComplete(int result, String path)
                                    {
                                        if (result == SOSaveAsComplete_Succeeded)
                                        {
                                            setFooterText(path);
                                            mFileState.setUserPath(path);

                                            if (goingBack)
                                                prefinish();
                                            if (mFinished)
                                                return;

                                            /*
                                             * Make sure that when we close, we don't
                                             * think there are outstanding changes.
                                             */
                                            mFileState.setHasChanges(false);
                                            onSelectionChanged();

                                            //  reload the file
                                            reloadFile();

                                            /*
                                             * If the 'saveAs' was successful the template
                                             * flag will be reset and 'save' allowed.
                                             */
                                            mIsTemplate = mFileState.isTemplate();
                                        }
                                        else if (result == SOSaveAsComplete_Error)
                                        {
                                            // Save failed. Revoke the user path
                                            mFileState.setUserPath(null);
                                        }
                                        else if (result == SOSaveAsComplete_Cancelled)
                                        {
                                            //  cancelled
                                        }

                                        if (completionHandler != null)
                                            completionHandler.onComplete( result, path );
                                    }
                                });

                        return;
                    }
                    catch(UnsupportedOperationException e)
                    {
                        return;
                    }

                }
            }, new Runnable() {
                @Override
                public void run() {
                    //  no
                }
            });
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    private void createFlowModeChangeListener()
    {
        //  set up a listener for when the change in flow mode is done
        mSession.addLoadListener(new SODocSession.SODocSessionLoadListener()
        {
            @Override public void onPageLoad(int pageNum) {}
            @Override public void onError(int error, int errorNum) {}
            @Override public void onCancel() {}
            @Override public void onDocComplete() {}
            @Override public void onSelectionChanged(final int startPage, final int endPage)
            {
                //  remove the listener
                mSession.removeLoadListener(this);

                //  when reflow mode is changed, there's no (good) relationship in the position
                //  within the document before and after the mode change.  So we'll just scroll
                //  to the top.
                mDocView.scrollTo(mDocView.getScrollX(), 0);
                if (usePagesView())
                    mDocPageListView.scrollTo(mDocPageListView.getScrollX(), 0);
                setCurrentPage(0);

                //  initial "fit to width"
                if (mDocView.getReflowMode())
                {
                    mDocView.setReflowWidth();
                    mDocView.onScaleEnd(null);
                }
                else
                {
                    float scale = 1.0f;
                    if (usePagesView() && isPageListVisible())
                        scale = ((float) getResources().getInteger(R.integer.sodk_editor_page_width_percentage)) / 100f;
                    mDocView.setScale(scale);
                    mDocView.scaleChildren();
                }

                if (usePagesView())
                    mDocPageListView.fitToColumns();

                //  redo the layout
                layoutNow();
            }

            @Override public void onLayoutCompleted()
            {
                onLayoutChanged();
            }
        });
    }

    public void onReflowButton(final View v)
    {
        //  set up a listener for when the change in flow mode is done
        createFlowModeChangeListener();

        SODoc doc = (SODoc)getDoc();

        if (doc.getFlowMode()==SODoc.FLOW_MODE_NORMAL) {
            if (usePagesView())
                mDocPageListView.setReflowMode(true);
            mDocView.setReflowMode(true);
            doc.setFlowMode(SODoc.FLOW_MODE_REFLOW, getDocView().getReflowWidth(), getDocView().getReflowHeight());
            mDocView.mLastReflowWidth = getDocView().getReflowWidth();
        }
        else {
            mDocView.setReflowMode(false);
            if (usePagesView())
                mDocPageListView.setReflowMode(false);
            doc.setFlowMode(SODoc.FLOW_MODE_NORMAL, getDocView().getReflowWidth(), 0);
        }
    }

    //  for no-UI API
    public void setFlowMode(int mode)
    {
        SODoc doc = (SODoc)getDoc();
        //  do nothing if the mode is the same
        if (mode == doc.getFlowMode())
            return;

        //  set up a listener for when the change in flow mode is done
        createFlowModeChangeListener();

        if (mode==SODoc.FLOW_MODE_NORMAL)
        {
            mDocView.setReflowMode(false);
            if (usePagesView())
                mDocPageListView.setReflowMode(false);
            doc.setFlowMode(SODoc.FLOW_MODE_NORMAL, getDocView().getReflowWidth(), 0);
        }

        if (mode==SODoc.FLOW_MODE_REFLOW)
        {
            if (usePagesView())
                mDocPageListView.setReflowMode(true);
            mDocView.setReflowMode(true);
            doc.setFlowMode(SODoc.FLOW_MODE_REFLOW, getDocView().getReflowWidth(), getDocView().getReflowHeight());
            mDocView.mLastReflowWidth = getDocView().getReflowWidth();
        }

        if (mode==SODoc.FLOW_MODE_RESIZE)
        {
            if (usePagesView())
                mDocPageListView.setReflowMode(true);
            mDocView.setReflowMode(true);
            doc.setFlowMode(SODoc.FLOW_MODE_RESIZE, getDocView().getReflowWidth(), getDocView().getReflowHeight());
            mDocView.mLastReflowWidth = getDocView().getReflowWidth();
        }
    }

    //  for no-UI API
    public int getFlowMode()
    {
        return ((SODoc)getDoc()).getFlowMode();
    }

    public void onInsertImageButton(final View v)
    {
        //  hide the keyboard and wait for it to be fully hidden
        //  before proceeding.
        final NUIDocView view = this;
        showKeyboard(false, new Runnable()
        {
            @Override
            public void run()
            {
                // Use the custom data leakage handlers if available.
                if (mDataLeakHandlers != null)
                {
                    try
                    {
                        mDataLeakHandlers.insertImageHandler(view);
                    }
                    catch(UnsupportedOperationException e)
                    {
                    }

                    return;
                }
                else
                {
                    //  the editor no longer has this capability built-in.
                    throw new UnsupportedOperationException();
                }
            }
        });
    }

    public void onInsertPhotoButton(final View v)
    {
        //  we may need to ask the user to grant permission
        askForCameraPermission(new Runnable()
        {
            @Override
            public void run() {

                //  We now have the permission we need.

                //  hide the keyboard and wait for it to be fully hidden
                //  before proceeding.
                showKeyboard(false, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // Use the custom data leakage handlers if available.
                        if (mDataLeakHandlers != null)
                        {
                            try
                            {
                                mDataLeakHandlers.insertPhotoHandler(NUIDocView.this);
                            }
                            catch(UnsupportedOperationException e)
                            {
                            }

                            return;
                        }
                        else
                        {
                            //  the editor no longer has this capability built-in.
                            throw new UnsupportedOperationException();
                        }
                    }
                });
            }
        });
    }

    private void askForCameraPermission(final Runnable runnable)
    {
        //  normally we don't need permission to do an image capture using the camera.
        //  but if the developer has requested android.Manifest.permission.CAMERA in their
        //  manifest for some reason, then we need to ask the user to grant it.

        //  is android.Manifest.permission.CAMERA requested in the manifest?
        if (!Utilities.isPermissionRequested(getContext(), android.Manifest.permission.CAMERA)) {
            //  no, we can proceed.
            runnable.run();
            return;
        }

        //  has the permission already been granted?
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //  yes, we can proceed.
            runnable.run();
            return;
        }

        //  set up a permission handler
        final BaseActivity activity = BaseActivity.getCurrentActivity();
        final int REQUEST_CODE = 1;
        activity.setPermissionResultHandler(new BaseActivity.PermissionResultHandler() {
            @Override
            public boolean handle(int requestCode, String[] permissions, int[] grantResults) {
                activity.setPermissionResultHandler(null);

                if (requestCode == REQUEST_CODE) {

                    //  check if permission is granted
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //  permission is granted, so proceed, back on the main thread
                        new Handler().post(runnable);
                    } else {
                        //  permission denied.
                        //  Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA))
                        {
                            //  Show permission explanation dialog...
                            Utilities.yesNoMessage(activity, activity.getString(R.string.sodk_editor_permission_denied), activity.getString(R.string.sodk_editor_permission_camera_why),
                                    activity.getString(R.string.sodk_editor_yes), activity.getString(R.string.sodk_editor_no), new Runnable() {
                                        @Override
                                        public void run() {
                                            //  yes, try again
                                            askForCameraPermission(runnable);
                                        }
                                    }, new Runnable() {
                                        @Override
                                        public void run() {
                                            //  no
                                            Utilities.showMessage(activity,
                                                    activity.getString(R.string.sodk_editor_permission_denied),
                                                    activity.getString(R.string.sodk_editor_permission_camera));
                                        }
                                    });
                        }
                        else
                        {
                            //  don't show an explanation
                            Utilities.showMessage(activity,
                                    activity.getString(R.string.sodk_editor_permission_denied),
                                    activity.getString(R.string.sodk_editor_permission_camera));
                        }
                    }
                }

                return true;
            }
        });

        //  now ask the user to grant.
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE);
    }

    public void doInsertImage(String path)
    {
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            mDataLeakHandlers.onActivityResult(requestCode, resultCode, data);
            return;
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    protected void updateUndoUIAppearance()
    {
        //  take care on shutdown
        if (mSession==null || mSession.getDoc()==null)
            return;

        SODoc soldoc = (SODoc)mSession.getDoc();
        int currentEdit = soldoc.getCurrentEdit();
        int numEdits = soldoc.getNumEdits();

        setButtonEnabled(mUndoButton, currentEdit>0);
        setButtonEnabled(mRedoButton, currentEdit < numEdits);
    }

    private void setButtonEnabled(Button button, boolean enabled)
    {
        button.setEnabled(enabled);

        int color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_button_enabled_tint);
        if (!enabled)
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_button_disabled_tint);

        setButtonColor(button, color);
    }

    protected void setButtonColor(Button button, int color)
    {
        Drawable[] drawables = button.getCompoundDrawables();
        for (int i=0;i< drawables.length;i++) {
            if (drawables[i]!=null)
                DrawableCompat.setTint(drawables[i], color);
        }
    }

    protected void updateReviewUIAppearance()
    {
    }

    protected void updateEditUIAppearance()
    {
    }

    protected void updateSaveUIAppearance()
    {
        if (null!=mSaveButton)
        {
            boolean enabled = documentHasBeenModified();

            //  disable if editing not enabled
            if (!mDocCfgOptions.isEditingEnabled())
                enabled = false;

            //  disable for unsaved templates.
            if (mIsTemplate)
                enabled = false;

            mSaveButton.setEnabled(enabled);
        }
    }

    protected void updateUIAppearance()
    {
        //  selection changed, so update the appearance of various buttons

        //  file save
        updateSaveUIAppearance();

        //  determine whether we've got an area or a caret as the selection.
        boolean areaIsSelected = false;
        boolean cursorIsSelected = false;
        ArDkSelectionLimits limits = getDocView().getSelectionLimits();
        if (limits != null) {
            boolean active = limits.getIsActive();
            areaIsSelected = active && !limits.getIsCaret();
            cursorIsSelected = active && limits.getIsCaret();
        }

        // don't process document modification tabs in read-only mode.
        if (! mDocCfgOptions.isEditingEnabled())
        {
            // in case editing is disabled, we have 'copy' button in file menu.
            // update the button state.
            if (null!=mCopyButton2)
                mCopyButton2.setEnabled(areaIsSelected && ((SODoc)mSession.getDoc()).getSelectionCanBeCopied());

            return;
        }

        //  edit
        updateEditUIAppearance();

        //  undo and redo
        updateUndoUIAppearance();

        //  review
        updateReviewUIAppearance();

        //  insert
        updateInsertUIAppearance();

        //  send update UI notification
        doUpdateCustomUI();
    }

    protected void updateInsertUIAppearance()
    {
        //  do we have a caret selection?
        boolean selCaret = false;
        ArDkSelectionLimits limits = getDocView().getSelectionLimits();
        if (limits != null) {
            selCaret = limits.getIsActive() && limits.getIsCaret();
        }

        if (mInsertImageButton != null && mDocCfgOptions.isImageInsertEnabled())
        {
            mInsertImageButton.setEnabled(selCaret);
        }

        if (mInsertPhotoButton != null && mDocCfgOptions.isPhotoInsertEnabled())
        {
            mInsertPhotoButton.setEnabled(selCaret);
        }
    }

    protected void setInsertTabVisibility()
    {
    }

    public void onSelectionChanged()
    {
        // Take care on shutdown.
        if (mSession == null || mSession.getDoc() == null || mFinished)
        {
            return;
        }

        mDocView.onSelectionChanged();
        if (usePagesView() && isPageListVisible())
            mDocPageListView.onSelectionChanged();

        updateUIAppearance();

        reportViewChanges();
    }

    public void triggerRender()
    {
        if (mDocView!=null)
            mDocView.triggerRender();
        if (mDocPageListView!=null && usePagesView() && isPageListVisible())
            mDocPageListView.triggerRender();
    }

    private long lastTypingTime = 0;
    public void onTyping()
    {
        //  record last typing time
        lastTypingTime = System.currentTimeMillis();
    }

    public boolean wasTyping()
    {
        // if typing took place within the last 500 msec, return true;
        long now = System.currentTimeMillis();
        return (now-lastTypingTime) <500;
    }

    public void setIsComposing(boolean isComposing)
    {
        mIsComposing = isComposing;
    }

    public boolean getIsComposing()
    {
        return mIsComposing;
    }

    public void onSelectionMonitor(int startPage, int endPage)
    {
        onSelectionChanged();
    }

    public void onLayoutChanged()
    {
        //  this is called when a core layout is done.

        // Take care on shutdown.
        if (mSession == null || mSession.getDoc() == null || mFinished)
            return;

        mDocView.onLayoutChanged();
    }

    protected void onTabChanging(String oldTabId, String newTabId)
    {
    }

    @Override
    public void onTabChanged(String tabId)
    {
        //  watch tab transitions
        onTabChanging(mCurrentTab, tabId);

        //  fixes https://bugs.ghostscript.com/show_bug.cgi?id=701155
        getDocView().saveComment();

        //  TODO: move the "review" bits into the doc class.

        if (tabId.equals(activity().getString(R.string.sodk_editor_tab_review)) && !((SODoc)getDocView().getDoc()).docSupportsReview())
        {
            //  user tapped "review", but this doc is not reviewable.
            //  show a message
            Utilities.showMessage(activity(), activity().getString(R.string.sodk_editor_not_supported), activity().getString(R.string.sodk_editor_cant_review_doc_body));

            //  ... and restore the previous state, because onTabChanged is called after the fact.
            setTab(mCurrentTab);
            if (mCurrentTab.equals(activity().getString(R.string.sodk_editor_tab_hidden)))
                onSearchButton(mSearchButton);
            onSelectionChanged();
            return;
        }

        if (tabId.equals(activity().getString(R.string.sodk_editor_tab_single)))
        {
            //  restore the previous state, because onTabChanged is called after the fact.
            setTab(mCurrentTab);

            onSelectionChanged();

            //  pop up a menu (actually a ListPopupWindow)
            final Activity activity = activity();

            mListPopupWindow = new ListPopupWindow(activity());
            mListPopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(activity(), R.drawable.sodk_editor_menu_popup));
            mListPopupWindow.setModal(true);
            mListPopupWindow.setAnchorView(getSingleTabView());

            //  an adapter for the popup with the values we want
            //  use a known id value for the TextViews in the menu. See SOTextView.init().
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity(), R.layout.sodk_editor_menu_popup_item, R.id.sodk_editor_text_view);
            mListPopupWindow.setAdapter(adapter);

            NUIDocView.TabData tabData[] = getTabData();
            int n = tabData.length;
            for (int i=0; i<n; i++)
            {
                // Omit disabled tabs from popup options.
                if (tabData[i].visibility != View.VISIBLE)
                {
                    continue;
                }

                String s = tabData[i].name;
                adapter.add(s);
                SOTextView tv = (SOTextView)activity().getLayoutInflater().inflate(R.layout.sodk_editor_menu_popup_item, null);
                tv.setText(s);
            }

            if (hasSearch()) {
                adapter.add(activity().getString(R.string.sodk_editor_tab_find));
                SOTextView tv = (SOTextView) activity().getLayoutInflater().inflate(R.layout.sodk_editor_menu_popup_item, null);
                tv.setText(activity().getString(R.string.sodk_editor_tab_find));

            }

            //  establish a click handler
            mListPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (mListPopupWindow!=null)
                        mListPopupWindow.dismiss();
                    mListPopupWindow = null;

                    onPreTabChange();

                    String title = adapter.getItem(position);
                    if (title.equals(activity().getString(R.string.sodk_editor_tab_find)))
                    {
                        onSearch();
                        setSingleTabTitle(title);
                    }
                    else
                    {
                        if (title.equals(activity().getString(R.string.sodk_editor_tab_review)) && !((SODoc)getDocView().getDoc()).docSupportsReview())
                        {
                            //  user tapped "review", but this doc is not reviewable.
                            //  show a message
                            Utilities.showMessage(activity(), activity().getString(R.string.sodk_editor_not_supported), activity().getString(R.string.sodk_editor_cant_review_doc_body));
                        }
                        else
                        {
                            changeTab(title);
                            setSingleTabTitle(title);
                            tabHost.setCurrentTabByTag(title);
                            onNewTabShown(title);
                        }
                    }
                    //  re-measure the single tab
                    measureTabs();

                    setOneTabColor(getSingleTabView(), true);
                }
            });

            //  set the width
            mListPopupWindow.setContentWidth(measureContentWidth(adapter));

            //  hide the keyboard and show the popup
            showKeyboard(false, new Runnable() {
                @Override
                public void run() {
                    mListPopupWindow.show();
                }
            });

            return;
        }

        changeTab(tabId);

        if (!Utilities.isPhoneDevice(getContext()))
        {
            onNewTabShown(tabId);
        }
    }

    private int measureContentWidth(ListAdapter listAdapter) {
        ViewGroup mMeasureParent = null;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final ListAdapter adapter = listAdapter;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(getContext());
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }


    protected View getSingleTabView()
    {
        int tabCount = tabHost.getTabWidget().getTabCount();
        return tabHost.getTabWidget().getChildTabViewAt(tabCount-1);
    }

    protected void changeTab (String tabId)
    {
        mCurrentTab = tabId;

        setSingleTabTitle(tabId);

        onSelectionChanged();

        if (!mCurrentTab.equals(getContext().getString(R.string.sodk_editor_tab_find)) &&
                !mCurrentTab.equals(getContext().getString(R.string.sodk_editor_tab_hidden)))
        {
            //  hide the search tab if we're switching away from it.
            //  In the phone case we come through here twice, once for the "HIDDEN" tab
            //  and once for the real tab.
            findViewById(R.id.searchTab).setVisibility(View.GONE);
            showSearchSelected(false);
        }

        //  show/hide the pages view
        handlePagesTab(tabId);

        //  fix up background and text colors
        setTabColors(tabId);

        //  when changing tabs, the toolbar height may be different,
        //  so re-layout the document view.
        mDocView.layoutNow();
    }

    protected void setTabColors(String tabId)
    {
        Iterator it = tabMap.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            String text = (String)pair.getKey();
            View v = (View)pair.getValue();

            setOneTabColor(v, tabId.equals(text));
        }

        setOneTabColor(getSingleTabView(), true);
    }

    protected void setOneTabColor(View v, boolean selected)
    {
        //  find the background shape in the layer-list
        LayerDrawable bgDrawable = (LayerDrawable)v.getBackground();
        GradientDrawable shape = (GradientDrawable)bgDrawable.findDrawableByLayerId(R.id.tab_bg_color);

        //  get the text view
        SOTextView tv = v.findViewById(R.id.tabText);

        if (selected)
        {
            //  selected
            shape.setColor(getTabSelectedColor());
            tv.setTextColor(getTabSelectedTextColor());
            for (Drawable drawable : tv.getCompoundDrawables()){
                if (drawable != null)
                    drawable.setColorFilter(getTabSelectedTextColor(), PorterDuff.Mode.SRC_IN);
            }
        }
        else
        {
            //  not selected
            shape.setColor(getTabUnselectedColor());
            tv.setTextColor(getTabUnselectedTextColor());
            for (Drawable drawable : tv.getCompoundDrawables()){
                if (drawable != null)
                    drawable.setColorFilter(getTabUnselectedTextColor(), PorterDuff.Mode.SRC_IN);
            }
        }
    }

    protected int getTabSelectedTextColor()
    {
        return ContextCompat.getColor(activity(), R.color.sodk_editor_header_text_color_selected);
    }

    protected int getTabUnselectedTextColor()
    {
        return ContextCompat.getColor(activity(), R.color.sodk_editor_header_text_color);
    }

    protected int getTabSelectedColor()
    {
        int colorFromDocType = getResources().getInteger(R.integer.sodk_editor_ui_doc_tab_color_from_doctype);
        int color = 0;

        if (colorFromDocType == 0)
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_color_selected);
        else
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_doc_color);

        return color;
    }

    protected int getTabUnselectedColor()
    {
        int colorFromDocType = getResources().getInteger(R.integer.sodk_editor_ui_doc_tabbar_color_from_doctype);
        int color = 0;

        if (colorFromDocType == 0)
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_color);
        else
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_doc_color);

        return color;
    }

    protected void handlePagesTab(String tabId)
    {
        if (tabId.equals(activity().getString(R.string.sodk_editor_tab_pages)))
            showPages();
        else
            hidePages();
    }

    protected boolean isSearchVisible()
    {
        //  determines whether the search toolbar is visible by inspecting
        //  the search text field.
        View v = findViewById(R.id.search_text_input);
        if (v!=null && v.getVisibility()==View.VISIBLE && v.isShown()) {
            return true;
        }

        return false;
    }

    protected void showSearchSelected (boolean selected)
    {
        //  set the view
        if (mSearchButton!=null)
        {
            mSearchButton.setSelected(selected);

            //  colorize
            if (selected)
                setButtonColor(mSearchButton, ContextCompat.getColor(activity(), R.color.sodk_editor_button_tint));
            else
                setButtonColor(mSearchButton, ContextCompat.getColor(activity(), R.color.sodk_editor_header_button_enabled_tint));
        }
    }

    public void onSaveButton(final View v)
    {
        preSave();
        doSave();
    }

    public void onCustomSaveButton(final View v)
    {
        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            // if doc cannot be saved, show 'cannot save' msg and return.
            if (!mDocCfgOptions.isDocSavable())
            {
                Utilities.showMessage(
                    activity(),
                    getContext().getString(R.string.sodk_editor_error),
                    getContext().getString(R.string.sodk_editor_has_no_permission_to_save));
                return;
            }

            preSaveQuestion(new Runnable() {
                @Override
                public void run() {
                    //  yes

                    String path = mFileState.getUserPath();
                    if (path == null)
                        path = mFileState.getOpenedPath();
                    File file = new File(path);

                    preSave();

                    try
                    {
                        mDataLeakHandlers.customSaveHandler(file.getName(),
                                mSession.getDoc(),
                                mCustomDocdata,
                                new SOCustomSaveComplete()
                                {
                                    @Override
                                    public void onComplete(int     result,
                                                           String  path,
                                                           boolean shouldClose)
                                    {
                                        /*
                                         * Make sure that when we close, we don't think
                                         * there are outstanding changes.
                                         */
                                        mFileState.setHasChanges(false);

                                        if (result == SOCustomSaveComplete_Succeeded)
                                        {
                                            mFileState.setHasChanges(false);
                                        }

                                        // Close the document view if required.
                                        if (shouldClose)
                                        {
                                            prefinish();
                                        }
                                    }
                                });
                    }
                    catch(UnsupportedOperationException e)
                    {
                        return;
                    }
                    catch(IOException e)
                    {
                        return;
                    }

                    return;
                }
            }, new Runnable() {
                @Override
                public void run() {
                    //  no
                }
            });
        }
    }

    private void doSave()
    {
        if (mIsTemplate)
        {
            /*
             * In the case where the save keyboard shortcut is executed on
             * a template file treat as a 'saveAs' request.
             */
            doSaveAs(false, null);
            return;
        }

        preSaveQuestion(new Runnable() {
            @Override
            public void run() {
                //  yes

                //  save the document
                final ProgressDialog spinner = Utilities.createAndShowWaitSpinner(getContext());
                mSession.getDoc().saveTo(mFileState.getInternalPath(), new SODocSaveListener() {
                    @Override
                    public void onComplete(final int result, final int err) {
                        spinner.dismiss();
                        if (result == SODocSave_Succeeded) {
                            mFileState.saveFile();
                            updateUIAppearance();

                            if (mDataLeakHandlers!=null)
                                mDataLeakHandlers.postSaveHandler(
                                        new SOSaveAsComplete()
                                        {
                                            @Override
                                            public boolean onFilenameSelected( String path )
                                            {
                                                return true;
                                            }

                                            @Override
                                            public void onComplete(int result, String path)
                                            {
                                                //  reload the file
                                                reloadFile();
                                            }
                                        }
                                );

                        } else {

                            String message = String.format(activity().getString(com.artifex.sonui.editor.R.string.sodk_editor_error_saving_document_code), err);
                            Utilities.showMessage(activity(), activity().getString(com.artifex.sonui.editor.R.string.sodk_editor_error), message);
                        }
                    }
                });

            }
        }, new Runnable() {
            @Override
            public void run() {
                //  no
            }
        });

    }

    public void preSave()
    {
    }

    public void onSaveAsButton(final View v)
    {
        preSave();
        doSaveAs(false, null);
    }

    public void onSavePDFButton(final View v)
    {
        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                File file = new File(mFileState.getOpenedPath());

                mDataLeakHandlers.saveAsPdfHandler(file.getName(),
                                                   mSession.getDoc());

                return;
            }
            catch(UnsupportedOperationException e)
            {
                return;
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    public void onPrintButton(final View v)
    {
        // expired document cannot be printed.
        if (mDocCfgOptions.isDocExpired())
        {
            Utilities.showMessage(
                activity(),
                getContext().getString(R.string.sodk_editor_error),
                getContext().getString(R.string.sodk_editor_has_no_permission_to_print));
            return;
        }

        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                mDataLeakHandlers.printHandler(mSession.getDoc());

                return;
            }
            catch(UnsupportedOperationException e)
            {
                return;
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    protected void onPauseCommon()
    {
        saveState();

        mIsActivityActive = false;

        resetInputView();
    }

    public void onPause(final Runnable whenDone)
    {
        onPauseCommon();

        //  not if we've been finished
        if (mDocView!=null && mDocView.finished()) {
            whenDone.run();
            return;
        }

        //  not if we've not been opened
        if (mFileState == null || mDocView == null || mDocView.getDoc()==null) {
            whenDone.run();
            return;
        }

        if (mDataLeakHandlers != null)
        {
            //  let the data leak handler deal with pause.
            ArDkDoc doc = mDocView.getDoc();
            mDataLeakHandlers.pauseHandler(doc, doc.getHasBeenModified(), new Runnable(){
                @Override
                public void run() {
                    whenDone.run();
                }
            });
        }
        else
        {
            whenDone.run();
        }
    }

    public void releaseBitmaps()
    {
        //  release the bitmaps (making them null)
        setValid(false);
        recycleBitmaps();
        setBitmapsInViews();
    }

    private void setValid(boolean val)
    {
        if (mDocView!=null)
            mDocView.setValid(val);
        if (usePagesView())
            if (mDocPageListView!=null)
                mDocPageListView.setValid(val);
    }

    private void setBitmapsInViews()
    {
        //  tell the doc and page list views what bitmaps to use.
        //  these might be null.
        if (mDocView!=null)
            mDocView.setBitmaps(bitmaps);
        if (usePagesView())
            if (mDocPageListView!=null)
                mDocPageListView.setBitmaps(bitmaps);
    }

    protected void onResumeCommon()
    {
        //  keep track of the current view
        mCurrentNUIDocView = this;

        if (mDocUserPath != null)
        {
            //  create new bitmaps
            createBitmaps();
            setBitmapsInViews();
        }

        //  If a reload was requested while we were in the background, do it now
        if (mForceReloadAtResume)
        {
            mForceReloadAtResume = false;

            getDoc().setForceReloadAtResume(true);
            reloadFile();
        }
        else if (mForceReload)
        {
            mForceReload = false;

            getDoc().setForceReload(true);
            reloadFile();
        }

        mIsActivityActive = true;

        focusInputView();

        //  do a new layout so the content is redrawn.
        DocView docView = getDocView();
        if (docView!=null)
            docView.forceLayout();

        //  don't forget the page list (bug 699304)
        if (usePagesView())
        {
            DocListPagesView pagesView = getDocListPagesView();
            if (pagesView != null)
                pagesView.forceLayout();
        }
    }

    public void onResume()
    {
        onResumeCommon();

        //  when we resume, the keyboard will not be showing.
        keyboardHeight = 0;
        onShowKeyboard(false);

        //  did we have changes when we went into the background?
        SOFileState state = SOFileState.getAutoOpen(getContext());
        if (state != null && mFileState != null)
        {
            //  The doc may have been saved since we went to the background
            //  because the Save As dialog is really an activity, which will bg us.
            //  so don't adjust the hasChanges value in that case.
            if (state.getLastAccess() > mFileState.getLastAccess())
            {
                mFileState.setHasChanges(state.hasChanges());
            }
        }

        //  clear autoOpen
        SOFileState.clearAutoOpen(getContext());

        if (mDataLeakHandlers != null)
        {
            // Complete any pending photo/image insertion.
            mDataLeakHandlers.doInsert();
        }
    }

    public void onShareButton(final View v)
    {
        preSave();

        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                File file = new File(mFileState.getOpenedPath());

                mDataLeakHandlers.shareHandler(file.getName(),
                                               mSession.getDoc());

                return;
            }
            catch(NullPointerException e)
            {
                return;
            }
            catch(UnsupportedOperationException e)
            {
                return;
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    public void onOpenInButton(final View v)
    {
        preSave();

        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                File file = new File(mFileState.getOpenedPath());

                mDataLeakHandlers.openInHandler(file.getName(),
                                                mSession.getDoc());

                return;
            }
            catch(NullPointerException e)
            {
                return;
            }
            catch(UnsupportedOperationException e)
            {
                return;
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    public void onOpenPDFInButton(final View v)
    {
        preSave();

        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                File file = new File(mFileState.getOpenedPath());

                mDataLeakHandlers.openPdfInHandler(file.getName(),
                                                   mSession.getDoc());

                return;
            }
            catch(NullPointerException e)
            {
                return;
            }
            catch(UnsupportedOperationException e)
            {
                return;
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }

    }

    public void onProtectButton(final View v)
    {
    }

    public boolean documentHasBeenModified()
    {
        return mSession!=null
                && mSession.getDoc()!=null
                && mFileState!=null
                && (mSession.getDoc().getHasBeenModified() || mFileState.hasChanges());
    }

    //  this is incremented each time a new search is started.
    private int mSearchCounter = 0;
    private void incrementSearchCounter() {
        if (mSearchCounter > 1000)
            mSearchCounter = 0;
        mSearchCounter++;
    }

    //  handler for posting searches
    private Handler mSearchHandler = null;

    private void searchCommon(final boolean backwards)
    {
        if (mSearchHandler==null)
            mSearchHandler = new Handler();

        mSearchHandler.post(new Runnable() {
            @Override
            public void run()
            {
                //  must have a doc.
                ArDkDoc doc = getDoc();
                if (doc==null)
                    return;

                //  not if we're already searching
                if (mIsSearching)
                    return;
                mIsSearching = true;

                //  new search counter
                incrementSearchCounter();

                //  now do it.
                doc.setSearchBackwards(backwards);
                doSearch();
            }
        });
    }

    public void onSearchNext(final View v)
    {
        searchCommon(false);
    }

    public void onSearchPrevious(final View v)
    {
        searchCommon(true);
    }

    private void doSearch()
    {
        Utilities.hideKeyboard(getContext());

        showSearchProgress();

        ArDkDoc doc = getDoc();

        //  if our UI is showing, get the search string from it.
        //  otherwise, assume it's been set elsewhere.
        if (mShowUI)
        {
            String s = mSearchText.getText().toString();
            doc.setSearchString(s);
        }

        doc.search();
    }

    //  search dialog
    private ProgressDialog mSearchProgressDialog = null;

    //  true if a progress dialog has been scheduled
    private boolean mProgressIsScheduled = false;

    //  handler for posting delayed progress
    private Handler mProgressHandler = null;

    private void showSearchProgress()
    {
        //  schedule one progress at a time, so we don't get
        //  a bunch of posted Runnables piling up.
        if (mProgressIsScheduled)
            return;
        mProgressIsScheduled = true;

        //  keep a copy of the current counter.
        //  one second from now, if it's still the same, then show the dialog.
        final int counter = mSearchCounter;

        //  go away for 1000 msec
        if (mProgressHandler==null)
            mProgressHandler = new Handler();
        mProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                //  ... 1000 msec later

                mProgressIsScheduled = false;

                //  we may have stopped searching
                if (!mIsSearching)
                    return;

                //  the search that triggered us may be over.
                if (counter != mSearchCounter)
                    return;

                //  fix bug 698870 (go back while searching)
                ArDkDoc theDoc = getDoc();
                if (theDoc==null)
                    return;

                //  now show the dialog
                if (mSearchProgressDialog==null)
                    mSearchProgressDialog = new ProgressDialog(getContext(), R.style.sodk_editor_alert_dialog_style);
                mSearchProgressDialog.setMessage(getResources().getString(R.string.sodk_editor_searching)+"...");
                mSearchProgressDialog.setCancelable(false);
                mSearchProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                        getResources().getString(R.string.sodk_editor_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //  cancel the search
                                getDoc().cancelSearch();
                            }
                        });
                mSearchProgressDialog.show();
            }
        }, 1000);
        //  NOTHING AFTER THIS PLEASE
    }

    private void hideSearchProgress()
    {
        if (mSearchProgressDialog!=null)
            mSearchProgressDialog.dismiss();
    }

    public boolean showKeyboard()
    {
        //  show keyboard
        Utilities.showKeyboard(getContext());

        return true;
    }

    //  this variant of showKeyboard will wait for the layout changes resulting from
    //  showing or hiding the keyboard to complete before running the supplied Runnable.
    //  if no change to the keyboard visibility would result, it calls the Runnable
    //  immediately.  If a change to the keyboard visibility would result, it
    //  stores a reference to the runnable, which is then acted upon in onShowKeyboard().

    protected Runnable mShowHideKeyboardRunnable = null;
    protected void showKeyboard(boolean show, Runnable runnable)
    {
        boolean showing = getKeyboardHeight()>0;
        if (show)
        {
            Utilities.showKeyboard(getContext());
            if (showing)
                runnable.run();
            else
                mShowHideKeyboardRunnable = runnable;
        }
        else
        {
            Utilities.hideKeyboard(getContext());
            if (!showing)
                runnable.run();
            else
                mShowHideKeyboardRunnable = runnable;
        }
    }

    protected void onShowKeyboardPreventPush(boolean bShow)
    {
        //  This bit of code prevents the activity's content from getting pushed
        //  up when the keyboard appears, by adding padding to the view.
        //  We also hide the footer when the keyboard is visible.

        boolean fullScreen = (activity().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        if (fullScreen)
        {
            Activity activity = ((Activity) getContext());
            View content = activity.findViewById(android.R.id.content);
            if (bShow) {

                //  up through API 29, when the keyboard appeared, the content's height did not change.
                //  so it was necessary to add padding to bring the content's bottom border up above the keyboard.
                //  starting in API 30, the content height is reduced by the keyboard's height,
                //  so it's no longer necessary to add the padding.

                if(Build.VERSION.SDK_INT < 30 )
                    content.setPadding(0, 0, 0, getKeyboardHeight());
                findViewById(R.id.footer).setVisibility(View.GONE);
            } else {
                if(Build.VERSION.SDK_INT < 30 )
                    content.setPadding(0, 0, 0, 0);
                findViewById(R.id.footer).setVisibility(View.VISIBLE);
            }
        }

    }

    protected boolean keyboardShown = false;
    public boolean isKeyboardVisible() {return keyboardShown;}

    public void onShowKeyboard(final boolean bShow)
    {
        //  ignore this if we're not the active app
        if (!isActivityActive())
            return;

        //  ignore this if we've got no pages
        if (getPageCount()<=0)
            return;

        keyboardShown = bShow;

        //  prevents the activity's content from getting pushed up
        onShowKeyboardPreventPush(bShow);

        //  show or hide the tabs and header as appropriate
        if (!isFullScreen())
            showUI(!bShow);

        if (usePagesView())
        {
            DocListPagesView pagesView = getDocListPagesView();
            if (pagesView != null)
                pagesView.onShowKeyboard(bShow);
        }

        //  delay the rest so the layout can settle first
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);

                //  tell the doc view about the keyboard change
                DocView docview = getDocView();
                if (docview != null)
                    docview.onShowKeyboard(bShow);

                //  run and dispose of the optional Runnable.
                if (mShowHideKeyboardRunnable != null)
                {
                    mShowHideKeyboardRunnable.run();
                    mShowHideKeyboardRunnable = null;
                }

                layoutNow();
            }
        });
    }

    public void showUI(final boolean bShow)
    {
        //  If there's a registered handler for exiting full screen, run it
        if (bShow)
        {
            if (mExitFullScreenRunnable != null)
                mExitFullScreenRunnable.run();
            //  set this here in case we were started with no UI
            mFullscreen = false;
        }

        //  if we're started with no UI, keep it that way.
        if (!mShowUI)
            return;

        View tabhost = findViewById(R.id.tabhost);
        View header  = findViewById(R.id.header);

        if (isLandscapePhone())
        {
            if (!bShow && tabhost.getVisibility()!=View.GONE && !isSearchVisible())
            {
                tabhost.setVisibility(View.GONE);
                header.setVisibility(View.GONE);
                layoutNow();
            }
            else if (bShow && tabhost.getVisibility()!=View.VISIBLE)
            {
                tabhost.setVisibility(View.VISIBLE);
                header.setVisibility(View.VISIBLE);
                layoutNow();
            }
        }
        else
        {
            if (tabhost.getVisibility()!=View.VISIBLE)
            {
                tabhost.setVisibility(View.VISIBLE);
                header.setVisibility(View.VISIBLE);
                layoutNow();
            }
        }

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                afterShowUI(bShow);
            }
        });
        //  NOTHING after this please.
    }

    protected void afterShowUI(boolean bShow)
    {
        if (mDocCfgOptions.isFullscreenEnabled())
        {
            if (bShow)
            {
                if (mFullscreenToast!=null)
                    mFullscreenToast.cancel();

                //  restore footer and page list, which may have been hidden by full screen mode
                findViewById(R.id.footer).setVisibility(View.VISIBLE);
                if (isPagesTab())
                    showPages();

                mFullscreen = false;
                if (getDocView() != null)
                {
                    getDocView().onFullscreen(false);
                }
                layoutNow();
            }
        }
    }

    protected boolean isPagesTab()
    {
        //  check if the current tab is the "pages" tab
        return getCurrentTab().equals(activity().getString(R.string.sodk_editor_tab_pages));
    }

    public void goToPage(int page)
    {
        goToPage(page, false);
    }

    public void goToPage(int page, boolean fast)
    {
        mDocView.scrollToPage(page, fast);
        if (usePagesView()) {
            //  ask the page list to highlight this as the current page
            mDocPageListView.setCurrentPage(page);
            mDocPageListView.scrollToPage(page, fast);
        }

        //  update the current page
        mCurrentPageNum = page;
        setCurrentPage(page);
    }

    public void onFirstPageButton(View v)
    {
        mDocView.addPageHistory(0);
        mDocView.goToFirstPage();
        if (usePagesView())
            mDocPageListView.goToFirstPage();
        setCurrentPage(0);
    }

    public void onLastPageButton(View v)
    {
        mDocView.addPageHistory(getPageCount()-1);
        mDocView.goToLastPage();
        if (usePagesView())
            mDocPageListView.goToLastPage();
        setCurrentPage(getPageCount()-1);
    }

    @Override
    public void onClick(View v)
    {
        if (v == null)
            return;

        // Ignore button presses while we are finishing up.
        if (mFinished)
        {
            return;
        }

        //  file toolbar buttons
        if (v==mSaveAsButton)
            onSaveAsButton(v);
        if (v==mSaveButton)
            onSaveButton(v);
        if (v==mCustomSaveButton)
            onCustomSaveButton(v);
        if (v==mSavePdfButton)
            onSavePDFButton(v);
        if (v==mPrintButton)
            onPrintButton(v);
        if (v==mShareButton)
            onShareButton(v);
        if (v==mOpenInButton)
            onOpenInButton(v);
        if (v==mOpenPdfInButton)
            onOpenPDFInButton(v);
        if (v==mProtectButton)
            onProtectButton(v);

        //  edit toolbar buttons
        //  this code moved to the edit toolbar object

        if (v==mCopyButton2)
            doCopy();

        //  page toolbar buttons
        if (v==mFirstPageButton)
            onFirstPageButton(v);
        if (v==mLastPageButton)
            onLastPageButton(v);
        if (v==mReflowButton)
            onReflowButton(v);

        //  undo and redo, search and back
        if (v==mUndoButton)
            onUndoButton(v);
        if (v==mRedoButton)
            onRedoButton(v);
        if (v==mSearchButton)
            onSearchButton(v);
        if (v==mSearchNextButton)
            onSearchNext(v);
        if (v==mSearchPreviousButton)
            onSearchPrevious(v);
        if (v==mBackButton)
            goBack();

        //  insert
        if (v==mInsertImageButton)
            onInsertImageButton(v);
        if (v==mInsertPhotoButton)
            onInsertPhotoButton(v);

        //  full screen
        if (mDocCfgOptions.isFullscreenEnabled() && mFullscreenButton!=null && v==mFullscreenButton)
            onFullScreen(v);
    }

    public void clickSheetButton(int index, boolean searching) {}

    public int getBorderColor()
    {
        return ContextCompat.getColor(getContext(), R.color.sodk_editor_selected_page_border_color);
    }

    protected View createToolbarButton(int id)
    {
        View v = findViewById(id);
        if (v != null) {
            v.setOnClickListener(this);
        }
        return v;
    }

    public boolean isLandscapePhone()
    {
        return ((mLastOrientation == ORIENTATION_LANDSCAPE) &&
                (Utilities.isPhoneDevice(getContext())));
    }

    public boolean canCanManipulatePages() {return false;}

    public boolean doKeyDown(int keyCode, KeyEvent event)
    {
        boolean cmd    = event.isAltPressed();
        boolean ctrl   = event.isCtrlPressed();
        boolean shift  = event.isShiftPressed();

        BaseActivity activity = ((BaseActivity)getContext());

        switch (event.getKeyCode())
        {
            case KeyEvent.KEYCODE_FORWARD_DEL:
                if (inputViewHasFocus())
                {
                    onTyping();
                    ((SODoc)getDoc()).onForwardDeleteKey();

                    /*
                     * Let the input view know that the selection has been
                     * modified.
                     */
                    updateInputView();

                    return true;
                }
                return false;


            case KeyEvent.KEYCODE_DEL:
                if (inputViewHasFocus())
                {
                    onTyping();
                    ((SODoc)getDoc()).onDeleteKey();

                    /*
                     * Let the input view know that the selection has been
                     * modified.
                     */
                    updateInputView();

                    return true;
                }
                return false;


            case KeyEvent.KEYCODE_BACK:
                if (activity.isSlideShow())
                    activity.finish();
                else
                    onBackPressed();
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
            {

                ArDkSelectionLimits limits = getDocView().getSelectionLimits();

                if (limits == null  || ! limits.getIsActive())
                {
                    //  no focus, so handle as paging
                    if (cmd || ctrl)
                        pageUp();
                    else
                        lineUp();
                    return true;
                }

                // Fallthrough
            }

            case KeyEvent.KEYCODE_DPAD_DOWN:
            {
                ArDkSelectionLimits limits = getDocView().getSelectionLimits();

                if (limits == null  || ! limits.getIsActive())
                {
                    //  no focus, so handle as paging
                    if (cmd || ctrl)
                        pageDown();
                    else
                        lineDown();
                    return true;
                }

                // Fallthrough
            }

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (inputViewHasFocus()) {
                    onTyping();
                    doArrowKey(event);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_C:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  copy
                    doCopy();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_V:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  paste
                    onTyping();
                    doPaste();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_X:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  cut
                    onTyping();
                    doCut();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_Z:
                if (ctrl || cmd) {
                    onTyping();
                    if (shift) {
                        //  redo
                        doRedo();
                    }
                    else {
                        //  undo
                        doUndo();
                    }
                    return true;
                }
                break;

//            case KeyEvent.KEYCODE_A:
//                if (inputViewHasFocus() && (ctrl || cmd)) {
//                    //  select all
//                    doSelectAll();
//                    return true;
//                }
//                break;

            case KeyEvent.KEYCODE_S:
                if (ctrl || cmd) {
                    //  save
                    doSave();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_B:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  bold
                    onTyping();
                    doBold();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_I:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  italic
                    onTyping();
                    doItalic();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_U:
                if (inputViewHasFocus() && (ctrl || cmd)) {
                    //  underline
                    onTyping();
                    doUnderline();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                if (inputViewHasFocus()) {
                    mIsComposing = false;
                }
                break;

            case KeyEvent.KEYCODE_SPACE:
            {
                ArDkSelectionLimits limits = getDocView().getSelectionLimits();

                if (limits == null  || ! limits.getIsActive())
                {
                    if (shift)
                        pageUp();
                    else
                        pageDown();
                    return true;
                }
            }
        }

        //  this covers most of the keys
        if (inputViewHasFocus()) {
            char pressedKey = (char) event.getUnicodeChar();
            if (pressedKey != '\0') {
                onTyping();
                String s = "";
                s += pressedKey;
                ((SODoc)getDoc()).setSelectionText(s);

                // Update the input view with the current docment content.
                updateInputView();
            }
        }


        return true;
    }

    private void pageUp()
    {
        DocView dv = getDocView();
        int scrollDistance = dv.getHeight()*9/10;
        dv.smoothScrollBy(0, scrollDistance, 400);
    }

    private void pageDown()
    {
        DocView dv = getDocView();
        int scrollDistance = -dv.getHeight()*9/10;
        dv.smoothScrollBy(0, scrollDistance, 400);
    }

    private void lineUp()
    {
        DocView dv = getDocView();
        int scrollDistance = dv.getHeight()*1/20;
        dv.smoothScrollBy(0, scrollDistance, 100);
    }

    private void lineDown()
    {
        DocView dv = getDocView();
        int scrollDistance = -dv.getHeight()*1/20;
        dv.smoothScrollBy(0, scrollDistance, 100);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        BaseActivity activity = ((BaseActivity)getContext());
        if (!activity.isSlideShow())
        {
            View focused = activity.getCurrentFocus();
            if (focused != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            {
                //  someone has focus, it's likely that the keyboard is showing.
                //  release focus, hide the keyboard.
                focused.clearFocus();
                Utilities.hideKeyboard(getContext());
                return true;
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    //  put a file's name in the footer,
    private void setFooterText(String path)
    {
        if (path==null || path.isEmpty())
            return;

        String name = new File(path).getName();
        if (name!=null && !name.isEmpty())
        {
            //  use the simple name
            mFooter.setText(name);
        }
        else
        {
            //  problem with the simple name, use the full path
            mFooter.setText(path);
        }
    }

    public void onReflowScale()
    {
        //  handle possible appearance of new pages
        mDocView.onReflowScale();
        if (usePagesView())
            mDocPageListView.onReflowScale();
    }

    public void reloadFile()
    {
    }

    private boolean mForceReload = false;
    public void forceReload()
    {
        mForceReload = true;
    }

    //  this is used if we save while pausing.
    //  in that case we defer reloading until we resume
    private boolean mForceReloadAtResume = false;
    public void forceReloadAtResume()
    {
        mForceReloadAtResume = true;
    }

    public void onAuthorButton(View v)
    {
        final ArDkDoc doc = getDocView().getDoc();
        AuthorDialog.show(activity(), new AuthorDialog.AuthorDialogListener() {
            @Override
            public void onOK(String author) {
                //  set the author in the document
                doc.setAuthor(author);
                //  ... and save it as a preference.
                Object store =
                        Utilities.getPreferencesObject(activity(),
                                Utilities.generalStore);

                Utilities.setStringPreference(store, "DocAuthKey", author);
            }
            @Override
            public void onCancel() {
            }
        }, doc.getAuthor());

    }

    protected Toast mFullscreenToast;
    protected boolean mFullscreen = false;
    public boolean isFullScreen()
    {
        if (!mDocCfgOptions.isFullscreenEnabled())
            return false;
        return mFullscreen;
    }

    protected void onFullScreenHide()
    {
        findViewById(R.id.tabhost).setVisibility(View.GONE);
        findViewById(R.id.header).setVisibility(View.GONE);
        findViewById(R.id.footer).setVisibility(View.GONE);
        hidePages();
        layoutNow();
    }

    protected void onFullScreen(View v)
    {
        //  not after we're done
        if (mFinished)
            return;
        if (getDocView()==null)
            return;

        //  no need
        if (isFullScreen())
            return;

        if (!mDocCfgOptions.isFullscreenEnabled())
            return;

        mFullscreen = true;

        //  hide keyboard
        Utilities.hideKeyboard(getContext());

        //  tell the document view
        getDocView().onFullscreen(true);

        //  hide everything
        onFullScreenHide();

        //  show a reminder in the upper right corner
        if (mFullscreenToast == null)
        {
            String message = getContext().getString(R.string.sodk_editor_fullscreen_warning);
            mFullscreenToast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
            mFullscreenToast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
        }
        mFullscreenToast.show();
    }

    private TabData findTabdataForId(String id)
    {
        if (mTabs!=null) {
            for (int i = 0; i < mTabs.length; i++) {
                TabData data = mTabs[i];
                if (id.equals(data.name))
                    return data;
            }
        }
        return null;
    }

    private static final String WAS_ANIM_KEY = "scroll_was_animated";
    private static final String STORE_NAME = Utilities.generalStore;
    private boolean wasAnimated()
    {
        Object store = Utilities.getPreferencesObject(getContext(), STORE_NAME);
        if (store != null) {
            String value = Utilities.getStringPreference(store, WAS_ANIM_KEY, "TRUE");
            if (value != null) {
                if (!value.equals("TRUE"))
                    return false;
            }
        }

        return true;
    }

    private void setWasAnimated()
    {
        Object store = Utilities.getPreferencesObject(getContext(), STORE_NAME);
        if (store != null) {
            Utilities.setStringPreference(store, WAS_ANIM_KEY, "TRUE");
        }
    }

    private void onNewTabShown(String id)
    {
        //  this is called each time a tab is newly shown.
        //  we're using it to animate the toolbar's scrollability.

        //  look for the tab data matching the id
        TabData data = findTabdataForId(id);
        if (data == null)
            return;

        //  find the content
        LinearLayout layout = findViewById(data.contentId);
        if (layout == null || layout.getChildCount() == 0)
            return;

        //  find the horizontal scroll view, which should be the first child
        final SOHorizontalScrollView hsv = (SOHorizontalScrollView) layout.getChildAt(0);
        if (hsv == null)
            return;

        //  might we animate it?
        if (!hsv.mayAnimate())
            return;

        //  only do this once
        if (wasAnimated())
            return;
        setWasAnimated();

        //  start animating
        hsv.startAnimate();

        //  show an informative message
        Utilities.showMessageAndWait(
                activity(),
                getContext().getString(R.string.sodk_editor_scrollable_title),
                getContext().getString(R.string.sodk_editor_scrollable_message),
                R.style.sodk_editor_alert_dialog_style_nodim,
                new Runnable() {
                    @Override
                    public void run() {
                        //  dialog is dismissed so stop animating.
                        hsv.stopAnimate();
                    }
                }
        );
    }

    //  optional listener for document events.
    private DocumentListener mDocumentListener = null;

    public void gotoInternalLocation(int page, RectF box)
    {
        //  Add a history entry for the spot we're leaving.
        DocView dv = getDocView();
        dv.addHistory(dv.getScrollX(), dv.getScrollY(), dv.getScale(), true);

        //  add history for where we're going
        int dy = dv.scrollBoxToTopAmount(page, box);
        dv.addHistory(dv.getScrollX(), dv.getScrollY()-dy, dv.getScale(), false);

        //  scroll to the new page and box
        dv.scrollBoxToTop(page, box);
    }

    //  for no-UI API, see NUIDocViewPDF
    public boolean isDrawModeOn()
    {
        return false;
    }

    //  for no-UI API, see NUIDocViewPDF
    public boolean isNoteModeOn()
    {
        return false;
    }

    //  for no-UI API
    public boolean canDeleteSelection()
    {
        //  determine whether we've got an area or a caret as the selection.
        boolean areaIsSelected = false;
        boolean cursorIsSelected = false;
        ArDkSelectionLimits limits = getDocView().getSelectionLimits();
        if (limits != null) {
            boolean active = limits.getIsActive();
            areaIsSelected = active && !limits.getIsCaret();
            cursorIsSelected = active && limits.getIsCaret();
        }

        return areaIsSelected && mSession.getDoc().getSelectionCanBeDeleted();
    }

    //  for no-UI API, see NUIDocViewPDF
    public void setDrawModeOn()
    {
    }

    //  for no-UI API, see NUIDocViewPDF
    public void setDrawModeOff()
    {
    }

    //  for no-UI API
    public void deleteSelection()
    {
        getDoc().clearSelection();
    }

    //  for no-UI API
    public void deleteSelectedText()
    {
        getDoc().selectionDelete();
    }

    //  for no-UI API
    public void setSelectionText(String text) {
        ((SODoc)getDoc()).setSelectionText(text);
    }

    //  for no-UI API
    public String getSelectedText()
    {
        return ((SODoc)getDoc()).getSelectionAsText();
    }

    //  for no-UI API, see NUIDocViewPDF
    public void highlightSelection()
    {
    }

    //  for no-UI API, see NUIDocViewPDF
    public void setLineColor(int color)
    {
    }

    //  for no-UI API, see NUIDocViewPDF
    public void setLineThickness(float size)
    {
    }

    //  for no-UI API
    protected Runnable mExitFullScreenRunnable = null;
    public void enterFullScreen(Runnable onExit)
    {
        mExitFullScreenRunnable = onExit;
        Utilities.hideKeyboard(this.getContext());
        onFullScreen(null);
    }

    //  for no-UI API
    public void setDocumentListener(DocumentListener listener)
    {
        mDocumentListener = listener;
    }

    //  for no-UI API
    public void providePassword(String password)
    {
        if (mSession != null)
        {
            ArDkDoc doc = mSession.getDoc();
            if (doc != null)
            {
                doc.providePassword(password);
            }
        }
    }

    //  for no-UI API
    public boolean isAlterableTextSelection()
    {
        ArDkDoc doc = getDoc();
        if (doc!=null)
            return doc.getSelectionIsAlterableTextSelection();
        return false;
    }

    //  for no-UI API
    public void searchForward(String s)
    {
        searchCommon(s, false);
    }

    //  for no-UI API
    public void searchBackward(String s)
    {
        searchCommon(s, true);
    }

    //  for no-UI API
    private String lastSearchString = "";
    private void searchCommon(String s, boolean backwards)
    {
        ArDkDoc doc = getDoc();
        if (doc != null)
        {
            if (doc.isSearchRunning())
                return;
            doc.setSearchBackwards(backwards);
            Utilities.hideKeyboard(getContext());
            if (!s.equals(lastSearchString)) {
                //  whenever the search text changes, reset the search start
                setSearchStart();
                doc.setSearchString(s);
            }
            doc.search();
        }
    }

    //  for no-UI API
    public void save()
    {
        doSave();
    }

    //  for no-UI API
    public void saveAs()
    {
        doSaveAs(false, null);
    }

    //  for no-UI API
    public boolean isDocumentModified()
    {
        return documentHasBeenModified();
    }

    //  for no-UI API
    public void print()
    {
        //  lifted from retail DataLeakHandlers

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            /*
             * Print the current document using the Android PrintService.
             *
             * The current document will be exported to a PDF file, in a 'print'
             * sub-folder of the temporary storage root and passed to the
             * print service from there.
             *
             * The temporary file will be saved using the SecureFS API's for
             * secure builds.
             */
            new PrintHelperPdf().print(getContext(), getDoc());
        }
        else
        {
            Utilities.showMessage((Activity)getContext(),
                    "Not Supported",
                    "Printing is not supported for this version of Android.");
        }
    }

    //  for no-UI API
    public void share()
    {
        onShareButton(null);
    }

    //  for no-UI API
    public void openIn()
    {
        onOpenInButton(null);
    }

    //  for no-UI API, see NUIDocViewPdf
    public void setNoteModeOn()
    {
    }

    //  for no-UI API, see NUIDocViewPdf
    public void setNoteModeOff()
    {
    }

    //  for no-UI API
    public void author()
    {
        onAuthorButton(null);
    }

    //  for no-UI API
    public void firstPage()
    {
        gotoPage(0);
    }

    //  for no-UI API
    public void lastPage()
    {
        gotoPage(getPageCount()-1);
    }

    //  for no-UI API
    private void gotoPage(int pageNumber)
    {
        mDocView.addPageHistory(pageNumber);
        mDocView.scrollToPage(pageNumber, true);
        if (usePagesView())
            mDocPageListView.scrollToPage(pageNumber, true);
        setCurrentPage(pageNumber);
    }

    public int getPageNumber()
    {
        return mCurrentPageNum+1;
    }

    public void historyNext()
    {
    }

    public void historyPrevious()
    {
    }

    public boolean hasHistory() {return false;}

    //  for no-UI API
    public boolean hasPreviousHistory()
    {
        History history = getDocView().getHistory();
        if (history!=null)
        {
            return history.canPrevious();
        }
        return false;
    }

    //  for no-UI API
    public boolean hasNextHistory()
    {
        History history = getDocView().getHistory();
        if (history!=null)
        {
            return history.canNext();
        }
        return false;
    }

    //  for no-UI API
    public void tableOfContents()
    {
    }

    //  for no-UI API
    public boolean isTOCEnabled()
    {
        return false;
    }

    //  optional Runnable for notifying no-UI apps to update their UI
    private Runnable mOnUpdateUIRunnable = null;
    public void setOnUpdateUI(Runnable runnable)
    {
        mOnUpdateUIRunnable = runnable;
    }

    protected void doUpdateCustomUI()
    {
        if (mFinished)
            return;

        if (mOnUpdateUIRunnable!=null)
            mOnUpdateUIRunnable.run();
    }


    //  for no-UI API
    public void redactMarkText()
    {
    }

    //  for no-UI API
    public void redactMarkArea()
    {
    }

    //  for no-UI API
    public boolean redactIsMarkingArea()
    {
        return false;
    }

    //  for no-UI API
    public void redactRemove()
    {
    }

    //  for no-UI API
    public void redactApply()
    {
    }

    //  for no-UI API
    public Boolean canMarkRedaction()
    {
        return false;
    }

    //  for no-UI API
    public Boolean canRemoveRedaction()
    {
        return false;
    }

    //  for no-UI API
    public Boolean canApplyRedactions()
    {
        return false;
    }

    //  for the no-UI
    public void saveTo(final String path, final SODocSaveListener listener)
    {
        preSave();

        preSaveQuestion(new Runnable() {
            @Override
            public void run() {
                // yes

                getDoc().saveTo(path, new SODocSaveListener() {
                    @Override
                    public void onComplete(int result, int err) {
                        if (result == SODocSave_Succeeded)
                        {
                            setFooterText(path);
                            mFileState.setUserPath(path);

                            /*
                             * Make sure that when we close, we don't
                             * think there are outstanding changes.
                             */
                            mFileState.setHasChanges(false);
                            onSelectionChanged();

                            //  reload the file
                            reloadFile();

                            /*
                             * If the 'saveAs' was successful the template
                             * flag will be reset and 'save' allowed.
                             */
                            mIsTemplate = mFileState.isTemplate();
                        }
                        else if (result == SODocSave_Error)
                        {
                            // Save failed. Revoke the user path
                            mFileState.setUserPath(null);
                        }
                        else if (result == SODocSave_Cancelled)
                        {
                            //  cancelled
                        }

                        //  now tell our caller
                        listener.onComplete(result, err);
                    }
                });
            }
        }, new Runnable() {
            @Override
            public void run() {
                // no
            }
        });
    }

    //  for no-UI API
    public interface ChangePageListener {
        void onPage(int pageNumber);
    }
    private ChangePageListener mChangePageListener = null;
    public void setPageChangeListener(ChangePageListener listener) {mChangePageListener=listener;}

    //  this may be called by DocView to update the UI buttons
    public void updateUI()
    {
        updateUIAppearance();
    }

    //  for no-UI API
    public boolean canUndo()
    {
        if (! mDocCfgOptions.isEditingEnabled())
            return false;
        if (mSession.getDoc() instanceof SODoc)
        {
            int currentEdit = ((SODoc)mSession.getDoc()).getCurrentEdit();
            if (currentEdit > 0)
                return true;
        }

        return false;
    }

    //  for no-UI API
    public boolean canRedo()
    {
        if (! mDocCfgOptions.isEditingEnabled())
            return false;
        if (mSession.getDoc() instanceof SODoc)
        {
            int currentEdit = ((SODoc)mSession.getDoc()).getCurrentEdit();
            int numEdits = ((SODoc)mSession.getDoc()).getNumEdits();
            if (currentEdit < numEdits)
                return true;
        }

        return false;
    }

    //  for no-UI API
    public boolean select(Point p)
    {
        if (mDocView != null)
            return mDocView.select(p, null);
        return false;
    }

    //  for no-UI API
    public boolean select (Point topLeft, Point bottomRight)
    {
        if (mDocView != null)
            return mDocView.select(topLeft, bottomRight);
        return false;
    }

    //  for no-UI API
    public int getScrollPositionX()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionX();
        return -1;
    }

    //  for no-UI API
    public int getScrollPositionY()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionY();
        return -1;
    }

    //  for no-UI API
    public float getScaleFactor()
    {
        if (mDocView != null)
            return mDocView.getScaleFactor();
        return -1;
    }

    //  for no-UI API
    public void setScaleAndScroll (float newScale, int newX, int newY)
    {
        if (mDocView != null)
            mDocView.setScaleAndScroll(newScale, newX, newY);
    }

    //  keep track of changes in these values with each call to reportViewChanges
    private float scalePrev = 0;
    private int scrollXPrev = -1;
    private int scrollYPrev = -1;
    private Rect selectionPrev = null;


    //  the following function is called when scrolling, scaling, or selection changes.
    //  it does some filtering to weed out repetitions, and then calls the no-UI
    //  DocumentListener, if there is one.

    public void reportViewChanges()
    {
        //  don't bother if there is no listener
        if (mDocumentListener==null)
            return;

        if (mDocView == null)
            return;

        //  get current scale, scroll values
        float scale = mDocView.getScale();
        int scrollX = mDocView.getScrollX();
        int scrollY = mDocView.getScrollY();

        //  get the selection rect, if any
        Rect selectionRect = null;
        ArDkSelectionLimits limits = mDocView.getSelectionLimits();
        DocPageView selStartPage = mDocView.getSelectionStartPage();
        if (limits != null && selStartPage != null)
        {
            RectF box = limits.getBox();
            if (box != null) {
                selectionRect = selStartPage.pageToView(box);
                selectionRect.offset((int) selStartPage.getX(), (int) selStartPage.getY());
            }
        }

        //  see if anything's changed since the last call
        boolean changed = false;
        if (scale!=scalePrev)
            changed = true;
        if (scrollX!=scrollXPrev)
            changed = true;
        if (scrollY!=scrollYPrev)
            changed = true;
        if (!Utilities.compareRects(selectionPrev, selectionRect))
            changed = true;
        if (!changed)
            return;  //  nothing changed

        //  save new values for next time
        scalePrev = scale;
        scrollXPrev = scrollX;
        scrollYPrev = scrollY;
        selectionPrev = selectionRect;

        //  call the listener
        mDocumentListener.onViewChanged(scale, scrollX, scrollY, selectionRect);
    }
}
