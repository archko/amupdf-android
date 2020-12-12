package com.artifex.sonui.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.webkit.MimeTypeMap;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkUtils;
import com.artifex.solib.FileUtils;
import com.artifex.solib.ArDkDoc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.RuntimeException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.artifex.solib.SODoc;
import com.artifex.solib.SOInputStream;
import com.artifex.solib.SOOutputStream;

public class Utilities
{
    private static final String mDebugTag = "Utilities";
    private static String       mFileStateForPrint = null;

    public static void showMessage(final Activity activity, final String title, final String body)
    {
        showMessage(activity, title, body, activity.getResources().getString(R.string.sodk_editor_ok));
    }

    //  This keeps track of the last-displayed alert dialog
    //  shown by one of the methods in this class.
    //  An activity that's showing one of these can dismiss it by
    //  calling dismissCurrentAlert()
    //
    //  this is designed to address http://bugs.ghostscript.com/show_bug.cgi?id=698650
    //  where NUIActivity.finish() is called while the alert is showing.
    //  NUIActivity.finish() now calls dismissCurrentAlert().
    //
    //  While we're at it, we'll make sure there's only one alert at a time showing,
    //  by removing one that may be showing before displaying another.

    private static AlertDialog currentMessageDialog = null;

    //  for no-UI
    public interface MessageHandler {
        void showMessage(String title, String body, String okLabel, Runnable whenDone);

        void yesNoMessage(String title, String body,
                                        String yesButtonLabel, String noButtonLabel,
                                        Runnable yesRunnable, Runnable noRunnable);
    }
    private static MessageHandler mMessageHandler = null;
    public static void setMessageHandler(MessageHandler handler){mMessageHandler=handler;}

    public static void showMessage(final Activity activity, final String title, final String body, final String okLabel)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, okLabel, null);
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog =
                new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(okLabel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });
    }

    public static void dismissCurrentAlert()
    {
        if (currentMessageDialog != null)
        {
            try {
                currentMessageDialog.dismiss();
            }
            catch (Exception e) {
            }

            currentMessageDialog = null;
        }
    }

    public static void showMessageAndFinish(final Activity activity, final String title, final String body)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, activity.getResources().getString(R.string.sodk_editor_ok), new Runnable() {
                        @Override
                        public void run() {
                            activity.finish();
                        }
                    });
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog = new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(R.string.sodk_editor_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                                activity.finish();
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });
    }

    public static void showMessageAndWait(final Activity activity, final String title, final String body, final int styleId, final Runnable runnable)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, activity.getResources().getString(R.string.sodk_editor_ok), runnable);
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog = new AlertDialog.Builder(activity, styleId)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(R.string.sodk_editor_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                                runnable.run();
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });
    }

    public static void showMessageAndWait(final Activity activity, final String title, final String body, final Runnable runnable)
    {
        showMessageAndWait(activity, title, body, R.style.sodk_editor_alert_dialog_style, runnable);
    }

    public static void yesNoMessage(final Activity activity, final String title, final String body,
                                    final String yesButtonLabel, final String noButtonLabel,
                                    final Runnable yesRunnable, final Runnable noRunnable)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.yesNoMessage(title, body, yesButtonLabel, noButtonLabel, yesRunnable, noRunnable);
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                dismissCurrentAlert();

                AlertDialog.Builder dialog = new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style);

                dialog.setTitle(title);
                dialog.setMessage(body);

                dialog.setCancelable(false);

                dialog.setPositiveButton(yesButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (yesRunnable!=null)
                            yesRunnable.run();
                    }
                });

                dialog.setNegativeButton(noButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (noRunnable!=null)
                            noRunnable.run();
                    }
                });

                currentMessageDialog = dialog.create();
                currentMessageDialog.show();
            }
        });
    }


    public static int colorForDocType(Context context, String docName)
    {
        String ext = FileUtils.getExtension(docName);
        return colorForDocExt(context, ext);
    }

    public static int colorForDocExt(Context context, String ext)
    {
        if (FileUtils.matchFileExtension(ext, ArDkUtils.DOC_TYPES) ||
                FileUtils.matchFileExtension(ext, ArDkUtils.DOCX_TYPES))
            return ContextCompat.getColor(context, R.color.sodk_editor_header_doc_color);

        if (FileUtils.matchFileExtension(ext, ArDkUtils.XLS_TYPES) ||
                FileUtils.matchFileExtension(ext, ArDkUtils.XLSX_TYPES))
            return ContextCompat.getColor(context, R.color.sodk_editor_header_xls_color);

        if (FileUtils.matchFileExtension(ext, ArDkUtils.PPT_TYPES) ||
                FileUtils.matchFileExtension(ext, ArDkUtils.PPTX_TYPES))
            return ContextCompat.getColor(context, R.color.sodk_editor_header_ppt_color);

        if (FileUtils.matchFileExtension(ext, ArDkUtils.IMG_TYPES) ||
                FileUtils.matchFileExtension(ext, ArDkUtils.SO_IMG_TYPES))
            return ContextCompat.getColor(context, R.color.sodk_editor_header_image_color);

        switch (ext)
        {
            case "pdf":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_pdf_color);
            case "txt":
            case "csv":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_txt_color);
            case "hwp":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_hwp_color);
            case "svg":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_svg_color);
            case "cbz":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_cbz_color);
            case "epub":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_epub_color);
            case "xps":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_xps_color);
            case "xhtml":
            case "fb2":
                return ContextCompat.getColor(
                    context, R.color.sodk_editor_header_fb2_color);
        }

        return ContextCompat.getColor(context,
                                      R.color.sodk_editor_header_unknown_color);
    }

    public static int iconForDocType(String docName)
    {
        String ext = FileUtils.getExtension(docName);
        return iconForDocExt(ext);
    }

    public static int iconForDocExt(String ext)
    {
        if (FileUtils.matchFileExtension(ext, ArDkUtils.DOC_TYPES))
            return R.drawable.sodk_editor_icon_doc;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.DOCX_TYPES))
            return R.drawable.sodk_editor_icon_docx;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.XLS_TYPES))
            return R.drawable.sodk_editor_icon_xls;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.XLSX_TYPES))
            return R.drawable.sodk_editor_icon_xlsx;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.PPT_TYPES))
            return R.drawable.sodk_editor_icon_ppt;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.PPTX_TYPES))
            return R.drawable.sodk_editor_icon_pptx;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.IMG_TYPES))
            return R.drawable.sodk_editor_icon_image;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.SO_IMG_TYPES))
            return R.drawable.sodk_editor_icon_image;

        switch (ext)
        {
            case "pdf":
                return R.drawable.sodk_editor_icon_pdf;
            case "txt":
            case "csv":
                return R.drawable.sodk_editor_icon_txt;
            case "hwp":
                return R.drawable.sodk_editor_icon_hangul;
            case "svg":
                return R.drawable.sodk_editor_icon_svg;
            case "cbz":
                return R.drawable.sodk_editor_icon_cbz;
            case "epub":
                return R.drawable.sodk_editor_icon_epub;
            case "xps":
                return R.drawable.sodk_editor_icon_xps;
            case "fb2":
            case "xhtml":
                return R.drawable.sodk_editor_icon_fb2;
        }
        return R.drawable.sodk_editor_icon_any;
    }

    public static String removeExtension(String filePath)
    {
        File f = new File(filePath);

        // if it's a directory, don't remove the extension
        if (f.isDirectory())
            return filePath;

        String name = f.getName();

        // Now we know it's a file - don't need to do any special hidden
        // checking or contains() checking because of:
        final int lastPeriodPos = name.lastIndexOf('.');
        if (lastPeriodPos <= 0)
        {
            // No period after first character - return name as it was passed in
            return filePath;
        }
        else
        {
            // Remove the last period and everything after it
            File renamed = new File(f.getParent(), name.substring(0, lastPeriodPos));
            return renamed.getPath();
        }
    }

    private static String getMimeTypeFromExtension (String path)
    {
        String mime = null;
        String ext = FileUtils.getExtension(path);

        if (ext.compareToIgnoreCase("xps")==0)
            mime = "application/vnd.ms-xpsdocument";
        if (ext.compareToIgnoreCase("cbz")==0)
            mime = "application/x-cbz";
        if (ext.compareToIgnoreCase("svg")==0)
            mime = "image/svg+xml";

        return mime;
    }

    public static String getMimeType (String path)
    {
        String ext = FileUtils.getExtension(path);
        String mime = null;

        if (ext != null) {
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }

        if (mime==null) {
            mime = getMimeTypeFromExtension(path);
        }

        return mime;
    }

    public static void passwordDialog( final Activity activity, final passwordDialogListener listener)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                dismissCurrentAlert();
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style);
                LayoutInflater li = LayoutInflater.from(activity);
                View promptsView = li.inflate(R.layout.sodk_editor_password_prompt, null);

                dialog.setCancelable(false);  //  user must tap the cancel button

                final SOEditText et = (SOEditText)(promptsView.findViewById(R.id.editTextDialogUserInput));

                dialog.setView(promptsView);

                dialog.setTitle("");

                dialog.setPositiveButton(activity.getResources().getString(R.string.sodk_editor_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Utilities.hideKeyboard(activity, et);
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (listener!=null)
                        {
                            String password = et.getText().toString();
                            listener.onOK(password);
                        }
                    }
                });

                dialog.setNegativeButton(activity.getResources().getString(R.string.sodk_editor_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utilities.hideKeyboard(activity, et);
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (listener!=null)
                            listener.onCancel();
                    }
                });

                currentMessageDialog = dialog.create();
                currentMessageDialog.show();
            }
        });
    }

    public interface passwordDialogListener
    {
        void onOK(String password);
        void onCancel();
    }

    public interface SigningFactoryListener
    {
        NUIPKCS7Signer getSigner( Activity context );
        NUIPKCS7Verifier getVerifier( Activity context );
    }

    public static SigningFactoryListener mSigningFactory = null;

    public static void setSigningFactoryListener (SigningFactoryListener factory)
    {
        mSigningFactory = factory;
    }

    public static NUIPKCS7Signer getSigner( Activity context )
    {
        if (mSigningFactory != null)
            return mSigningFactory.getSigner( context );
        else
            return null;
    }

    public static NUIPKCS7Verifier getVerifier( Activity context )
    {
        if (mSigningFactory != null)
            return mSigningFactory.getVerifier( context );
        else
            return null;
    }


    public static final String generalStore = "general";

    public static Object getPreferencesObject(Context context,
                                              String  storeName)
    {
        /*
         * All apps are required to provide an SOPersistentStorage
         * implementation.
         */
        if (mPersistentStorage == null)
        {
            Log.d(mDebugTag, "No implementation of the SOPersistentStorage " +
                             "interface found");

            throw new RuntimeException();
        }

        return mPersistentStorage.getStorageObject(context, storeName);
    }

    public static void setStringPreference(Object storageObject,
                                           String key,
                                           String value)
    {
        /*
         * All apps are required to provide an SOPersistentStorage
         * implementation.
         */
        if (mPersistentStorage == null)
        {
            Log.d(mDebugTag, "No implementation of the SOPersistentStorage " +
                             "interface found");

            throw new RuntimeException();
        }

        mPersistentStorage.setStringPreference(storageObject, key, value);
        return;
    }

    public static String getStringPreference(Object storageObject,
                                             String key,
                                             String defaultValue)
    {
        /*
         * All apps are required to provide an SOPersistentStorage
         * implementation.
         */
        if (mPersistentStorage == null)
        {
            Log.d(mDebugTag, "No implementation of the SOPersistentStorage " +
                             "interface found");

            throw new RuntimeException();
        }

        return mPersistentStorage.getStringPreference(storageObject,
                                                      key,
                                                      defaultValue);
    }

    public static Map<String,?> getAllStringPreferences(Object storageObject)
    {
        /*
         * All apps are required to provide an SOPersistentStorage
         * implementation.
         */
        if (mPersistentStorage == null)
        {
            Log.d(mDebugTag, "No implementation of the SOPersistentStorage " +
                             "interface found");

            throw new RuntimeException();
        }

        return mPersistentStorage.getAllStringPreferences(storageObject);
    }

    public static void removePreference(Object storageObject, String key)
    {
        /*
         * All apps are required to provide an SOPersistentStorage
         * implementation.
         */
        if (mPersistentStorage == null)
        {
            Log.d(mDebugTag, "No implementation of the SOPersistentStorage " +
                             "interface found");

            throw new RuntimeException();
        }

        mPersistentStorage.removePreference(storageObject, key);
        return;
    }

    public static boolean isPhoneDevice(Context context)
    {
        if (Utilities.isChromebook(context))
            return false;
        
        Configuration configuration = context.getResources().getConfiguration();

        // The current width of the available screen space, in dp units,
        // corresponding to screen width resource qualifier.
        int screenWidthDp = configuration.screenWidthDp;

        // The smallest screen size an application will see in normal operation, corresponding to
        // smallest screen width resource qualifier.
        int smallestScreenWidthDp = configuration.smallestScreenWidthDp;

        int minimum_tablet_width = context.getResources().getInteger(R.integer.sodk_editor_minimum_tablet_width);
        return (smallestScreenWidthDp < minimum_tablet_width);
    }

    public static boolean isLandscapePhone(Context context)
    {
        if (isPhoneDevice(context))
        {
            Point p = getRealScreenSize(context);
            if (p.x>p.y)
            {
                return true;
            }
        }
        return false;
    }

    public static int convertDpToPixel(float dp)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int)Math.round(px);
    }

    //  this function returns the size of the screen as the app sees it.
    //  In split screen mode, this is *smaller* than the real screen size.
    public static Point getScreenSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        return new Point(metrics.widthPixels, metrics.heightPixels);
    }

    //  this function returns the *actual* size of the screen.
    public static Point getRealScreenSize(Activity activity)
    {
        WindowManager wm  = activity.getWindowManager();
        return getRealScreenSize(wm);
    }

    //  this function returns the *actual* size of the screen.
    public static Point getRealScreenSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return getRealScreenSize(wm);
    }

    //  this function returns the *actual* size of the screen.
    private static Point getRealScreenSize(WindowManager wm)
    {
        Display display = wm.getDefaultDisplay();

        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17)
        {
            //  new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        }
        else if (Build.VERSION.SDK_INT >= 14)
        {
            //  reflection for this weird in-between time
            try
            {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            }
            catch (Exception e)
            {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("sonui", "Couldn't use reflection to get the real display metrics.");
            }
        }
        else
        {
            //this may not be 100% accurate, but it's all we've got
            realWidth = display.getWidth();
            realHeight = display.getHeight();
            Log.e("sonui", "Can't get real display matrix.");
        }

        return new Point(realWidth, realHeight);
    }

    public static void hideKeyboard(Context context)
    {
        if (Activity.class.isInstance(context))
        {
            Activity activity = (Activity)context;
            //  fix https://bugs.ghostscript.com/show_bug.cgi?id=702763
            //  get the window token from the activity content
            IBinder windowToken = activity.findViewById(android.R.id.content).getRootView().getWindowToken();
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    public static void hideKeyboard(Context context, View view)
    {
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Context context)
    {
        //  show keyboard
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static boolean isDocTypeSupported(String filename)
    {
        String ext = FileUtils.getExtension(filename);

        if (FileUtils.matchFileExtension(ext, ArDkUtils.DOC_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.DOCX_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.XLS_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.XLSX_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.PPT_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.PPTX_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.MUPDF_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.IMG_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.SO_IMG_TYPES))
            return true;
        if (FileUtils.matchFileExtension(ext, ArDkUtils.SO_OTHER_TYPES))
            return true;

        return false;
    }

    public static void setFileStateForPrint(String filename)
    {
        mFileStateForPrint = filename;
    }

    public static String getFileStateForPrint()
    {
        return mFileStateForPrint;
    }

    public static File getRootDirectory(Context context)
    {
        return Environment.getExternalStorageDirectory();
    }

    public static File getDownloadDirectory(Context context)
    {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    private static String getHtmlColorStringFromResource(@ColorRes int resourceId, Context context)
    {
        int htmlHexCodeLen = 6; /* HTML color hex codes do not include the alpha channel */
        int colorInt = context.getResources().getColor(resourceId);
        int colorStringLen;

        /* The hex string function doesn't pad the integer, but most resources have an alpha
        channel above 00 to be visible, so should be 7 or 8 digits */
        String colorString = Integer.toHexString(colorInt);
        colorStringLen = colorString.length();

        if (colorStringLen > htmlHexCodeLen)
        {
            colorString = colorString.substring(colorStringLen - htmlHexCodeLen);
            colorString = "#" + colorString;
        }
        else
        {
            colorString = "rgba(0, 0, 0, 0);"; /* Transparent */
        }

        return colorString;
    }

    @SuppressWarnings("deprecation")
    public static void setFilenameText(SOTextView tv, String filename)
    {
        String name;
        Context context =  tv.getContext();
        int lastDot = filename.lastIndexOf(".");
        String textColor = Utilities.getHtmlColorStringFromResource(R.color.sodk_editor_filename_textcolor, context);
        String extColor = Utilities.getHtmlColorStringFromResource(R.color.sodk_editor_extension_textcolor, context);
        if (lastDot>=0)
        {
            name = "<font color='" + textColor + "'>" + filename.substring(0,lastDot)+ "</font>"
                    + "<font color='"+ extColor + "'>" + filename.substring(lastDot) + "</font>";
        }
        else
        {
            name = "<font color='" + textColor + "'>" + filename + "</font>";
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            tv.setText(Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
        else
            tv.setText(Html.fromHtml(name), TextView.BufferType.SPANNABLE);
    }

    public static String getSelectionFontName(ArDkDoc doc)
    {
        String fontname = ((SODoc)doc).getSelectionFontName();
        if (fontname != null) {
            if (fontname.startsWith("-") || fontname.startsWith("+"))
                fontname = "";
        }
        return fontname;
    }

    public static String getApplicationName(Context context)
    {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId                    = applicationInfo.labelRes;

        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() :
                               context.getString(stringId);
    }

    private static SODataLeakHandlers mDataLeakHandlers = null;

    public static void setDataLeakHandlers (SODataLeakHandlers handlers)
    {
        mDataLeakHandlers = handlers;
    }

    public static SODataLeakHandlers getDataLeakHandlers()
    {
        return mDataLeakHandlers;
    }

    private static SOPersistentStorage mPersistentStorage = null;

    public static void setPersistentStorage(SOPersistentStorage storage)
    {
        mPersistentStorage = storage;
    }

    public static SOPersistentStorage getPersistentStorage()
    {
        return mPersistentStorage;
    }

    private static SODocSession.SODocSessionLoadListenerCustom
                                                mSessionLoadListener = null;

    public static void setSessionLoadListener(
                           SODocSession.SODocSessionLoadListenerCustom listener)
    {
        mSessionLoadListener = listener;
    }

    public static SODocSession.SODocSessionLoadListenerCustom
                                                        getSessionLoadListener()
    {
        return mSessionLoadListener;
    }

    private static final Set<String> RTL;

    static
    {
        Set<String> lang = new HashSet<String>();
        lang.add("ar"); // Arabic
        lang.add("dv"); // Divehi
        lang.add("fa"); // Persian (Farsi)
        lang.add("ha"); // Hausa
        lang.add("he"); // Hebrew
        lang.add("iw"); // Hebrew (old code)
        lang.add("ji"); // Yiddish (old code)
        lang.add("ps"); // Pashto, Pushto
        lang.add("ur"); // Urdu
        lang.add("yi"); // Yiddish
        RTL = Collections.unmodifiableSet(lang);
    }

    public static boolean isRTL(Context context)
    {
        InputMethodManager inputMethodManager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        InputMethodSubtype inputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();

        // Handle the case whereby the IMM does not have a subtype.
        if (inputMethodSubtype == null)
        {
            return false;
        }

        String tag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)  //  API 24
            tag = inputMethodSubtype.getLanguageTag();
        else
            tag = inputMethodSubtype.getLocale();

        boolean result = RTL.contains(tag);
        return result;
    }

    public static String getSDCardPath(Context context)
    {
        //  if there is an SD card available, it should be the second one
        //  in the list returned by Context.getExternalFilesDirs().

        File[] externals = context.getExternalFilesDirs(null);
        Log.d("sdcard", String.format("getSDCardPath: there are %d external locations", externals.length));

        //  must have at least two
        if (externals != null && externals.length>1 && externals[1]!=null)
        {
            //  path to the second one
            String path = externals[1].getAbsolutePath();
            if (path != null)
            {
                Log.d("sdcard", String.format("getSDCardPath: possible sd card path is %s", path));

                //  the path will look something like
                //  /storage/something/Android/data/com.picsel.tgv.app.smartoffice/files
                //  strip away everything starting with "/android/"
                int i = path.toLowerCase().indexOf("/android/");
                if (i>0)
                {
                    //  got it.
                    path = path.substring(0, i);
                    Log.d("sdcard", String.format("getSDCardPath: SD card is at %s", path));
                    return path;
                }
                else
                {
                    Log.d("sdcard", String.format("getSDCardPath: did not find /Android/ in %s", path));
                }
            }
            else
            {
                Log.d("sdcard", String.format("getSDCardPath: 2nd path is null"));
            }
        }
        else
        {
            Log.d("sdcard", String.format("getSDCardPath: too few external locations"));
        }

        return null;
    }

    public static int[] screenToWindow(int[] screenLoc, Context context)
    {
        //  this function converts a screen location to one relative
        //  to the window for the given context.
        //  if we're in split screen mode, these can be different.

        //  where is our window on screen?
        int decorLoc[] = new int[2];
        ((Activity)context).getWindow().getDecorView().getLocationOnScreen(decorLoc);

        //  convert to window-relative
        int newLoc[] = new int[2];
        newLoc[0] = screenLoc[0] - decorLoc[0];
        newLoc[1] = screenLoc[1] - decorLoc[1];

        return newLoc;
    }

    protected static String preInsertImage(Context context, String path)
    {
        //  this path is returned. We start assuming no change is made.
        String modifiedPath = path;

        //  input stream for reading files, which works with secureFS.
        SOInputStream inStream;

        //  get image dimensions
        int width = 0;
        int height = 0;
        inStream = new SOInputStream(path);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inStream, null, options);
            height = options.outHeight;
            width = options.outWidth;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (inStream != null)
                inStream.close();
        }

        //  get image angle
        inStream = new SOInputStream(path);
        int angle = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(inStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
            }
        } catch (IOException e) {
        }
        finally {
            if (inStream != null)
                inStream.close();
        }

        //  maximum image dimension fot scaling down. Somewhat arbitrary
        int maxDim = 1536;

        //  Check if rotation or scaling is necessary
        if (angle!=0 || width>maxDim || height>maxDim)
        {
            //  we're going to scale and/or rotate, so get the full bitmap now.
            Bitmap src = null;
            inStream = new SOInputStream(path);
            try {
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inPreferredConfig = Bitmap.Config.RGB_565;
                src = BitmapFactory.decodeStream(inStream);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (inStream != null)
                    inStream.close();
            }

            if (src != null)
            {
                // scale
                if (width>maxDim || height>maxDim)
                {
                    float ratiow  = (float)maxDim/(float)(width);
                    float ratioh  = (float)maxDim/(float)(height);
                    float ratio = Math.min(ratiow, ratioh);
                    int newW = (int)(ratio*(float)width);
                    int newH = (int)(ratio*(float)height);
                    Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, false);
                    src = scaled;
                    width = newW;
                    height = newH;
                }

                //  rotate
                if (angle != 0)
                {
                    Matrix matrix = new Matrix();
                    matrix.preRotate(angle);
                    Bitmap rotated = Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
                    src = rotated;
                }

                //  write it out
                String newPath = FileUtils.getTempPathRoot(context) + File.separator + "scaled_or_rotated_" + UUID.randomUUID() + ".png";
                SOOutputStream outStream = new SOOutputStream(newPath);
                if (outStream != null)
                {
                    src.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    //  return the rotated/scaled path
                    modifiedPath = newPath;
                }
            }
        }

        return modifiedPath;
    }

    public static boolean isValidFilename(String name)
    {
        //  test to see if a filename is valid.
        //  currently it rejects names that start with "."

        if (name==null)
            return false;
        if (name.isEmpty())
            return false;
        if (name.startsWith("."))
            return false;

        //  also let's reject names with leading or trailing space
        //  fixes #700536
        if (name.startsWith(" "))
            return false;
        if (name.endsWith(" "))
            return false;

        //  fix 702881
        if (name.contains(File.separator))
            return false;

        return true;
    }

    public static String getOpenErrorDescription(Context context, int errcode)
    {
        String desc;
        switch(errcode)
        {
            case ArDkLib.SmartOfficeDocErrorType_UnsupportedEncryption:
                desc = context.getString(R.string.sodk_editor_doc_uses_unsupported_enc);
                break;
            default:
                desc = String.format(context.getString(R.string.sodk_editor_doc_open_error), errcode);
                break;
        }
        return desc;
    }

    public static ProgressDialog createAndShowWaitSpinner(Context context) {
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

    public static ProgressDialog displayPleaseWaitWithCancel(Context context, final Runnable onCancel)
    {
        //  this progress dialog appears after a 1-second delay.
        final ProgressDialogDelayed dialog = new ProgressDialogDelayed(context, 1000);

        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setTitle(context.getString(R.string.sodk_editor_please_wait));
        dialog.setMessage(null);

        if (onCancel!=null) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.sodk_editor_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    onCancel.run();
                }
            });
        }

        dialog.show();

        return dialog;
    }

    public static boolean isChromebook(Context context)
    {
        //  detect if we're running on a Chromebook.
        //  this comes from
        //      https://stackoverflow.com/questions/39784415/how-to-detect-programmatically-if-android-app-is-running-in-chrome-book-or-in

        return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
    }

    public static String formatFloat(float value)
    {
        //  This function formats a floating point number using the
        //  smallest number of decimal places necessary. If that's 0,
        //  then the decimal point is dropped as well.
        //
        //  String.format is used so the result is in the default locale.

        if (value % 1 == 0) {
            //  it's a whole number
            return String.format("%.0f", value);
        }

        //  count the decimal places and format with the
        //  appropriate format specifier.
        String text = Double.toString(Math.abs((double)value));  //  creates, for example, "1.23"
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt, value);
    }

    //
    //  this function reformats a date according to the current locale.
    //
    public static String formatDateForLocale(Context context, String inDateStr, String inFormat)
    {
        try
        {
            //  parse the incoming string using the given format
            SimpleDateFormat df = new SimpleDateFormat(inFormat);
            Date date = df.parse(inDateStr);

            //  reformat the date with the system's date format, which will be locale-specific
            java.text.DateFormat dateFormat;
            String format = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
            if (TextUtils.isEmpty(format))
                dateFormat = DateFormat.getDateFormat(context);
            else
                dateFormat = new SimpleDateFormat(format);
            String result = dateFormat.format(date);

            //  add the time, also locale-specific
            dateFormat = android.text.format.DateFormat.getTimeFormat(context);
            result += " " + dateFormat.format(date);

            //  return the reformatted date
            return result;
        }
        catch (Exception e)
        {
        }

        //  if we got here, there was some error, so just return the original string
        return inDateStr;
    }

    public static boolean isEmulator()
    {
        //  see:  https://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator

        if ("goldfish".equals(Build.HARDWARE))
            return true;
        if ("ranchu".equals(Build.HARDWARE))
            return true;

        if (Build.FINGERPRINT.contains("generic"))
            return true;

        return false;
    }

    // ISO8601 date time string to Date
    public static Date iso8601ToDate(String iso8601)
    {
        String str = iso8601.replace("[Zz]", "+00:00");
        try
        {
            str = str.substring(0, 22) + str.substring(23); // remove ':'
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(str);
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (ParseException  e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //  this function retutns the width of the widest child View
    //  in the given ListView
    public static int getListViewWidth(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return 0;
        }

        int maxWidth = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            maxWidth = Math.max(maxWidth, listItem.getMeasuredWidth());
        }

        return maxWidth;
    }

    public static boolean isPermissionRequested(Context context, String permission)
    {
        //  this function check the list of requested permissions
        //  (those that appear in uses-permission items in the manifest)
        //  for the given permission and returns true if found.

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String p : info.requestedPermissions) {
                    if (p.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //  compare two rects for equality
    //  returns true if they are the same
    public static boolean compareRects(Rect r1, Rect r2)
    {
        if (r1==null) {
            if (r2==null) {
                //  both are null
                return true;
            }
            else {
                //  one is null, the other not
                return false;
            }
        }
        else {
            if (r2==null) {
                //  one is null, the other not
                return false;
            }
            else {
                //  both are not null, so compare
                return r1.equals(r2);
            }
        }
    }

}
