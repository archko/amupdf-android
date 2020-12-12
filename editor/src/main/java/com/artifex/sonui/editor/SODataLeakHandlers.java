package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Intent;

import java.io.IOException;
import java.lang.UnsupportedOperationException;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ConfigOptions;

/**
 * This interface specifies the basis for implementing a class allowing
 * proprietary handling of application buttons actioning data save/export
 * from the application.
 */

public interface SODataLeakHandlers
{
    /**
     * This method initialises the DataLeakHandlers object <br><br>.
     *
     * @param activity     The current activity.
     * @param cfgOpts      The Config Options to reference.
     *
     * @throws IOException if the temporary folder cannot be created.
     */
    public void initDataLeakHandlers(Activity activity, ConfigOptions cfgOpts)
        throws IOException;

    /**
     * This method finalizes the DataLeakHandlers object <br><br>.
     */
    public void finaliseDataLeakHandlers();

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
                                 final Intent data);

    /**
     * This method will be called when the application "Insert Image" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to select an image for insertion
     * into the document.
     *
     * The image file selection implementation should involve separate activity.
     * The activity result and data can be picked up in onActivityResult().
     *
     * @param docView The document view object.
     *
     * {@link #doInsert()} will be called to action the insert of the
     * selected file into the document.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void insertImageHandler(NUIDocView docView)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "Insert Photo" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to capture an image for insertion
     * into the document.
     *
     * The image capture implementation should involve a separate activity.
     * The activity result and data can be picked up in onActivityResult().
     *
     * @param docView The document view object.
     *
     * {@link #doInsert()} will be called to action the insert of the
     * selected file into the document.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void insertPhotoHandler(NUIDocView docView)
        throws UnsupportedOperationException;

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
    public void pauseHandler(ArDkDoc doc, boolean hasModifications, Runnable whenDone);

    /**
     * This method will be called when the application is in the correct state
     * to insert a previously captured/selected image.
     */
    public void doInsert();

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
        throws UnsupportedOperationException;

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
        throws UnsupportedOperationException;

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
    public void customSaveHandler(String               filename,
                                  ArDkDoc doc,
                                  String               customDocData,
                                  SOCustomSaveComplete completionCallback)
        throws UnsupportedOperationException, IOException;

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
    public void saveAsHandler(String           filename,
                              ArDkDoc doc,
                              SOSaveAsComplete completionCallback)
        throws UnsupportedOperationException;


    /**
     * This method will be called after the file is saved internally.<br><br>
     *
     * The implementation can be empty.
     */
    public void postSaveHandler(SOSaveAsComplete completionCallback);


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
    public void saveAsPdfHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

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
    public void openInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

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
    public void openPdfInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "Share" button is
     * pressed.<br><br>
     *
     * The implementor should save the document to a suitable location
     * suitable for sharing in a partner application.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void shareHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;
}
