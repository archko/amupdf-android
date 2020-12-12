package com.artifex.solib;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.artifex.mupdf.fitz.Document;

public class MuPDFLib extends ArDkLib
{
    private static MuPDFLib singleton = null;

    private MuPDFLib()
    {
    }

    public static MuPDFLib getLib(Activity activity)
    {
        if (singleton == null) {
            Log.w(mDebugTag,"creating new SOLib");
            singleton = new MuPDFLib();

            /*
             * All apps are required to provide an SOClipboardHandler
             * implementation.
             */
            if (mClipboardHandler == null)
            {
                Log.d(mDebugTag,
                      "No implementation of the SOClipboardHandler " +
                      "interface found");

                throw new RuntimeException();
            }
        }

        return (MuPDFLib)singleton;
    }

    @Override
    public ArDkDoc openDocument(final String path, final SODocLoadListener listener, final Context context, ConfigOptions cfg)
    {
        //  create the MuPDFDoc and start its worker
        final MuPDFDoc mupdfDoc = new MuPDFDoc(Looper.myLooper(), listener, context, cfg);
        mupdfDoc.startWorker();

        //  do the rest in the background
        mupdfDoc.getWorker().add(new Worker.Task()
        {
            private boolean docOpened = false;
            private boolean needsPassword = false;
            public void work()
            {
                //  open the doc
                Document doc = MuPDFDoc.openFile(path);
                if (doc==null) {
                    return;
                }
                docOpened = true;
                mupdfDoc.setDocument(doc);
                mupdfDoc.setOpenedPath(path);

                //  do we need a password?
                if (doc.needsPassword()) {
                    needsPassword = true;
                    return;
                }

                //  things to do after the password is validated
                mupdfDoc.afterValidation();
            }

            public void run()
            {
                //  did the open fail?
                if (!docOpened)
                {
                    if (listener!=null) {
                        //  don't know what errorNum to use, so 0.
                        listener.onError(ArDkLib.SmartOfficeDocErrorType_UnableToLoadDocument, 0);
                    }
                    return;
                }

                //  did we need a password?
                if (needsPassword) {
                    if (listener!=null)
                        listener.onError(ArDkLib.SmartOfficeDocErrorType_PasswordRequest, 0);
                    return;
                }

                //  start loading pages
                mupdfDoc.loadNextPage();
            }
        });

        return mupdfDoc;

    }

    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public ArDkBitmap createBitmap(int w, int h)
    {
        return new MuPDFBitmap(w, h);
    }

    public boolean isTrackChangesEnabled()
    {
        return false;
    }

    @Override
    public void reclaimMemory()
    {
        //  ask mupdf to empty the store.
        com.artifex.mupdf.fitz.Context.emptyStore();

        //  ask system to to garbage collection
        Runtime.getRuntime().gc();
    }
}
