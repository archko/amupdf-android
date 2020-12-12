package com.artifex.sonui.editor;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.ArDkUtils;

public class NUIActivity extends BaseActivity
{
    protected NUIView        mNUIView;
    private   ConfigOptions  mDocConfigOpts;
    private   SODataLeakHandlers mDataLeakHandlers;

    public ConfigOptions getDocConfigOptions() { return mDocConfigOpts; }

    //  the session in use by this activity
    private SODocSession session = null;

    //  ViewingState can be accessed here.
    private ViewingState mViewingState = null;
    public ViewingState getViewingState() {return mViewingState;}
    public void setViewingState(ViewingState state){mViewingState=state;}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //  remember our configuration
        mLastConfiguration = getResources().getConfiguration();

        initialise();
    }

    // override this method in secure environments to handle any restrictions on
    // external document access such as generic VIEW intents
    protected void initialise()
    {
        // our default implementation just calls initialiseInternal()
        initialiseInternal();
    }

    // duplicate the current application configuration
    private ConfigOptions duplicateAppConfigurations()
    {
        ConfigOptions cfg = null;
        try
        {
            cfg = ArDkLib.getAppConfigOptions().clone();
        }
        catch(CloneNotSupportedException ex)
        {
            ex.printStackTrace();
        }

        return cfg;
    }

    private void setupDocumentSpecifics(Intent intent)
    {
        // Set the configuration for the new document.
        if (intent.hasExtra("CONFIG_OPTS"))
        {
            // configuration was delivered via intent.
            mDocConfigOpts = (ConfigOptions) intent.getExtras().getParcelable("CONFIG_OPTS");
        }
        else
        {
            // create one from the application configuration.
            mDocConfigOpts = duplicateAppConfigurations();
        }

        try
        {
            // prepare data leak hanlder for the document
            // create an instance of the same object type as the application data leak handler.
            SODataLeakHandlers appDataLeakHandler = Utilities.getDataLeakHandlers();
            mDataLeakHandlers = appDataLeakHandler.getClass().newInstance();
        }
        catch (InstantiationException ex) {
            ex.printStackTrace();
            mDataLeakHandlers = null;
        }
        catch (IllegalAccessException ex) {
            ex.printStackTrace();
            mDataLeakHandlers = null;
        }
    }

    private void initialiseInternal()
    {
        Intent intent = getIntent();

        //  code was removed here that was designed to fix bug
        //  698321 - "crash after app idle overnight"
        //  now that NUIActivity is creating the session (as opposed to
        //  ExplorerActivity), that code is no longer necessary.

        //  start the activity
        start(intent, false);
    }

    @Override
    public void onNewIntent (final Intent intent)
    {
        //  this activity uses android:launchMode="singleTask"
        //  if no activity exists when launched, we'll go through onCreate().
        //  if one exists, we'll instead land here, with new intent data.
        //
        //  The doc that's currently being edited may have changes.
        //  if that's the case we give the user a choice of staying with the
        //  existing doc, or losing those changes and going with the new doc.

        if (isDocModified())
        {
            //  the current doc is modified, so ask what to do.
            Utilities.yesNoMessage(this, getString(R.string.sodk_editor_new_intent_title),
                    getString(R.string.sodk_editor_new_intent_body),
                    getString(R.string.sodk_editor_new_intent_yes_button), getString(R.string.sodk_editor_new_intent_no_button),
                    new Runnable() {
                        @Override
                        public void run() {
                            // User responded to move to the new document.

                            /*
                             * End the current document session before moving
                             * to the next.
                             */
                            if (mNUIView != null)
                                mNUIView.endDocSession(true);

                            // Load the new document.
                            start(intent, true);
                        }
                    },

                    new Runnable() {
                        @Override
                        public void run() {
                            // User responded to stay with the current doc.

                            // Cancel any pending edit session
                            SODocSession.SODocSessionLoadListenerCustom
                                sessionLoadListener =
                                    Utilities.getSessionLoadListener();

                            if (sessionLoadListener != null)
                            {
                                sessionLoadListener.onSessionReject();
                            }

                            // We're done with this listener.
                            Utilities.setSessionLoadListener(null);
                        }
                    });

        }
        else
        {
            // End the current document session before moving to the next.
            if (mNUIView != null)
                mNUIView.endDocSession(true);

            /*
             * The current doc is not modified, so we'll go with the new doc
             * without asking.
             */
            start(intent, true);
        }
    }


    protected void checkIAP()
    {
        // Override in derived classes in flavours that use IAP.
    }

    private void start(Intent intent, boolean newIntent)
    {
        final Bundle extras = intent.getExtras();
        boolean createSession = false;

        // create document specifics
        setupDocumentSpecifics(intent);

        if (mDocConfigOpts.isDocExpired())
        {
            mNUIView = null;

            // Inform the user this operation is prohibited, and exit.
            Utilities.showMessageAndFinish(
                this,
                getString(R.string.sodk_editor_error),
                getString(R.string.sodk_editor_has_no_permission_to_open));
            return;
        }

        if (extras != null)
            createSession = extras.getBoolean("CREATE_SESSION", false);

        //  create the session
        if (createSession)
        {
            final boolean createThumbnail = extras.getBoolean("CREATE_THUMBNAIL", false);
            final String url = extras.getString("FILE_URL", "");

            ArDkLib lib = ArDkUtils.getLibraryForPath(this, url);
            session = new SODocSession(this, lib);

            final NUIActivity activity = this;

            session.addLoadListener(new SODocSession.SODocSessionLoadListener()
            {
                private boolean thumbnailCreated = false;

                @Override
                public void onPageLoad(int pageNum)
                {
                    //  make a thumbnail for the Recent file list, if the first page
                    //  is available, AND if directed to by the extra CREATE_THUMBNAIL.
                    if (pageNum>=1 && !thumbnailCreated && createThumbnail)
                    {
                        thumbnailCreated = true;
                        SOFileState state = SOFileDatabase.getDatabase().stateForPath(session.getUserPath(), false);
                        if (state != null)
                            session.createThumbnail(state);
                    }
                }

                @Override
                public void onDocComplete() {
                }

                @Override
                public void onError(int error, int errorNum) {
                    String title = activity.getString(R.string.sodk_editor_unable_to_load_document_title);
                    String body = Utilities.getOpenErrorDescription(activity, error);
                    Utilities.showMessage(activity, title, body);
                }

                @Override
                public void onCancel() {
                    session.abort();
                }

                @Override
                public void onSelectionChanged(int startPage, int endPage) {
                }

                @Override
                public void onLayoutCompleted() {
                }
            });

            //  open the session
            session.open(url, getDocConfigOptions());  //  this starts the actual loading
        }

        //  set up UI
        setContentView(R.layout.sodk_editor_doc_view_activity);

        //  find the SO component
        mNUIView = (NUIView) findViewById(R.id.doc_view);

        // pass document specific configuration.
        mNUIView.setDocConfigOptions(mDocConfigOpts);
        mNUIView.setDocDataLeakHandler(mDataLeakHandlers);

        //  set a listener for when it's done
        mNUIView.setOnDoneListener(new NUIView.OnDoneListener()
        {
            @Override
            public void done()
            {
                NUIActivity.super.finish();
            }
        });

        //  get information about the document
        //  in this example we're getting it from the Explorer view
        int         startPage      = -1;
        SOFileState state          = null;
        String      foreignData    = null;
        String      customDocData  = null;

        /*
         * We generally arrive here when we're started using Open With.
         * In these cases, we'll treat it as a template so that
         * 'Save' is unavailable.
         */
        boolean isTemplate = true;

        if (extras != null)
        {
            startPage = extras.getInt("START_PAGE", -1);
            state = SOFileState.fromString(extras.getString("STATE"), SOFileDatabase.getDatabase());

            foreignData = extras.getString("FOREIGN_DATA", null);

            /*
             * The intent originator may override the default template setting.
             * eg. A file open from an SDK customer.
             */
            isTemplate    = extras.getBoolean("IS_TEMPLATE", true);
            customDocData = extras.getString("CUSTOM_DOC_DATA");

            //  a fix for Bug 700298
            //  When the font is changed while we're in the background, it causes
            //  OnCreate to be called again, and we don't honor the saved document
            //  state that was created when we were backgrounded.
            //  This fix accounts for that by checking for the state.

            //  This fix does NOT apply to the onNewIntent scenario.
            //  In that case we are being given the doc to open in the new intent.

            if (state==null && !newIntent) {
                state = SOFileState.getAutoOpen(this);
                if (state!=null)
                    createSession = false;  //  insure we open from the new state
            }
        }

        if (customDocData == null)
        {
            // Any registered listener is not valid for this session,
            Utilities.setSessionLoadListener(null);
        }

        //  create ViewingState if necessary
        if (mViewingState == null) {
            if (startPage != -1) {
                //  we were given a 1-based starting page via the START_PAGE extra
                //  this would be coming from SDK users
                mViewingState = new ViewingState(startPage - 1);
            } else if (state != null) {
                //  auto-open state
                mViewingState = new ViewingState(state);
            } else {
                if (createSession && session != null) {
                    //  NUI's Explorer
                    mViewingState = new ViewingState(SOFileDatabase.getDatabase().stateForPath(session.getUserPath(), isTemplate));
                } else {
                    //  open with
                    mViewingState = new ViewingState(0);
                }
            }
        }

        //  start
        if (createSession)
        {
            //  this is for NUI's Explorer
            mNUIView.start(session, mViewingState, foreignData);
        }
        else if (state != null)
        {
            //  this is for opening saved auto-open states
            mNUIView.start(state, mViewingState);
        }
        else
        {
            Uri uri = intent.getData();
            String mimeType = intent.getType();
            mNUIView.start(uri, isTemplate, mViewingState, customDocData, mimeType);
        }

        // Check the IAP status and update the UI accordingly.
        checkIAP();
    }

    @Override
    public void finish()
    {
        if(mNUIView == null)
        {
            // if we haven't initialised (generally due to lack of permissions)
            // then simply call our super's finish() and go.
            super.finish();
        }
        else
        {
            //  dismiss any currently-showing alert.
            Utilities.dismissCurrentAlert();

            onBackPressed();
        }
    }

    private void doPause(final Runnable whenDone)
    {
        if (mNUIView != null)
        {
            mNUIView.onPause(new Runnable() {
                @Override
                public void run() {

                    //  release the rendering bitmaps when we go to the background to conserve
                    //  memory. They are recreated when we come forward, because the doc coming
                    //  forward could be a different format (mupdf) or a different window size.
                    mNUIView.releaseBitmaps();

                    if (whenDone!=null)
                        whenDone.run();
                }
            });
        }
        else
        {
            if (whenDone!=null)
                whenDone.run();
        }

        //  a fix for 700683 (PRINT menu does not respond)
        //  but may re-break 697802, but on Chromebook only
        if (Utilities.isChromebook(this))
            PrintHelperPdf.setPrinting(false);
    }

    @Override
    public void onPause()
    {
        doPause(null);
        super.onPause();
    }

    protected void doResumeActions()
    {
        if (mNUIView != null)
            mNUIView.onResume();

        //  check for uiMode changes
        checkUIMode(getResources().getConfiguration());
    }

    @Override
    protected void onResume()
    {
        //  when we resume, we assume we've no longer got a child activity.
        mChildIntent = null;

        super.onResume();

        doResumeActions();
    }

    //  if we create a new activity using any of the three methods listed below,
    //  it will be recorded here.
    private Intent mChildIntent = null;
    public Intent childIntent() {return mChildIntent;}

    @Override
    public void startActivityForResult(Intent intent, int requestCode)
    {
        mChildIntent = intent;
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options)
    {
        mChildIntent = intent;
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startActivity(Intent intent)
    {
        mChildIntent = intent;
        super.startActivity(intent, null);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mNUIView != null)
            mNUIView.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if (mNUIView != null)
            mNUIView.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (mNUIView != null)
            mNUIView.onActivityResult(requestCode, resultCode, data);
    }

    // Time, in ms, of the last key down event.
    private long mLastKeyTime = 0;

    // Key code of the last key down event.
    private int mLastKeyCode = -1;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (mNUIView==null)
            return true;

        long eventTime = event.getEventTime();

        // Impose a minimum 100ms key repeat rate.
        if ((mLastKeyCode != keyCode) || (eventTime - mLastKeyTime > 100))
        {
            mLastKeyTime = eventTime;
            mLastKeyCode = keyCode;

            return mNUIView.doKeyDown(keyCode, event);
        }

        return true;
    }

    private Configuration mLastConfiguration;
    private void checkUIMode(Configuration newConfig)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            if (newConfig.uiMode != mLastConfiguration.uiMode)
            {
                //  uiMode has changed.

                if (getCurrentActivity() != null) {
                    //  Call doPause() so that our state gets saved.
                    //  after saving is done, the Runnable will be executed.
                    //  if there's no current activity, this will already have been done
                    //  so don't do it twice
                    doPause(new Runnable() {
                        @Override
                        public void run() {
                            NUIActivity.super.finish();
                            startActivity(getIntent());
                        }
                    });
                }
                else {
                    //  Here, it's likely that onPause() has already been called
                    NUIActivity.super.finish();
                    startActivity(getIntent());
                }
            }
        }

        //  remember for next time
        mLastConfiguration = getResources().getConfiguration();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        //  config was changed, probably orientation.
        //  call the super
        super.onConfigurationChanged(newConfig);

        //  check for uiMode changes
        checkUIMode(newConfig);

        //  ... then our main view
        if (mNUIView!=null)
            mNUIView.onConfigurationChange(newConfig);
    }

    public boolean isDocModified()
    {
        if (mNUIView!=null)
            return mNUIView.isDocModified();
        return false;
    }

    // update document cfgs from app cfgs that might be changed.
    private void updateRestrictionCfgs()
    {
        if (mDocConfigOpts == null)
            return;
        ConfigOptions appCfgOpts = ArDkLib.getAppConfigOptions();
        ConfigOptions cfg = mDocConfigOpts;

        cfg.setExtClipboardOutEnabled(appCfgOpts.isExtClipboardOutEnabled());
        cfg.setOpenInEnabled(appCfgOpts.isOpenInEnabled());
        cfg.setOpenPdfInEnabled(appCfgOpts.isOpenPdfInEnabled());


        cfg.setExtClipboardInEnabled(appCfgOpts.isExtClipboardInEnabled());
        cfg.setImageInsertEnabled(appCfgOpts.isImageInsertEnabled());
        cfg.setPhotoInsertEnabled(appCfgOpts.isPhotoInsertEnabled());

        cfg.setPrintingEnabled(appCfgOpts.isPrintingEnabled());

        cfg.setSecurePrintingEnabled(appCfgOpts.isSecurePrintingEnabled());

        cfg.setNonRepudiationCertOnlyFilterEnabled(appCfgOpts.isNonRepudiationCertFilterEnabled());

        cfg.setAppAuthEnabled(appCfgOpts.isAppAuthEnabled());

        cfg.setAppAuthTimeout(appCfgOpts.getAppAuthTimeout());
    }

    protected void setConfigurableButtons()
    {
        // restrictions are updated to the application configuration,
        // so it is needed to apply them to the document configuration
        // before update ui.
        updateRestrictionCfgs();

        if (mNUIView!=null)
            mNUIView.setConfigurableButtons();
    }
}
