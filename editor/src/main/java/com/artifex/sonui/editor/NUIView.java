package com.artifex.sonui.editor;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.artifex.solib.ConfigOptions;
import com.artifex.solib.FileUtils;

public class NUIView extends FrameLayout
{
    protected NUIDocView mDocView;
    private ConfigOptions      mDocCfgOpts; // document configuration
    private SODataLeakHandlers mLeakHandlers; // document dataleak handler

    public NUIView(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public void setDocConfigOptions(ConfigOptions cfg)
    {
        mDocCfgOpts = cfg;
    }

    public void setDocDataLeakHandler(SODataLeakHandlers handler)
    {
        mLeakHandlers = handler;
    }

    private void initialize(Context context)
    {
    }

    protected void makeNUIView(String path)
    {
        //  get the file's extension
        String ext = FileUtils.getExtension(path);

        //  these are handled by mupdf
        switch (ext) {
            case "svg":
            case "epub":
            case "xps":
            case "fb2":
            case "xhtml":
            case "cbz":
            case "jpg":
            case "png":
            case "gif":
            case "bmp":
                mDocView = new NUIDocViewMuPdf(getContext());
        }

        if (mDocView == null && ext.equals("pdf"))
            mDocView = new NUIDocViewPdf(getContext());

        //  ask our factory
        if (mDocView == null)
            mDocView = NUIViewFactory.makeNUIView(path, getContext());

        //  fall back to "other"
        if (mDocView == null)
            mDocView = new NUIDocViewOther(getContext());

        // tell NUIDocView which configOptions/dataLeakHandler to use.
        mDocView.setDocSpecifics(mDocCfgOpts, mLeakHandlers);
    }

    protected void makeNUIView(Uri uri, String mimeType)
    {
        String ext = FileUtils.getFileTypeExtension(getContext(), uri, mimeType);
        String path = "SomeFileName" + "." + ext;
        makeNUIView(path);
    }

    //  start the view given a session
    public void start (SODocSession session, ViewingState viewingState, String foreignData)
    {
        makeNUIView(session.getUserPath());
        addView(mDocView);
        mDocView.start(session, viewingState, foreignData, mDoneListener);
    }

    //  start the view given a saved auto-open state.
    public void start (SOFileState fileState, ViewingState viewingState)
    {
        makeNUIView(fileState.getOpenedPath());
        addView(mDocView);
        mDocView.start(fileState, viewingState, mDoneListener);
    }

    //  start the view given a path.
    public void start(Uri uri, boolean template, ViewingState viewingState, String customDocData, String mimeType)
    {
        makeNUIView(uri, mimeType);
        addView(mDocView);
        mDocView.start(uri, template, viewingState, customDocData, mDoneListener, true);
    }

    public void onDestroy()
    {
        mDocView.onDestroy();
    }

    public void onPause(final Runnable whenDone)
    {
        if (mDocView != null) {
            mDocView.onPause(new Runnable() {
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

        Utilities.hideKeyboard(getContext());
    }

    public void releaseBitmaps()
    {
        if (mDocView != null)
            mDocView.releaseBitmaps();
    }

    public void onResume()
    {
        if (mDocView != null)
            mDocView.onResume();
    }

    public void onBackPressed()
    {
        if (mDocView != null)
            mDocView.onBackPressed();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (mDocView != null)
            mDocView.onActivityResult(requestCode, resultCode, data);
    }

    protected OnDoneListener mDoneListener = null;
    public void setOnDoneListener(OnDoneListener l) {mDoneListener = l;}
    public interface OnDoneListener
    {
        void done();
    }

    public boolean doKeyDown(int keyCode, KeyEvent event)
    {
        if (mDocView != null)
            return mDocView.doKeyDown(keyCode, event);

        return false;
    }

    public void onConfigurationChange(Configuration newConfig)
    {
        //  config was changed, probably orientation.
        //  call the doc view
        mDocView.onConfigurationChange(newConfig);
    }

    public boolean isDocModified()
    {
        if (mDocView!=null)
            return mDocView.documentHasBeenModified();
        return false;
    }

    public void endDocSession(boolean silent)
    {
        if (mDocView!=null)
        {
            mDocView.endDocSession(silent);
        }
    }

    public void setConfigurableButtons()
    {
        if (mDocView!=null)
        {
            mDocView.setConfigurableButtons();
        }
    }

    public boolean hasSearch()
    {
        if (mDocView!=null)
            return mDocView.hasSearch();
        return false;
    }

    public boolean hasPages()
    {
        if (mDocView!=null)
            return mDocView.usePagesView();
        return false;
    }

    public boolean hasHistory()
    {
        if (mDocView!=null)
            return mDocView.hasHistory();
        return false;
    }

    public boolean hasReflow()
    {
        if (mDocView!=null)
            return mDocView.hasReflow();
        return false;
    }
}

