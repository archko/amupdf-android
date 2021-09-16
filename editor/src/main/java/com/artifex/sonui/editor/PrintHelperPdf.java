package com.artifex.sonui.editor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.WindowManager;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.FileUtils;
import com.artifex.solib.SODocSaveListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/*

This is a very simple implementation of printing,
wherein we use library to export the document to a PDF file,
and hand that result to the printing API.

The API handles rendering the pages for preview, and delivering
that to the selected printer.

 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class PrintHelperPdf
{
    private static boolean printing = false;

    private static ProgressDialog createAndShowWaitSpinner(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);

        try {
            dialog.show();
        }
        catch (WindowManager.BadTokenException e) {
        }

        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.sodk_editor_wait_spinner);

        return dialog;
    }

    public static void setPrinting(boolean value)
    {
        //  a fix for 700683 (PRINT menu does not respond)
        //  in Chromebook, there's no true backgrounding.
        //  so when activities pause, we call this to free up
        //  this class so it can be used again.
        //
        //  may re-break 697802, but on Chromebook only
        printing = value;
    }

    public void printPDF(final Context context, final String printPath, final Runnable onDone)
    {
        //  one at a time, please.
        if (printing) {
            onDone.run();
            return;
        }
        printing = true;

        printPath(context, printPath, false, new Runnable() {
            @Override
            public void run() {
                printing = false;
            }
        });
    }

    public void print(final Context context, ArDkDoc doc, final Runnable onDone)
    {
        //  one at a time, please.
        if (printing) {
            if (onDone!=null)
                onDone.run();
            return;
        }
        printing = true;

        //  make a temp path
        final String printPath = FileUtils.getTempPathRoot(context) +
                                 "/print/" + UUID.randomUUID() + ".pdf";
        FileUtils.createDirectory(printPath);
        FileUtils.deleteFile(printPath);

        //  save as PDF
        final ProgressDialog spinner = createAndShowWaitSpinner(context);
        doc.saveToPDF(printPath, false, new SODocSaveListener()
        {
            @Override
            public void onComplete(final int result, final int err)
            {
                spinner.dismiss();

                if (result == SODocSave_Succeeded)
                {
                    printPath(context, printPath, true, new Runnable() {
                        @Override
                        public void run() {
                            printing = false;
                            if (onDone!=null)
                                onDone.run();
                        }
                    });
                }
                else
                {
                    String message = String.format(context.getString(com.artifex.sonui.editor.R.string.sodk_editor_error_saving_document_code), err);
                    Utilities.showMessage((Activity)context, context.getString(com.artifex.sonui.editor.R.string.sodk_editor_error), message);
                    printing = false;
                    if (onDone!=null)
                        onDone.run();
                }
            }
        });
    }

    private void printPath(final Context context, final String printPath, final boolean deleteWhenDone, final Runnable onDone)
    {
        PrintDocumentAdapter pda = new PrintDocumentAdapter()
        {
            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback)
            {
                Object       input  = null;
                OutputStream output = null;

                try
                {
                    input = FileUtils.getFileHandleForReading(printPath);
                    output = new FileOutputStream(destination.getFileDescriptor());

                    byte[] buf = new byte[1024];
                    int bytesRead;

                    while ((bytesRead =
                            FileUtils.readFromFile(input, buf)) > 0)
                    {
                        output.write(buf, 0, bytesRead);
                    }

                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                } catch (FileNotFoundException ee)
                {
                    //Catch exception
                }
                catch (Exception e)
                {
                    //Catch exception
                }
                finally
                {
                    try
                    {
                        //  in bug 698315, input was null
                        if (input!=null)
                            FileUtils.closeFile(input);
                        if (output!=null)
                            output.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras)
            {
                if (cancellationSignal.isCanceled())
                {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo pdi = new PrintDocumentInfo.Builder("Name of file").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build();
                callback.onLayoutFinished(pdi, true);
            }

            @Override
            public void onFinish()
            {
                // Remove the temporary file when the printing system is finished.
                if (deleteWhenDone)
                    FileUtils.deleteFile(printPath);

                printing = false;
                if (onDone!=null)
                    onDone.run();
            }
        };

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        String jobName = Utilities.getApplicationName(context) + " Document";
        printManager.print(jobName, pda, null);
    }
}
