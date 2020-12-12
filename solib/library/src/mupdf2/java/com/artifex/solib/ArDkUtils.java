package com.artifex.solib;

import android.app.Activity;
import android.content.Context;

public class ArDkUtils {

    public static ArDkBitmap createBitmapForPath(String path, int w, int h)
    {
        return new MuPDFBitmap(w, h);
    }

    public static ArDkLib getLibraryForPath(Activity activity, String path)
    {
        return MuPDFLib.getLib(activity);
    }

    public static String[] getLibVersionInfo(Context context)
    {
        return null;
    }

    public static boolean isTrackChangesEnabled(Activity activity)
    {
        return false;
    }

    public static boolean isAnimationEnabled(Activity activity)
    {
        return false;
    }

    public static final String[] DOC_TYPES = new String[]{};
    public static final String[] DOCX_TYPES = new String[]{};
    public static final String[] XLS_TYPES = new String[]{};
    public static final String[] XLSX_TYPES = new String[]{};
    public static final String[] PPT_TYPES = new String[]{};
    public static final String[] PPTX_TYPES = new String[]{};
    public static final String[] SO_IMG_TYPES = new String[]{};
    public static final String[] SO_OTHER_TYPES = new String[]{};

    public static final String[] MUPDF_TYPES = new String[]
            {"pdf", "epub", "svg", "xps", "fb2", "cbz", "xhtml"};
    public static final String[] IMG_TYPES = new String[]
            {"bmp", "jpg", "jpeg", "gif", "png"};

}
