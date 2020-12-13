package cn.archko.pdf.activities;

/**
 * This file contains a, minimal, example implementation of the
 * SODataLeakHandlers interface.
 *
 * This class is mandatory for all NUI Editor based applications.
 *
 * NB. All methods in the interface are executed on the UI thread.
 *     Calls to SODoc methods *must* also be made on this thread.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.File;
import java.lang.ClassNotFoundException;
import java.lang.ExceptionInInitializerError;
import java.lang.LinkageError;
import java.lang.SecurityException;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;

import com.artifex.solib.FileUtils;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODocSaveListener;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.SOSecureFS;
import com.artifex.solib.ConfigOptions;
import com.artifex.sonui.editor.NUIDocView;
import com.artifex.sonui.editor.PrintHelperPdf;
import com.artifex.sonui.editor.SODataLeakHandlers;
import com.artifex.sonui.editor.SOSaveAsComplete;
import com.artifex.sonui.editor.SOCustomSaveComplete;

import cn.archko.mupdf.R;

public class DataLeakHandlers implements SODataLeakHandlers
{
    private final  String           mDebugTag  = "DataLeakHandlers";

    //  a list of files to delete when the document is closed
    private ArrayList<String> mDeleteOnClose = null;
    public void addDeleteOnClose(String path) {mDeleteOnClose.add(path);}

    private Activity       mActivity;         // The calling activity.
    private ConfigOptions  mConfigOptions;    // The configuration settings.
    private SOSecureFS     mSecureFs;         // Instance of SecureFS.
    private String         mSecurePath;       // Root of the secure container
    private String         mSecurePrefix;     // The secure prefix
    private String         mTempFolderPath;   // Temporary folder path.
    private ProgressDialog mProgressDialog;   // Save?export progress dialog


    //////////////////////////////////////////////////////////////////////////
    // Utility Methods.
    //////////////////////////////////////////////////////////////////////////

    /**
     * This method displays a progress dialog <br><be>.
     *
     * @param title      The dialog title.
     * @param message    The dialog message.
     * @param cancelable If set to true The dialog can be dismissed.
     */
    private void displayProgressDialogue(final String  title,
                                         final String  message,
                                         final boolean cancelable)
    {
        mProgressDialog = new ProgressDialog(mActivity,
                                             R.style.Theme_AppCompat_Dialog_Alert);

        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(cancelable);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    /**
     * This method displays an informative dialog <br><be>.
     *
     * @param title   The dialog title.
     * @param message The dialog message.
     */
    private void displayDialogue(final String title, final String message)
    {
        AlertDialog alertDialog = new AlertDialog(
            new ContextThemeWrapper(mActivity,
                                    R.style.Theme_AppCompat_Dialog_Alert))
        {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event)
            {
                /*
                 * Dismiss the dialog on a touch anywhere on the
                 * screen.
                 */
                dismiss();

                return false;
            }
        };

        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.show();
    }

    //////////////////////////////////////////////////////////////////////////
    // Methods Required By Interface.
    //////////////////////////////////////////////////////////////////////////

    /**
     * This method initialises the DataLeakHandlers object <br><br>.
     *
     * @param activity     The current activity.
     * @param cfgOpts      The Config Options to reference.
     *
     * @throws IOException if the temporary folder cannot be created.
     */
    public void initDataLeakHandlers(Activity activity, ConfigOptions cfgOpts)
        throws IOException
    {
        mConfigOptions = cfgOpts;

        mActivity      = activity;

        String errorBase =
            "DataLeakHandlers experienced unexpected exception [%s]";

        try
        {
            // Search for a registered SecureFS instance.
            mSecureFs = ArDkLib.getSecureFS();
            if (mSecureFs==null)
                throw new ClassNotFoundException();

            mSecurePath = mSecureFs.getSecurePath();
            mSecurePrefix = mSecureFs.getSecurePrefix();

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
            Log.i(mDebugTag, "SecureFS implementation unavailable");
        }

        mTempFolderPath = FileUtils.getTempPathRoot(mActivity)+
                          File.separator + "dataleak" + File.separator;

        if (! FileUtils.fileExists(mTempFolderPath))
        {
            if (! FileUtils.createDirectory(mTempFolderPath))
            {
                throw new IOException();
            }
        }

        mDeleteOnClose = new ArrayList<String>();
    }

    /**
     * This method finalizes the DataLeakHandlers object <br><br>.
     */
    public void finaliseDataLeakHandlers()
    {
        //  delete temp files
        if (mDeleteOnClose!=null) {
            for (int i = 0; i < mDeleteOnClose.size(); i++) {
                FileUtils.deleteFile(mDeleteOnClose.get(i));
        }
            mDeleteOnClose.clear();
    }
    }

    /**
     * This method will be called when the application is in the correct state
     * to insert a previously captured/selected image.
     */
    public void doInsert()
    {
    }

    /**
     * This method will be called when the application is backgrounded.<br><br>
     *
     * The implementor may save the document to a temporary location to allow
     * restoration on a future run.
     *
     * @param doc               The document object.
     * @param sModificationsdoc True if the document has unsaved modifications.
     * @param whenDone          run this when everything is done. Can be null.
     */
    public void pauseHandler(ArDkDoc doc, boolean hasModifications, Runnable whenDone)
    {
        if (whenDone!=null)
            whenDone.run();
    }

    /**
     * This method will be called when a launched activity exits.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify
     *                    who this result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int          requestCode,
                                 int          resultCode,
                                 final Intent data)
    {
    }

    /**
     * This method will be called when the application "Print" button is
     * pressed.<br><br>
     *
     * The implementor should save the document, most likely as Pdf, then use
     * the available API's to print the Pdf document.
     *
     * @param doc The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void printHandler(ArDkDoc doc)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        if (! mConfigOptions.isPrintingEnabled())
        {
           throw new UnsupportedOperationException();
        }

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
            new PrintHelperPdf().print(mActivity, doc);
        }
        else
        {
            displayDialogue(
                "Not Supported",
                "Printing is not supported for this version of Android.");
        }
    }

    /**
     * This method will be called when an external link has been activated.
     * <br><br>
     *
     * The implementor should launch an application to display the link target.
     *
     * @param url The Url to be launched.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void launchUrlHandler(String url)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        if (! mConfigOptions.isLaunchUrlEnabled())
        {
           throw new UnsupportedOperationException();
        }

        displayDialogue("Information",
                        "Please implement a custom Url launch handler");
    }

    /**
     * This method will be called when the application "Insert Image" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to select an image file then
     * insert that image into the document.
     *
     * The image file selection implementation may involve a separate activity.
     * The activity result and data can be picked up in onActivityResult().
     *
     * @param docView The document view object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void insertImageHandler(NUIDocView docView)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        throw new UnsupportedOperationException();
    }


    /**
     * This method will be called when the application "Insert Photo" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to take an image then
     * insert that image into the document.
     *
     * The image taking implementation may involve a separate activity.
     * The activity result and data can be picked up in onActivityResult().
     *
     * @param docView The document view object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void insertPhotoHandler(NUIDocView docView)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        throw new UnsupportedOperationException();
    }

    /**
     * This method will be called when the application custom save button is
     * pressed.<br><br>
     *
     * This is currently for internal use only and should not be implemented
     * by customers.
     *
     * The implementor should execute the save then inform application of the
     * new location or failure/cancellation of the operation via
     * completionCallback().
     *
     * @param filename           The name of the file being edited.
     * @param doc                The document object.
     * @param customDocData      Custom data passed in the document open
     *                           intent.
     * @param completionCallback The method to be called once the operation is
     *                           complete.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     * @throws IOException on any file IO related error
     */
    public void customSaveHandler(String                    filename,
                                  ArDkDoc                      doc,
                                 final String               customDocData,
                                 final SOCustomSaveComplete completionCallback)
        throws UnsupportedOperationException, IOException
    {
    }

    /**
     * This method will be called when the application "SaveAs" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to select the file save location,
     * execute the save then inform application of the new location or
     * failure/cancellation of the operation via completionCallback().
     *
     * @param filename           The name of the file being edited.
     * @param doc                The document object.
     * @param completionCallback The method to be called once the operation is
     *                           complete.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    @Override
    public void saveAsHandler(String                 filename,
                              ArDkDoc                  doc,
                              final SOSaveAsComplete completionCallback)
        throws UnsupportedOperationException

    {
        // If this feature is disabled throw an exception
        if (! mConfigOptions.isSaveAsEnabled())
        {
           throw new UnsupportedOperationException();
        }

        // Start a progress dialog.
        displayProgressDialogue("Saving Document", "", false);

        /*
         * Allow the user to select the save location and file name.
         *
         * Here we use the original file name and a  hard coded location.
         */
        final String newPath = mTempFolderPath + filename;

        // Save the document.
        doc.saveTo(newPath, new SODocSaveListener()
        {
            @Override
            public void onComplete(int result, int err)
            {
                // Dismiss the progress dialog.
                if (mProgressDialog != null)
                {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (result == SODocSave_Succeeded)
                {
                    displayDialogue("Information",
                                    "Document saved to '" + newPath     +
                                    "'.\n\n"                            +
                                    "Please implement a custom saveAs " +
                                    "handler");

                    // Inform the application of the new file name and location.
                    completionCallback.onComplete(
                        SOSaveAsComplete.SOSaveAsComplete_Succeeded,
                        newPath);
                }
                else
                {
                    displayDialogue("Information",
                                    String.format("saveAsHandler failed: %d %d", result, err));

                    /*
                     * Inform the application of the failure/cancellation of
                     * the operation.
                     */
                    completionCallback.onComplete(
                        SOSaveAsComplete.SOSaveAsComplete_Error,
                        null);
                }
            }
        });
    }

    /**
     * This method will be called after the file is saved internally.<br><br>
     *
     * The implementation can be minimal.
     */
    @Override
    public void postSaveHandler(SOSaveAsComplete completionCallback)
    {
        completionCallback.onComplete(0, null);
    }

    /**
     * This method will be called when the application "SavePdfAs" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to select the file save location,
     * then execute the export.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    @Override
    public void saveAsPdfHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        throw new UnsupportedOperationException();
    }

    /**
     * This method will be called when the application "OpenIn" button is
     * pressed.<br><br>
     *
     * The implementor should save the document to a suitable location
     * suitable for opening in a partner application..
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    @Override
    public void openInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException

    {
        // If this feature is disabled throw an exception
        if (! mConfigOptions.isOpenInEnabled())
        {
           throw new UnsupportedOperationException();
        }

        // Start a progress dialog.
        displayProgressDialogue("Saving Document", "", false);

        final String tempPath = mTempFolderPath + filename;

        // Save the document to a temporary location.
        doc.saveTo(tempPath, new SODocSaveListener()
        {
            @Override
            public void onComplete(int result, int err)
            {
                // Dismiss the progress dialog.
                if (mProgressDialog != null)
                {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (result == SODocSave_Succeeded)
                {
                    // As an example, add this to the list of files
                    // to be deleted on closing document.
                    addDeleteOnClose(tempPath);

                    displayDialogue("Information",
                                    "Document saved to '" + tempPath    +
                                    "'.\n\n"                            +
                                    "Please implement a custom openIn " +
                                    "handler");
                }
                else
                {
                    displayDialogue("Information",
                            String.format("openInHandler failed: %d %d", result, err));
                }

                if (mSecureFs != null)
                {
                    /*
                     * Convert the file path into a real path for sharing
                     * purposes.
                     */
                    String realPath =
                        tempPath.replace(mSecurePrefix, mSecurePath);
                }

                // Do something with the file then delete it.
            }
        });
    }

    /**
     * This method will be called when the application "OpenPdfIn" button is
     * pressed.<br><br>
     *
     * The implementor should export the document to a suitable location
     * suitable for opening in a partner application.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    @Override
    public void openPdfInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        throw new UnsupportedOperationException();
    }

    /**
     * This method will be called when the application "Share" button is
     * pressed.<br><br>
     *
     * The implementor should save the document to a suitable location
     * suitable for opening in a partner application.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    @Override
    public void shareHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException
    {
        // If this feature is disabled throw an exception
        if (! mConfigOptions.isShareEnabled())
        {
           throw new UnsupportedOperationException();
        }

        // Start a progress dialog.
        displayProgressDialogue("Saving Document", "", false);

        final String tempPath = mTempFolderPath + filename;

        // Save the document to a temporary location.
        doc.saveTo(tempPath, new SODocSaveListener()
        {
            @Override
            public void onComplete(int result, int err)
            {
                // Dismiss the progress dialog.
                if (mProgressDialog != null)
                {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (result == SODocSave_Succeeded)
                {
                    // As an example, add this to the list of files
                    // to be deleted on closing document.
                    addDeleteOnClose(tempPath);

                    displayDialogue("Information",
                                    "Document saved to '" + tempPath    +
                                    "'.\n\n"                                 +
                                    "Please implement a custom share " +
                                    "handler");
                }
                else
                {
                    displayDialogue("Information",
                            String.format("shareHandler failed: %d %d", result, err));
                }

                if (mSecureFs != null)
                {
                    // Convert the file path into a real path for sharing
                    // purposes.
                    String realPath =
                        tempPath.replace(mSecurePrefix, mSecurePath);
                }

                // Do something with the file then delete it.
            }
        });
    }
}
