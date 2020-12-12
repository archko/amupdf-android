package com.artifex.sonui.editor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import com.artifex.solib.ConfigOptions;
import com.artifex.solib.ArDkBitmap;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODocLoadListener;
import com.artifex.solib.SOOutputStream;
import com.artifex.solib.ArDkPage;
import com.artifex.solib.SOPageListener;
import com.artifex.solib.ArDkRender;
import com.artifex.solib.SORenderListener;
import com.artifex.solib.ArDkUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;

public class SODocSession {

    private ArDkDoc mDoc;
    public ArDkDoc getDoc() {return mDoc;}

    private String mUserPath = null;
    public String getUserPath() {return mUserPath;}

    private boolean mOpen = false;
    private boolean mCancelled = false;
    private boolean mLoadError = false;

    private final ArDkLib mLibrary;

    private final Activity mActivity;

    public SODocSession(Activity activity, ArDkLib library)
    {
        mActivity = activity;
        mLibrary = library;
    }

    private SOFileState mFileState = null;
    public void setFileState(SOFileState state) {mFileState=state;}
    public SOFileState getFileState() {return mFileState;}

    public interface SODocSessionLoadListener
    {
        void onPageLoad (int pageNum);
        void onDocComplete();
        void onError(int error, int errorNum);
        void onCancel();
        void onSelectionChanged(int startPage, int endPage);
        void onLayoutCompleted();
    }

    public interface SODocSessionLoadListenerCustom extends
        SODocSessionLoadListener
    {
        void onSessionReject();
        void onSessionComplete(boolean silent);
    }

    private int mPageCount = 0;
    private boolean mCompleted = false;

    //  an array of load listeners, managed by
    //  addLoadListener() and removeLoadListener()
    private CopyOnWriteArrayList<SODocSessionLoadListener> mListeners = new CopyOnWriteArrayList<>();

    //  add a new load listener
    public void addLoadListener(final SODocSessionLoadListener listener) {

        if (listener!=null)
        {
            //  add it to the list
            mListeners.add(listener);

            if (mPageCount>0)
                listener.onPageLoad(mPageCount);
            if (mCompleted)
                listener.onDocComplete();
        }

        mListenerCustom = Utilities.getSessionLoadListener();

        if (mListenerCustom!=null)
        {
            if (mPageCount>0)
                mListenerCustom.onPageLoad(mPageCount);
            if (mCompleted)
                mListenerCustom.onDocComplete();
        }
    }

    //  remove a load listener
    public void removeLoadListener(final SODocSessionLoadListener listener) {
        mListeners.remove(listener);
    }

    private void clearListeners()
    {
        mListeners.clear();
    }

    private SODocSessionLoadListenerCustom mListenerCustom=null;

    //  an optional Runnable to use when a document password is required.
    private Runnable mPasswordHandler = null;
    public void setPasswordHandler(Runnable r) {mPasswordHandler=r;}

    public void open(String path, ConfigOptions cfg)
    {
        mUserPath = path;

        mPageCount = 0;
        mCompleted = false;

        mOpen = true;
        final SODocSession session = this;
        mDoc = mLibrary.openDocument(path, new SODocLoadListener() {
            @Override
            public void onPageLoad(int pageNum) {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen || mCancelled)
                    return;

                mPageCount = Math.max(pageNum, mPageCount);

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onPageLoad(pageNum);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onPageLoad(pageNum);
                }
            }

            @Override
            public void onDocComplete() {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen || mCancelled || mLoadError)
                    return;

                mCompleted = true;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onDocComplete();
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onDocComplete();
                }
            }

            @Override
            public void onError(final int error, final int errorNum)
            {
                if (error == ArDkLib.SmartOfficeDocErrorType_PasswordRequest)
                {
                    //  if an optional password runnable has been specified, use it
                    //  instead of handling it below.
                    if (mPasswordHandler!=null)
                    {
                        mPasswordHandler.run();
                        return;
                    }

                    Activity currentActivity = BaseActivity.getCurrentActivity();
                    Utilities.passwordDialog(currentActivity, new Utilities.passwordDialogListener()
                    {
                        @Override
                        public void onOK(String password)
                        {
                            //  yes
                            mDoc.providePassword(password);
                        }

                        @Override
                        public void onCancel()
                        {
                            //  cancelled.
                            mDoc.abortLoad();

                            if (mOpen) {
                                for (SODocSessionLoadListener listener : mListeners) {
                                    listener.onCancel();
                                }
                                if (mListenerCustom!=null)
                                    mListenerCustom.onCancel();
                            }

                            mCancelled = true;
                        }
                    });
                    return;
                }

                mLoadError = true;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onError(error, errorNum);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onError(error, errorNum);
                }

                mOpen = false;
            }

            @Override
            public void onSelectionChanged(final int startPage, final int endPage)
            {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen)
                    return;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onSelectionChanged(startPage, endPage);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onSelectionChanged(startPage, endPage);
                }
            }

            public void onLayoutCompleted()
            {
                //  we might arrive here after a core layout is done
                if (!mOpen)
                    return;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onLayoutCompleted();
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onLayoutCompleted();
                }
            }
        }, mActivity, cfg);
    }

    public void abort()
    {
        //  dismiss current alert.  This could be a message, or perhaps a password dialog.
        Utilities.dismissCurrentAlert();

        mOpen = false;
        clearListeners();
        mListenerCustom = null;

        //  stop loading
        if (mDoc != null)
            mDoc.abortLoad();

        //  destroy first-page render and page
        //  this must come before destroying the doc.
        cleanup();
    }

    public void destroy()
    {
        abort();

        //  destroy the doc
        if (mDoc != null) {
            mDoc.destroyDoc();
            mDoc = null;
        }
    }

    public void endSession(boolean silent)
    {
        // End any current document session
        if (mListenerCustom != null)
        {
            mListenerCustom.onSessionComplete(silent);
            mListenerCustom = null;
        }

        abort();
    }

    public boolean isOpen() {return mOpen;}
    public boolean isCancelled() {return mCancelled;}

    //  these objects are used to render a thumbnail of the first page\
    private ArDkRender mRender = null;
    private ArDkPage mPage = null;

    public void createThumbnail(final SOFileState mFileState)
    {
        //  re-use, or create, a path to use for the thumbnail
        String t = mFileState.getThumbnail();
        if (t==null || t.isEmpty())
            t = SOFileDatabase.uniqueThumbFilePath();
        final String path = t;
        mFileState.setThumbnail(path);

        //  delete existing thumbnail file
        mFileState.deleteThumbnailFile();

        //  get the first page
        mPage = getDoc().getPage(0, new SOPageListener() {
            @Override
            public void update(RectF area) {
            }
        });

        //  calculate the thumbnail size
        int w = (int) mActivity.getResources().getDimension(R.dimen.sodk_editor_thumbnail_size);
        PointF zoomFit = mPage.zoomToFitRect(w, 1);
        double zoom = Math.max(zoomFit.x, zoomFit.y);
        Point size = mPage.sizeAtZoom(zoom);

        //  render the first page
        //  not inverted
        final ArDkBitmap sobitmap =
                ArDkUtils.createBitmapForPath(mFileState.getInternalPath(), size.x, size.y);
        PointF origin = new PointF(0,0);
        mRender = mPage.renderAtZoom(zoom, origin, sobitmap, new SORenderListener() {
            @Override
            public void progress(int error) {

                //  save rendered bitmap to a file
                Bitmap bitmap = sobitmap.getBitmap();
                OutputStream stream  = new SOOutputStream(path);
                if (stream!=null)
                {
                    try
                    {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                        stream.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                //  destroy the render and page
                cleanup();
            }
        }, false);
    }

    private void cleanup()
    {
        if (mRender!=null)
            mRender.destroy();
        mRender = null;

        if (mPage !=null)
            mPage.releasePage();
        mPage = null;
    }

    public boolean hasLoadError()
    {
        return mLoadError;
    }
}
